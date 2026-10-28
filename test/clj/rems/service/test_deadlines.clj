(ns rems.service.test-deadlines
  (:require [clj-time.core :as time]
            [clojure.test :refer :all]
            [rems.config]
            [rems.service.deadlines :as deadlines]))

(def ^:private zone (time/time-zone-for-id "Europe/Budapest"))

(defn- at [y m d]
  (time/date-time y m d 10))

(defn- reviewed-application [requested]
  {:application/state :application.state/submitted
   :application/user-roles {"reviewer" #{:reviewer}
                            "done" #{:past-reviewer}}
   :application/events [{:event/type :application.event/submitted
                         :event/time (at 2026 1 1)}
                        {:event/type :application.event/review-requested
                         :event/time requested
                         :application/reviewers [{:userid "reviewer"} {:userid "done"}]}]})

(defn- returned-application [returned]
  {:application/state :application.state/returned
   :application/events [{:event/type :application.event/submitted
                         :event/time (at 2026 1 1)}
                        {:event/type :application.event/returned
                         :event/time returned}]})

(defn- reminder-types [application now]
  (mapv (juxt :reminder/type #(or (:reminder/days-left %) (:reminder/days-overdue %)))
        (deadlines/reminders application now zone)))

(deftest test-days-between
  (is (= 0 (deadlines/days-between (at 2026 3 1) (time/date-time 2026 3 1 20) zone)))
  (is (= 1 (deadlines/days-between (time/date-time 2026 3 1 21) (time/date-time 2026 3 2 1) zone)))
  (is (= -3 (deadlines/days-between (at 2026 3 4) (at 2026 3 1) zone))))

(deftest test-reminders
  (with-redefs [rems.config/env {:review-deadline-days 20
                                 :re-review-deadline-days 10
                                 :changes-deadline-days 30}]
    (let [application (reviewed-application (at 2026 3 1))] ; deadline 03.21.
      (testing "reviews are reminded of 7 and 3 days before the deadline"
        (is (= [[:review-due-soon 7]] (reminder-types application (at 2026 3 14))))
        (is (= [] (reminder-types application (at 2026 3 15))))
        (is (= [[:review-due-soon 3]] (reminder-types application (at 2026 3 18)))))
      (testing "the reviews already done are not reminded of"
        (is (= ["reviewer"] (map :reminder/reviewer (deadlines/reminders application (at 2026 3 14) zone)))))
      (testing "overdue reviews are reminded of on the first day and then weekly"
        (is (= [] (reminder-types application (at 2026 3 21))))
        (is (= [[:review-overdue 1]] (reminder-types application (at 2026 3 22))))
        (is (= [] (reminder-types application (at 2026 3 23))))
        (is (= [[:review-overdue 8]] (reminder-types application (at 2026 3 29))))))
    (let [application (returned-application (at 2026 3 1))] ; deadline 03.31.
      (testing "the changes are reminded of 7 and 3 days before the deadline"
        (is (= [[:changes-due-soon 7]] (reminder-types application (at 2026 3 24))))
        (is (= [[:changes-due-soon 3]] (reminder-types application (at 2026 3 28)))))
      (testing "an overdue change is reminded of once"
        (is (= [[:changes-overdue 1]] (reminder-types application (at 2026 4 1))))
        (is (= [] (reminder-types application (at 2026 4 8))))))
    (testing "no reminders after the decision"
      (is (= [] (reminder-types (assoc (reviewed-application (at 2026 3 1)) :application/state :application.state/approved)
                                (at 2026 3 14)))))))
