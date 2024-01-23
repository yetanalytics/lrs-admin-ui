(ns com.yetanalytics.lrs-admin-ui.functions
  (:require [clojure.string :as cstr]))

(defn ps-event
  "Helper function that will prevent default action
   and stop propagation for an event, a common task."
  [e]
  (.preventDefault e)
  (.stopPropagation e))

(defn ps-event-val
  "Not only prevents defaults but extracts value for form elements"
  [e]
  (ps-event e)
  (.. e -target -value))

(defn get-event-key
  "Return a keyword representing the key pressed during a keydown or keypress
   `event`. Common return values include `:space`, `:enter`, `:escape`,
   `:arrowup`, `:arrowdown`, `:pageup`, `:pagedown`, `:home`, and `:end`."
  [event]
  (let [keystr (cstr/lower-case (.. event -key))]
    (if (= " " keystr)
      :space
      (keyword keystr))))

(defn child-event?
  "Return `true` if `event` was triggered via a child of the current element."
  [event]
  (.contains (.. event -currentTarget)
             (.. event -relatedTarget)))

(defn elide
  "Given a string and a length will either return the string if less
  than the length or will substr and add ellipses with the combined length"
  [text len]
  (if (> len (count text))
    text
    (str (subs text 0 (- len 3)) "...")))

(defn rand-alpha-str
  "Produce a random alphanumeric string of the given length"
  [length]
  (apply str (repeatedly length #(rand-nth "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"))))
