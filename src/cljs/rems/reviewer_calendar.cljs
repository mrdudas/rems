(ns rems.reviewer-calendar
  "The /reviewer-calendar page: the open reviews of the reviewer pool on a
  timeline with their deadlines, and requesting new reviews. See
  `:reviewer-pool-organization`."
  (:require [re-frame.core :as rf]
            [rems.atoms :refer [document-title]]
            [rems.collapsible :as collapsible]
            [rems.fetcher :as fetcher]
            [rems.flash-message :as flash-message]
            [rems.spinner :as spinner]
            [rems.text :refer [localize-reviewer-status text text-format]]
            [rems.util :refer [post!]]))

(rf/reg-event-fx
 ::enter-page
 (fn [{:keys [db]} _]
   {:db (dissoc db ::selected-application)
    :dispatch [::calendar]}))

(fetcher/reg-fetcher ::calendar "/api/applications/reviewer-calendar")
(fetcher/reg-fetcher ::available-reviewers "/api/applications/reviewers")

(rf/reg-sub ::selected-application (fn [db _] (::selected-application db)))

(rf/reg-event-fx
 ::select-application
 (fn [{:keys [db]} [_ application-id]]
   {:db (assoc db ::selected-application application-id)
    :dispatch-n [(when application-id
                   [::available-reviewers {:application-id application-id}])]}))

(rf/reg-event-fx
 ::request-review
 (fn [_ [_ application-id reviewer]]
   (let [description [text :t.actions/request-review]]
     (post! "/api/applications/request-review"
            {:params {:application-id application-id
                      :reviewers [(:userid reviewer)]
                      :comment ""}
             :handler (flash-message/default-success-handler
                       :top description
                       (fn [_]
                         (rf/dispatch [::calendar])
                         (rf/dispatch [::available-reviewers {:application-id application-id}])))
             :error-handler (flash-message/default-error-handler :top description)}))
   {}))

;;;; timeline

(def ^:private day-ms (* 24 60 60 1000))
(def ^:private days-before 14)
(def ^:private days-after 42)

(defn- start-of-today []
  (let [now (js/Date.)]
    (.getTime (js/Date. (.getFullYear now) (.getMonth now) (.getDate now)))))

(defn- timeline []
  (let [today (start-of-today)]
    {:today today
     :start (- today (* days-before day-ms))
     :end (+ today (* days-after day-ms))}))

(defn- position
  "The position of the time `t` (ms) on the timeline in percent, clamped."
  [{:keys [start end]} t]
  (-> (/ (- t start) (- end start))
      (* 100)
      (max 0)
      (min 100)))

(defn- ms [s]
  (when s (.getTime (js/Date. s))))

(defn- date-text [t]
  (.toISOString (js/Date. t)))

(defn- format-date [t]
  (subs (date-text t) 0 10))

(def ^:private lane-height 30)

(defn- review-bar [line lane review]
  (let [requested (ms (:review/requested review))
        deadline (ms (:review/deadline review))
        overdue? (pos? (:review/overdue-days review))
        end (if overdue? (.getTime (js/Date.)) (or deadline requested))
        left (position line requested)
        deadline-position (position line (or deadline requested))
        right (position line end)]
    [:a.reviewer-calendar-bar
     {:href (str "/application/" (:application/id review))
      :title (str (:application/external-id review)
                  (when-some [description (:application/description review)]
                    (str " – " description))
                  " – " (text-format :t.reviewer-calendar/deadline (some-> deadline format-date))
                  (when overdue?
                    (str " – " (text-format :t.reviewer-calendar/overdue (:review/overdue-days review)))))
      :style {:position :absolute
              :top (+ 2 (* lane lane-height))
              :height (- lane-height 4)
              :left (str left "%")
              :width (str (max 1 (- right left)) "%")
              :display :flex
              :overflow :hidden
              :border-radius 4
              :color :white
              :font-size "0.8rem"
              :line-height (str (- lane-height 4) "px")
              :white-space :nowrap
              :text-decoration :none}}
     ;; the time until the deadline
     [:span {:style {:flex (str "0 0 " (if (= right left) 100 (* 100 (/ (- deadline-position left) (- right left)))) "%")
                     :background-color "#2c6e9e"
                     :padding-left 4
                     :overflow :hidden}}
      (:application/external-id review)]
     ;; the time after the deadline
     (when overdue?
       [:span {:style {:flex "1 1 auto"
                       :background-color "#c62828"
                       :padding-left 4
                       :overflow :hidden}}
        (text-format :t.reviewer-calendar/overdue (:review/overdue-days review))])]))

