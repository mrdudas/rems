(ns rems.actions.designate-supervisor
  (:require [re-frame.core :as rf]
            [rems.actions.components :refer [action-button action-form-view comment-field collapse-action-form perform-action-button]]
            [rems.flash-message :as flash-message]
            [rems.text :refer [text]]
            [rems.util :refer [post!]]))

(rf/reg-event-fx
 ::reset-form
 (fn [_ [_ element-id]]
   {:dispatch [:rems.actions.components/set-comment (str element-id "-comment") ""]}))

;; The API allows us to add attachments to these commands
;; but this is left out from the UI for simplicity
(rf/reg-event-fx
 ::designate-supervisor
 (fn [_ [_ {:keys [collapse-id application-id member comment on-finished]}]]
   (let [description [text :t.actions/designate-supervisor]]
     (post! "/api/applications/designate-supervisor"
            {:params {:application-id application-id
                      :supervisor (select-keys member [:userid])
                      :comment comment}
             :handler (flash-message/default-success-handler
                       :change-members
                       description
                       (fn [_]
                         (collapse-action-form collapse-id)
                         (on-finished)))
             :error-handler (flash-message/default-error-handler :change-members description)}))
   {}))

(defn- qualify-parent-id [parent-id]
  (str parent-id "-supervisor"))

(defn designate-supervisor-action-button [parent-id]
  (let [element-id (qualify-parent-id parent-id)]
    [action-button {:id element-id
                    :text (text :t.actions/designate-supervisor)
                    :on-click #(rf/dispatch [::reset-form element-id])}]))

(defn- designate-supervisor-view
  [{:keys [parent-id on-send]}]
  (let [element-id (qualify-parent-id parent-id)]
    [action-form-view element-id
     (text :t.actions/designate-supervisor)
     [[perform-action-button {:id (str element-id "-submit")
                              :text (text :t.actions/designate-supervisor)
                              :class "btn-primary"
                              :on-click on-send}]]
     [comment-field {:field-key (str element-id "-comment")
                     :label (text :t.form/add-comment)}]
     {:collapse-id parent-id}]))

(defn designate-supervisor-form [parent-id member application-id on-finished]
  (let [element-id (qualify-parent-id parent-id)
        comment @(rf/subscribe [:rems.actions.components/comment (str element-id "-comment")])]
    [designate-supervisor-view {:parent-id parent-id
                                :on-send #(rf/dispatch [::designate-supervisor {:application-id application-id
                                                                                :collapse-id element-id
                                                                                :comment comment
                                                                                :member member
                                                                                :on-finished on-finished}])}]))
