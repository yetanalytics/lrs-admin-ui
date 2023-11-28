(ns com.yetanalytics.lrs-admin-ui.views.dialog
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]))

(defn dialog
  []
  (let [{:keys [prompt
                choices]} @(re-frame/subscribe [:dialog/data])]
    (into [:dialog
           {:ref #(re-frame/dispatch [:dialog/set-ref %])}
           [:p prompt]]
          (for [{:keys [label dispatch]} choices]
            [:button.btn-blue-bold
             {:on-click #(re-frame/dispatch [:dialog/dispatch dispatch])}
             label]))))