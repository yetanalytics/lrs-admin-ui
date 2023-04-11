(ns com.yetanalytics.lrs-admin-ui.input
  (:require [cljs.spec.alpha :as s :include-macros true]
            [com.yetanalytics.lrs-admin-ui.functions.password :as pass]))

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
         #(>= (count %) u-min-len)
         (partial re-matches #"^[a-zA-Z0-9]*$")))

(s/def ::valid-new-account
  (s/keys :req-un [:valid-account/password
                   :valid-account/username]))

(s/def :valid-update-password/old-password
  string?) ;; Allow the default dev password for instance

(s/def :valid-update-password/new-password
  :valid-account/password)

(s/def ::valid-update-password
  (s/and
   (s/keys :req-un [:valid-update-password/old-password
                    :valid-update-password/new-password])
   (fn neither-nil [{:keys [old-password new-password]}]
     (and old-password new-password))
   (fn not-the-same [{:keys [old-password new-password]}]
     (not= old-password new-password))))
