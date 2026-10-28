(ns rems.service.permits
  "EPKEB: issuing and revoking the approvals (permits) of approved
  applications, see docs/epkeb/virtualis-engedely.md and `:enable-permits`."
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [rems.common.application-util :as application-util]
            [rems.config :refer [env]]
            [rems.db.applications]
            [rems.db.organizations]
            [rems.db.permits]
            [rems.db.users]))

(defn- field-value
  "The answer to the field `field-id` of the forms of the application."
  [application field-id]
  (some (fn [form]
          (some #(when (= field-id (:field/id %))
                   (not-empty (str (:field/value %))))
                (:form/fields form)))
        (:application/forms application)))

(defn- researchers
  "The researchers are the members other than the supervisor, or the applicant
  if there are no other members."
  [application]
  (let [supervisor (get-in application [:application/supervisor :userid])
        members (remove #(= supervisor (:userid %)) (:application/members application))]
    (->> (if (seq members) members [(:application/applicant application)])
         (map application-util/get-member-name)
         sort
         vec)))

(defn permit-data
  "The data of the permit at the time of issuing, so that the permit does not
  change when the application does."
  [application approver]
  (let [organization (some-> (get-in application [:application/organization :organization/id])
                             rems.db.organizations/get-organization-by-id-raw)
        original-approvals (field-value application "original-approval")]
    (cond-> {:permit/type (if original-approvals "abbreviated" "normal")
             :permit/language (or (field-value application "language") "hu")
             :permit/institution (:organization/name organization)
             :permit/researchers (researchers application)
             :permit/title (or (not-empty (:application/description application))
                               (field-value application "title"))
             :permit/approver (application-util/get-member-name approver)}
      ;; the permit names the co-chairs of the time of issuing, see :permit-co-chairs
      (:permit-co-chairs env) (assoc :permit/co-chairs (:permit-co-chairs env))
      (field-value application "department") (assoc :permit/department (field-value application "department"))
      (:application/supervisor application) (assoc :permit/supervisor (application-util/get-member-name (rems.db.users/get-user (get-in application [:application/supervisor :userid]))))
      original-approvals (assoc :permit/original-approvals original-approvals))))

(defn handle-events
  "Process manager: issues the permit when an application is approved, and
  marks it revoked when the application is revoked. Returns no commands."
  [new-events]
  (when (:enable-permits env)
    (doseq [event new-events]
      (case (:event/type event)
        :application.event/approved
        (let [application-id (:application/id event)]
          (when-not (rems.db.permits/get-permit-for-application application-id)
            (let [application (rems.db.applications/get-application-internal application-id)
                  approver (rems.db.users/get-user (:event/actor event))
                  number (rems.db.permits/issue-permit! application-id
                                                        (:event/time event)
                                                        (permit-data application approver))]
              (log/info "Issued permit" number "for application" application-id))))

        :application.event/revoked
        (rems.db.permits/revoke-permit! (:application/id event) (:event/time event))

        nil)))
  [])

(defn permit-url [permit]
  (str (:public-url env) "permit/" (:permit/token permit)))

(defn get-permit-for-application
  "The permit of the application with its public link, for the users who can
  see the application."
  [application-id]
  (when-some [permit (rems.db.permits/get-permit-for-application application-id)]
    (assoc permit :permit/url (permit-url permit))))

(defn researchers-text [permit]
  (str/join ", " (:permit/researchers permit)))
