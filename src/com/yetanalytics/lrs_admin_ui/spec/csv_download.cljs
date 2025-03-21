(ns com.yetanalytics.lrs-admin-ui.spec.csv-download
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.lrs-reactions.spec :as rs]))

;; TODO: More properties (e.g. query properties)

(s/def ::property-paths
  (s/every ::rs/path))

(s/def :validation/property-paths
  (s/and not-empty ::rs/identityPaths))
