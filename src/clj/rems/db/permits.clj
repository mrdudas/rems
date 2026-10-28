(ns rems.db.permits
  "EPKEB: the approvals (permits) issued for approved applications, see
  docs/epkeb/virtualis-engedely.md"
  (:require [clj-time.core :as time]
            [rems.config :refer [env]]
            [rems.db.core :as db]
            [rems.json :as json]
            [rems.util :refer [secure-token]]))

(defn format-number [year sequence]
  (format "%d-%03d" year sequence))

(defn- format-permit [row]
  (when row
    (merge (json/parse-string (:data row))
           {:permit/token (:token row)
            :permit/number (format-number (:year row) (:sequence row))
            :permit/application-id (:appid row)
            :permit/issued (:issued row)
            :permit/revoked (:revoked row)})))

(defn- next-sequence
  "The next sequence number of the year. Numbering can continue a manual
  numbering with `:permit-first-number`, e.g. {:year 2026 :sequence 229}."
  [year]
  (let [max-sequence (or (:max (db/get-permit-max-sequence {:year year})) 0)
        first-number (:permit-first-number env)
        first-sequence (if (= year (:year first-number))
                         (:sequence first-number 1)
                         1)]
    (max (inc max-sequence) first-sequence)))

(defn issue-permit!
  "Issues a permit with the next reference number of the year of `issued`.
  The caller holds the application event table lock (see
  rems.service.command/command!), so the numbers cannot collide."
  [application-id issued data]
  (let [year (time/year issued)
        sequence (next-sequence year)]
    (db/add-permit! {:token (secure-token)
                     :year year
                     :sequence sequence
                     :application application-id
                     :issued issued
                     :data (json/generate-string data)})
    (format-number year sequence)))

(defn get-permit-by-token [token]
  (format-permit (db/get-permit-by-token {:token token})))

(defn get-permit-for-application [application-id]
  (format-permit (db/get-permit-by-application {:application application-id})))

(defn revoke-permit! [application-id revoked]
  (db/revoke-permit! {:application application-id :revoked revoked}))
