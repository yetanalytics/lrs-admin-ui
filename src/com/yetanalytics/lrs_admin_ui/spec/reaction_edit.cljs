(ns com.yetanalytics.lrs-admin-ui.spec.reaction-edit
  "Duplicates com.yetanalytics.lrs-reactions.spec, but relaxed for edit"
  (:require [cljs.spec.alpha :as s :include-macros true]
            [com.yetanalytics.lrs-reactions.spec :as rs]))

;; Conditions do not have logic validation
(s/def ::condition
  (s/keys :req-un
          [(or
            (and ::rs/path ::rs/op
                 (or ::rs/val ::rs/ref))
            (or ::and ::or ::not))]))

;; Booleans lack :min-count
(s/def ::and (s/every ::condition
                      :gen-max 3))
(s/def ::or (s/every ::condition
                     :gen-max 3))
;; NOT is nilable
(s/def ::not (s/nilable ::condition))

;; A :sort-idx is added to top-level conditions for ordering
(s/def ::sort-idx nat-int?)

(s/def ::top-level-condition
  (s/merge ::condition
           (s/keys :req-un [::sort-idx])))

;; Condition map vals are nilable
(s/def ::conditions
  (s/map-of simple-keyword?
            (s/nilable ::top-level-condition)
            :gen-max 3))

;; :identityPaths is only checked for structure
(s/def ::identityPaths
  (s/every ::rs/path))

(s/def ::ruleset
  (s/keys :req-un [::identityPaths
                   ::conditions
                   ::rs/template]))

;; Representation
(s/def ::id string?)
(s/def ::title string?)
(s/def ::active boolean?)
(s/def ::created string?)
(s/def ::modified string?)


(s/def ::reaction
  (s/keys :req-un [::title
                   ::ruleset
                   ::active]
          :opt-un [::id
                   ::created
                   ::modified
                   ::rs/error]))
