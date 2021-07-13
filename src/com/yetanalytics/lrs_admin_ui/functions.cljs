(ns com.yetanalytics.lrs-admin-ui.functions
  (:require [re-frame.core                    :refer [subscribe dispatch-sync]]
            [com.yetanalytics.lrs-admin-ui.db :as db]
            [clojure.pprint :refer [pprint]]))


(defn ps-event
  "Helper function that will prevent default action
   and stop propagation for an event, a common task."
  [e]
  (.preventDefault e)
  (.stopPropagation e))

(defn ps-event-val
  "Not only pevents defaults but extracts value for form elements"
  [e]
  (ps-event e)
  (.. e -target -value))
