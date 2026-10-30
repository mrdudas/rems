(ns rems.reviewer-dashboard
  "The reviewer dashboard on the /reviewer-calendar page: the members of the
  reviewer pool with their statistics in a month, a half-year or a year,
  editing the pool and the absences of the reviewers, and sending messages
  to the reviewers. See `:reviewer-pool-organization`."
  (:require [clojure.string :as str]
            [medley.core :refer [indexed remove-nth]]
            [re-frame.core :as rf]
            [rems.atoms :refer [enrich-user]]
            [rems.common.util :refer [conj-vec]]
            [rems.config]
            [rems.dropdown :as dropdown]
            [rems.fetcher :as fetcher]
            [rems.flash-message :as flash-message]
            [rems.spinner :as spinner]
            [rems.text :refer [localize-reviewer-status localized text text-format]]
            [rems.util :refer [post! put!]]))

;;;; period

(defn- pad [n]
  (if (< n 10) (str "0" n) (str n)))

(defn- last-day-of-month [year month]
  (.getDate (js/Date. year month 0)))

(defn- current-period [kind]
  (let [now (js/Date.)
        year (.getFullYear now)
        month (inc (.getMonth now))]
    {:kind kind
     :year year
     :index (case kind
              :month month
              :half (if (<= month 6) 1 2)
              :year 1)}))

(defn- period-months
  "The first and the last month of the period."
  [{:keys [kind index]}]
  (case kind
    :month [index index]
    :half (if (= 1 index) [1 6] [7 12])
    :year [1 12]))

(defn- period-dates [{:keys [year] :as period}]
  (let [[first-month last-month] (period-months period)]
    {:from (str year "-" (pad first-month) "-01")
     :to (str year "-" (pad last-month) "-" (pad (last-day-of-month year last-month)))}))

(defn- shift-period
  "The period `n` periods after `period` (or before, if `n` is negative)."
  [{:keys [kind year index] :as period} n]
  (let [per-year (case kind :month 12 :half 2 :year 1)
        position (+ (* year per-year) (dec index) n)]
    (assoc period
           :year (quot position per-year)
           :index (inc (mod position per-year)))))

