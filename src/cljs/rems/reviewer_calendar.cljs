(ns rems.reviewer-calendar
  "The /reviewer-calendar page: the open and done reviews of the reviewer
  pool on a scrollable timeline with their deadlines, and requesting new
  reviews. See `:reviewer-pool-organization`."
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [rems.atoms :refer [document-title]]
            [rems.collapsible :as collapsible]
            [rems.config]
            [rems.fetcher :as fetcher]
            [rems.flash-message :as flash-message]
            [rems.reviewer-dashboard :as reviewer-dashboard]
            [rems.reviews :refer [recommendation-text]]
            [rems.spinner :as spinner]
            [rems.text :refer [localize-reviewer-status localize-state text text-format]]
            [rems.util :refer [post!]]))

(rf/reg-event-fx
 ::enter-page
 (fn [{:keys [db]} _]
   {:db (dissoc db ::selected-application)
    :dispatch-n [[::calendar]
                 [::reviewer-dashboard/enter-page]]}))

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
(def ^:private day-width
  "The width of a day on the timeline in pixels."
  24)
(def ^:private name-width 224)
(def ^:private action-width 144)
(def ^:private lane-height 46)
(def ^:private min-bar-days
  "The minimum length of a bar in days, so that its text can be read."
  9)
(def ^:private scroll-days
  "How far the earlier and later buttons scroll."
  28)

(defn- start-of-day [t]
  (let [d (js/Date. t)]
    (.getTime (js/Date. (.getFullYear d) (.getMonth d) (.getDate d)))))

(defn- ms [s]
  (when s (.getTime (js/Date. s))))

(defn- format-date [t]
  (subs (.toISOString (js/Date. t)) 0 10))

(defn- review-end
  "The end of the bar of the review: when it was done, today if it is
  overdue, or else the deadline."
  [review now]
  (or (ms (:review/done review))
      (if (pos? (:review/overdue-days review))
        now
        (or (ms (:review/deadline review)) (ms (:review/requested review))))))

