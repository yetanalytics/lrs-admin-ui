(ns com.yetanalytics.lrs-admin-ui.views.reactions.errors
  (:require [re-frame.core :refer [subscribe]]
            [cljs.pprint :as pprint]))

(defn render-ruleset-errors
  [path]
  (let [errors @(subscribe
                 [:reaction/edit-ruleset-spec-errors-at-path
                  path])]
    [:pre
     (with-out-str
       (pprint/pprint errors))]))
