(ns rems.reviewer-absences
  "EPKEB: the absences of the current user as a member of the reviewer pool,
  on the profile page. See `:reviewer-pool-organization`."
  (:require [medley.core :refer [remove-nth]]
            [re-frame.core :as rf]
            [rems.collapsible :as collapsible]
            [rems.common.util :refer [conj-vec]]
            [rems.fetcher :as fetcher]
            [rems.flash-message :as flash-message]
            [rems.globals]
            [rems.reviewer-dashboard :refer [absences-editor absences-params valid-absence?]]
            [rems.text :refer [text]]
            [rems.util :refer [put!]]))

(defn- fetch-success [result]
  (rf/dispatch [::set-absences (vec (:reviewer/absences result))]))

(fetcher/reg-fetcher ::my-absences "/api/reviewer-pool/my-absences" {:on-success fetch-success})

(rf/reg-event-fx
 ::enter-page
 (fn [_ _]
   (when (:reviewer-pool-organization @rems.globals/config)
     {:dispatch [::my-absences]})))

(rf/reg-sub ::absences (fn [db _] (::absences db)))
(rf/reg-event-db ::set-absences (fn [db [_ absences]] (assoc db ::absences absences)))
(rf/reg-event-db ::set-field (fn [db [_ index k value]] (assoc-in db [::absences index k] value)))
(rf/reg-event-db ::add-absence (fn [db _] (update db ::absences conj-vec {:start "" :end "" :comment ""})))
(rf/reg-event-db ::remove-absence (fn [db [_ index]] (update db ::absences #(vec (remove-nth index %)))))

(rf/reg-event-fx
 ::save
 (fn [{:keys [db]} _]
   (let [description [text :t.reviewer-dashboard/save]]
     (put! "/api/reviewer-pool/absences"
           {:params {:userid (:userid @rems.globals/user)
                     :reviewer/absences (absences-params (::absences db))}
            :handler (flash-message/default-success-handler :reviewer-absences description
                                                            (fn [_] (rf/dispatch [::my-absences])))
            :error-handler (flash-message/default-error-handler :reviewer-absences description)}))
   {}))

(defn my-absences
  "The absences of the current user, when the user is an active member of
  the reviewer pool."
  []
  (let [reviewer @(rf/subscribe [::my-absences])
        absences @(rf/subscribe [::absences])]
    (when (and reviewer (not= "inactive" (name (:reviewer/status reviewer))))
      [collapsible/component
       {:id "reviewer-absences"
        :open? true
        :title (text :t.reviewer-dashboard/my-absences)
        :always [:div
                 [:p (text :t.reviewer-dashboard/my-absences-intro)]
                 [flash-message/component :reviewer-absences]
                 [absences-editor absences
                  {:on-change #(rf/dispatch [::set-field %1 %2 %3])
                   :on-add #(rf/dispatch [::add-absence])
                   :on-remove #(rf/dispatch [::remove-absence %])}]
                 [:div
                  [:button.btn.btn-primary {:type :button
                                            :disabled (not (every? valid-absence? absences))
                                            :on-click #(rf/dispatch [::save])}
                   (text :t.reviewer-dashboard/save)]]]}])))
