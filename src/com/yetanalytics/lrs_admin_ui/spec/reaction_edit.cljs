(ns com.yetanalytics.lrs-admin-ui.spec.reaction-edit
  "Duplicates lrsql.spec.reaction, but relaxed for edit"
  (:require [cljs.spec.alpha  :as s :include-macros true]
            [xapi-schema.spec :as xs]))

(s/def ::condition-name
  string?)

(s/def ::path
  (s/every
   (s/or :string string?
         :index nat-int?)
   :gen-max 4))

(s/def ::val (s/or :string string?
                   :number number?
                   :null nil?
                   :boolean boolean?))

(s/def :edit-ref/condition ::condition-name)

(s/def ::ref
  (s/keys :req-un [:edit-ref/condition
                   ::path]))

(s/def ::op
  #{"gt"
    "lt"
    "gte"
    "lte"
    "eq"
    "noteq"
    "like"
    "contains"})

(s/def ::condition
  (s/or
   :clause
   (s/or :clause-val
         (s/keys :req-un [::path
                          ::op
                          ::val])
         :clause-ref
         (s/keys :req-un [::path
                          ::op
                          ::ref]))
   :boolean
   (s/or :and (s/keys :req-un [::and])
         :or (s/keys :req-un [::or])
         :not (s/keys :req-un [::not]))))

(s/def ::and (s/every ::condition
                      :gen-max 3))
(s/def ::or (s/every ::condition
                     :gen-max 3))
(s/def ::not (s/nilable ::condition))

(s/def ::sort-idx nat-int?)

(s/def ::top-level-condition
  (s/merge ::condition
           (s/keys :req-un [::sort-idx])))

(s/def ::conditions
  (s/map-of simple-keyword?
            (s/nilable ::top-level-condition)
            :gen-max 3))

(s/def ::identityPaths
  (s/every ::path))

;; A JSON structure resembling a statement, but with path refs to cond results
(s/def ::template ::xs/any-json)

(s/def ::ruleset
  (s/keys :req-un [::identityPaths
                   ::conditions
                   ::template]))

;; Representation
(s/def ::id string?)
(s/def ::active boolean?)
(s/def ::created string?)
(s/def ::modified string?)

(s/def :lrsql.spec.edit-reaction.error/type
  #{"ReactionQueryError"
    "ReactionTemplateError"
    "ReactionInvalidStatementError"})

(s/def :lrsql.spec.edit-reaction.error/message string?)

(s/def ::error
  (s/nilable
   (s/keys :req-un [:lrsql.spec.edit-reaction.error/type
                    :lrsql.spec.edit-reaction.error/message])))

(s/def ::title string?)

(s/def ::reaction
  (s/keys :req-un [::title
                   ::ruleset
                   ::active]
          :opt-un [::id
                   ::created
                   ::modified
                   ::error]))
