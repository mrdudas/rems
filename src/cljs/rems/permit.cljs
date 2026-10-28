(ns rems.permit
  "EPKEB: the permit (approval) of an approved application, see
  `:enable-permits`."
  (:require [re-frame.core :as rf]
            [rems.collapsible :as collapsible]
            [rems.fetcher :as fetcher]
            [rems.globals]
            [rems.text :refer [localize-time text text-format]]))

(fetcher/reg-fetcher ::permit "/api/applications/:id/permit" {:path-params (fn [db] {:id (::application-id db)})})

(rf/reg-sub ::application-id (fn [db _] (::application-id db)))

(rf/reg-event-fx
 ::load
 (fn [{:keys [db]} [_ application-id]]
   {:db (-> db
            (assoc ::application-id application-id)
            (dissoc ::permit))
    :dispatch [::permit]}))

(defn- approved-or-revoked? [application]
  (contains? #{:application.state/approved :application.state/revoked}
             (:application/state application)))

(defn permit-card [application]
  (when (and (:enable-permits @rems.globals/config)
             (approved-or-revoked? application))
    (let [application-id (:application/id application)
          loaded? (= application-id @(rf/subscribe [::application-id]))
          permit (when loaded? @(rf/subscribe [::permit]))]
      (when-not loaded?
        (rf/dispatch [::load application-id]))
      (when (:permit/number permit)
        [collapsible/component
         {:id "permit-collapsible"
          :title (text :t.permit/title)
          :always [:div
                   [:p
                    [:strong (:permit/number permit)]
                    " – "
                    (if (:permit/revoked permit)
                      [:span {:style {:color "#CD214F" :font-weight 700}}
                       (text-format :t.permit/revoked (localize-time (:permit/revoked permit)))]
                      [:span {:style {:color "#1E5E3A" :font-weight 700}} (text :t.permit/valid)])]
                   [:p (text :t.permit/share)]
                   [:p [:a {:href (:permit/url permit) :target "_blank"} (:permit/url permit)]]
                   [:a.btn.btn-primary {:href (str (:permit/url permit) "/pdf") :target "_blank"} (text :t.permit/download)]]}]))))
