(ns com.yetanalytics.lrs-admin-ui.functions.upload
  (:require [com.yetanalytics.lrs-admin-ui.functions :refer [ps-event]]))

(defn process-upload-event
  "Take a file input `event`, read in the value of the uploaded value,
   then pass it to `f`."
  [event f]
  (ps-event event)
  (let [target    (.. event -currentTarget)
        text-file (aget (.. target -files) 0)]
    ;; Resolve the promise for the text file, should always be the 0th item.
    (.then (js/Promise.resolve (.text text-file))
           (fn [upload-val]
             (f upload-val)
             ;; clear out the temp file holding the input, so it can be reused.
             (set! (.. target -value) "")))))
