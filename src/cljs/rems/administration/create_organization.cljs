(ns rems.administration.create-organization
  (:require [clojure.string :as str]
            [medley.core :refer [indexed map-vals remove-nth]]
            [re-frame.core :as rf]
            [rems.administration.administration :as administration]
            [rems.administration.components :refer [localized-text-field perform-action-button text-field]]
            [rems.administration.items :as items]
            [rems.atoms :as atoms :refer [enrich-user document-title]]
            [rems.common.util :refer [+email-regex+ conj-vec]]
            [rems.collapsible :as collapsible]
            [rems.config :as config]
            [rems.dropdown :as dropdown]
            [rems.fetcher :as fetcher]
            [rems.flash-message :as flash-message]
            [rems.globals]
            [rems.spinner :as spinner]
            [rems.text :refer [localize-reviewer-status localized text text-format]]
            [rems.util :refer [navigate! post! put! trim-when-string]]))

(rf/reg-event-fx
 ::enter-page
 (fn [{:keys [db]} [_ organization-id]]
   {:db (assoc db
               ::organization-id organization-id
               ::available-owners nil
               ::editing? (some? organization-id)
               ::form {:type :organization/default})
    :dispatch-n [[::available-owners]
                 (when organization-id [::organization])]}))

(rf/reg-sub ::organization-id (fn [db _] (::organization-id db)))
(rf/reg-sub ::editing? (fn [db _] (::editing? db)))

;;; form state

(rf/reg-sub ::form (fn [db _] (::form db)))
(rf/reg-event-db ::set-form-field (fn [db [_ keys value]] (assoc-in db (concat [::form] keys) value)))

(rf/reg-event-db
 ::fetch-organization-success
 (fn [db [_ organization]]
   (update db ::form merge organization)))

