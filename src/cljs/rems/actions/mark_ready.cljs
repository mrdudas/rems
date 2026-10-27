(ns rems.actions.mark-ready
  (:require [re-frame.core :as rf]
            [rems.actions.components :refer [action-attachment action-button comment-field action-form-view command! perform-action-button]]
            [rems.text :refer [text]]))

(def ^:private action-form-id "mark-ready")

(rf/reg-event-fx
 ::open-form
 (fn [_ _]
   {:dispatch-n [[:rems.actions.components/set-comment action-form-id ""]
                 [:rems.actions.components/set-attachments action-form-id []]]}))

(rf/reg-event-fx
 ::send-mark-ready
 (fn [_ [_ {:keys [application-id attachments comment on-finished]}]]
   (command! :application.command/mark-ready
             {:application-id application-id
              :comment comment
              :attachments attachments}
             {:description [text :t.actions/mark-ready]
              :collapse action-form-id
              :on-finished on-finished})
   {}))

(defn mark-ready-action-button []
  [action-button {:id action-form-id
                  :text (text :t.actions/mark-ready)
                  :on-click #(rf/dispatch [::open-form])}])

(defn mark-ready-view
  [{:keys [application-id on-send]}]
  [action-form-view action-form-id
   (text :t.actions/mark-ready)
   [[perform-action-button {:id "mark-ready"
                            :text (text :t.actions/mark-ready)
                            :class "btn-primary"
                            :on-click on-send}]]
   [:div
    (text :t.actions/mark-ready-intro)
    [comment-field {:field-key action-form-id
                    :label (text :t.form/add-comment)}]
    [action-attachment {:field-key action-form-id
                        :application-id application-id}]]])

(defn mark-ready-form [application-id on-finished]
  (let [comment @(rf/subscribe [:rems.actions.components/comment action-form-id])
        attachments @(rf/subscribe [:rems.actions.components/attachments action-form-id])]
    [mark-ready-view {:application-id application-id
                     :on-send #(rf/dispatch [::send-mark-ready {:application-id application-id
                                                               :comment comment
                                                               :attachments attachments
                                                               :on-finished on-finished}])}]))
