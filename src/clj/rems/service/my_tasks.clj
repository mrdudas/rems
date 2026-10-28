(ns rems.service.my-tasks
  "The tasks of the user on the applications, by role: what the contact
  person, the researcher, the supervisor, the reviewer and the chair should
  do next."
  (:require [rems.common.application-util :as application-util]
            [rems.db.applications]
            [rems.service.reviewer-calendar :as reviewer-calendar]))

(defn- roles-of [application userid]
  (get-in application [:application/user-roles userid] #{}))

(defn- editable? [application]
  (contains? #{:application.state/draft :application.state/returned}
             (:application/state application)))

(defn- submitted? [application]
  (= :application.state/submitted (:application/state application)))

(defn- open-question?
  "A public remark of the handling users (e.g. a question of a reviewer) that
  the applying users have not answered yet."
  [application]
  (let [remarks (->> (:application/events application)
                     (filter #(= :application.event/remarked (:event/type %))))
        last-question (last (filter #(and (:event/public %)
                                          (application-util/is-handling-user? application (:event/actor %)))
                                    remarks))
        last-answer (last (remove #(application-util/is-handling-user? application (:event/actor %))
                                  remarks))]
    (boolean (and last-question
                  (or (nil? last-answer)
                      (pos? (compare (:event/time last-question) (:event/time last-answer))))))))

(defn- waiting-for-supervisor? [application]
  (and (:application/supervisor application)
       (not (:application/supervision-approved application))))

(defn- overview [application]
  {:application/id (:application/id application)
   :application/external-id (:application/external-id application)
   :application/description (:application/description application)
   :application/state (:application/state application)
   :application/last-activity (:application/last-activity application)})

(defn- task [application type & [details]]
  (merge (overview application)
         {:task/type type}
         details))

(defn- applicant-task
  "The task of the contact person (applicant)."
  [application]
  (cond
    (and (submitted? application) (open-question? application))
    (task application :answer-question)

    (= :application.state/returned (:application/state application))
    (task application :returned)

    (not (editable? application))
    nil

    (and (:application/marked-ready application)
         (not (waiting-for-supervisor? application)))
    (task application :submit)

    (:application/marked-ready application)
    (task application :waiting-for-supervisor)

    :else
    (task application :draft)))

(defn- member-task
  "The task of a researcher (member)."
  [application]
  (cond
    (and (submitted? application) (open-question? application))
    (task application :answer-question)

    (= :application.state/returned (:application/state application))
    (task application :returned)

    (and (editable? application)
         (not (:application/marked-ready application)))
    (task application :fill-in)))

(defn- supervisor-task [application]
  (when (and (editable? application)
             (not (:application/supervision-approved application)))
    (task application :approve-supervision
          {:task/marked-ready (boolean (:application/marked-ready application))})))

(defn- reviewer-tasks [application userid]
  (for [review (reviewer-calendar/open-reviews application)
        :when (= userid (:userid review))]
    (task application :review (dissoc review :userid
                                      :application/id :application/external-id :application/description))))

(defn- handler-task
  "The task of a chair (handler) on a submitted application."
  [application]
  (when (submitted? application)
    (let [can-request-reviews? (contains? (get-in application [:application/role-permissions :handler])
                                          :application.command/request-review)
          requested (reviewer-calendar/reviewers-since-submission application)
          open (reviewer-calendar/open-reviews application)
          overdue (filter (comp pos? :review/overdue-days) open)]
      (cond
        (seq overdue)
        (task application :reviews-overdue {:task/overdue-days (apply max (map :review/overdue-days overdue))})

        (and can-request-reviews? (< (count requested) 2))
        (task application :request-reviews {:task/reviewers (count requested)})

        (seq open)
        (task application :reviews-in-progress {:task/reviewers (count requested)
                                                :task/open-reviews (count open)})

        :else
        (task application :decide)))))

(defn get-my-tasks
  "The tasks of `userid`, grouped by role. Each task has the application, the
  `:task/type` and details depending on the type."
  [userid]
  (let [applications (->> (rems.db.applications/get-all-unrestricted-applications)
                          (filter #(seq (roles-of % userid))))
        tasks-for (fn [role f]
                    (->> applications
                         (filter #(contains? (roles-of % userid) role))
                         (mapcat #(let [result (f %)]
                                    (if (map? result) [result] result)))
                         (remove nil?)
                         (sort-by :application/id)
                         vec))]
    {:applicant (tasks-for :applicant applicant-task)
     :member (tasks-for :member member-task)
     :supervisor (tasks-for :supervisor supervisor-task)
     :reviewer (tasks-for :reviewer #(reviewer-tasks % userid))
     :handler (tasks-for :handler handler-task)}))
