(ns com.yetanalytics.lrs-admin-ui.db
  (:require [cljs.spec.alpha :as s :include-macros true]
            [re-frame.core :as re-frame]
            [xapi-schema.spec :as xs]
            [com.yetanalytics.lrs-admin-ui.spec.reaction :as rs]
            [com.yetanalytics.lrs-admin-ui.spec.reaction-edit :as rse]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spec to define the db
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def :session/page keyword?)
(s/def :session/token (s/nilable string?))
(s/def :session/username (s/nilable string?))
(s/def ::session
  (s/keys :req-un [:session/page
                   :session/token
                   :session/username]))

(s/def :login/username (s/nilable string?))
(s/def :login/password (s/nilable string?))
(s/def ::login
  (s/keys :req-un [:login/username
                   :login/password]))


(s/def :credential/api-key string?)
(s/def :credential/secret-key string?)

(s/def :credential/scope string?)
(s/def :credential/scopes (s/every :credential/scope))

(s/def ::credential
  (s/keys :req-un [:credential/api-key
                   :credential/secret-key
                   :credential/scopes]))

(s/def ::credentials
  (s/every ::credential))

(s/def :new-account/username (s/nilable string?))
(s/def :new-account/password (s/nilable string?))

(s/def ::new-account
  (s/keys :req-un [:new-account/username
                   :new-account/password]))

(s/def :account/username (s/nilable string?))
(s/def :account/account-id string?)

(s/def ::account
  (s/keys :req-un [:account/account-id
                   :account/username]))

(s/def ::accounts (s/every ::account))

(s/def :update-password/old-password (s/nilable string?))
(s/def :update-password/new-password (s/nilable string?))

(s/def ::update-password
  (s/keys :req-un [:update-password/old-password
                   :update-password/new-password]))

(s/def :browser/content (s/nilable vector?))
(s/def :browser/address (s/nilable string?))
(s/def :browser/more-link (s/nilable string?))
(s/def :browser/batch-size int?)
(s/def :browser/back-stack vector?)
(s/def :browser/credential (s/nilable
                            (s/keys :req-un [:credential/api-key
                                             :credential/secret-key
                                             :credential/scopes])))

(s/def ::browser
  (s/keys :req-un [:browser/content
                   :browser/address
                   :browser/credential
                   :browser/more-link
                   :browser/back-stack
                   :browser/batch-size]))

(s/def ::server-host string?)
(s/def ::xapi-prefix string?) ;; default /xapi
(s/def ::proxy-path (s/nilable string?)) ;; default nil

(s/def ::pref-lang keyword?)
(s/def ::language map?)

(s/def :notification/id int?)
(s/def :notification/error? boolean?)
(s/def :notification/msg (s/nilable string?))

(s/def ::notification (s/keys :req-un [:notification/error?
                                       :notification/msg]))

(s/def ::notifications (s/every ::notification))

(s/def ::enable-statement-html boolean?)
(s/def ::enable-admin-delete-actor boolean?)

(s/def ::stmt-get-max int?)

(s/def ::oidc-auth boolean?)
(s/def ::oidc-enable-local-admin boolean?)

(s/def ::enable-admin-status boolean?)

(s/def :status.data/statement-count nat-int?)
(s/def :status.data/actor-count nat-int?)
(s/def :status.data/last-statement-stored (s/nilable ::xs/timestamp))
(s/def :status.data/platform-frequency (s/map-of string? nat-int?))

(s/def :status.data.timeline/stored ::xs/timestamp)
(s/def :status.data.timeline/count nat-int?)

(s/def :status.data/timeline
  (s/every
   (s/keys :req-un [:status.data.timeline/stored
                    :status.data.timeline/count])))

(s/def :status/data
  (s/keys :opt-un [:status.data/statement-count
                   :status.data/actor-count
                   :status.data/last-statement-stored
                   :status.data/platform-frequency
                   :status.data/timeline]))

(s/def :status.params/timeline-unit
  #{"year"
    "month"
    "day"
    "hour"
    "minute"
    "second"})
(s/def :status.params/timeline-since
  ::xs/timestamp)
(s/def :status.params/timeline-until
  ::xs/timestamp)

(s/def :status/params
  (s/keys :opt-un [:status.params/timeline-unit
                   :status.params/timeline-since
                   :status.params/timeline-until]))

;; map of vis type to loading state
(s/def :status/loading
  (s/map-of #{"statement-count"
              "actor-count"
              "last-statement-stored"
              "platform-frequency"
              "timeline"}
            boolean?))

(s/def ::status
  (s/keys
   :opt-un [:status/data
            :status/params
            :status/loading]))

(s/def ::enable-reactions boolean?)
(s/def ::reactions (s/every ::rs/reaction))
(s/def ::reaction-focus ::rs/id)
(s/def ::editing-reaction ::rse/reaction)

(s/def :reaction-template-error/message string?)
(s/def ::reaction-template-error
  (s/keys :req-un [:reaction-template-error/message]))

(s/def ::editing-reaction-template-errors
  (s/every ::reaction-template-error))
(s/def ::editing-reaction-template-json string?)

(s/def ::dialog-ref any?)

(s/def :dialog-choice/label string?)
(s/def :dialog-choice/dispatch vector?)
(s/def ::dialog-choice
  (s/keys :req-un [:dialog-choice/label
                   :dialog-choice/dispatch]))

(s/def :dialog-data/prompt string?)
(s/def :dialog-data/choices
  (s/every ::dialog-choice :min-count 1))
(s/def ::dialog-data
  (s/keys :req-un [:dialog-data/prompt
                   :dialog-data/choices]))

(s/def ::no-val-logout-url string?)

(s/def ::db (s/keys :req [::session
                          ::credentials
                          ::login
                          ::browser
                          ::accounts
                          ::server-host
                          ::xapi-prefix
                          ::proxy-path
                          ::language
                          ::pref-lang
                          ::notifications
                          ::enable-statement-html
                          ::enable-admin-delete-actor
                          ::stmt-get-max
                          ::oidc-auth
                          ::oidc-enable-local-admin
                          ::enable-admin-status
                          ::status
                          ::update-password
                          ::enable-reactions
                          ::reactions]
                    :opt [::reaction-focus
                          ::editing-reaction
                          ::editing-reaction-template-errors
                          ::editing-reaction-template-json
                          ::dialog-ref
                          ::dialog-data
                          ::no-val-logout-url]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Continuous DB Validation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn check-and-throw
  "Throw an exception if the app db does not match the spec."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "Spec check failed in: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor
  (re-frame/after
   (partial check-and-throw ::db)))
