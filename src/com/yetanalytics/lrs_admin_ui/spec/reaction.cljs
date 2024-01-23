(ns com.yetanalytics.lrs-admin-ui.spec.reaction
  "Duplicates lrsql.spec.reaction" ;; TODO: Use a common cljc source
  (:require [cljs.spec.alpha  :as s :include-macros true]
            [com.yetanalytics.lrs-reactions.spec :as rs]))

;; Representation
(s/def ::id string?)
(s/def ::active boolean?)
(s/def ::created string?)
(s/def ::modified string?)
(s/def ::title string?)

(s/def ::reaction
  (s/keys :req-un [::id
                   ::title
                   ::active
                   ::rs/ruleset
                   ::rs/error
                   ::created
                   ::modified]))

(s/def ::new-reaction
  (s/keys :req-un [::title
                   ::rs/ruleset
                   ::active]))
