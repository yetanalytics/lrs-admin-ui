(ns com.yetanalytics.lrs-admin-ui.spec.reaction-edit
  "Duplicates com.yetanalytics.lrs-reactions.spec, but relaxed for edit"
  (:require [cljs.spec.alpha :as s :include-macros true]
            [clojure.string :as cstr]
            [com.yetanalytics.lrs-reactions.spec :as rs]))

;; Validation functions

(defn keywordizable-string?
  "Can `s` be converted into a valid simple keyword?"
  [s]
  (and string? ; string needs to be keywordizable into simple strings
       (not (cstr/starts-with? s "@"))
       (not (cstr/includes? s " "))
       (not (cstr/includes? s "/"))))

(defn distinct-name-vector?
  "Is `v` a vector whose entries' `:name` properties are all distinct?"
  [v]
  (and (vector? v)
       (= (->> v (map :name) count)
          (->> v (map :name) distinct count))))

;; Basic condition specs for app-db specs

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

;; Condition specs for both app-db and validation

(s/def :condition/name
  string?)

(s/def :validation/name
  keywordizable-string?)

(s/def ::top-level-condition
  (s/or :not-empty
        (s/merge ::condition
                 (s/keys :req-un [:condition/name]))
        :empty
        (s/keys :req-un [:condition/name])))

(s/def :validation/top-level-condition
  (s/or :not-empty
        (s/merge ::rs/condition
                 (s/keys :req-un [:validation/name]))
        :empty
        (s/and (s/keys :req-un [:validation/name])
               #(= '(:name) (keys %)))))

(s/def ::conditions
  (s/coll-of ::top-level-condition
             :kind vector?
             :gen-max 3))

(s/def :validation/conditions
  (s/coll-of :validation/top-level-condition
             :kind distinct-name-vector?
             :min-count 1
             :gen-max 3))

;; :identityPaths is only checked for structure
(s/def ::identityPaths
  (s/every ::rs/path))

(s/def ::ruleset
  (s/keys :req-un [::identityPaths
                   ::conditions
                   ::rs/template]))

(s/def :validation/ruleset
  (s/keys :req-un [::rs/identityPaths
                   ::rs/template
                   :validation/conditions]))

;; Representation
(s/def ::id string?)
(s/def ::title string?)
(s/def ::active boolean?)
(s/def ::created string?)
(s/def ::modified string?)

(s/def ::reaction
  (s/keys :req-un [::title
                   ::active
                   ::ruleset]
          :opt-un [::id
                   ::created
                   ::modified
                   ::rs/error]))

;; TODO: Use fully-qualified namespace alias in Clojure 1.11
(s/def :validation/reaction
  (s/keys :req-un [::title
                   ::active
                   :validation/ruleset]
          :opt-un [::id
                   ::created
                   ::modified]))
