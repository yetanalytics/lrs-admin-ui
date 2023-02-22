(ns com.yetanalytics.lrs-admin-ui.functions.time
  (:require [goog.string :refer [format]]
            [goog.string.format]))

(def tz-offset-mins
  (.getTimezoneOffset (js/Date.)))

(def tz-offset-string
  (format "%s%02d:%02d"
          (if (pos-int? tz-offset-mins)
            "-"
            "+")
          (quot tz-offset-mins 60)
          (rem tz-offset-mins 60)))

(defn local-datetime->utc
  [local-datetime-str]
  (-> local-datetime-str
      (str tz-offset-string)
      (js/Date.)
      .toISOString))

(defn utc->local-datetime
  [utc-str]
  (let [date-ms (.parse js/Date utc-str)
        local-date-ms (- date-ms (* 60000 tz-offset-mins))
        date (js/Date. local-date-ms)]
    (subs (.toISOString date) 0 19)))

(defn two-weeks-ago
  "Return a timestamp two weeks before the current time."
  []
  (-> js/Date
      .now
      ;; two weeks ago
      (- 12096e5)
      (js/Date.)
      .toISOString))
