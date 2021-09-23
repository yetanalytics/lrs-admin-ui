(ns com.yetanalytics.lrs-admin-ui.input
  (:require [cljs.spec.alpha :as s :include-macros true]
            [com.yetanalytics.lrs-admin-ui.functions.password :as pass]
            [clojure.set :refer [intersection]]))

;; Minimum lengths
(def p-min-len 10)
(def u-min-len 7)

(s/def :valid-account/password
  (let [p-contains? (fn [pass chars]
                      (not-empty (intersection (set pass)
                                               (set chars))))]
    (s/and string?
           #(>= (count %) p-min-len)
           #(p-contains? % pass/digit-chars)
           #(p-contains? % pass/upper-chars)
           #(p-contains? % pass/lower-chars)
           #(p-contains? % pass/special-chars))))


(s/def :valid-account/username
  (s/and string?
         #(> (count %) u-min-len)
         (partial re-matches #"^[a-zA-Z0-9]*$")))

(s/def ::valid-new-account
  (s/keys :req-un [:valid-account/password
                   :valid-account/username]))
