(ns com.yetanalytics.lrs-admin-ui.views.reactions
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.reaction :as rfns]
            [com.yetanalytics.lrs-admin-ui.functions.upload :as upload]
            [com.yetanalytics.lrs-reactions.path :as rpath]
            [com.yetanalytics.lrs-admin-ui.functions.time :refer [iso8601->local-display]]
            [com.yetanalytics.lrs-admin-ui.functions.tooltip :refer [tooltip-info]]
            [com.yetanalytics.lrs-admin-ui.views.form :as form]
            [com.yetanalytics.lrs-admin-ui.views.reactions.path :as p]
            [com.yetanalytics.lrs-admin-ui.views.reactions.template :as t]
            [com.yetanalytics.lrs-admin-ui.spec.reaction-edit :as rse]
            [goog.string :as gstr]
            [goog.string.format]))

(defn- short-error
  [{:keys [type message]}]
  (str (case type
         "ReactionQueryError" "Query"
         "ReactionTemplateError" "Template"
         "ReactionInvalidStatementError" "Invalid Statement")
       ": "
       message))

(defn- reactions-table
  []
  [:table {:class "reactions-table"}
   [:thead {:class "bg-primary text-white"}
    [:tr
     [:th {:scope "col"} @(subscribe [:lang/get :reactions.col.title])]
     [:th {:scope "col"} @(subscribe [:lang/get :reactions.col.conds])]
     [:th {:scope "col"} @(subscribe [:lang/get :reactions.col.created])]
     [:th {:scope "col"} @(subscribe [:lang/get :reactions.col.modified])]
     [:th {:scope "col"} @(subscribe [:lang/get :reactions.col.status])]
     [:th {:scope "col"} @(subscribe [:lang/get :reactions.col.error])]
     [:th {:scope "col" :class "action"} @(subscribe [:lang/get :reactions.col.action])]]]
   (into [:tbody]
         (for [{:keys [id
                       title
                       created
                       modified
                       active
                       error
                       ruleset]} @(subscribe [:reaction/list])]
           [:tr {:class "reaction-row"
                 :on-click #(dispatch [:reaction/set-focus id])}
            [:td {:data-label @(subscribe [:lang/get :reactions.col.title])} title]
            [:td {:data-label @(subscribe [:lang/get :reactions.col.conds])} (count (:conditions ruleset))]
            [:td {:data-label @(subscribe [:lang/get :reactions.col.created])} (iso8601->local-display created)]
            [:td {:data-label @(subscribe [:lang/get :reactions.col.modified])} (iso8601->local-display modified)]
            [:td {:data-label @(subscribe [:lang/get :reactions.col.status])} (if (true? active) "Active" "Inactive")]
            [:td {:data-label @(subscribe [:lang/get :reactions.col.error])} (if error (short-error error) "[None]")]
            [:td {:data-label @(subscribe [:lang/get :reactions.col.action])}
             [:ul {:class "action-icon-list"}
              [:li
               [:a {:href "#!"
                    :class "icon-edit"
                    :on-click (fn [e]
                                (fns/ps-event e)
                                (dispatch [:reaction/edit id]))}
                @(subscribe [:lang/get :reactions.action.edit])]]
              [:li
               [:a {:href "#!"
                    :class "icon-delete"
                    :on-click (fn [e]
                                (fns/ps-event e)
                                (dispatch [:reaction/delete-confirm id]))}
                @(subscribe [:lang/get :reactions.action.delete])]]]]]))])

(defn- reactions-list-buttons
  []
  [:div {:class "api-keys-table-actions"}
   [:input {:type "button",
            :class "btn-brand-bold",
            :on-click #(dispatch [:reaction/new])
            :value @(subscribe [:lang/get :reactions.add])}]
   ;; TODO: Currently the "Download All" button is unimplemented because
   ;; there is no corresponding "Upload All" button (and implementing that
   ;; would be tricky because there is no multi-reaction insert). However,
   ;; we may want to implement this in the future.
   #_[:input {:type "button"
              :class "btn-brand-bold"
              :on-click #(dispatch [:reaction/download-all])
              :value @(subscribe [:lang/get :reactions.download-all])}]])

