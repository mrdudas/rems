(ns rems.my-tasks
  "The /my-tasks page: the tasks of the user on the applications, by role."
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [rems.atoms :refer [document-title]]
            [rems.collapsible :as collapsible]
            [rems.fetcher :as fetcher]
            [rems.flash-message :as flash-message]
            [rems.globals]
            [rems.spinner :as spinner]
            [rems.text :refer [localized text text-format]]))

(rf/reg-event-fx
 ::enter-page
 (fn [_ _]
   {:dispatch [::my-tasks]}))

(fetcher/reg-fetcher ::my-tasks "/api/applications/my-tasks")

(defn- date-text
  "The ISO date of a time, which is a string or a date object."
  [t]
  (some-> t js/Date. .toISOString (subs 0 10)))

(defn- days-left [deadline]
  (when deadline
    (js/Math.ceil (/ (- (.getTime (js/Date. deadline)) (.getTime (js/Date.)))
                     (* 24 60 60 1000)))))

(defn- task-text
  "What the user should do, and whether it is urgent."
  [task]
  (case (keyword (:task/type task))
    :answer-question [(text :t.my-tasks/answer-question) :urgent]
    :returned (if-some [deadline (:task/deadline task)]
                (let [days (:task/days-left task)]
                  (if (neg? days)
                    [(text-format :t.my-tasks/returned-overdue (date-text deadline) (- days)) :overdue]
                    [(text-format :t.my-tasks/returned-deadline (date-text deadline) days) :urgent]))
                [(text :t.my-tasks/returned) :urgent])
    :submit [(text :t.my-tasks/submit) :urgent]
    :waiting-for-supervisor [(text :t.my-tasks/waiting-for-supervisor) :waiting]
    :draft [(text :t.my-tasks/draft) :normal]
    :fill-in [(text :t.my-tasks/fill-in) :normal]
    :approve-supervision [(if (:task/marked-ready task)
                            (text :t.my-tasks/approve-supervision-ready)
                            (text :t.my-tasks/approve-supervision))
                          (if (:task/marked-ready task) :urgent :normal)]
    :review (let [overdue (:review/overdue-days task)]
              (if (pos? overdue)
                [(text-format :t.my-tasks/review-overdue (date-text (:review/deadline task)) overdue) :overdue]
                [(text-format :t.my-tasks/review (date-text (:review/deadline task)) (days-left (:review/deadline task)))
                 (if (some-> (days-left (:review/deadline task)) (<= 3)) :urgent :normal)]))
    :reviews-overdue [(text-format :t.my-tasks/reviews-overdue (:task/overdue-days task)) :overdue]
    :request-reviews [(text-format :t.my-tasks/request-reviews (:task/reviewers task)) :urgent]
    :reviews-in-progress [(text-format :t.my-tasks/reviews-in-progress (:task/open-reviews task) (:task/reviewers task)) :waiting]
    :decide [(text :t.my-tasks/decide) :urgent]
    :decide-mixed [(text :t.my-tasks/decide-mixed) :urgent]
    :changes-overdue [(text-format :t.my-tasks/changes-overdue (date-text (:task/deadline task)) (:task/overdue-days task)) :overdue]
    [(str (:task/type task)) :normal]))

(def ^:private badge-colors
  {:overdue {:background-color "#FBE9EE" :color "#8E1236" :border-color "#F0B9C8"}
   :urgent {:background-color "#EAF2FC" :color "#14509A" :border-color "#C3D9F4"}
   :waiting {:background-color "#F4F6FA" :color "#5B6B80" :border-color "#E3E8EF"}
   :normal {:background-color "#FFFFFF" :color "#3A4A60" :border-color "#E3E8EF"}})

(defn- task-row [task]
  (let [[description urgency] (task-text task)
        id (:application/id task)]
    [:a.my-task {:href (str "/application/" id)
                 :style {:display :flex
                         :align-items :center
                         :gap "1rem"
                         :padding "0.75rem 1rem"
                         :border-top "1px solid #E3E8EF"
                         :color :inherit
                         :text-decoration :none}}
     [:div {:style {:flex "0 0 6rem" :font-weight 700}} (:application/external-id task)]
     [:div {:style {:flex "1 1 auto" :min-width 0}}
      [:div {:style {:overflow :hidden :text-overflow :ellipsis :white-space :nowrap}}
       (or (not-empty (:application/description task))
           [:span.text-muted (text :t.my-tasks/no-title)])]]
     ;; the procedure (e.g. normal or abbreviated)
     [:div.text-muted {:style {:flex "0 0 10rem" :font-size "0.875rem"}}
      (str/join ", " (map (comp localized :catalogue-item/title) (:application/resources task)))]
     [:div {:style (merge {:flex "0 0 auto"
                           :padding "0.25rem 0.75rem"
                           :border "1px solid"
                           :border-radius "999px"
                           :font-size "0.875rem"
                           :font-weight 600}
                          (badge-colors urgency))}
      description]]))

(def ^:private sections
  [[:handler :t.my-tasks/as-handler]
   [:reviewer :t.my-tasks/as-reviewer]
   [:applicant :t.my-tasks/as-applicant]
   [:supervisor :t.my-tasks/as-supervisor]
   [:member :t.my-tasks/as-member]])

(defn- section [role title tasks]
  [collapsible/component
   {:id (str "my-tasks-" (name role))
    :open? true
    :title (str (text title) " (" (count tasks) ")")
    :always [:div
             (into [:div] (for [task tasks]
                            ^{:key [(:application/id task) (:task/type task)]}
                            [task-row task]))
             (when (:reviewer-pool-organization @rems.globals/config)
               (case role
                 :handler [:div {:style {:padding "0.75rem 1rem 0"}}
                           [:a {:href "/reviewer-calendar"} (text :t.navigation/reviewer-calendar) " →"]]
                 :reviewer [:div {:style {:padding "0.75rem 1rem 0"}}
                            [:a {:href "/profile"} (text :t.reviewer-dashboard/my-absences-link) " →"]]
                 nil))]}])

(defn my-tasks-page []
  (let [tasks @(rf/subscribe [::my-tasks])
        loading? @(rf/subscribe [::my-tasks :fetching?])
        contact? (some #{:organization-contact} @rems.globals/roles)
        non-empty (for [[role title] sections
                        :let [role-tasks (get tasks role)]
                        :when (seq role-tasks)]
                    [role title role-tasks])]
    [:div
     [document-title (text :t.navigation/my-tasks)]
     [flash-message/component :top]
     (when contact?
       [:div.mb-3
        [:a.btn.btn-primary {:href "/catalogue"} (text :t.my-tasks/new-application)]])
     (cond
       (and loading? (not tasks))
       [spinner/big]

       (empty? non-empty)
       [:p (text :t.my-tasks/no-tasks)]

       :else
       (into [:div.spaced-sections]
             (for [[role title role-tasks] non-empty]
               ^{:key role}
               [section role title role-tasks])))]))
