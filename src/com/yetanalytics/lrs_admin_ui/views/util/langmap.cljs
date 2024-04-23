(ns com.yetanalytics.lrs-admin-ui.views.util.langmap
  (:require
   [re-frame.core :refer [subscribe]]
   [clojure.string :as cstr]))

(defn langmap
  "Takes a language map `m` and optionally `:override`, a vector of language
   prefix strings. Returns (in this order) either the first match 
   to the highest override, the system preference, or the first entry if no 
   match. `:prefs` defaults to `[en-us en]`."
  [m & {:keys [overrides]
        :or   {overrides ["en-us" "en"]}}]
  (let [matches?  (fn [pref k]
                    (cstr/starts-with? (cstr/lower-case (name k))
                                       (cstr/lower-case pref)))
        lang-keys (keys m)
        pref-keys (->> lang-keys
                       (filter (partial matches? @(subscribe [:db/pref-lang]))))
        over-keys (reduce (fn [agg ovrrd]
                            (->> lang-keys
                                 (filter (partial matches? ovrrd))
                                 (concat agg)))
                          []
                          overrides)]
    (get m (or (first over-keys)
               (first pref-keys)
               (first lang-keys)))))