(defn- reactions-list
  []
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    @(subscribe [:lang/get :reactions.title])
    [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.title])}]
    @(subscribe [:lang/get :reactions.title.beta])]
   [:p ]
   [:div {:class "tenant-wrapper"}
    [reactions-list-buttons]
    [reactions-table]
    [reactions-list-buttons]]])

(defn- render-path
  [path]
  [:code (rfns/path->string path)])

(defn- val-type
  [val]
  (cond
    (string? val)
    "string"
    (number? val)
    "number"
    (boolean? val)
    "boolean"
    (nil? val)
    "null"))

(defn- render-or-edit-path
  [mode path-path path & {:keys [remove-fn
                                 validate?
                                 open-next?]
                          :or {validate? true
                               open-next?  false}}]
  (if (contains? #{:edit :new} mode)
    [p/path-input path
     :add-fn (fn []
               (dispatch [:reaction/add-path-segment
                          path-path]))
     :del-fn (fn []
               (dispatch [:reaction/del-path-segment
                          path-path]))
     :change-fn (fn [seg-val]
                  (dispatch [:reaction/change-path-segment
                             path-path
                             seg-val
                             open-next?]))
     :remove-fn remove-fn
     :validate? validate?]
    [render-path path]))

(def ops {"eq"       "Equal"
          "gt"       "Greater Than"
          "lt"       "Less Than"
          "gte"      "Greater Than or Equal"
          "lte"      "Less Than or Equal"
          "noteq"    "Not Equal"
          "like"     "Like (String Matching)"
          "contains" "Contains (Array Element)"})

(defn- render-or-edit-op
  [mode op-path op]
  (if (contains? #{:edit :new} mode)
    (into [:select
           {:value op
            :class "round"
            :on-change (fn [e]
                         (dispatch
                          [:reaction/set-op
                           op-path
                           (fns/ps-event-val e)]))}
           (for [[k v] ops]
             ^{:key (gstr/format "condition-op-%s-%s" op-path k)}
             [:option {:value k} v])])
    [:code (get ops op)]))

(defn- select-val-type
  [val-path path val]
  (let [{:keys [leaf-type]} (rpath/analyze-path path)]
    (if (and leaf-type (not= 'json leaf-type))
      (str (name leaf-type) ": ")
      (into [:select
             {:value (rfns/val-type val)
              :class "round short"
              :on-change (fn [e]
                           (dispatch [:reaction/set-val-type
                                      val-path
                                      (fns/ps-event-val e)]))}]
            (for [t (if leaf-type
                      (when (= 'json leaf-type)
                        ["string" "number" "boolean" "null"])
                      [(rfns/val-type val)])]
              [:option
               {:value t}
               t])))))

(defn- val-input
  [val-path path val]
  ;; path type wins over val type
  (let [{:keys [leaf-type]} (rpath/analyze-path path)
        val-type (or
                  (and
                   leaf-type
                   (#{"string" "number" "boolean" "null"} (name leaf-type)))
                  (rfns/val-type val))]
    (case val-type
       "null"
       [:input
        {:disabled true
         :class "round"
         :value "null"}]
       "boolean"
       [:select
        {:value (str val)
         :class "round"
         :on-change
         (fn [e]
           (dispatch [:reaction/set-val
                      val-path
                      (case (fns/ps-event-val e)
                        "true" true
                        "false" false)]))}
        [:option
         {:value "true"}
         "true"]
        [:option
         {:value "false"}
         "false"]]
       "number"
       [:input
        {:type "number"
         :value val
         :class "round"
         :on-change
         (fn [e]
           (dispatch [:reaction/set-val
                      val-path
                      (js/parseFloat
                       (fns/ps-event-val e))]))}]
       "string"
       [:input
        {:type "text"
         :class "round"
         :value val
         :on-change
         (fn [e]
           (dispatch [:reaction/set-val
                      val-path
                      (fns/ps-event-val e)]))}])))

(defn- render-or-edit-val
  [mode val-path path val]
  [:div.val
   (if (contains? #{:edit :new} mode)
     [:<>
      [select-val-type val-path path val]
      [val-input val-path path val]]
     [:<>
      [:span (str (val-type val) ": ")]
      [:code (if (nil? val) "null" (str val))]])])

(defn- render-or-edit-ref-condition
  [mode ref-condition-path condition]
  (if (contains? #{:edit :new} mode)
    (into [:select
           {:value condition
            :class "round"
            :on-change
            (fn [e]
              (dispatch [:reaction/set-ref-condition
                         ref-condition-path
                         (fns/ps-event-val e)]))}]
          (for [condition-name @(subscribe [:reaction/edit-condition-names])]
            [:option
             {:value condition-name}
             condition-name]))
    [:span condition]))

(defn- render-ref
  [mode ref-path {:keys [condition path]}]
  [:dl.ref
   [:dt "Condition"]
   [:dd
    [render-or-edit-ref-condition
     mode
     (conj ref-path :condition)
     condition]]
   [:dt "Path"]
   [:dd
    [render-or-edit-path
     mode
     (conj ref-path :path)
     path]]])

(defn clause-type-tooltips [key]
  (get {:and   @(subscribe [:lang/get :tooltip.reactions.clause-type.and])
        :or    @(subscribe [:lang/get :tooltip.reactions.clause-type.or])
        :not   @(subscribe [:lang/get :tooltip.reactions.clause-type.not])
        :logic @(subscribe [:lang/get :tooltip.reactions.clause-type.logic])}
       key))

(defn- clause-label
  [mode
   reaction-path
   type-key]
  [:div.clause-type-label
   {:class (when (and (not (contains? #{:edit :new} mode)) ; FIXME: Refactor
                      (not (contains? #{:and :or :not} type-key)))
             "empty")}
   (if (contains? #{:edit :new} mode)
     [:<>
      [:select
       {:value (name type-key)
        :class "round short"
        :on-change
        (fn [e]
          (dispatch [:reaction/set-clause-type
                     reaction-path
                     (fns/ps-event-val e)]))}
       [:option
        {:value "logic"}
        "Statement Criteria"]
       [:option
        {:value "and"}
        "Boolean AND"]
       [:option
        {:value "or"}
        "Boolean OR"]
       [:option
        {:value "not"}
        "Boolean NOT"]]
      [tooltip-info {:value (clause-type-tooltips type-key)}]] 
     (case type-key :and "AND" :or "OR" :not "NOT" ""))])

(defn- delete-icon
  [& {:keys [on-click to-delete-desc]
      :or   {on-click       (fn [] (println 'delete))
             to-delete-desc ""}}]
  [:div.delete-icon
   [:a {:href "#"
        :on-click (fn [e]
                    (fns/ps-event e)
                    (on-click))}
    (gstr/format 
     @(subscribe [:lang/get :reactions.details.conditions.delete-button]) 
     to-delete-desc)
    [:img {:src @(subscribe [:resources/icon "icon-delete-brand.svg"])}]]])

(defn- add-condition
  []
  [:div.add-icon
   [:a {:href "#"
        :on-click (fn [e]
                    (fns/ps-event e)
                    (dispatch [:reaction/add-condition]))}
    @(subscribe [:lang/get :reactions.details.conditions.add-condition])
    [:img {:src @(subscribe [:resources/icon "add.svg"])}]]])

(defn- add-clause
  [parent-path]
  [:div.add-clause
   [form/action-dropdown
    {:options     [{:value :logic
                    :label "Statement Criteria"}
                   {:value :and
                    :label "Boolean AND"}
                   {:value :or
                    :label "Boolean OR"}
                   {:value :not
                    :label "Boolean NOT"}]
     :label       (gstr/format
                   @(subscribe [:lang/get :reactions.details.conditions.add-clause])
                   (if (> (count parent-path) 3) "sub-" "")
                   (case (last parent-path)
                     :and   "`Boolean AND` clause"
                     :or    "`Boolean OR` clause"
                     :not   "`Boolean NOT` clause"
                     "condition"))
     :label-left? true
     :class       "round"
     :select-fn   (fn [v]
                    (dispatch [:reaction/add-clause
                               parent-path
                               v]))}]])

(declare render-clause)

(defn clause-nest-class
  [reaction-path]
  (let [ops (filter keyword? (subvec reaction-path 3))]
    (if (even? (count ops)) "even" "odd")))

(defn- render-and
  [mode reaction-path and-clauses]
  [:div.clause.boolean.and
   {:class (clause-nest-class reaction-path)}
   [clause-label mode reaction-path :and]
   (when (empty? and-clauses)
     [:ul.reaction-error-list
      [:li @(subscribe [:lang/get :reactions.details.conditions.and-instructions])]])
   (into [:div.boolean-body]
         (map-indexed
          (fn [idx clause]
            [render-clause
             mode
             (into reaction-path [:and idx])
             clause])
          and-clauses))
   (when (contains? #{:edit :new} mode)
     [:<>
      [add-clause
       (conj reaction-path :and)]
      [delete-icon
       :to-delete-desc "'Boolean AND' clause"
       :on-click
       (fn []
         (dispatch
          [:reaction/delete-clause reaction-path]))]])])

(defn- render-or
  [mode reaction-path or-clauses]
  [:div.clause.boolean.or
   {:class (clause-nest-class reaction-path)}
   [clause-label mode reaction-path :or]
   (when (empty? or-clauses)
     [:ul.reaction-error-list
      [:li @(subscribe [:lang/get :reactions.details.conditions.or-instructions])]])
   (into [:div.boolean-body]
         (map-indexed
          (fn [idx clause]
            [render-clause
             mode
             (into reaction-path [:or idx])
             clause])
          or-clauses))
   (when (contains? #{:edit :new} mode)
     [:<>
      [add-clause
       (conj reaction-path :or)]
      [delete-icon
       :to-delete-desc "'Boolean OR' clause"
       :on-click
       (fn []
         (dispatch
          [:reaction/delete-clause reaction-path]))]])])

(defn- render-not
  [mode reaction-path not-clause]
  [:div.clause.boolean.not
   {:class (clause-nest-class reaction-path)}
   [clause-label mode reaction-path :not]
   (when (nil? not-clause)
     [:ul.reaction-error-list
      [:li @(subscribe [:lang/get :reactions.details.conditions.not-instructions])]])
   [:div.boolean-body
    (when not-clause
      [render-clause mode (conj reaction-path :not) not-clause])]
   (when (and (contains? #{:edit :new} mode)
              (nil? not-clause))
     [add-clause
      (conj reaction-path :not)])
   (when (contains? #{:edit :new} mode)
     [delete-icon
      :to-delete-desc "'Boolean NOT' clause"
      :on-click
      (fn []
        (dispatch
         [:reaction/delete-clause reaction-path]))])])

(defn- render-logic-errors
  [clause-path]
  (when-let [problems @(subscribe [:reaction/edit-spec-errors-in
                                   clause-path])]
    (let [pred-set (into #{}
                         (map :pred problems))]
      (cond-> [:ul.reaction-error-list]
        (pred-set
         'com.yetanalytics.lrs-reactions.spec/valid-clause-path?)
        (conj
         [:li
          @(subscribe [:lang/get :reactions.errors.incomplete-path])])
        (pred-set
         'com.yetanalytics.lrs-reactions.spec/valid-like-val?)
        (conj [:li
               @(subscribe [:lang/get :reactions.errors.like-string])])))))

(defn- render-logic
  [mode reaction-path clause]
  (let [{:keys [path op val ref]} clause]
          [:div.clause.op
           {:class (clause-nest-class reaction-path)}
           [clause-label mode reaction-path :logic]
           (when (contains? #{:edit :new} mode)
             [render-logic-errors
              reaction-path])
           (cond-> [:dl.op-list
                    [:dt @(subscribe [:lang/get :reactions.details.conditions.statement-path])
                     [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.statement-path])}]]
                    [:dd
                     [render-or-edit-path
                      mode
                      (conj reaction-path :path)
                      path
                      :open-next? true]]
                    [:dt @(subscribe [:lang/get :reactions.details.conditions.operation])
                     [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.operation])}]]
                    [:dd [render-or-edit-op
                          mode
                          (conj reaction-path :op)
                          op]]
                    [:dt
                     (if (contains? #{:edit :new} mode)
                       [:select
                        {:value (if ref "ref" "val")
                         :class "round short"
                         :on-change
                         (fn [e]
                           (dispatch [:reaction/set-val-or-ref
                                      reaction-path
                                      (fns/ps-event-val e)]))}
                        [:option
                         {:value "ref"}
                         @(subscribe [:lang/get :reactions.details.conditions.reference])]
                        [:option
                         {:value "val"}
                         @(subscribe [:lang/get :reactions.details.conditions.value])]]
                       (if ref
                         @(subscribe [:lang/get :reactions.details.conditions.reference])
                         @(subscribe [:lang/get :reactions.details.conditions.value])))
                     [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.comparator])}]]]
             (not ref) (conj [:dd [render-or-edit-val
                                   mode
                                   (conj reaction-path :val)
                                   path
                                   val]])
             ref (conj [:dd
                        [render-ref
                         mode
                         (conj reaction-path :ref)
                         ref]]))
           (when (contains? #{:edit :new} mode)
             [delete-icon
              :to-delete-desc "Statement Criteria"
              :on-click
              (fn []
                (dispatch
                 [:reaction/delete-clause reaction-path]))])]))

(defn- render-clause
  [mode
   reaction-path
   {and-clauses :and
    or-clauses  :or
    not-clause  :not
    :keys [op sort-idx]
    :as clause}]
  (cond
    and-clauses
    [render-and
     mode reaction-path and-clauses]
    or-clauses
    [render-or
     mode reaction-path or-clauses]
    (find clause :not)
    [render-not
     mode reaction-path not-clause]
    op
    [render-logic
     mode reaction-path clause]
    ;; if it is top-level & empty, do not render
    sort-idx nil))

(defn- render-condition-name-errors
  [condition-name]
  (when (not (rse/keywordizable-string? condition-name))
    [:ul.reaction-error-list
     [:li @(subscribe [:lang/get :reactions.errors.invalid-condition-name])]]))

(defn- render-or-edit-condition-name
  [mode condition-path condition-name]
  [:div.condition-name
   (if (contains? #{:edit :new} mode)
     [:input
      {:type  "text"
       :class "round"
       :value (name condition-name)
       :on-change
       (fn [e]
         (dispatch [:reaction/set-condition-name
                    (conj condition-path :name)
                    (fns/ps-event-val e)]))}]
     condition-name)
   [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.condition-title])}]])

(defn- render-condition-errors
  "Render individual condition errors."
  [condition]
  (when (empty? (select-keys condition [:and :or :not :path]))
    [:ul.reaction-error-list
     [:li @(subscribe [:lang/get :reactions.errors.one-clause])]]))

(defn- render-conditions
  [mode conditions]
  (into [:div.conditions]
        (map-indexed
         (fn [idx condition*]
           (let [condition-name (if (contains? #{:edit :new} mode)
                                  (get condition* :name)
                                  (first condition*))
                 condition      (if (contains? #{:edit :new} mode)
                                  condition*
                                  (second condition*))
                 condition-path [:ruleset :conditions idx]]
             [:div.condition
              (when (contains? #{:edit :new} mode)
                [render-condition-name-errors condition-name])
              [render-or-edit-condition-name
               mode condition-path condition-name]
              (when (contains? #{:edit :new} mode)
                [render-condition-errors condition])
              [:div.condition-body
               (when condition ; condition can be nil during edit
                 [render-clause
                  mode
                  condition-path
                  condition])]
              (when (contains? #{:edit :new} mode)
                [delete-icon
                 :to-delete-desc "Condition"
                 :on-click
                 (fn []
                   (dispatch [:reaction/delete-condition idx]))])
              (when (and (contains? #{:edit :new} mode)
                         (= condition {:name condition-name})) ; when empty
                [add-clause
                 condition-path])]))
         conditions)))

(defn- render-identity-paths
  [_mode _identity-paths]
  (let [open? (r/atom false)]
    (fn [_mode _identity-paths]
      [:<>
       [:label {:for "reaction-identity-paths"}
        [:span {:on-click #(swap! open? not)
                :class (str "pane-collapse" (when @open? " expanded"))}
         @(subscribe [:lang/get :reactions.identity-paths])
         [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.identity-path])}]]]
       [:div {:id "reaction-identity-paths"}
        (when @open?
          (let [edit? (contains? #{:edit :new} _mode)]
            [:<>
             (into
              [:ul.identity-paths {:class (when (not edit?) "view")}]
              (map-indexed
               (fn [idx path]
                 (let [path-path [:ruleset :identityPaths idx]]
                   [:li
                    [render-or-edit-path
                     _mode
                     path-path
                     path
                     :remove-fn
                     #(dispatch [:reaction/delete-identity-path idx])
                     :open-next? true]]))
               _identity-paths))
             (when edit?
               [:span.add-identity-path
                [:a {:href "#"
                     :on-click (fn [e]
                                 (fns/ps-event e)
                                 (dispatch [:reaction/add-identity-path]))}
                 @(subscribe [:lang/get :reactions.identity-paths.add])
                 [:img {:src @(subscribe [:resources/icon "add.svg"])}]]])]))]])))

(defn- render-conditions-errors
  "Render out top-level conditions errors, currently there is only one, an empty
  conditions map."
  [conditions]
  (let [empty-err? (empty? conditions)
        dupe-err?  (not (rse/distinct-name-vector? conditions))]
    (when (or empty-err? dupe-err?)
      [:ul.reaction-error-list
       (when empty-err?
         [:li @(subscribe [:lang/get :reactions.errors.one-condition])])
       (when dupe-err?
         [:li @(subscribe [:lang/get :reactions.errors.dupe-condition-names])])])))

(defn- ruleset-view
  [mode
   {:keys [identityPaths
           conditions
           template]}]
  ;; Cannot use <dl> elements here since that would cause DOM nesting errors
  [:div.reaction-ruleset {:id "ruleset-view"}
   ;; TODO: Properly redo divs to remove extraneous nesting
   [:hr]

   [:label {:for "reaction-ruleset-conditions"}
    @(subscribe [:lang/get :reactions.details.ruleset.conditions])
    [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.ruleset.conditions])}]]
   [:div {:id "reaction-ruleset-conditions"}
    (when (contains? #{:edit :new} mode)
      [render-conditions-errors conditions])
    [render-conditions mode conditions]
    (when (contains? #{:edit :new} mode)
      [add-condition :to-add-desc ""])]
   
   [:hr]

   [:label {:for "reaction-ruleset-templates"}
    @(subscribe [:lang/get :reactions.template.title])
    [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.template])}]]
   [:div {:id "reaction-ruleset-conditions"}
    [t/render-or-edit-template mode template]]
   
   [:hr]

   [render-identity-paths
    mode identityPaths]])

(defn- render-error
  [?error]
  (if ?error
    [:dl.reaction-error
     [:dt "Error Type"]
     [:dd
      (case (:type ?error)
        "ReactionQueryError" "Query Error"
        "ReactionTemplateError" "Template Error"
        "ReactionInvalidStatementError" "Invalid Statement Error")]
     [:dt "Error Message"]
     [:dd (:message ?error)]]
    "None"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reaction Actions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- back-button []
  [:input {:type     "button",
           :class    "btn-brand-bold",
           :on-click #(dispatch [:reaction/back-to-list])
           :value    @(subscribe [:lang/get :reactions.buttons.back])}])

(defn- edit-button [id]
  [:input {:type     "button",
           :class    "btn-brand-bold",
           :on-click #(dispatch [:reaction/edit id])
           :value    @(subscribe [:lang/get :reactions.buttons.edit])}])

(defn- download-button [id]
  [:input {:type     "button"
           :class    "btn-brand-bold"
           :on-click #(dispatch [:reaction/download id])
           :value    @(subscribe [:lang/get :reactions.buttons.download])}])

(defn- create-button []
  [:input {:type     "button"
           :class    "btn-brand-bold"
           :on-click #(dispatch [:reaction/save-edit])
           :value    @(subscribe [:lang/get :reactions.buttons.create])}])

(defn- save-button []
  [:input {:type     "button"
           :class    "btn-brand-bold"
           :on-click #(dispatch [:reaction/save-edit])
           :value    @(subscribe [:lang/get :reactions.buttons.save])}])

(defn- revert-button []
  [:input {:type     "button"
           :class    "btn-brand-bold"
           :on-click #(dispatch [:reaction/revert-edit])
           :value    @(subscribe [:lang/get :reactions.buttons.revert])}])

(defn- upload-button []
  [:span
   [:label {:for   "reaction-upload"
            :class "file-input-button"}
    @(subscribe [:lang/get :reactions.buttons.upload])]
   [:input {:id        "reaction-upload"
            :type      "file"
            :class     "hidden-file-input"
            :accept    ".json"
            :on-change (fn [ev]
                         (upload/process-upload-event
                          ev
                          (fn [data]

                            (dispatch [:reaction/upload-edit data]))))}]])

(defn- reaction-focus-actions
  [id]
  [:div {:class "api-keys-table-actions"}
   [back-button]
   [edit-button id]
   [download-button id]])

(defn- reaction-edit-actions
  [error?]
  [:div {:class "api-keys-table-actions"}
   [back-button]
   (when (not error?)
     [save-button])
   [revert-button]])

(defn- reaction-new-actions
  [error?]
  [:div {:class "api-keys-table-actions"}
   [back-button]
   [upload-button]
   (when (not error?)
     [create-button])])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reaction View
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- edit-title
  [title]
  [:input
   {:type  "text"
    :class "round"
    :value title
    :on-change (fn [e]
                 (dispatch
                  [:reaction/edit-title
                   (fns/ps-event-val e)]))}])

(defn- edit-status
  [active]
  [:select
   {:value (if (true? active) "active" "inactive")
    :class "round"
    :on-change (fn [e]
                 (dispatch
                  [:reaction/edit-status
                   (fns/ps-event-val e)]))}
   [:option
    {:value "active"}
    "Active"]
   [:option
    {:value "inactive"}
    "Inactive"]])

(defn- reaction-info-panel-right [{:keys [created modified error]}]
  [:dl.reaction-info-panel.right
   [:dt @(subscribe [:lang/get :reactions.details.created])]
   [:dd (or (iso8601->local-display created) "[New]")]
  
   [:dt @(subscribe [:lang/get :reactions.details.modified])]
   [:dd (or (iso8601->local-display modified) "[New]")]
  
   [:dt @(subscribe [:lang/get :reactions.details.error])]
   [:dd [render-error error]]])

(defn- reaction-info-title-dt []
  [:dt @(subscribe [:lang/get :reactions.details.title])
   [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.reaction-title])}]])

(defn- reaction-info-id-dt []
  [:dt @(subscribe [:lang/get :reactions.details.id])
   [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.reaction-id])}]])

(defn- reaction-info-status-dt []
  [:dt @(subscribe [:lang/get :reactions.details.status])
   [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.reaction-status])}]])

(defn- reaction-info-panel-focus [{:keys [id title active] :as reaction}]
  [:div.reaction-info-panel
   [reaction-info-panel-right reaction]
   [:dl.reaction-info-panel.left
    [reaction-info-title-dt]
    [:dd title]
    
    [reaction-info-id-dt]
    [:dd id]
    
    [reaction-info-status-dt]
    [:dd (if active "Active" "Inactive")]]])

(defn- reaction-info-panel-edit [{:keys [id title active] :as reaction}]
  [:div.reaction-info-panel
   [reaction-info-panel-right reaction]
   [:dl.reaction-info-panel.left
    [reaction-info-title-dt]
    [:dd [edit-title title]]
    
    [reaction-info-id-dt]
    [:dd id]
    
    [reaction-info-status-dt]
    [:dd [edit-status active]]]])

(defn- reaction-info-panel-new [{:keys [title active] :as reaction}]
  [:div.reaction-info-panel
   [reaction-info-panel-right reaction]
   [:dl.reaction-info-panel.left
    [reaction-info-title-dt]
    [:dd [edit-title title]]
    
    [reaction-info-status-dt]
    [:dd [edit-status active]]]])

(defn- reaction-edit-invalid
  []
  [:div.reaction-edit-invalid
   @(subscribe [:lang/get :reactions.errors.invalid])])

(defn- reaction-focus []
  (let [{:keys [id ruleset]
         :as reaction} @(subscribe [:reaction/focus])]
    [:div {:class "left-content-wrapper"}
     [:h2 {:class "content-title"}
      @(subscribe [:lang/get :reactions.focus.title])]
     [:div {:class "tenant-wrapper"}
      [reaction-focus-actions id]
      [reaction-info-panel-focus reaction]  
      [ruleset-view :focus ruleset]
      [reaction-focus-actions id]]]))

(defn- reaction-edit []
  (let [{:keys [ruleset]
         :as reaction} @(subscribe [:reaction/editing])
        error?  (or
                 (some? @(subscribe [:reaction/edit-spec-errors]))
                 (seq @(subscribe [:reaction/edit-template-errors])))]
    [:div {:class "left-content-wrapper"}
     [:h2 {:class "content-title"}
      @(subscribe [:lang/get :reactions.edit.title])]
     [:div {:class "tenant-wrapper"}
      [reaction-edit-actions error?]
      (when error? [reaction-edit-invalid])
      [reaction-info-panel-edit reaction]
      [ruleset-view :edit ruleset]
      [reaction-edit-actions error?]]]))

(defn- reaction-new []
  (let [{:keys [ruleset]
         :as reaction} @(subscribe [:reaction/editing])
        error?  (or
                 (some? @(subscribe [:reaction/edit-spec-errors]))
                 (seq @(subscribe [:reaction/edit-template-errors])))]
    [:div {:class "left-content-wrapper"}
     [:h2 {:class "content-title"}
      @(subscribe [:lang/get :reactions.new.title])]
     [:div {:class "tenant-wrapper"}
      [reaction-new-actions error?]
      (when error? [reaction-edit-invalid])
      [reaction-info-panel-new reaction]
      [ruleset-view :new ruleset]
      [reaction-new-actions error?]]]))

(defn reactions
  []
  (let [mode @(subscribe [:reaction/mode])]
    (case mode
      :list
      [reactions-list]
      :focus
      [reaction-focus]
      :edit
      [reaction-edit]
      :new
      [reaction-new])))