(defn- timeline
  "The timeline from a few weeks before the first review until a few weeks
  after the last deadline, starting on a Monday."
  [reviewers]
  (let [now (.getTime (js/Date.))
        today (start-of-day now)
        reviews (mapcat :reviews reviewers)
        first-day (start-of-day (apply min (- today (* 8 7 day-ms))
                                       (map (comp ms :review/requested) reviews)))
        ;; Monday on or before the first day
        start (- first-day (* day-ms (mod (+ 6 (.getDay (js/Date. first-day))) 7)))
        end (apply max (+ today (* 8 7 day-ms))
                   (map #(+ (review-end % now) (* 14 day-ms)) reviews))]
    {:now now
     :today today
     :start start
     :days (js/Math.ceil (/ (- end start) day-ms))}))

(defn- x
  "The position of the time `t` (ms) on the timeline in pixels."
  [{:keys [start]} t]
  (* day-width (/ (- t start) day-ms)))

(defn- lanes
  "Puts the reviews in lanes so that the bars do not overlap, and returns
  the reviews with their `::lane`."
  [line reviews]
  (loop [reviews (sort-by (comp ms :review/requested) reviews)
         lane-ends []
         result []]
    (if-some [review (first reviews)]
      (let [left (x line (ms (:review/requested review)))
            right (max (+ left (* min-bar-days day-width)) (x line (review-end review (:now line))))
            lane (or (first (keep-indexed #(when (<= %2 left) %1) lane-ends))
                     (count lane-ends))]
        (recur (rest reviews)
               (assoc lane-ends lane (+ right 2))
               (conj result (assoc review ::lane lane))))
      result)))

(def ^:private recommendation-colors
  {:approve "#2E7D4F"
   :revise "#A86A00"
   :reject "#8E1236"
   :not-competent "#5B6B80"})

(defn- review-status
  "The state of the review as text: done with the recommendation, overdue,
  or the deadline."
  [review]
  (cond
    (:review/done review)
    (str (text-format :t.reviewer-calendar/done (format-date (ms (:review/done review))))
         (when-some [recommendation (some-> (:review/recommendation review) keyword)]
           (str " – " (recommendation-text recommendation))))

    (pos? (:review/overdue-days review))
    (text-format :t.reviewer-calendar/overdue (:review/overdue-days review))

    :else
    (text-format :t.reviewer-calendar/deadline (some-> (ms (:review/deadline review)) format-date))))

(defn- review-bar [line review]
  (let [requested (ms (:review/requested review))
        deadline (or (ms (:review/deadline review)) requested)
        done? (some? (:review/done review))
        overdue? (pos? (:review/overdue-days review))
        left (x line requested)
        right (max (+ left (* min-bar-days day-width)) (x line (review-end review (:now line))))
        width (- right left)
        title (str (:application/external-id review)
                   (when-some [description (:application/description review)]
                     (str " – " description)))
        state (localize-state (keyword (:application/state review)))
        status (review-status review)]
    [:a.reviewer-calendar-bar
     {:href (str "/application/" (:application/id review))
      :title (str title "\n" state " · " status)
      :style {:position :absolute
              :top (+ 3 (* (::lane review) lane-height))
              :height (- lane-height 6)
              :left left
              :width width
              :overflow :hidden
              :border-radius 6
              :color :white
              :text-decoration :none
              :background (cond
                            done? (get recommendation-colors (some-> (:review/recommendation review) keyword) "#5B6B80")
                            ;; the part after the deadline is red
                            overdue? (let [split (* 100 (/ (- (x line deadline) left) width))]
                                       (str "linear-gradient(to right, #2c6e9e " split "%, #c62828 " split "%)"))
                            :else "#2c6e9e")
              :opacity (when done? 0.85)}}
     [:div {:style {:padding "3px 6px"
                    :font-size "0.78rem"
                    :line-height 1.25
                    :white-space :nowrap
                    :overflow :hidden
                    :text-overflow :ellipsis}}
      [:div {:style {:font-weight 600 :overflow :hidden :text-overflow :ellipsis}} title]
      [:div {:style {:overflow :hidden :text-overflow :ellipsis}} state " · " status]]]))

(defn- date-ms
  "The start of the date YYYY-MM-DD in the local time zone."
  [date]
  (let [[year month day] (map js/parseInt (.split date "-"))]
    (.getTime (js/Date. year (dec month) day))))

(defn- absence-blocks
  "The absences of the reviewer, greyed out."
  [line reviewer]
  [:<>
   (for [absence (:reviewer/absences reviewer)
         :let [left (x line (date-ms (:start absence)))
               right (x line (+ (date-ms (:end absence)) day-ms))]]
     ^{:key (:start absence)}
     [:div {:title (str (text :t.reviewer-calendar/absent) ": " (reviewer-dashboard/absence-text absence))
            :style {:position :absolute :top 0 :bottom 0
                    :left left
                    :width (- right left)
                    :background "repeating-linear-gradient(135deg, #d5dae2 0, #d5dae2 6px, #e6e9ee 6px, #e6e9ee 12px)"
                    :color "#5B6B80"
                    :font-size "0.75rem"
                    :padding "2px 4px"
                    :overflow :hidden
                    :white-space :nowrap}}
      (text :t.reviewer-calendar/absent)])])

(defn- absent-today? [reviewer]
  (let [today (format-date (- (.getTime (js/Date.)) (* 60000 (.getTimezoneOffset (js/Date.)))))]
    (some #(and (<= (compare (:start %) today) 0)
                (<= (compare today (:end %)) 0))
          (:reviewer/absences reviewer))))

(defn- weeks
  "The Mondays of the timeline."
  [line]
  (for [week (range (quot (:days line) 7))]
    (+ (:start line) (* week 7 day-ms))))

(defn- grid
  "The week lines and the line of today."
  [line]
  [:<>
   (for [t (weeks line)]
     ^{:key t}
     [:div {:style {:position :absolute :top 0 :bottom 0 :width 1
                    :left (x line t)
                    :background-color "#e1e5eb"}}])
   [:div {:style {:position :absolute :top 0 :bottom 0 :width 2
                  :left (x line (:now line))
                  :background-color "#16233A"}}]])

(def ^:private sticky-left
  {:position :sticky :left 0 :z-index 2 :background-color "#fff"})

(def ^:private sticky-right
  {:position :sticky :right 0 :z-index 2 :background-color "#fff"})

(defn- week-header [line application]
  [:div {:style {:display :flex :border-bottom "1px solid #ddd"}}
   [:div {:style (merge sticky-left {:flex (str "0 0 " name-width "px")})}]
   [:div {:style {:flex (str "0 0 " (* day-width (:days line)) "px")
                  :position :relative
                  :height 40
                  :font-size "0.75rem"
                  :color "#5B6B80"}}
    (for [t (weeks line)
          :let [date (js/Date. t)]]
      ^{:key t}
      [:div {:style {:position :absolute :left (+ 3 (x line t)) :top 2 :white-space :nowrap}}
       ;; the month on the first week of the month
       [:div {:style {:font-weight 600 :height "1.1em"}}
        (when (<= (.getDate date) 7)
          (.toLocaleDateString date (name @rems.config/current-language) #js {:year "numeric" :month "long"}))]
       [:div (subs (format-date (+ t (* 12 60 60 1000))) 5)]])
    [:div {:style {:position :absolute :left (- (x line (:now line)) 20) :bottom 0
                   :width 40 :text-align :center :font-weight 700 :color "#16233A"
                   :background-color "#fff"}}
     (text :t.reviewer-calendar/today)]]
   (when application
     [:div {:style (merge sticky-right {:flex (str "0 0 " action-width "px")})}])])

(defn- reviewer-row [line reviewer {:keys [application available requested]}]
  (let [reviews (lanes line (:reviews reviewer))
        open (remove :review/done reviews)
        lane-count (max 1 (inc (apply max -1 (map ::lane reviews))))
        overdue? (some (comp pos? :review/overdue-days) open)]
    [:div.reviewer-calendar-row {:style {:display :flex
                                         :border-bottom "1px solid #ddd"}}
     [:div {:style (merge sticky-left
                          {:flex (str "0 0 " name-width "px")
                           :padding "6px 8px 6px 0"
                           :border-right "1px solid #ddd"})}
      [:div {:style {:font-weight :bold :color (when overdue? "#c62828")}} (:name reviewer)]
      [:div {:style {:font-size "0.8rem" :color "#666"}}
       (:reviewer/organization reviewer)
       " · " (localize-reviewer-status (:reviewer/status reviewer))
       " · " (text-format :t.reviewer-calendar/open-reviews (count open))]]
     [:div {:style {:flex (str "0 0 " (* day-width (:days line)) "px")
                    :position :relative
                    :height (* lane-count lane-height)
                    :background-color "#f6f8fb"}}
      [absence-blocks line reviewer]
      [grid line]
      (for [review reviews]
        ^{:key (str (:application/id review) "/" (:review/requested review))}
        [review-bar line review])]
     (when application
       [:div {:style (merge sticky-right
                            {:flex (str "0 0 " action-width "px")
                             :padding "6px 0 6px 8px"
                             :border-left "1px solid #ddd"
                             :display :flex
                             :align-items :center})}
        (cond
          (contains? requested (:userid reviewer))
          [:span.text-muted (text :t.reviewer-calendar/already-requested)]

          (absent-today? reviewer)
          [:span.text-muted (text :t.reviewer-calendar/absent)]

          (contains? available (:userid reviewer))
          [:button.btn.btn-primary.btn-sm
           {:type :button
            :on-click #(rf/dispatch [::request-review (:application/id application) reviewer])}
           (text :t.reviewer-calendar/request)]

          :else
          [:span.text-muted {:title (text :t.reviewer-calendar/not-requestable)}
           (text :t.reviewer-calendar/not-requestable-short)])])]))

(defn- scroll-to-today!
  "Scrolls the timeline so that today is at a third of the visible part."
  [^js element line]
  (set! (.-scrollLeft element)
        (- (+ name-width (x line (:now line)))
           (/ (.-clientWidth element) 3))))

(defn- calendar-timeline
  "The timeline in a horizontally scrolling box. It opens at today, and can
  be scrolled to the past and the future."
  []
  (r/with-let [element (atom nil)
               scrolled? (atom false)]
    (let [calendar @(rf/subscribe [::calendar])
          selected @(rf/subscribe [::selected-application])
          application (some #(when (= selected (:application/id %)) %) (:waiting-applications calendar))
          available (set (map :userid @(rf/subscribe [::available-reviewers])))
          line (timeline (:reviewers calendar))
          scroll! (fn [days]
                    (some-> ^js @element (.scrollBy #js {:left (* days day-width) :behavior "smooth"})))]
      [:div
       [:div.d-flex.flex-wrap.align-items-center.mb-2 {:style {:gap "0.5rem"}}
        [:button.btn.btn-secondary.btn-sm {:type :button :on-click #(scroll! (- scroll-days))}
         "‹ " (text :t.reviewer-calendar/earlier)]
        [:button.btn.btn-secondary.btn-sm {:type :button :on-click #(some-> @element (scroll-to-today! line))}
         (text :t.reviewer-calendar/today)]
        [:button.btn.btn-secondary.btn-sm {:type :button :on-click #(scroll! scroll-days)}
         (text :t.reviewer-calendar/later) " ›"]]
       [:div {:ref (fn [el]
                     (when el
                       (reset! element el)
                       (when-not @scrolled?
                         (reset! scrolled? true)
                         (scroll-to-today! el line))))
              ;; the zero width keeps the box from widening the page
              :style {:width 0
                      :min-width "100%"
                      :overflow-x :auto
                      :border "1px solid #ddd"
                      :border-radius 8}}
        [:div {:style {:width :max-content :min-width "100%"}}
         [week-header line application]
         (for [reviewer (:reviewers calendar)]
           ^{:key (:userid reviewer)}
           [reviewer-row line reviewer {:application application
                                        :available available
                                        :requested (set (:application/reviewers application))}])]]])))

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
        selected @(rf/subscribe [::selected-application])]
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
          :title (text :t.reviewer-calendar/title)
          :always [:div
                   [:p (text :t.reviewer-calendar/intro)]
                   [calendar-timeline]]}]
        [collapsible/component
         {:id "reviewer-dashboard"
          :open? true
          :title (text :t.reviewer-dashboard/title)
          :always [reviewer-dashboard/reviewer-dashboard]}]])]))
