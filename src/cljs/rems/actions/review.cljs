(ns rems.actions.review
  (:require [re-frame.core :as rf]
            [rems.actions.components :refer [action-attachment action-button comment-field action-form-view command! perform-action-button]]
            [rems.globals]
            [rems.text :refer [text]]))

(def ^:private action-form-id "review")

(rf/reg-sub ::recommendation (fn [db _] (::recommendation db)))
(rf/reg-event-db ::set-recommendation (fn [db [_ value]] (assoc db ::recommendation value)))

(rf/reg-event-fx
 ::open-form
 (fn [_ _]
   {:dispatch-n [[::set-recommendation nil]
                 [:rems.actions.components/set-comment action-form-id ""]
                 [:rems.actions.components/set-attachments action-form-id []]]}))

(rf/reg-event-fx
 ::send-review
 (fn [_ [_ {:keys [application-id comment attachments recommendation on-finished]}]]
   (command! :application.command/review
             (cond-> {:application-id application-id
                      :comment comment
                      :attachments attachments}
               recommendation (assoc :recommendation recommendation))
             {:description [text :t.actions/review]
              :collapse action-form-id
              :on-finished on-finished})
   {}))

(defn review-action-button []
  [action-button {:id action-form-id
                  :text (text :t.actions/review)
                  :on-click #(rf/dispatch [::open-form])}])

;; see :enable-review-recommendations
(def ^:private recommendations
  [[:approve :t.recommendations/approve]
   [:revise :t.recommendations/revise]
   [:reject :t.recommendations/reject]
   [:not-competent :t.recommendations/not-competent]])

(defn- recommendation-field [selected]
  [:fieldset.form-group
   [:legend.col-form-label (text :t.recommendations/label)]
   (for [[value label] recommendations
         :let [id (str "review-recommendation-" (name value))]]
     ^{:key value}
     [:div.form-check
      [:input.form-check-input {:type :radio
                                :id id
                                :name "review-recommendation"
                                :checked (= value selected)
                                :on-change #(rf/dispatch [::set-recommendation value])}]
      [:label.form-check-label {:for id} (text label)]])])

(defn review-view
  [{:keys [application-id disabled on-send recommendation]}]
  [action-form-view action-form-id
   (text :t.actions/review)
   [[perform-action-button {:id "review-button"
                            :text (text :t.actions/review)
                            :class "btn-primary"
                            :disabled disabled
                            :on-click on-send}]]
   [:<>
    (when (:enable-review-recommendations @rems.globals/config)
      [recommendation-field recommendation])
    [comment-field {:field-key action-form-id
                    :label (text :t.form/add-comments-not-shown-to-applicant)}]
    [action-attachment {:field-key action-form-id
                        :application-id application-id}]]])

(defn review-form [application-id on-finished]
  (let [comment @(rf/subscribe [:rems.actions.components/comment action-form-id])
        attachments @(rf/subscribe [:rems.actions.components/attachments action-form-id])
        recommendation @(rf/subscribe [::recommendation])
        recommendations? (:enable-review-recommendations @rems.globals/config)]
    [review-view {:application-id application-id
                  :recommendation recommendation
                  :disabled (if recommendations?
                              (nil? recommendation)
                              (and (empty? comment) (empty? attachments)))
                  :on-send #(rf/dispatch [::send-review {:application-id application-id
                                                         :comment comment
                                                         :attachments attachments
                                                         :recommendation (when recommendations? recommendation)
                                                         :on-finished on-finished}])}]))