(defn- week-header [line]
  [:div {:style {:position :relative :height 22 :font-size "0.75rem" :color "#666"}}
   ;; the end of the timeline has no label, since it would not fit
   (for [week (range (- (quot days-before 7)) (quot days-after 7))
         :let [t (+ (:today line) (* week 7 day-ms))]]
     ^{:key week}
     [:span {:style {:position :absolute
                     :left (str (position line t) "%")
                     :transform "translateX(-50%)"
                     :white-space :nowrap}}
      (if (zero? week)
        (text :t.reviewer-calendar/today)
        (subs (format-date t) 5))])])

(defn- reviewer-row [line reviewer {:keys [application available requested]}]
  (let [reviews (:reviews reviewer)
        lanes (max 1 (count reviews))
        overdue? (some (comp pos? :review/overdue-days) reviews)]
    [:div.reviewer-calendar-row {:style {:display :flex
                                         :align-items :center
                                         :border-top "1px solid #ddd"
                                         :padding "4px 0"}}
     [:div {:style {:flex "0 0 14rem" :padding-right "0.5rem"}}
      [:div {:style {:font-weight :bold :color (when overdue? "#c62828")}} (:name reviewer)]
      [:div {:style {:font-size "0.8rem" :color "#666"}}
       (:reviewer/organization reviewer)
       " · " (localize-reviewer-status (:reviewer/status reviewer))
       " · " (text-format :t.reviewer-calendar/open-reviews (count reviews))]]
     [:div {:style {:flex "1 1 auto"
                    :position :relative
                    :height (* lanes lane-height)
                    :background-color "#f6f6f6"}}
      [:div {:style {:position :absolute :top 0 :bottom 0 :width 2
                     :left (str (position line (:today line)) "%")
                     :background-color "#333"}}]
      (for [[lane review] (map-indexed vector reviews)]
        ^{:key (:application/id review)}
        [review-bar line lane review])]
     (when application
       [:div {:style {:flex "0 0 9rem" :padding-left "0.5rem"}}
        (cond
          (contains? requested (:userid reviewer))
          [:span.text-muted (text :t.reviewer-calendar/already-requested)]

          (contains? available (:userid reviewer))
          [:button.btn.btn-primary.btn-sm
           {:type :button
            :on-click #(rf/dispatch [::request-review (:application/id application) reviewer])}
           (text :t.reviewer-calendar/request)]

          :else
          [:span.text-muted {:title (text :t.reviewer-calendar/not-requestable)}
           (text :t.reviewer-calendar/not-requestable-short)])])]))

(defn- application-selection [applications selected]
  [:div.form-group
   [:label {:for "reviewer-calendar-application"} (text :t.reviewer-calendar/select-application)]
   [:select.form-control
    {:id "reviewer-calendar-application"
     :value (or selected "")
     :on-change #(let [value (.. % -target -value)]
                   (rf/dispatch [::select-application (when (seq value) (js/parseInt value))]))}
    [:option {:value ""} ""]
    (for [application applications]
      ^{:key (:application/id application)}
      [:option {:value (:application/id application)}
       (str (:application/external-id application)
            (when-some [description (:application/description application)]
              (str " – " description))
            " (" (text-format :t.reviewer-calendar/reviewers-requested (count (:application/reviewers application))) ")")])]])

(defn reviewer-calendar-page []
  (let [calendar @(rf/subscribe [::calendar])
        loading? @(rf/subscribe [::calendar :fetching?])
        selected @(rf/subscribe [::selected-application])
        application (some #(when (= selected (:application/id %)) %) (:waiting-applications calendar))
        available (set (map :userid @(rf/subscribe [::available-reviewers])))
        line (timeline)]
    [:div
     [document-title (text :t.navigation/reviewer-calendar)]
     [flash-message/component :top]
     (if (and loading? (not calendar))
       [spinner/big]
       [:div.spaced-sections
        [collapsible/component
         {:id "reviewer-calendar-request"
          :open? true
          :title (text :t.reviewer-calendar/request-reviews)
          :always [:<>
                   [:p (text :t.reviewer-calendar/request-reviews-intro)]
                   (if (seq (:waiting-applications calendar))
                     [application-selection (:waiting-applications calendar) selected]
                     [:p (text :t.reviewer-calendar/no-waiting-applications)])]}]
        [collapsible/component
         {:id "reviewer-calendar"
          :open? true
          :title (text :t.navigation/reviewer-calendar)
          :always [:div
                   [:p (text :t.reviewer-calendar/intro)]
                   [:div {:style {:display :flex}}
                    [:div {:style {:flex "0 0 14rem"}}]
                    [:div {:style {:flex "1 1 auto"}} [week-header line]]
                    (when application [:div {:style {:flex "0 0 9rem"}}])]
                   (for [reviewer (:reviewers calendar)]
                     ^{:key (:userid reviewer)}
                     [reviewer-row line reviewer {:application application
                                                  :available available
                                                  :requested (set (:application/reviewers application))}])]}]])]))
