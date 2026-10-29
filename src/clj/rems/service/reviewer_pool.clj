(ns rems.service.reviewer-pool
  "The reviewer pool of the `:reviewer-pool-organization`: who can be requested
  to review an application, with statistics on their workload."
  (:require [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clojure.string :as str]
            [rems.auth.util :refer [throw-forbidden]]
            [rems.config :refer [env]]
            [rems.db.applications]
            [rems.db.organizations]
            [rems.db.outbox]
            [rems.db.user-settings]
            [rems.db.users]
            [rems.service.reviewer-calendar :as reviewer-calendar]
            [rems.service.util]
            [rems.text :refer [text-format with-language]]
            [rems.util :refer [getx-user-id]]))

(def ^:private requestable-statuses #{:active :limited})

(defn- time-zone []
  (time/time-zone-for-id (:deadline-reminders-time-zone env "Europe/Budapest")))

(defn- date-string
  "The date of the time `t` as YYYY-MM-DD in the local time zone."
  [t]
  (time-format/unparse (time-format/with-zone (time-format/formatters :year-month-day) (time-zone)) t))

(defn absent?
  "Whether the reviewer pool `entry` has an absence that covers the
  `date` (YYYY-MM-DD)."
  [entry date]
  (boolean (some #(and (<= (compare (:start %) date) 0)
                       (<= (compare date (:end %)) 0))
                 (:reviewer/absences entry))))

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
  reviewer pool is not in use. The reviewers who are absent today are not
  available. Each user has the delegating organization, the
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
           (remove #(absent? % (date-string (time/now))))
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

;;; the reviewer dashboard

(defn- pool-organization []
  (some-> (:reviewer-pool-organization env) rems.db.organizations/get-organization-by-id-raw))

(defn may-edit-pool?
  "The owners of the `:reviewer-pool-organization` and the administrators
  may edit the pool."
  []
  (boolean (some-> (pool-organization) rems.service.util/may-edit-organization?)))

(defn- in-period? [from to t]
  (and t
       (let [date (date-string t)]
         (and (<= (compare from date) 0)
              (<= (compare date to) 0)))))

(defn- days [from to]
  (/ (- (.getMillis to) (.getMillis from)) (* 24 60 60 1000.0)))

(defn- reviewer-statistics
  "The statistics of the reviewers of the `applications` in the period
  from `from` to `to` (YYYY-MM-DD, inclusive), by userid."
  [applications from to]
  (let [requested (frequencies (for [request (review-requests applications)
                                     :when (in-period? from to (:time request))]
                                 (:userid request)))
        done (->> (mapcat reviewer-calendar/done-reviews applications)
                  (filter #(in-period? from to (:review/done %)))
                  (group-by :userid))
        open (->> applications
                  (filter #(= :application.state/submitted (:application/state %)))
                  (mapcat reviewer-calendar/open-reviews)
                  (group-by :userid))
        total-done (count (apply concat (vals done)))]
    (fn [userid]
      (let [reviews (get done userid)
            on-time (count (filter #(or (nil? (:review/deadline %))
                                        (not (time/after? (:review/done %) (:review/deadline %))))
                                   reviews))]
        {:requested (get requested userid 0)
         :done (count reviews)
         :share (if (pos? total-done) (/ (count reviews) (double total-done)) 0.0)
         :on-time on-time
         :average-days (when (seq reviews)
                         (/ (reduce + (map #(days (:review/requested %) (:review/done %)) reviews))
                            (count reviews)))
         :recommendations (frequencies (keep :review/recommendation reviews))
         :open (count (get open userid))
         :overdue (count (filter (comp pos? :review/overdue-days) (get open userid)))}))))

(defn get-dashboard
  "The reviewer pool with the statistics of each reviewer in the period
  from `from` to `to` (YYYY-MM-DD, inclusive), or nil when the pool is not
  in use. `:may-edit` tells whether the user may edit the pool."
  [from to]
  (when-some [pool (rems.db.organizations/get-reviewer-pool (:reviewer-pool-organization env))]
    (let [applications (rems.db.applications/get-all-unrestricted-applications)
          stats-of (reviewer-statistics applications from to)
          today (date-string (time/now))]
      {:may-edit (may-edit-pool?)
       :today today
       :reviewers (vec
                   (for [entry pool
                         :let [user (rems.db.users/get-user (:userid entry))]
                         :when user]
                     (assoc user
                            :reviewer/organization (:reviewer/organization entry)
                            :reviewer/status (:reviewer/status entry)
                            :reviewer/absences (vec (sort-by :start (:reviewer/absences entry)))
                            :reviewer/absent (absent? entry today)
                            :reviewer/stats (stats-of (:userid entry)))))})))

(defn- check-may-edit-pool! []
  (when-not (may-edit-pool?)
    (throw-forbidden "only the owners of the reviewer pool organization may edit the pool")))

(defn- valid-absence? [absence]
  (<= (compare (:start absence) (:end absence)) 0))

(defn set-reviewer!
  "Adds the reviewer to the pool, or changes the delegating organization,
  the status or the absences of a member of the pool. The status
  `:inactive` revokes the right to review."
  [{:keys [userid] :as entry}]
  (check-may-edit-pool!)
  (cond
    (not (rems.db.users/get-user userid))
    {:success false :errors [{:type :t.reviewer-dashboard.errors/unknown-user}]}

    (not (rems.db.organizations/get-organization-by-id-raw (:reviewer/organization entry)))
    {:success false :errors [{:type :t.reviewer-dashboard.errors/unknown-organization}]}

    (not-every? valid-absence? (:reviewer/absences entry))
    {:success false :errors [{:type :t.reviewer-dashboard.errors/invalid-absence}]}

    :else
    (let [entry (select-keys entry [:userid :reviewer/organization :reviewer/status :reviewer/absences])]
      (rems.db.organizations/update-organization!
       (:reviewer-pool-organization env)
       (fn [organization]
         (update organization :organization/reviewers
                 (fn [reviewers]
                   (if (some #(= userid (:userid %)) reviewers)
                     (mapv #(if (= userid (:userid %)) entry %) reviewers)
                     (conj (vec reviewers) entry))))))
      {:success true})))

(defn- email-of [userid]
  (or (:notification-email (rems.db.user-settings/get-user-settings userid))
      (:email (rems.db.users/get-user userid))))

(defn send-message!
  "Sends an email from the current user to a member of the reviewer pool.
  Replies go to the sender."
  [{:keys [userid subject body]}]
  (let [sender (rems.db.users/get-user (getx-user-id))
        language (or (:language (rems.db.user-settings/get-user-settings userid))
                     (:default-language env))]
    (cond
      (not (some #(= userid (:userid %)) (rems.db.organizations/get-reviewer-pool (:reviewer-pool-organization env))))
      {:success false :errors [{:type :t.reviewer-dashboard.errors/not-in-pool}]}

      (or (str/blank? subject) (str/blank? body))
      {:success false :errors [{:type :t.reviewer-dashboard.errors/empty-message}]}

      (not (email-of userid))
      {:success false :errors [{:type :t.reviewer-dashboard.errors/no-email}]}

      :else
      (do
        (rems.db.outbox/put! {:outbox/type :email
                              :outbox/deadline (time/plus (time/now) (:email-retry-period env))
                              :outbox/email (cond-> {:to-user userid
                                                     :subject (str/trim subject)
                                                     :body (with-language language
                                                             (str (str/trim body)
                                                                  (text-format :t.email.reviewer-message/signature
                                                                               (:name sender (:userid sender)))))}
                                              (email-of (:userid sender)) (assoc :reply-to (email-of (:userid sender))))})
        {:success true}))))
