(ns com.yetanalytics.lrs-admin-ui.functions.time
  (:require
   [clojure.string :as cs]
   [goog.string :refer [format]]
   [goog.string.format]))

(defn tz-offset-mins*
  ([]
   (tz-offset-mins* (js/Date.)))
  ([^js/Date d]
   (.getTimezoneOffset d)))

(defn local-datetime->utc
  [local-datetime-str]
  (let [[date-part time-part] (cs/split local-datetime-str #"T")
        [y mo d]              (map js/Number (cs/split date-part #"-"))
        [h mi s]              (map js/Number (cs/split time-part #":"))]
    (.toISOString (new js/Date y (- mo 1) d h mi s))))

(defn utc->local-datetime
  [utc-str]
  (let [date-ms (.parse js/Date utc-str)
        local-date-ms (- date-ms
                         (* 60000
                            (tz-offset-mins* (new js/Date utc-str))))
        date (js/Date. local-date-ms)]
    (subs (.toISOString date) 0 19)))

(defn iso8601->local-display
  [iso8601-str]
  (let [date (js/Date. (.parse js/Date iso8601-str))]
    (format "%s, %s" (.toLocaleDateString date) (.toLocaleTimeString date))))

(defn- two-weeks-ago
  "Return a timestamp two weeks before the current time."
  []
  (-> js/Date
      .now
      ;; two weeks ago
      (- 12096e5)
      (js/Date.)
      .toISOString))

(defn timeline-since-default
  []
  (two-weeks-ago))

(defn timeline-until-default
  []
  (.toISOString (js/Date.)))

(defn ms->local [ms]
  (let [date (js/Date. ms)]
    (format "%s, %s" (.toLocaleDateString date) (.toLocaleTimeString date))))