(fetcher/reg-fetcher ::organization "/api/organizations/:id" {:path-params (fn [db] {:id (::organization-id db)})
                                                              :on-success #(rf/dispatch [::fetch-organization-success %])})

;;; form submit

(defn- valid-review-email? [languages review-email]
  (and (when (map? (:name review-email))
         (not-any? str/blank? (vals (:name review-email))))
       (= (set languages)
          (set (keys (:name review-email))))
       (not (str/blank? (:email review-email)))
       (re-matches +email-regex+ (:email review-email))))

(defn- valid-owner? [owner]
  (not (str/blank? owner)))

(defn- valid-localized-string? [localized-string]
  (some (comp not str/blank?) (vals localized-string)))

(defn- valid-create-request? [request languages]
  (and
   (not (str/blank? (:organization/id request)))
   (valid-localized-string? (:organization/short-name request))
   (valid-localized-string? (:organization/name request))
   (every? valid-owner? (:organization/owners request))
   (every? (partial valid-review-email? languages) (:organization/review-emails request))))

(defn- build-review-email [review-email]
  {:name (when (map? (:name review-email))
           (map-vals trim-when-string (:name review-email)))
   :email (trim-when-string (:email review-email))})

(defn- build-users [users]
  (mapv #(select-keys % [:userid]) users))

(defn- build-reviewers [reviewers]
  (->> reviewers
       (filter :userid)
       (mapv #(-> %
                  (select-keys [:userid :reviewer/organization :reviewer/status])
                  (update :reviewer/status (fnil keyword :active))))))

(defn- valid-reviewers? [reviewers]
  (every? #(and (:userid %) (not (str/blank? (:reviewer/organization %))))
          reviewers))

(defn build-create-request [form languages]
  (let [request {:organization/id (trim-when-string (:organization/id form))
                 :organization/short-name (map-vals trim-when-string (:organization/short-name form))
                 :organization/name (map-vals trim-when-string (:organization/name form))
                 :organization/owners (build-users (:organization/owners form))
                 :organization/contacts (build-users (:organization/contacts form))
                 :organization/chairs (build-users (:organization/chairs form))
                 :organization/reviewers (build-reviewers (:organization/reviewers form))
                 :organization/review-emails (mapv build-review-email (:organization/review-emails form))}]
    (when (and (valid-create-request? request languages)
               (valid-reviewers? (:organization/reviewers form)))
      request)))

(defn- valid-edit-request? [request languages]
  (and (valid-localized-string? (:organization/short-name request))
       (valid-localized-string? (:organization/name request))
       (every? valid-owner? (:organization/owners request))
       (every? (partial valid-review-email? languages) (:organization/review-emails request))))

(defn build-edit-request [id form languages]
  (let [request {:organization/id (:organization/id form)
                 :organization/short-name (map-vals trim-when-string (:organization/short-name form))
                 :organization/name (map-vals trim-when-string (:organization/name form))
                 :organization/owners (build-users (:organization/owners form))
                 :organization/contacts (build-users (:organization/contacts form))
                 :organization/chairs (build-users (:organization/chairs form))
                 :organization/reviewers (build-reviewers (:organization/reviewers form))
                 :organization/review-emails (mapv build-review-email (:organization/review-emails form))}]
    (when (and (valid-edit-request? request languages)
               (valid-reviewers? (:organization/reviewers form)))
      request)))

(rf/reg-event-fx
 ::create-organization
 (fn [_ [_ request]]
   (let [description [text :t.administration/create-organization]]
     (post! "/api/organizations/create"
            {:params request
             :handler (flash-message/default-success-handler
                       :top description #(do (rems.config/fetch-organizations!)
                                             (navigate! (str "/administration/organizations/" (:organization/id %)))))
             :error-handler (flash-message/default-error-handler :top description)}))
   {}))

(rf/reg-event-fx
 ::edit-organization
 (fn [_ [_ request]]
   (let [description [text :t.administration/edit-organization]]
     (put! "/api/organizations/edit"
           {:params request
            :handler (flash-message/default-success-handler
                      :top description #(do (rems.config/fetch-organizations!)
                                            (navigate! (str "/administration/organizations/" (:organization/id %)))))
            :error-handler (flash-message/default-error-handler :top description)}))
   {}))

(rf/reg-event-db ::set-owners (fn [db [_ owners]] (assoc-in db [::form :organization/owners] (sort-by :userid owners))))
(rf/reg-event-db ::set-users (fn [db [_ k users]] (assoc-in db [::form k] (sort-by :userid users))))

(fetcher/reg-fetcher ::available-owners "/api/organizations/available-owners" {:result (partial mapv enrich-user)})



 ;;;; UI

(def ^:private context
  {:get-form ::form
   :update-form ::set-form-field})

(def ^:private owners-dropdown-id "owners-dropdown")

(defn- organization-id-field []
  [text-field context {:keys [:organization/id]
                       :label (text :t.administration/id)
                       :readonly @(rf/subscribe [::editing?])}])

(defn- organization-short-name-field []
  [localized-text-field context {:keys [:organization/short-name]
                                 :label (text :t.administration/short-name)}])

(defn- organization-name-field []
  [localized-text-field context {:keys [:organization/name]
                                 :label (text :t.administration/title)}])

(defn- save-organization []
  (let [form @(rf/subscribe [::form])
        id @(rf/subscribe [::organization-id])
        languages @rems.config/languages
        request (if id
                  (build-edit-request id form languages)
                  (build-create-request form languages))]
    (atoms/save-action {:id :save
                        :disabled (nil? request)
                        :on-click (when request
                                    (if id
                                      #(rf/dispatch [::edit-organization request])
                                      #(rf/dispatch [::create-organization request])))})))

(defn- cancel-button []
  [atoms/link {:class "btn btn-secondary"}
   (if @(rf/subscribe [::editing?])
     (str "/administration/organizations/" @(rf/subscribe [::organization-id]))
     "/administration/organizations")
   (text :t.administration/cancel)])

(defn- organization-owners-field []
  (let [form @(rf/subscribe [::form])
        all-owners @(rf/subscribe [::available-owners])
        selected-owners (set (map :userid (get-in form [:organization/owners])))
        organization-id (:organization/id form)
        org-owner? (->> @(rf/subscribe [:owned-organizations])
                        (some (comp #{organization-id} :organization/id)))]
    [:div.form-group
     [:label.administration-field-label {:for owners-dropdown-id}
      (text-format :t.label/optional (text :t.administration/owners))]
     [dropdown/dropdown
      {:id owners-dropdown-id
       :items all-owners
       :item-key :userid
       :item-label :display
       :item-selected? #(contains? selected-owners (% :userid))
       :multi? true
       :disabled? (and @(rf/subscribe [::editing?])
                       (or (not org-owner?)
                           (and (:restrict-organization-owners @rems.globals/config)
                                (not (contains? (set @rems.globals/roles) :owner)))))
       :on-change #(rf/dispatch [::set-owners %])}]]))

(defn- organization-users-field
  "Multi-select of users for the organization key `k`, e.g. `:organization/contacts`."
  [{:keys [id k label]}]
  (let [form @(rf/subscribe [::form])
        all-users @(rf/subscribe [::available-owners])
        selected-users (set (map :userid (get form k)))]
    [:div.form-group
     [:label.administration-field-label {:for id}
      (text-format :t.label/optional label)]
     [dropdown/dropdown
      {:id id
       :items all-users
       :item-key :userid
       :item-label :display
       :item-selected? #(contains? selected-users (% :userid))
       :multi? true
       :on-change #(rf/dispatch [::set-users k %])}]]))

(defn- organization-contacts-field []
  [organization-users-field {:id "contacts-dropdown"
                             :k :organization/contacts
                             :label (text :t.administration/contacts)}])

(defn- organization-chairs-field []
  [organization-users-field {:id "chairs-dropdown"
                             :k :organization/chairs
                             :label (text :t.administration/chairs)}])

(defn- remove-review-email-button [field-index]
  [items/remove-button {:on-click #(rf/dispatch [::remove-review-email field-index])
                        :data-index field-index}])

(rf/reg-event-db
 ::add-review-email
 (fn [db [_]]
   (update-in db [::form :organization/review-emails] conj-vec {})))


(rf/reg-event-db
 ::remove-review-email
 (fn [db [_ field-index]]
   (update-in db
              [::form :organization/review-emails]
              (fn [review-emails]
                (vec (remove-nth field-index review-emails))))))


(defn- organization-review-email-field [field-index]
  [:div.form-field
   [:div.form-field-header
    [:h4 (text-format :t.administration/review-email-n (inc field-index))]
    [:div.form-field-controls [remove-review-email-button field-index]]]
   [:div.row
    [:div.col-md
     [localized-text-field context {:keys [:organization/review-emails field-index :name]
                                    :label (text :t.administration/name)}]]
    [:div.col-md
     [text-field context {:keys [:organization/review-emails field-index :email]
                          :label (text :t.administration/email)}]]]])

(defn- organization-review-emails-field []
  (let [form @(rf/subscribe [::form])]
    [:div.form-group
     [:label.administration-field-label
      {:for :review-emails}
      (str (text :t.administration/review-emails) " " (text :t.administration/optional))]
     (for [[field-index _review-email] (indexed (:organization/review-emails form))]
       ^{:key field-index} [organization-review-email-field field-index])
     [:div.dashed-group.text-center
      [:a#add-review-email {:href "#"
                            :on-click (fn [event]
                                        (.preventDefault event)
                                        (rf/dispatch [::add-review-email]))}
       (text :t.administration/add)]]]))

;; the reviewer pool of the `:reviewer-pool-organization`

(def ^:private reviewer-statuses [:active :limited :paused :inactive])

(rf/reg-event-db
 ::add-reviewer
 (fn [db [_]]
   (update-in db [::form :organization/reviewers] conj-vec {:reviewer/status :active})))

(rf/reg-event-db
 ::remove-reviewer
 (fn [db [_ index]]
   (update-in db [::form :organization/reviewers] #(vec (remove-nth index %)))))

(rf/reg-event-db
 ::set-reviewer
 (fn [db [_ index k value]]
   (assoc-in db [::form :organization/reviewers index k] value)))

(defn- reviewer-row [index reviewer]
  (let [users @(rf/subscribe [::available-owners])
        organizations @(rf/subscribe [:organizations])
        id (str "reviewer-" index)]
    [:div.form-field
     [:div.form-field-header
      [:h4 (text-format :t.administration/reviewer-n (inc index))]
      [:div.form-field-controls
       [items/remove-button {:on-click #(rf/dispatch [::remove-reviewer index])
                             :data-index index}]]]
     [:div.row
      [:div.col-md
       [:label.administration-field-label {:for (str id "-user")} (text :t.administration/reviewer)]
       [dropdown/dropdown
        {:id (str id "-user")
         :items users
         :item-key :userid
         :item-label :display
         :item-selected? #(= (:userid %) (:userid reviewer))
         :on-change #(rf/dispatch [::set-reviewer index :userid (:userid %)])}]]
      [:div.col-md
       [:label.administration-field-label {:for (str id "-organization")} (text :t.administration/reviewer-organization)]
       [:select.form-control
        {:id (str id "-organization")
         :value (or (:reviewer/organization reviewer) "")
         :on-change #(rf/dispatch [::set-reviewer index :reviewer/organization (.. % -target -value)])}
        [:option {:value ""} ""]
        (for [organization organizations
              :let [organization-id (:organization/id organization)]]
          ^{:key organization-id}
          [:option {:value organization-id} (localized (:organization/short-name organization))])]]
      [:div.col-md
       [:label.administration-field-label {:for (str id "-status")} (text :t.administration/reviewer-status-label)]
       [:select.form-control
        {:id (str id "-status")
         :value (name (or (:reviewer/status reviewer) :active))
         :on-change #(rf/dispatch [::set-reviewer index :reviewer/status (keyword (.. % -target -value))])}
        (for [status reviewer-statuses]
          ^{:key status}
          [:option {:value (name status)} (localize-reviewer-status status)])]]]]))

(defn- organization-reviewers-field []
  (let [form @(rf/subscribe [::form])]
    [:div.form-group
     [:label.administration-field-label {:for :reviewers}
      (text :t.administration/reviewer-pool)]
     [:p (text :t.administration/reviewer-pool-explanation)]
     (for [[index reviewer] (indexed (:organization/reviewers form))]
       ^{:key index} [reviewer-row index reviewer])
     [:div.dashed-group.text-center
      [:a#add-reviewer {:href "#"
                        :on-click (fn [event]
                                    (.preventDefault event)
                                    (rf/dispatch [::add-reviewer]))}
       (text :t.administration/add)]]]))

(defn- reviewer-pool-organization? [form]
  (let [pool-organization (:reviewer-pool-organization @rems.globals/config)]
    (and pool-organization
         (= pool-organization (:organization/id form)))))

(defn create-organization-page []
  (let [loading? (or @(rf/subscribe [::available-owners :fetching?])
                     @(rf/subscribe [::organization :fetching?]))
        editing? @(rf/subscribe [::editing?])
        title (if editing?
                (text :t.administration/edit-organization)
                (text :t.administration/create-organization))]
    [:div
     [administration/navigator]
     [document-title title]
     [flash-message/component :top]
     [collapsible/component
      {:id "create-organization"
       :title title
       :always (if loading?
                 [:div#organization-loader [spinner/big]]
                 [:div#organization-editor.fields
                  [organization-id-field]
                  [organization-short-name-field]
                  [organization-name-field]
                  [organization-owners-field]
                  (when (:enable-organization-contacts @rems.globals/config)
                    [:<>
                     [organization-contacts-field]
                     [organization-chairs-field]])
                  (when (reviewer-pool-organization? @(rf/subscribe [::form]))
                    [organization-reviewers-field])
                  [organization-review-emails-field]
                  [:div.col.commands
                   [cancel-button]
                   [perform-action-button (save-organization)]]])}]]))
