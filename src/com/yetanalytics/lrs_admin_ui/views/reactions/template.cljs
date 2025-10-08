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

(defn- dynamic-var-condition
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

(defn- add-segment [path reaction-version]
  (let [{:keys [next-keys]} (rpath/analyze-path
                             path
                             :xapi-version reaction-version)]
    (vec (conj path
               (if (= '[idx] next-keys)
                 0
                 (or (first next-keys)
                     ""))))))

(defn- del-segment [path]
  (vec (butlast path)))

(defn- change-segment [path new-val]
  (assoc path (dec (count path)) new-val))

(defn dynamic-variables
  []
  (let [open?     (r/atom false)
        condition (r/atom "") ; TODO: Store in an app-db buffer
        path      (r/atom [""])]
    (fn []
      (let [reaction-version @(subscribe [:reaction/version])]
        [:div.dynamic-variables
         [:span
          {:on-click #(swap! open? not)
           :class (str "pane-collapse" (when @open? " expanded"))}
          @(subscribe [:lang/get :reactions.template.dynamic])
          [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.template.dynamic])}]]
         (when @open?
           [:div.inner
            [:p [:em @(subscribe [:lang/get :reactions.template.dynamic.instruction1])]]
            [:p [:em @(subscribe [:lang/get :reactions.template.dynamic.instruction2])]]
            [:p [:code "{\"$templatePath\": [\"condition_XYZ\", \"result\", \"success\"]}"]]
            [:p [:em @(subscribe [:lang/get :reactions.template.dynamic.instruction3])]]
            [:hr]
            [:p [:b @(subscribe [:lang/get :reactions.template.dynamic.step1])]]
            [dynamic-var-condition {:value     @condition
                                    :on-change #(reset! condition %)}]
            (when (seq @condition)
              [:<>
               [:hr]
               [:p [:b @(subscribe [:lang/get :reactions.template.dynamic.step2])]]
               [p/path-input
                @path
                :add-fn (fn [] (swap! path add-segment reaction-version))
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
                           :on-click #(fns/ps-event %)}]]])])])])]))))

(defn template-focus
  [template]
  [:pre.template
   (.stringify js/JSON (clj->js template) nil 2)])

(defn template-edit
  [_template]
  [:<>
   [dynamic-variables]
   [:h5 @(subscribe [:lang/get :reactions.template.template-json])
    [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.template.json])}]]
   [ed/buffered-json-editor
    {:buffer   (subscribe [:reaction/edit-template-buffer])
     :set-json #(dispatch [:reaction/set-template-json %])
     :save     #(dispatch [:reaction/update-template %])
     :error    #(dispatch [:reaction/set-template-errors %])}
    :keywordize-keys? false]])
