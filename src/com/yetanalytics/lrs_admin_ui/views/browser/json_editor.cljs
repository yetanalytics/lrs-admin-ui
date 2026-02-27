(ns com.yetanalytics.lrs-admin-ui.views.browser.json-editor
  (:require [com.yetanalytics.lrs-admin-ui.views.form.editor :as ed]))

#_(defn simpler-validation-display [buffer]
    (let [ ;; whether or not the error display is open and which details index
          display-ref (r/atom {:open      false
                               :expand-id nil})]
      (fn []
        (let [{:keys [status errors saved value]} @buffer]
          [:div {:class "validation-display-wrapper"}
           (if (= :valid status)
             [v/validation-static-display
              status
              "Valid"]
             ;; Error or Warning
             (let [message (format-error-message errors status)
                   dis-msg (format-error-details-message status)
                   display [v/validation-item-display
                            status
                            dis-msg
                            display-ref
                            :message
                            error-details-display
                            errors]]
               [v/validation-display
                status
                message
                display-ref
                display]))]))))

(defn manual-json-editor
;debounce

  "Create a JSON CodeMirror editor similar to buffered-json-editor
   | Key | Description
   | --- | ---
   | `buffer`   | A reactive object carrying the content of the buffer - a map of `:value`, `:saved`, `:json`, `:status`, and `:errors`
   | `set-json` | A function that saves the unparsed json value
   | `save`     | A function that dispatches an fx to save the parsed JS value to the buffer.
   | `error`    | A function that dispatches an fx to save an error to the buffer.
   "
  [{:keys [buffer
           error
           set-json]}]
  (let [{:keys [json]} @buffer]
    [:div
     #_[ed/validation-display
      {:buffer buffer}]
     [ed/editor {:value json}
      :on-change
      set-json]]))
