(ns com.yetanalytics.lrs-admin-ui.views.form.editor
  (:require [re-codemirror.core :as cm]
            ["codemirror/mode/javascript/javascript"]
            ["codemirror/addon/lint/json-lint"]
            ["codemirror/addon/edit/matchbrackets"]
            ["codemirror/addon/edit/closebrackets"]))

#_(defn eddy
  []
  [cm/codemirror
   {:mode              "application/json"
    :theme             "neo"
    :lineNumbers       true
    :lineWrapping      true
    :matchBrackets     true
    :autoCloseBrackets true
    :gutters           ["CodeMirror-link-markers"]
    :lint              true}
   {:name "form-input"}])
