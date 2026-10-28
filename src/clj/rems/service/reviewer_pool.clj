(ns rems.service.reviewer-pool
  "The reviewer pool of the `:reviewer-pool-organization`: who can be requested
  to review an application, with statistics on their workload."
  (:require [clj-time.core :as time]
            [rems.config :refer [env]]
            [rems.db.applications]
            [rems.db.organizations]
            [rems.db.users]))

(def ^:private requestable-statuses #{:active :limited})

(defn- this-year? [year t]
  (and t (= year (time/year t))))

(defn- review-requests [applications]
  (for [application applications
        event (:application/events application)
        :when (= :application.event/review-requested (:event/type event))
        reviewer (:application/reviewers event)]
    ;; the reviewers of enriched events are users, not userids
    {:userid (if (map? reviewer) (:userid reviewer) reviewer)
     :time (:event/time event)}))

(defn- reviewer-stats [applications year]
  (let [requests (group-by :userid (review-requests applications))
        in-progress (frequencies
                     (for [application applications
                           :when (= :application.state/submitted (:application/state application))
                           [userid roles] (:application/user-roles application)
                           :when (contains? roles :reviewer)]
                       userid))]
    (fn [userid]
      (let [times (map :time (get requests userid))]
        {:reviews-this-year (count (filter (partial this-year? year) times))
         :in-progress (get in-progress userid 0)
         :last-requested (last (sort times))}))))

(defn- organization-applications [applications year]
  (frequencies
   (for [application applications
         :when (this-year? year (:application/first-submitted application))
         :let [organization (get-in application [:application/organization :organization/id])]
         :when organization]
     organization)))

(defn get-available-reviewers
  "The users who can be requested to review the application, or nil when the
  reviewer pool is not in use. Each user has the delegating organization, the
  status and statistics:

  `:reviewer/stats` - reviews requested this year, reviews in progress and the
                      latest request of the reviewer
  `:reviewer/organization-stats` - applications submitted this year from the
                                   delegating organization and reviews
                                   requested this year from its reviewers"
  [application-id]
  (when-some [pool (rems.db.organizations/get-reviewer-pool (:reviewer-pool-organization env))]
    (let [applications (rems.db.applications/get-all-unrestricted-applications)
          year (time/year (time/now))
          application-organization (some->> application-id
                                            (rems.db.applications/get-application-internal)
                                            :application/organization
                                            :organization/id)
          stats-of (reviewer-stats applications year)
          applications-by-organization (organization-applications applications year)
          reviews-by-organization (frequencies
                                   (for [entry pool
                                         :let [stats (stats-of (:userid entry))]
                                         _ (range (:reviews-this-year stats))]
                                     (:reviewer/organization entry)))]
      (->> pool
           (filter #(contains? requestable-statuses (:reviewer/status %)))
           (remove #(and application-organization
                         (= application-organization (:reviewer/organization %))))
           (keep (fn [entry]
                   (when-some [user (rems.db.users/get-user (:userid entry))]
                     (let [organization (:reviewer/organization entry)]
                       (assoc user
                              :reviewer/organization organization
                              :reviewer/status (:reviewer/status entry)
                              :reviewer/stats (stats-of (:userid entry))
                              :reviewer/organization-stats {:applications (get applications-by-organization organization 0)
                                                            :reviews (get reviews-by-organization organization 0)})))))
           (sort-by (juxt (comp :reviews-this-year :reviewer/stats) :name))
           vec))))
