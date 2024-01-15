(ns com.yetanalytics.lrs-admin-ui.views.form.editor
  (:require [re-codemirror.core :as cm]
            [clojure.data :as data]
            [reagent.core :as r]
            [com.yetanalytics.lrs-admin-ui.views.form.validation :as v]
            ["codemirror/mode/javascript/javascript"]
            ["codemirror/addon/lint/json-lint"]
            ["codemirror/addon/edit/matchbrackets"]
            ["codemirror/addon/edit/closebrackets"]
            [goog.string :refer [format]]
            [goog.string.format]))

(defn- wrap-event
  [event handler]
  {event (fn [_ [cm _]] (handler (.getValue cm)))})

(defn editor
  "Generic CodeMirror editor container. Accepts `display-settings` to override
   the default display settings and an `on-change` callback function."
  [opts & {:keys [display-settings on-change on-cursor]}]
  (let [opts' (cond-> opts
                on-change
                (assoc :events (wrap-event "change" on-change))
                on-cursor
                (assoc :events (wrap-event "cursorActivity" on-cursor)))]
    [:div {:id "cm-mount-safety-container"}
     [cm/codemirror
      (merge {:mode              "application/json"
              :theme             "neo"
              :lineNumbers       true
              :lineWrapping      true
              :matchBrackets     true
              :autoCloseBrackets true
              :gutters           ["CodeMirror-link-markers"]
              :lint              true}
             display-settings)
      opts']]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Validation display
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; The errors come from the buffer rather than directly from the app DB so we
;; can use regular helper fns, rather than additional re-frame subscriptions,
;; to help materialize the view.

(defn- format-error-message
  "Return a formatted error message string."
  [errors status]
  (format "Invalid (%s %s%s)"
          (count errors)
          (if (= :warning status) "Warning" "Error")
          (if (> (count errors) 1) "s" "")))

(defn- format-error-details-message
  [status]
  (format "Click on %s to expand details."
          (if (= :warning status) "a warning" "an error")))

(defn- error-details-display
  "Return a syntax error display in the form of a text area."
  [{:keys [details] :as _error}]
  [:p {:class "validation-details"}
   [:textarea {:value     details
               :read-only true}]])

(defn- validation-display
  "Takes a buffer reactive object and displays status and potentially a
   dropdown with errors"
  [{:keys [buffer]}]
  (let [;; whether or not the error display is open and which details index
        display-ref (r/atom {:open      false
                             :expand-id nil})]
    (fn []
      (let [{:keys [status errors saved value]} @buffer]
        [:div {:class "validation-display-wrapper"}
         (if (= :valid status)
           ;; Valid
           ;; TODO: Show unsaved changes details?
           ;; TODO: Use unsaved subscription?
           (if (and saved
                    (->> (data/diff saved value) butlast (every? nil?) not))
             [v/validation-static-display
              status
              "Valid (with unsaved changes)"]
             [v/validation-static-display
              status
              "Valid"])
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; JSON Editors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn buffered-json-editor
  "Create a buffered JSON CodeMirror editor that accepts the following keys
   | Key | Description
   | --- | ---
   | `buffer`   | A reactive object carrying the content of the buffer - a map of `:value`, `:saved`, `:json`, `:status`, and `:errors`
   | `save`     | A function that dispatches an fx to save the parsed JSON value to the buffer.
   | `error`    | A function that dispatches an fx to save an error to the buffer.
   | `set-json` | A function that saves the unparsed json value

   Additionally there is the `keywordize-keys?` kwarg; if `true`, all keys
   should be keywordized, otherwise they should be kept as strings."
  [{:keys [buffer save error set-json]}
   & {:keys [keywordize-keys? on-cursor]
      :or   {keywordize-keys? true
             on-cursor        (fn [_])}}]
  (let [{:keys [json]} @buffer]
    [:div
     [validation-display
      {:buffer buffer}]
     [editor {:value json}
      :on-cursor on-cursor
      :on-change
      (fn [edit-val]
        (try (set-json edit-val)
             (save (js->clj (js/JSON.parse edit-val)
                            :keywordize-keys keywordize-keys?))
             (catch js/Error e
               (error [{:message "Invalid JSON Syntax"
                        :details (str e)}]))))]]))
