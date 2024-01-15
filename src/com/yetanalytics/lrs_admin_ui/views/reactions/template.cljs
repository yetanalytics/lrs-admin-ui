(ns com.yetanalytics.lrs-admin-ui.views.reactions.template
  (:require [com.yetanalytics.lrs-admin-ui.views.form.editor :as ed]
            [com.yetanalytics.lrs-admin-ui.functions         :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.tooltip :refer [tooltip-info]]
            [com.yetanalytics.lrs-admin-ui.functions.copy :refer [copy-text]]
            [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [goog.string :as gstr]
            [goog.string.format]
            [clojure.pprint :refer [pprint]]))

(defn- var-gen-condition
  [{:keys [on-change value]}]
  (into [:select
         {:value value
          :class "round"
          :on-change #(-> (fns/ps-event-val %)
                          on-change)}
         [:option {:value ""} "Select Condition"]]
        (for [condition-name @(subscribe [:reaction/edit-condition-names])]
          [:option
           {:value condition-name}
           condition-name])))

(defn var-gen
  []
  (let [open?     (r/atom false)
        condition (r/atom nil)
        path      (r/atom nil)]
    (fn []
      [:<>
       [:p [:a {:href "#!"
                :on-click #(swap! open? not)} 
            "Dynamic Variables"
            [tooltip-info {:value "You can use this tool to create variable declarations referencing the statements which match the condition(s) above, and then use them in your template to create a dynamic xAPI Reaction Statement."}]
            [:img {:src "/images/icons/code_generate.svg"}]]]
       (when @open?
         [:div.var-gen-wrapper
          [:p [:em "Reactions templates can be made dynamic by the use of injectable variables. These variables must come from a statement matching one of the conditions above."]]
          [:p [:em "Variables use a syntax with a JSON object containing a key of `$templatePath` and an array of the path in the statement of the value to extract, starting with which condition. For instance:"]]
          [:p [:code "{\"$templatePath\": [\"condition_XYZ\", \"result\", \"success\"]}"]]
          [:p [:em "The above example will retrieve (if it exists) the value of `$.result.success` from the statement matching `condition_XYZ` if the Reaction is successfully fired."]]
          [:hr]
          [:p [:b "Step 1: Select Condition"]]
          [var-gen-condition {:value     @condition
                              :on-change #(reset! condition %)}]
          (when (seq @condition)
            [:<>
             [:hr]
             [:p [:b "Step 2: Select Path"]]
             [:hr]
             [:p [:b "Step 3: Copy Variable Code"]]
             (let [var-string (gstr/format "{\"$templatePath\": [\"%s\", ]}" @condition)]
               [:<>
                [:code var-string]
                [copy-text
                 {:text var-string
                  :on-copy #(dispatch [:notification/notify false "Copied Template Variable to Clipboard!"])}
                 [:a {:class "icon-copy"
                      :on-click #(fns/ps-event %)}]]])])])])))

(defn edit-template
  []
  [:<>
   [var-gen]
   [:p [:b "Template JSON"]
    [tooltip-info {:value "The following is the JSON template which will be used to create the Reaction statement if the above conditions are met. You can customize this statement template to produce any valid xAPI Statement. Invalid xAPI will cause a Reaction error upon firing."}]]
   [ed/buffered-json-editor
    {:buffer   (subscribe [:reaction/edit-template-buffer])
     :set-json #(dispatch [:reaction/set-template-json %])
     :save     #(dispatch [:reaction/update-template %])
     :error    #(dispatch [:reaction/set-template-errors %])}
    :keywordize-keys? false]])

(defn render-or-edit-template
  [mode template]
  (if (contains? #{:edit :new} mode)
    [edit-template]
    [:pre.template
     (.stringify js/JSON (clj->js template) nil 2)]))
