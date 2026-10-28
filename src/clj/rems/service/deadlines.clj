(ns rems.service.deadlines
  "The deadlines of the EPKEB procedure and the reminders about them: the
  reviews (see `:review-deadline-days`) and the changes requested from the
  applicants (see `:changes-deadline-days`)."
  (:require [clj-time.core :as time]
            [rems.config :refer [env]]
            [rems.service.reviewer-calendar :as reviewer-calendar]))

(defn- date [t zone]
  (let [local (time/to-time-zone t zone)]
    (time/local-date (time/year local) (time/month local) (time/day local))))

(defn days-between
  "The number of calendar days from `from` to `to` in the time `zone`,
  negative if `to` is earlier."
  [from to zone]
  (let [a (date from zone)
        b (date to zone)]
    (if (time/before? b a)
      (- (time/in-days (time/interval (.toDateTimeAtStartOfDay b) (.toDateTimeAtStartOfDay a))))
      (time/in-days (time/interval (.toDateTimeAtStartOfDay a) (.toDateTimeAtStartOfDay b))))))

(defn changes-deadline
  "The deadline of the changes requested when the application was returned,
  or nil if the application is not returned."
  [application]
  (when-let [days (:changes-deadline-days env)]
    (when (= :application.state/returned (:application/state application))
      (when-some [returned (last (filter #(= :application.event/returned (:event/type %))
                                         (:application/events application)))]
        (time/plus (:event/time returned) (time/days days))))))

(def ^:private remind-days-before #{7 3})

(defn- overdue-reminder-day?
  "Overdue reviews are reminded of on the first day after the deadline and
  then weekly."
  [days-overdue]
  (and (pos? days-overdue)
       (zero? (mod (dec days-overdue) 7))))

(defn reminders
  "The reminders to send about the application `now`. Each reminder has the
  `:reminder/type`, the `:reminder/deadline`, and the days left
  (`:reminder/days-left`) or overdue (`:reminder/days-overdue`); reviews
  also the `:reminder/reviewer`."
  [application now zone]
  (concat
   (for [review (when (= :application.state/submitted (:application/state application))
                  (reviewer-calendar/open-reviews application))
         :let [deadline (:review/deadline review)]
         :when deadline
         :let [days-left (days-between now deadline zone)]
         reminder (cond
                    (contains? remind-days-before days-left)
                    [{:reminder/type :review-due-soon :reminder/days-left days-left}]

                    (overdue-reminder-day? (- days-left))
                    [{:reminder/type :review-overdue :reminder/days-overdue (- days-left)}])]
     (assoc reminder
            :reminder/deadline deadline
            :reminder/reviewer (:userid review)))
   (when-some [deadline (changes-deadline application)]
     (let [days-left (days-between now deadline zone)]
       (cond
         (contains? remind-days-before days-left)
         [{:reminder/type :changes-due-soon :reminder/deadline deadline :reminder/days-left days-left}]

         (= -1 days-left)
         [{:reminder/type :changes-overdue :reminder/deadline deadline :reminder/days-overdue 1}])))))
