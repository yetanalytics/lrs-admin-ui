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

(defn elide
  "Given a string and a length will either return the string if less
  than the length or will substr and add ellipses with the combined length"
  [text len]
  (if (> len (count text))
    text
    (str (subs text 0 (- len 3)) "...")))

(defn pass-gen
  "Given length generate a password with random letters numbers and specials"
  [n]
  (let [chars (map char (concat (range 48 57)
                                (range 97 122)
                                (range 65 90)
                                [\! \? \#]))
        password (take n (repeatedly #(rand-nth chars)))]
    (reduce str password)))
