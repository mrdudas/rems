(ns rems.reviews
  "The reviews of an application with the recommendations of the reviewers
  (see `:enable-review-recommendations`), and the questions and answers
  between the committee and the applicants."
  (:require [rems.collapsible :as collapsible]
            [rems.common.application-util :refer [is-handling-user?]]
            [rems.globals]
            [rems.text :refer [localize-time localize-user text text-format]]))

(def ^:private day-ms (* 24 60 60 1000))

(defn- ms [t]
  (when t (.getTime (js/Date. t))))

(defn- date-text [t]
  (when t (subs (.toISOString (js/Date. t)) 0 10)))

(defn- event-userids [users]
  (map #(if (map? %) (:userid %) %) users))

(defn- latest-submission [events]
  (last (filter #(= :application.event/submitted (:event/type %)) events)))

(defn- deadline
  "The deadline of a review requested at `requested` (ms), like in the
  reviewer calendar, see `:review-deadline-days`."
  [events requested]
  (let [returned? (some #(and (= :application.event/returned (:event/type %))
                              (< (ms (:event/time %)) requested))
                        events)
        days (if returned?
               (:re-review-deadline-days @rems.globals/config)
               (:review-deadline-days @rems.globals/config))]
    (when days
      (+ requested (* days day-ms)))))

(defn reviews
  "The reviews requested since the latest submission, one per reviewer, with
  the latest request and review of the reviewer."
  [application]
  (let [events (sort-by (comp ms :event/time) (:application/events application))
        submitted (some-> (latest-submission events) :event/time ms)
        since-submission (filter #(or (nil? submitted) (>= (ms (:event/time %)) submitted)) events)
        requests (for [event since-submission
                       :when (= :application.event/review-requested (:event/type event))
                       [userid user] (map vector
                                          (event-userids (:application/reviewers event))
                                          (:application/reviewers event))]
                   {:userid userid
                    :user (when (map? user) user)
                    :requested (ms (:event/time event))})
        reviewed (for [event since-submission
                       :when (= :application.event/reviewed (:event/type event))]
                   event)]
    (vec
     (for [[userid user-requests] (group-by :userid requests)
           :let [request (last (sort-by :requested user-requests))
                 review (last (filter #(and (= userid (:event/actor %))
                                            (>= (ms (:event/time %)) (:requested request)))
                                      reviewed))]]
       {:userid userid
        :name (or (get-in review [:event/actor-attributes :name])
                  (get-in request [:user :name])
                  userid)
        :requested (:requested request)
        :deadline (deadline events (:requested request))
        :reviewed (some-> review :event/time ms)
        :recommendation (some-> review :review/recommendation keyword)
        :comment (:application/comment review)}))))

(defn summary
  "`:pending` - the number of reviews not done yet
  `:recommendations` - the distinct recommendations of the done reviews
  `:unanimous` - the recommendation, if all reviews are done and agree"
  [application]
  (let [all (reviews application)
        done (filter :reviewed all)
        recommendations (set (keep :recommendation done))]
    {:reviews all
     :pending (- (count all) (count done))
     :recommendations recommendations
     :unanimous (when (and (seq all)
                           (= (count all) (count done))
                           (= 1 (count recommendations)))
                  (first recommendations))}))

(defn- recommendation-text [recommendation]
  (case recommendation
    :approve (text :t.recommendations/approve)
    :revise (text :t.recommendations/revise)
    :reject (text :t.recommendations/reject)
    :not-competent (text :t.recommendations/not-competent)
    nil))

(def ^:private recommendation-colors
  {:approve {:background-color "#E5F4EA" :color "#1E5E3A"}
   :revise {:background-color "#FFF6E0" :color "#7A5200"}
   :reject {:background-color "#FBE9EE" :color "#8E1236"}
   :not-competent {:background-color "#F4F6FA" :color "#3A4A60"}})

(defn- badge [style content]
  [:span {:style (merge {:display :inline-block
                         :padding "0.15rem 0.6rem"
                         :border-radius "999px"
                         :font-size "0.85rem"
                         :font-weight 600}
                        style)}
   content])

(defn- review-status [{:keys [reviewed deadline]}]
  (let [now (.getTime (js/Date.))]
    (cond
      reviewed
      [:span.text-muted (text-format :t.reviews/reviewed (date-text reviewed))]

      (and deadline (> now deadline))
      [:span {:style {:color "#CD214F" :font-weight 600}}
       (text-format :t.reviews/overdue (date-text deadline) (js/Math.floor (/ (- now deadline) day-ms)))]

      deadline
      [:span (text-format :t.reviews/deadline (date-text deadline) (js/Math.ceil (/ (- deadline now) day-ms)))]

      :else
      [:span (text :t.reviews/in-progress)])))

(defn decision-warning
  "Warns the handler when approving or rejecting if the reviews are missing or
  not unanimous. The chair may decide regardless."
  [application]
  (when (:enable-review-recommendations @rems.globals/config)
    (let [{:keys [reviews pending recommendations unanimous]} (summary application)]
      (when (and (seq reviews) (not unanimous))
        [:div.alert.alert-warning
         [:strong (text :t.reviews/not-unanimous)]
         " "
         (when (pos? pending)
           (text-format :t.reviews/pending pending))
         " "
         (when (seq recommendations)
           (text-format :t.reviews/recommendations
                        (->> recommendations (map recommendation-text) sort (interpose ", ") (apply str))))
         " "
         (text :t.reviews/chair-may-decide)]))))

(defn reviews-card
  "The reviews of the application for the handlers."
  [application]
  (when (and (:enable-review-recommendations @rems.globals/config)
             (some #{:handler} (:application/roles application)))
    (let [{:keys [reviews pending unanimous]} (summary application)]
      [collapsible/component
       {:id "reviews-collapsible"
        :title (text :t.reviews/title)
        :always
        (if (empty? reviews)
          [:p.mb-0 (text :t.reviews/no-reviews)]
          [:div
           (cond
             unanimous
             [:p [badge (recommendation-colors unanimous)
                  (text-format :t.reviews/unanimous (recommendation-text unanimous))]]

             (zero? pending)
             [:p [badge (recommendation-colors :reject) (text :t.reviews/not-unanimous)]])
           (for [review reviews]
             ^{:key (:userid review)}
             [:div {:style {:border-top "1px solid #E3E8EF" :padding "0.6rem 0"}}
              [:div {:style {:display :flex :justify-content :space-between :gap "1rem" :flex-wrap :wrap}}
               [:strong (:name review)]
               [:span
                (when-some [recommendation (:recommendation review)]
                  [:<> [badge (recommendation-colors recommendation) (recommendation-text recommendation)] " "])
                [review-status review]]]
              (when-some [comment (not-empty (:comment review))]
                [:div {:style {:white-space :pre-wrap :margin-top "0.35rem" :color "#3A4A60"}} comment])])])}])))

(defn- committee-actor?
  "Applicants see the handling users anonymized, without their roles."
  [application event]
  (or (= "rems-handler" (:event/actor event))
      (= "rems-handler" (get-in event [:event/actor-attributes :userid]))
      (is-handling-user? application (:event/actor event))))

(defn- question-or-answer? [application event]
  (and (= :application.event/remarked (:event/type event))
       (or (:event/public event)
           (not (committee-actor? application event)))))

(defn questions-card
  "The public remarks of the committee and the remarks of the applicants, as
  one thread of questions and answers. The applicants see the committee
  anonymized, as in the events."
  [application]
  (let [events (->> (:application/events application)
                    (filter (partial question-or-answer? application))
                    (sort-by (comp ms :event/time)))]
    (when (seq events)
      [collapsible/component
       {:id "questions-collapsible"
        :title (text :t.reviews/questions)
        :always
        [:div
         (for [event events
               :let [committee? (committee-actor? application event)]]
           ^{:key (:event/id event)}
           [:div {:style {:margin "0.5rem 0"
                          :padding "0.6rem 0.8rem"
                          :border-radius "10px"
                          :background-color (if committee? "#EAF2FC" "#F4F6FA")
                          :margin-left (when-not committee? "2rem")
                          :margin-right (when committee? "2rem")}}
            [:div {:style {:font-size "0.85rem" :color "#5B6B80"}}
             (localize-user (:event/actor-attributes event))
             " · " (localize-time (:event/time event))]
            [:div {:style {:white-space :pre-wrap}} (:application/comment event)]])
         [:p.text-muted.mb-0 {:style {:font-size "0.85rem"}} (text :t.reviews/questions-hint)]]}])))
