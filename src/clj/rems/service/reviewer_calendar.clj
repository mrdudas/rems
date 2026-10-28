(ns rems.service.reviewer-calendar
  "The reviewer calendar: the open reviews of the members of the reviewer pool
  with their deadlines, and the applications waiting for reviewers."
  (:require [clj-time.core :as time]
            [rems.config :refer [env]]
            [rems.db.applications]
            [rems.db.organizations]
            [rems.db.users]))

(def ^:private requestable-statuses #{:active :limited})

(defn- handler? [application userid]
  (contains? (get-in application [:application/user-roles userid]) :handler))

(defn- submitted? [application]
  (= :application.state/submitted (:application/state application)))

(defn- event-userids [users]
  ;; the users of enriched events are users, not userids
  (map #(if (map? %) (:userid %) %) users))

(defn review-deadline-days [application request-time]
  ;; a review requested after the application was returned for changes is a
  ;; re-review of the changed application
  (if (some #(and (= :application.event/returned (:event/type %))
                  (time/before? (:event/time %) request-time))
            (:application/events application))
    (:re-review-deadline-days env)
    (:review-deadline-days env)))

(defn open-reviews
  "The reviews requested from `userid` that are not done yet, i.e. the user
  still has the reviewer role, with the time of the latest request."
  [application]
  (let [latest-request (into {}
                             (for [event (:application/events application)
                                   :when (= :application.event/review-requested (:event/type event))
                                   userid (event-userids (:application/reviewers event))]
                               [userid (:event/time event)]))]
    (for [[userid roles] (:application/user-roles application)
          :when (contains? roles :reviewer)
          :let [requested (latest-request userid)]
          :when requested]
      (let [days (review-deadline-days application requested)
            deadline (when days (time/plus requested (time/days days)))
            now (time/now)]
        {:userid userid
         :application/id (:application/id application)
         :application/external-id (:application/external-id application)
         :application/description (:application/description application)
         :review/requested requested
         :review/deadline deadline
         :review/overdue-days (if (and deadline (time/after? now deadline))
                                (time/in-days (time/interval deadline now))
                                0)}))))

(defn reviewers-since-submission
  "The users requested to review since the latest submission."
  [application]
  (let [events (:application/events application)
        submitted (last (filter #(= :application.event/submitted (:event/type %)) events))]
    (->> events
         (filter #(and (= :application.event/review-requested (:event/type %))
                       submitted
                       (not (time/before? (:event/time %) (:event/time submitted)))))
         (mapcat (comp event-userids :application/reviewers))
         distinct)))

(defn get-calendar
  "The reviewer calendar for the handler `userid`, or nil when the reviewer
  pool is not in use. Only the applications that the user handles are shown.

  `:reviewers` - the active members of the pool, each with `:reviews`, the
                 open reviews with the request time, the deadline and the
                 days overdue
  `:waiting-applications` - the submitted applications that can still be
                            sent to reviewers, with the reviewers requested
                            since the latest submission"
  [userid]
  (when-some [pool (rems.db.organizations/get-reviewer-pool (:reviewer-pool-organization env))]
    (let [applications (->> (rems.db.applications/get-all-unrestricted-applications)
                            (filter #(handler? % userid))
                            (filter submitted?))
          reviews (group-by :userid (mapcat open-reviews applications))]
      {:reviewers (vec
                   (for [entry pool
                         :when (or (contains? requestable-statuses (:reviewer/status entry))
                                   (seq (reviews (:userid entry))))
                         :let [user (rems.db.users/get-user (:userid entry))]
                         :when user]
                     (assoc user
                            :reviewer/organization (:reviewer/organization entry)
                            :reviewer/status (:reviewer/status entry)
                            :reviews (->> (reviews (:userid entry))
                                          (map #(dissoc % :userid))
                                          (sort-by :review/requested)
                                          vec))))
       :waiting-applications (vec
                              (for [application (sort-by :application/id applications)
                                    :when (contains? (get-in application [:application/role-permissions :handler])
                                                     :application.command/request-review)]
                                {:application/id (:application/id application)
                                 :application/external-id (:application/external-id application)
                                 :application/description (:application/description application)
                                 :application/organization (get-in application [:application/organization :organization/id])
                                 :application/reviewers (vec (reviewers-since-submission application))}))})))
