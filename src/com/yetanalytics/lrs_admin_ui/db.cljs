(ns com.yetanalytics.lrs-admin-ui.db
  (:require [cljs.spec.alpha :as s :include-macros true]
            [re-frame.core   :as re-frame]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spec to define the db
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def :session/page keyword?)
(s/def :session/token
  (s/nilable string?))
(s/def ::session
  (s/keys :req-un [:session/page
                   :session/token]))

(s/def :login/username (s/nilable string?))
(s/def :login/password (s/nilable string?))
(s/def :login/error (s/nilable string?))
(s/def ::login
  (s/keys :req-un [:session/username
                   :session/password]
          :opt-un [:session/error]))


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

(s/def ::db (s/keys :req [::session
                          ::credentials
                          ::login]))

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
