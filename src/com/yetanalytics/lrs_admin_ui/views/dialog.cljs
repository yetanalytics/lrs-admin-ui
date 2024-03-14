(ns com.yetanalytics.lrs-admin-ui.views.dialog
  (:require [re-frame.core :as re-frame]))

(defn dialog
  []
  (let [{:keys [prompt
                choices]} @(re-frame/subscribe [:dialog/data])]
    (into [:dialog
           {:ref #(re-frame/dispatch [:dialog/set-ref %])}
           [:p prompt]]
          (for [{:keys [label dispatch]} choices]
            [:button.btn-brand-bold
             {:on-click #(re-frame/dispatch [:dialog/dispatch dispatch])}
             label]))))
