(ns com.yetanalytics.lrs-admin-ui.spec.reaction
  "Duplicates lrsql.spec.reaction" ;; TODO: Use a common cljc source
  (:require [cljs.spec.alpha  :as s :include-macros true]
            [xapi-schema.spec :as xs]
            [com.yetanalytics.lrs-admin-ui.functions.reaction :as rfns]))

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

(s/def :ref/condition ::condition-name)

(s/def ::ref
  (s/keys :req-un [:ref/condition
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

(defn- valid-like-val?
  [{:keys [op
           ref
           ;; note that val is conformed here
           val]}]
  (if (= op "like")
    (if-let [{ref-path :path} ref]
      (let [{:keys [leaf-type]} (rfns/analyze-path ref-path)]
        (= 'string leaf-type))
      (= :string
         (first val)))
    true))

(defn- valid-clause-path?
  [{:keys [path
           op
           val
           ref] :as clause}]
  (if path
    (let [{:keys [valid?
                  leaf-type
                  next-keys]} (rfns/analyze-path path)]
      (and valid?
           (if (= "contains" op)
             (= '[idx] next-keys)
             (and leaf-type
                  (or
                   (= 'json leaf-type) ;; anything goes
                   (if-let [{ref-path :path} ref]
                     (let [{ref-leaf-type :leaf-type}
                           (rfns/analyze-path ref-path)]
                       (= leaf-type ref-leaf-type))
                     (= (name leaf-type)
                        (name (first val)))))))))
    true))

(s/def ::condition
  (s/and
   (s/keys :req-un
           [(or
             (and ::path ::op
                  (or ::val ::ref))
             (or ::and ::or ::not))])
   valid-like-val?
   valid-clause-path?))

(s/def ::and (s/every ::condition
                      :min-count 1
                      :gen-max 3))
(s/def ::or (s/every ::condition
                     :min-count 1
                     :gen-max 3))
(s/def ::not ::condition)

(s/def ::conditions
  (s/map-of simple-keyword?
            ::condition
            :min-count 1
            :gen-max 3))

(defn- valid-identity-path?
  [path]
  (some? (:leaf-type (rfns/analyze-path path))))

(s/def ::identityPaths
  (s/every (s/and ::path
                  valid-identity-path?)))

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

(s/def :lrsql.spec.reaction.error/type
  #{"ReactionQueryError"
    "ReactionTemplateError"
    "ReactionInvalidStatementError"})

(s/def :lrsql.spec.reaction.error/message string?)

(s/def ::error
  (s/nilable
   (s/keys :req-un [:lrsql.spec.reaction.error/type
                    :lrsql.spec.reaction.error/message])))

(s/def ::title string?)

(s/def ::reaction
  (s/keys :req-un [::id
                   ::title
                   ::ruleset
                   ::active
                   ::created
                   ::modified
                   ::error]))

(s/def ::new-reaction
  (s/keys :req-un [::title
                   ::ruleset
                   ::active]))
