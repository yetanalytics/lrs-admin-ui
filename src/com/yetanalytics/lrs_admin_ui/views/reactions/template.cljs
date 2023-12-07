(ns com.yetanalytics.lrs-admin-ui.views.reactions.template
  (:require [com.yetanalytics.lrs-admin-ui.views.form.editor :as ed]
            [re-frame.core :as re-frame]))

(defn edit-template
  []
  [ed/buffered-json-editor
   {:buffer (re-frame/subscribe [:reaction/edit-template-buffer])
    :set-json (fn [json]
                (re-frame/dispatch [:reaction/set-template-json json]))
    :save (fn [v]
            (re-frame/dispatch [:reaction/update-template v]))
    :error (fn [errors]
             (re-frame/dispatch [:reaction/set-template-errors errors]))}
   :keywordize-keys? false])

(defn render-or-edit-template
  [mode template]
  (if (contains? #{:edit :new} mode)
    [edit-template]
    [:pre.template
     (.stringify js/JSON (clj->js template) nil 2)]))
