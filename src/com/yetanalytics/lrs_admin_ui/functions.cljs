(ns com.yetanalytics.lrs-admin-ui.functions
  (:require [re-frame.core :refer [subscribe dispatch-sync]]))

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

(defn elide
  "Given a string and a length will either return the string if less
  than the length or will substr and add ellipses with the combined length"
  [text len]
  (if (> len (count text))
    text
    (str (subs text 0 (- len 3)) "...")))
