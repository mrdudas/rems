(ns rems.actions.approve-supervision
  (:require [re-frame.core :as rf]
            [rems.actions.components :refer [action-attachment action-button comment-field action-form-view command! perform-action-button]]
            [rems.text :refer [text]]))

(def ^:private action-form-id "approve-supervision")

(rf/reg-event-fx
 ::open-form
 (fn [_ _]
   {:dispatch-n [[:rems.actions.components/set-comment action-form-id ""]
                 [:rems.actions.components/set-attachments action-form-id []]]}))

(rf/reg-event-fx
 ::send-approve-supervision
 (fn [_ [_ {:keys [application-id attachments comment on-finished]}]]
   (command! :application.command/approve-supervision
             {:application-id application-id
              :comment comment
              :attachments attachments}
             {:description [text :t.actions/approve-supervision]
              :collapse action-form-id
              :on-finished on-finished})
   {}))

(defn approve-supervision-action-button []
  [action-button {:id action-form-id
                  :text (text :t.actions/approve-supervision)
                  :on-click #(rf/dispatch [::open-form])}])

(defn approve-supervision-view
  [{:keys [application-id on-send]}]
  [action-form-view action-form-id
   (text :t.actions/approve-supervision)
   [[perform-action-button {:id "approve-supervision"
                            :text (text :t.actions/approve-supervision)
                            :class "btn-primary"
                            :on-click on-send}]]
   [:div
    (text :t.actions/approve-supervision-intro)
    [comment-field {:field-key action-form-id
                    :label (text :t.form/add-comment)}]
    [action-attachment {:field-key action-form-id
                        :application-id application-id}]]])

(defn approve-supervision-form [application-id on-finished]
  (let [comment @(rf/subscribe [:rems.actions.components/comment action-form-id])
        attachments @(rf/subscribe [:rems.actions.components/attachments action-form-id])]
    [approve-supervision-view {:application-id application-id
                              :on-send #(rf/dispatch [::send-approve-supervision {:application-id application-id
                                                                                 :comment comment
                                                                                 :attachments attachments
                                                                                 :on-finished on-finished}])}]))