(defn- period-title [{:keys [kind year index]}]
  (case kind
    :month (.toLocaleDateString (js/Date. year (dec index) 1)
                                (name @rems.config/current-language)
                                #js {:year "numeric" :month "long"})
    :half (text-format :t.reviewer-dashboard/half-year year index)
    :year (str year)))

;;;; state

(rf/reg-event-fx
 ::enter-page
 (fn [{:keys [db]} _]
   (let [period (or (::period db) (current-period :month))]
     {:db (assoc db
                 ::period period
                 ::editing nil
                 ::message nil)
      :dispatch [::dashboard (period-dates period)]})))

(fetcher/reg-fetcher ::dashboard "/api/reviewer-pool")
(fetcher/reg-fetcher ::available-users "/api/organizations/available-owners" {:result (partial mapv enrich-user)})

;; the page may render before ::enter-page has set the period
(rf/reg-sub ::period (fn [db _] (or (::period db) (current-period :month))))

(rf/reg-event-fx
 ::set-period
 (fn [{:keys [db]} [_ period]]
   {:db (assoc db ::period period)
    :dispatch [::dashboard (period-dates period)]}))

(defn- refresh! []
  (rf/dispatch [::refresh])
  (rf/dispatch [:rems.reviewer-calendar/calendar]))

(rf/reg-event-fx
 ::refresh
 (fn [{:keys [db]} _]
   {:dispatch [::dashboard (period-dates (::period db))]}))

;; editing a member of the pool, or a new one

(rf/reg-sub ::editing (fn [db _] (::editing db)))

(rf/reg-event-fx
 ::edit
 (fn [{:keys [db]} [_ reviewer absences-only?]]
   {:db (assoc db
               ::message nil
               ::editing (if reviewer
                           (-> reviewer
                               (select-keys [:userid :name :email :reviewer/organization :reviewer/status :reviewer/absences])
                               (update :reviewer/status keyword)
                               (assoc :absences-only? absences-only?))
                           {:new? true
                            :reviewer/status :active
                            :reviewer/absences []}))
    :dispatch-n [(when-not reviewer [::available-users])]}))

(rf/reg-event-db ::cancel-edit (fn [db _] (dissoc db ::editing)))
(rf/reg-event-db ::set-field (fn [db [_ ks value]] (assoc-in db (into [::editing] ks) value)))
(rf/reg-event-db ::add-absence (fn [db _] (update-in db [::editing :reviewer/absences] conj-vec {:start "" :end "" :comment ""})))
(rf/reg-event-db ::remove-absence (fn [db [_ index]] (update-in db [::editing :reviewer/absences] #(vec (remove-nth index %)))))

(defn valid-absence? [{:keys [start end]}]
  (and (not (str/blank? start))
       (not (str/blank? end))
       (<= (compare start end) 0)))

(defn- valid-entry? [entry]
  (and (:userid entry)
       (not (str/blank? (:reviewer/organization entry)))
       (every? valid-absence? (:reviewer/absences entry))))

(defn absences-params
  "The absences as the API takes them."
  [absences]
  (vec (for [absence absences]
         (cond-> (select-keys absence [:start :end])
           (not (str/blank? (:comment absence))) (assoc :comment (str/trim (:comment absence)))))))

(rf/reg-event-fx
 ::save
 (fn [{:keys [db]} _]
   (let [entry (::editing db)
         description [text :t.reviewer-dashboard/save]]
     (put! (if (:absences-only? entry)
             "/api/reviewer-pool/absences"
             "/api/reviewer-pool/reviewer")
           {:params (if (:absences-only? entry)
                      {:userid (:userid entry)
                       :reviewer/absences (absences-params (:reviewer/absences entry))}
                      {:userid (:userid entry)
                       :reviewer/organization (:reviewer/organization entry)
                       :reviewer/status (:reviewer/status entry)
                       :reviewer/absences (absences-params (:reviewer/absences entry))})
            :handler (flash-message/default-success-handler
                      :reviewer-dashboard description
                      (fn [_]
                        (rf/dispatch [::cancel-edit])
                        (refresh!)))
            :error-handler (flash-message/default-error-handler :reviewer-dashboard description)})
     {})))

;; a message to a reviewer

(rf/reg-sub ::message (fn [db _] (::message db)))

(rf/reg-event-db
 ::write-message
 (fn [db [_ reviewer]]
   (assoc db
          ::editing nil
          ::message {:reviewer reviewer :subject "" :body ""})))

(rf/reg-event-db ::cancel-message (fn [db _] (dissoc db ::message)))
(rf/reg-event-db ::set-message-field (fn [db [_ k value]] (assoc-in db [::message k] value)))

(rf/reg-event-fx
 ::send-message
 (fn [{:keys [db]} _]
   (let [{:keys [reviewer subject body]} (::message db)
         description [text :t.reviewer-dashboard/send]]
     (post! "/api/reviewer-pool/send-message"
            {:params {:userid (:userid reviewer)
                      :subject subject
                      :body body}
             :handler (flash-message/default-success-handler
                       :reviewer-dashboard description
                       (fn [_] (rf/dispatch [::cancel-message])))
             :error-handler (flash-message/default-error-handler :reviewer-dashboard description)})
     {})))

;;;; UI

(def ^:private statuses [:active :limited :paused :inactive])

(defn- organization-name [organization-id]
  (or (some #(when (= organization-id (:organization/id %))
               (localized (:organization/short-name %)))
            @(rf/subscribe [:organizations]))
      organization-id))

(defn- format-date [date]
  ;; YYYY-MM-DD to MM-DD
  (subs date 5))

(defn absence-text [absence]
  (str (:start absence) " – " (:end absence)
       (when-not (str/blank? (:comment absence))
         (str " (" (:comment absence) ")"))))

(defn- absences-cell [reviewer today]
  (let [upcoming (remove #(neg? (compare (:end %) today)) (:reviewer/absences reviewer))]
    (into [:div]
          (for [absence upcoming]
            [:div {:title (absence-text absence)
                   :style {:white-space :nowrap
                           :font-weight (when (<= (compare (:start absence) today) 0) 600)}}
             (format-date (:start absence)) " – " (format-date (:end absence))]))))

(defn- share-bar [share]
  [:div {:style {:display :flex :align-items :center :gap "0.4rem"}}
   [:div {:style {:flex "0 0 4rem" :height 8 :background-color "#E3E8EF" :border-radius 4}}
    [:div {:style {:width (str (js/Math.round (* 100 share)) "%")
                   :height 8
                   :background-color "#1F6FD1"
                   :border-radius 4}}]]
   [:span (str (js/Math.round (* 100 share)) "%")]])

(defn- number-cell [n]
  [:td.text-right n])

(defn- reviewer-row [reviewer {:keys [may-edit may-edit-absences today]}]
  (let [stats (:reviewer/stats reviewer)
        status (keyword (:reviewer/status reviewer))]
    [:tr {:style {:color (when (= :inactive status) "#8A96A8")}}
     [:td
      [:div {:style {:font-weight 600}} (:name reviewer)]
      [:div.small.text-muted (:email reviewer)]]
     [:td (organization-name (:reviewer/organization reviewer))]
     [:td (localize-reviewer-status status)
      (when (:reviewer/absent reviewer)
        [:div.small {:style {:color "#8E1236"}} (text :t.reviewer-calendar/absent)])]
     [:td [absences-cell reviewer today]]
     [number-cell (:requested stats)]
     [number-cell (:done stats)]
     [:td [share-bar (:share stats)]]
     [number-cell (if (pos? (:done stats))
                    (str (js/Math.round (* 100 (/ (:on-time stats) (:done stats)))) "%")
                    "–")]
     [number-cell (if-some [average (:average-days stats)]
                    (.toFixed average 1)
                    "–")]
     [:td.text-right
      (:open stats)
      (when (pos? (:overdue stats))
        [:span {:style {:color "#c62828"}} " (" (text-format :t.reviewer-dashboard/overdue-count (:overdue stats)) ")"])]
     [:td.text-right {:style {:white-space :nowrap}}
      (cond
        may-edit
        [:button.btn.btn-secondary.btn-sm.mr-1
         {:type :button :on-click #(rf/dispatch [::edit reviewer])}
         (text :t.reviewer-dashboard/edit)]

        may-edit-absences
        [:button.btn.btn-secondary.btn-sm.mr-1
         {:type :button :on-click #(rf/dispatch [::edit reviewer true])}
         (text :t.reviewer-dashboard/edit-absences)])
      [:button.btn.btn-secondary.btn-sm
       {:type :button :on-click #(rf/dispatch [::write-message reviewer])}
       (text :t.reviewer-dashboard/message)]]]))

(defn- period-selection []
  (let [period @(rf/subscribe [::period])]
    [:div.d-flex.flex-wrap.align-items-center.mb-3 {:style {:gap "0.5rem"}}
     [:select.form-control {:style {:width :auto}
                            :aria-label (text :t.reviewer-dashboard/period)
                            :value (name (:kind period))
                            :on-change #(rf/dispatch [::set-period (current-period (keyword (.. % -target -value)))])}
      [:option {:value "month"} (text :t.reviewer-dashboard/month)]
      [:option {:value "half"} (text :t.reviewer-dashboard/half)]
      [:option {:value "year"} (text :t.reviewer-dashboard/year)]]
     [:button.btn.btn-secondary.btn-sm {:type :button
                                        :aria-label (text :t.reviewer-calendar/earlier)
                                        :on-click #(rf/dispatch [::set-period (shift-period period -1)])}
      "‹"]
     [:strong {:style {:min-width "9rem" :text-align :center}} (period-title period)]
     [:button.btn.btn-secondary.btn-sm {:type :button
                                        :aria-label (text :t.reviewer-calendar/later)
                                        :on-click #(rf/dispatch [::set-period (shift-period period 1)])}
      "›"]]))

(defn- absence-row [index absence {:keys [on-change on-remove]}]
  (let [id (str "reviewer-absence-" index)]
    [:div.form-row.align-items-end.mb-2
     [:div.col-sm-3
      [:label.small {:for (str id "-start")} (text :t.reviewer-dashboard/absence-start)]
      [:input.form-control {:id (str id "-start")
                            :type :date
                            :value (:start absence)
                            :on-change #(on-change index :start (.. % -target -value))}]]
     [:div.col-sm-3
      [:label.small {:for (str id "-end")} (text :t.reviewer-dashboard/absence-end)]
      [:input.form-control {:id (str id "-end")
                            :type :date
                            :min (:start absence)
                            :value (:end absence)
                            :on-change #(on-change index :end (.. % -target -value))}]]
     [:div.col-sm-4
      [:label.small {:for (str id "-comment")} (text :t.reviewer-dashboard/absence-comment)]
      [:input.form-control {:id (str id "-comment")
                            :type :text
                            :value (or (:comment absence) "")
                            :on-change #(on-change index :comment (.. % -target -value))}]]
     [:div.col-sm-2
      [:button.btn.btn-outline-danger.btn-sm {:type :button
                                              :on-click #(on-remove index)}
       (text :t.reviewer-dashboard/remove)]]]))

(defn absences-editor
  "The rows of the `absences` with date inputs. `on-change` is called with
  the index, the key and the new value."
  [absences {:keys [on-add] :as handlers}]
  [:<>
   (for [[index absence] (indexed absences)]
     ^{:key index}
     [absence-row index absence handlers])
   [:button.btn.btn-secondary.btn-sm.mb-3 {:type :button :on-click on-add}
    (text :t.reviewer-dashboard/add-absence)]])

(defn- edit-form []
  (let [entry @(rf/subscribe [::editing])
        users @(rf/subscribe [::available-users])
        organizations @(rf/subscribe [:organizations])]
    [:div.card.mb-3
     [:div.card-body
      [:h3.h5 (cond
                (:new? entry) (text :t.reviewer-dashboard/add-reviewer)
                (:absences-only? entry) (text-format :t.reviewer-dashboard/absences-of (:name entry))
                :else (text-format :t.reviewer-dashboard/edit-reviewer (:name entry)))]
      (when-not (:absences-only? entry)
        [:div.form-row
         (when (:new? entry)
           [:div.form-group.col-md
            [:label {:for "reviewer-user"} (text :t.administration/reviewer)]
            [dropdown/dropdown
             {:id "reviewer-user"
              :items users
              :item-key :userid
              :item-label :display
              :item-selected? #(= (:userid %) (:userid entry))
              :on-change #(rf/dispatch [::set-field [:userid] (:userid %)])}]])
         [:div.form-group.col-md
          [:label {:for "reviewer-organization"} (text :t.administration/reviewer-organization)]
          [:select.form-control
           {:id "reviewer-organization"
            :value (or (:reviewer/organization entry) "")
            :on-change #(rf/dispatch [::set-field [:reviewer/organization] (.. % -target -value)])}
           [:option {:value ""} ""]
           (for [organization organizations
                 :let [organization-id (:organization/id organization)]]
             ^{:key organization-id}
             [:option {:value organization-id} (localized (:organization/short-name organization))])]]
         [:div.form-group.col-md
          [:label {:for "reviewer-status"} (text :t.administration/reviewer-status-label)]
          [:select.form-control
           {:id "reviewer-status"
            :value (name (:reviewer/status entry))
            :on-change #(rf/dispatch [::set-field [:reviewer/status] (keyword (.. % -target -value))])}
           (for [status statuses]
             ^{:key status}
             [:option {:value (name status)} (localize-reviewer-status status)])]
          [:small.form-text.text-muted (text :t.reviewer-dashboard/status-hint)]]])
      [:h4.h6.mt-2 (text :t.reviewer-dashboard/absences)]
      [:p.small.text-muted (text :t.reviewer-dashboard/absences-hint)]
      [absences-editor (:reviewer/absences entry)
       {:on-change #(rf/dispatch [::set-field [:reviewer/absences %1 %2] %3])
        :on-add #(rf/dispatch [::add-absence])
        :on-remove #(rf/dispatch [::remove-absence %])}]
      [:div.d-flex {:style {:gap "0.5rem"}}
       [:button.btn.btn-primary {:type :button
                                 :disabled (not (valid-entry? entry))
                                 :on-click #(rf/dispatch [::save])}
        (text :t.reviewer-dashboard/save)]
       [:button.btn.btn-secondary {:type :button :on-click #(rf/dispatch [::cancel-edit])}
        (text :t.reviewer-dashboard/cancel)]]]]))

(defn- message-form []
  (let [{:keys [reviewer subject body]} @(rf/subscribe [::message])]
    [:div.card.mb-3
     [:div.card-body
      [:h3.h5 (text-format :t.reviewer-dashboard/message-to (:name reviewer) (:email reviewer))]
      [:div.form-group
       [:label {:for "reviewer-message-subject"} (text :t.reviewer-dashboard/subject)]
       [:input.form-control {:id "reviewer-message-subject"
                             :type :text
                             :value subject
                             :on-change #(rf/dispatch [::set-message-field :subject (.. % -target -value)])}]]
      [:div.form-group
       [:label {:for "reviewer-message-body"} (text :t.reviewer-dashboard/body)]
       [:textarea.form-control {:id "reviewer-message-body"
                                :rows 6
                                :value body
                                :on-change #(rf/dispatch [::set-message-field :body (.. % -target -value)])}]
       [:small.form-text.text-muted (text :t.reviewer-dashboard/message-hint)]]
      [:div.d-flex {:style {:gap "0.5rem"}}
       [:button.btn.btn-primary {:type :button
                                 :disabled (or (str/blank? subject) (str/blank? body))
                                 :on-click #(rf/dispatch [::send-message])}
        (text :t.reviewer-dashboard/send)]
       [:button.btn.btn-secondary {:type :button :on-click #(rf/dispatch [::cancel-message])}
        (text :t.reviewer-dashboard/cancel)]]]]))

(defn reviewer-dashboard
  "The members of the reviewer pool with their statistics, editing and
  messages."
  []
  (let [dashboard @(rf/subscribe [::dashboard])
        loading? @(rf/subscribe [::dashboard :fetching?])
        editing @(rf/subscribe [::editing])
        message @(rf/subscribe [::message])
        options (select-keys dashboard [:may-edit :may-edit-absences :today])]
    [:div
     [:p (text :t.reviewer-dashboard/intro)]
     [flash-message/component :reviewer-dashboard]
     (when editing [edit-form])
     (when message [message-form])
     [period-selection]
     (if (and loading? (not dashboard))
       [spinner/big]
       [:div {:style {:width 0 :min-width "100%" :overflow-x :auto}}
        [:table.table.table-sm {:style {:font-size "0.9rem"}}
         [:thead
          [:tr
           [:th (text :t.reviewer-dashboard/reviewer)]
           [:th (text :t.administration/reviewer-organization)]
           [:th (text :t.administration/reviewer-status-label)]
           [:th (text :t.reviewer-dashboard/absences)]
           [:th.text-right {:title (text :t.reviewer-dashboard/requested-hint)} (text :t.reviewer-dashboard/requested)]
           [:th.text-right {:title (text :t.reviewer-dashboard/done-hint)} (text :t.reviewer-dashboard/done)]
           [:th {:title (text :t.reviewer-dashboard/share-hint)} (text :t.reviewer-dashboard/share)]
           [:th.text-right {:title (text :t.reviewer-dashboard/on-time-hint)} (text :t.reviewer-dashboard/on-time)]
           [:th.text-right {:title (text :t.reviewer-dashboard/average-days-hint)} (text :t.reviewer-dashboard/average-days)]
           [:th.text-right {:title (text :t.reviewer-dashboard/open-hint)} (text :t.reviewer-dashboard/open)]
           [:th]]]
         [:tbody
          (for [reviewer (sort-by (juxt #(= "inactive" (name (:reviewer/status %))) :name) (:reviewers dashboard))]
            ^{:key (:userid reviewer)}
            [reviewer-row reviewer options])]]])
     (when (:may-edit dashboard)
       [:button.btn.btn-primary {:type :button :on-click #(rf/dispatch [::edit nil])}
        (text :t.reviewer-dashboard/add-reviewer)])]))
