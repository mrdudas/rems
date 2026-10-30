(ns rems.api.reviewer-pool
  "EPKEB: the reviewer dashboard, i.e. the members of the reviewer pool with
  their statistics, editing the pool and sending messages to the reviewers.
  See `:reviewer-pool-organization`."
  (:require [compojure.api.sweet :refer :all]
            [rems.api.util :refer [not-found-json-response extended-logging]] ; required for route :roles
            [rems.schema-base :as schema-base]
            [rems.service.reviewer-pool]
            [ring.util.http-response :refer :all]
            [schema.core :as s]))

(def ^:private Date
  (s/constrained s/Str #(re-matches #"\d{4}-\d{2}-\d{2}" %)))

(s/defschema ReviewerStats
  {:requested s/Int
   :done s/Int
   :share s/Num
   :on-time s/Int
   :average-days (s/maybe s/Num)
   :recommendations {schema-base/ReviewRecommendation s/Int}
   :open s/Int
   :overdue s/Int})

(s/defschema ReviewerDashboard
  {:may-edit s/Bool
   :may-edit-absences s/Bool
   :today Date
   :reviewers [(merge schema-base/UserWithAttributes
                      {:reviewer/organization s/Str
                       :reviewer/status schema-base/ReviewerStatus
                       :reviewer/absences [schema-base/ReviewerAbsence]
                       :reviewer/absent s/Bool
                       :reviewer/stats ReviewerStats})]})

(s/defschema SetReviewerCommand schema-base/ReviewerPoolEntry)

(s/defschema MyAbsences
  {:reviewer/status schema-base/ReviewerStatus
   :reviewer/absences [schema-base/ReviewerAbsence]})

(s/defschema SetAbsencesCommand
  {:userid schema-base/UserId
   :reviewer/absences [schema-base/ReviewerAbsence]})

(s/defschema SendMessageCommand
  {:userid schema-base/UserId
   :subject s/Str
   :body s/Str})

(s/defschema Response
  {:success s/Bool
   (s/optional-key :errors) [s/Any]})

(def reviewer-pool-api
  (context "/reviewer-pool" []
    :tags ["reviewer-pool"]

    (GET "/" []
      :summary "The members of the reviewer pool with their statistics in the period from `from` to `to` (inclusive): reviews requested and done, the share of all done reviews, the reviews done before the deadline, the average days to review, the recommendations and the open and overdue reviews. Only with `:reviewer-pool-organization`."
      :roles #{:handler :owner :organization-owner}
      :query-params [from :- (describe Date "the first day of the period, YYYY-MM-DD")
                     to :- (describe Date "the last day of the period, YYYY-MM-DD")]
      :return (s/maybe ReviewerDashboard)
      (ok (rems.service.reviewer-pool/get-dashboard from to)))

    (PUT "/reviewer" request
      :summary "Add a reviewer to the pool, or change the delegating organization, the status or the absences of a member. The status `inactive` revokes the right to review. Only for the owners of the `:reviewer-pool-organization` and the administrators."
      :roles #{:owner :organization-owner}
      :body [command SetReviewerCommand]
      :return Response
      (extended-logging request)
      (ok (rems.service.reviewer-pool/set-reviewer! command)))

    (GET "/my-absences" []
      :summary "The status and the absences of the current user in the reviewer pool, or null when the user is not a member of the pool."
      :roles #{:logged-in}
      :return (s/maybe MyAbsences)
      (ok (rems.service.reviewer-pool/get-my-absences)))

    (PUT "/absences" request
      :summary "Set the absences of a member of the reviewer pool. The reviewer is not available for review requests during an absence. Only for the reviewer, the handlers, the owners of the `:reviewer-pool-organization` and the administrators."
      :roles #{:logged-in}
      :body [command SetAbsencesCommand]
      :return Response
      (extended-logging request)
      (ok (rems.service.reviewer-pool/set-absences! command)))

    (POST "/send-message" request
      :summary "Send an email to a member of the reviewer pool. Replies go to the sender."
      :roles #{:handler :owner :organization-owner}
      :body [command SendMessageCommand]
      :return Response
      (extended-logging request)
      (ok (rems.service.reviewer-pool/send-message! command)))))
