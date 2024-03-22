(ns com.yetanalytics.lrs-admin-ui.views.reactions.template
  (:require [com.yetanalytics.lrs-admin-ui.views.form.editor :as ed]
            [com.yetanalytics.lrs-admin-ui.functions         :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.tooltip :refer [tooltip-info]]
            [com.yetanalytics.lrs-admin-ui.functions.copy :refer [copy-text]]
            [com.yetanalytics.lrs-reactions.path :as rpath]
            [com.yetanalytics.lrs-admin-ui.views.reactions.path :as p]
            [clojure.string :refer [join]]
            [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [goog.string :as gstr]
            [goog.string.format]))

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

(defn- add-segment [path]
  (let [{:keys [next-keys]} (rpath/analyze-path
                             path)]
    (vec (conj path
               (if (= '[idx] next-keys)
                 0
                 (or (first next-keys)
                     ""))))))

(defn- del-segment [path]
  (vec (butlast path)))

(defn- change-segment [path new-val]
  (assoc path (dec (count path)) new-val))

(defn var-gen
  []
  (let [open?     (r/atom false)
        condition (r/atom "")
        path      (r/atom [""])]
    (fn []
      [:<>
       [:dt
        {:on-click #(swap! open? not)
         :class (str "paths-collapse" (when @open? " expanded"))}
        @(subscribe [:lang/get :reactions.template.dynamic])
        [tooltip-info {:value "You can use this tool to create variable declarations referencing the statements which match the condition(s) above, and then use them in your template to create a dynamic xAPI Reaction Statement."}]]
       (when @open?
         [:div.var-gen-wrapper
          [:p [:em @(subscribe [:lang/get :reactions.template.dynamic.instruction1])]]
          [:p [:em @(subscribe [:lang/get :reactions.template.dynamic.instruction2])]]
          [:p [:code "{\"$templatePath\": [\"condition_XYZ\", \"result\", \"success\"]}"]]
          [:p [:em @(subscribe [:lang/get :reactions.template.dynamic.instruction3])]]
          [:hr]
          [:p [:b @(subscribe [:lang/get :reactions.template.dynamic.step1])]]
          [var-gen-condition {:value     @condition
                              :on-change #(reset! condition %)}]
          (when (seq @condition)
            [:<>
             [:hr]
             [:p [:b @(subscribe [:lang/get :reactions.template.dynamic.step2])]]
             [p/path-input
              @path
              :add-fn (fn [] (swap! path add-segment))
              :del-fn (fn [] (swap! path del-segment))
              :change-fn (fn [new-val] (swap! path change-segment new-val))
              :validate? false]
             (when (and (seq @condition) (seq @path))
               [:<>
                [:hr]
                [:p [:b @(subscribe [:lang/get :reactions.template.dynamic.step3])]]
                [:p @(subscribe [:lang/get :reactions.template.dynamic.step3-text])]
                (let [path-string (join "\", \"" @path)
                      var-string  (gstr/format "{\"$templatePath\": [\"%s\", \"%s\"]}" @condition path-string)]
                  [:<>
                   [:code var-string]
                   [copy-text
                    {:text var-string
                     :on-copy #(dispatch [:notification/notify false @(subscribe [:lang/get :notification.reactions.copied-template-var])])}
                    [:a {:class "icon-copy"
                         :on-click #(fns/ps-event %)}]]])])])])])))

(defn edit-template
  []
  [:<>
   [var-gen]
   [:h5 [:b @(subscribe [:lang/get :reactions.template.template-json])]
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
