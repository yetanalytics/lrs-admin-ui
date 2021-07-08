(ns com.yetanalytics.lrs-admin-ui.db
  (:require [cljs.spec.alpha :as s :include-macros true]
            [re-frame.core   :as re-frame]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spec to define the db
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def :session/page keyword?)
(s/def ::session
  (s/keys :req-un [:session/page]))


(s/def ::db (s/keys :req [::session]))


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
