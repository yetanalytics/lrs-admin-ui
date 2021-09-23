(ns com.yetanalytics.lrs-admin-ui.input
  (:require [cljs.spec.alpha :as s :include-macros true]
            [com.yetanalytics.lrs-admin-ui.functions.password :as pass]
            [clojure.set :refer [intersection]]))

;; Minimum lengths
(def p-min-len 10)
(def u-min-len 7)

(def digit-set (set pass/digit-chars))
(def upper-set (set pass/upper-chars))
(def lower-set (set pass/lower-chars))
(def special-set (set pass/special-chars))

(s/def :valid-account/password
  (s/and string?
         #(>= (count %) p-min-len)
         #(let [pass-set   (set %)
                has-chars? (partial some pass-set)]
            (and
             (has-chars? digit-set)
             (has-chars? upper-set)
             (has-chars? lower-set)
             (has-chars? special-set)))))

(s/def :valid-account/username
  (s/and string?
         #(> (count %) u-min-len)
         (partial re-matches #"^[a-zA-Z0-9]*$")))

(s/def ::valid-new-account
  (s/keys :req-un [:valid-account/password
                   :valid-account/username]))
