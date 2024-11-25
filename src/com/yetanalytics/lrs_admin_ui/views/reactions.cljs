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

(defn- path-focus
  [path]
  [:code (rfns/path->string path)])

(defn- path-edit
  [path-path path & {:keys [remove-fn
                            validate?
                            open-next?]
                     :or   {validate?  true
                            open-next? false}}]
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
   :validate? validate?])

(def ops {"eq"       "Equal"
          "gt"       "Greater Than"
          "lt"       "Less Than"
          "gte"      "Greater Than or Equal"
          "lte"      "Less Than or Equal"
          "noteq"    "Not Equal"
          "like"     "Like (String Matching)"
          "contains" "Contains (Array Element)"})

(defn- op-focus
  [op]
  [:code (get ops op)])

(defn- op-edit
  [op-path op]
  (into [:select
         {:value     op
          :class     "round"
          :on-change (fn [e]
                       (dispatch
                        [:reaction/set-op
                         op-path
                         (fns/ps-event-val e)]))}
         (for [[k v] ops]
           ^{:key (gstr/format "condition-op-%s-%s" op-path k)}
           [:option {:value k} v])]))

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

(defn- val-focus
  [val]
  [:div.val
   [:span (str (val-type val) ": ")]
   [:code (if (nil? val) "null" (str val))]])

(defn- val-edit
  [val-path path val]
  [:div.val
   [select-val-type val-path path val]
   [val-input val-path path val]])

(defn- ref-condition-focus
  [condition]
  [:span condition])

(defn- ref-condition-edit
  [ref-condition-path condition]
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
           condition-name])))

(defn- ref-focus
  [{:keys [condition path]}]
  [:dl.ref
   [:dt "Condition"]
   [:dd [ref-condition-focus condition]]
   [:dt "Path"]
   [:dd [path-focus path]]])

(defn- ref-edit
  [ref-path {:keys [condition path]}]
  [:dl.ref
   [:dt "Condition"]
   [:dd [ref-condition-edit
         (conj ref-path :condition)
         condition]]
   [:dt "Path"]
   [:dd [path-edit
         (conj ref-path :path)
         path]]])

(defn clause-type-tooltips [key]
  (get {:and   @(subscribe [:lang/get :tooltip.reactions.clause-type.and])
        :or    @(subscribe [:lang/get :tooltip.reactions.clause-type.or])
        :not   @(subscribe [:lang/get :tooltip.reactions.clause-type.not])
        :logic @(subscribe [:lang/get :tooltip.reactions.clause-type.logic])}
       key))

(defn- clause-label-focus
  [type-key]
  [:div.clause-type-label
   {:class (when-not (contains? #{:and :or :not} type-key)
             "empty")}
   (case type-key :and "AND" :or "OR" :not "NOT" "")])

(defn- clause-label-edit
  [reaction-path type-key]
  [:div.clause-type-label
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
   [tooltip-info {:value (clause-type-tooltips type-key)}]])

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

(declare clause-focus)
(declare clause-edit)

(defn clause-nest-class
  [reaction-path]
  (let [ops (filter keyword? (subvec reaction-path 3))]
    (if (even? (count ops)) "even" "odd")))

(defn- and-clauses-focus
  [reaction-path and-clauses]
  [:div.clause.boolean.and
   {:class (clause-nest-class reaction-path)}
   [clause-label-focus :and]
   (when (empty? and-clauses)
     [:ul.reaction-error-list
      [:li @(subscribe [:lang/get :reactions.details.conditions.and-instructions])]])
   (into [:div.boolean-body]
         (map-indexed
          (fn [idx clause]
            [clause-focus
             (into reaction-path [:and idx])
             clause])
          and-clauses))])

(defn- and-clauses-edit
  [reaction-path and-clauses]
  [:div.clause.boolean.and
   {:class (clause-nest-class reaction-path)}
   [clause-label-edit reaction-path :and]
   (when (empty? and-clauses)
     [:ul.reaction-error-list
      [:li @(subscribe [:lang/get :reactions.details.conditions.and-instructions])]])
   (into [:div.boolean-body]
         (map-indexed
          (fn [idx clause]
            [clause-edit
             (into reaction-path [:and idx])
             clause])
          and-clauses))
   [add-clause
    (conj reaction-path :and)]
   [delete-icon
    :to-delete-desc "'Boolean AND' clause"
    :on-click
    (fn []
      (dispatch
       [:reaction/delete-clause reaction-path]))]])

(defn- or-clauses-focus
  [reaction-path or-clauses]
  [:div.clause.boolean.or
   {:class (clause-nest-class reaction-path)}
   [clause-label-focus :or]
   (when (empty? or-clauses)
     [:ul.reaction-error-list
      [:li @(subscribe [:lang/get :reactions.details.conditions.or-instructions])]])
   (into [:div.boolean-body]
         (map-indexed
          (fn [idx clause]
            [clause-focus
             (into reaction-path [:or idx])
             clause])
          or-clauses))])

(defn- or-clauses-edit
  [reaction-path or-clauses]
  [:div.clause.boolean.or
   {:class (clause-nest-class reaction-path)}
   [clause-label-edit reaction-path :or]
   (when (empty? or-clauses)
     [:ul.reaction-error-list
      [:li @(subscribe [:lang/get :reactions.details.conditions.or-instructions])]])
   (into [:div.boolean-body]
         (map-indexed
          (fn [idx clause]
            [clause-edit
             (into reaction-path [:or idx])
             clause])
          or-clauses))
   [add-clause
    (conj reaction-path :or)]
   [delete-icon
    :to-delete-desc "'Boolean OR' clause"
    :on-click
    (fn []
      (dispatch
       [:reaction/delete-clause reaction-path]))]])

(defn- not-clause-focus
  [reaction-path not-clause]
  [:div.clause.boolean.not
   {:class (clause-nest-class reaction-path)}
   [clause-label-focus :not]
   (when (nil? not-clause)
     [:ul.reaction-error-list
      [:li @(subscribe [:lang/get :reactions.details.conditions.not-instructions])]])
   [:div.boolean-body
    (when not-clause
      [clause-focus (conj reaction-path :not) not-clause])]])

(defn- not-clause-edit
  [reaction-path not-clause]
  [:div.clause.boolean.not
   {:class (clause-nest-class reaction-path)}
   [clause-label-edit reaction-path :not]
   (when (nil? not-clause)
     [:ul.reaction-error-list
      [:li @(subscribe [:lang/get :reactions.details.conditions.not-instructions])]])
   [:div.boolean-body
    (when not-clause
      [clause-edit (conj reaction-path :not) not-clause])]
   (when (nil? not-clause)
     [add-clause
      (conj reaction-path :not)])
   [delete-icon
    :to-delete-desc "'Boolean NOT' clause"
    :on-click
    (fn []
      (dispatch
       [:reaction/delete-clause reaction-path]))]])

(defn- logic-errors
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

(defn- logic-clause-focus
  [reaction-path clause]
  (let [{:keys [path op val ref]} clause]
    [:div.clause.op
     {:class (clause-nest-class reaction-path)}
     [clause-label-focus :logic]
     [:dl.op-list
      [:dt @(subscribe [:lang/get :reactions.details.conditions.statement-path])
       [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.statement-path])}]]
      [:dd [path-focus path]]
      [:dt @(subscribe [:lang/get :reactions.details.conditions.operation])
       [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.operation])}]]
      [:dd [op-focus op]]
      [:dt
       (if ref
         @(subscribe [:lang/get :reactions.details.conditions.reference])
         @(subscribe [:lang/get :reactions.details.conditions.value]))
       [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.comparator])}]]
      (if ref
        [:dd [ref-focus ref]]
        [:dd [val-focus val]])]]))

(defn- logic-clause-edit
  [reaction-path clause]
  (let [{:keys [path op val ref]} clause]
    [:div.clause.op
     {:class (clause-nest-class reaction-path)}
     [clause-label-edit reaction-path :logic]
     [logic-errors reaction-path]
     [:dl.op-list
      [:dt @(subscribe [:lang/get :reactions.details.conditions.statement-path])
       [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.statement-path])}]]
      [:dd
       [path-edit
        (conj reaction-path :path)
        path
        :open-next? true]]
      [:dt @(subscribe [:lang/get :reactions.details.conditions.operation])
       [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.operation])}]]
      [:dd [op-edit
            (conj reaction-path :op)
            op]]
      [:dt
       [:select
        {:value     (if ref "ref" "val")
         :class     "round short"
         :on-change (fn [e]
                      (dispatch [:reaction/set-val-or-ref
                                 reaction-path
                                 (fns/ps-event-val e)]))}
        [:option
         {:value "ref"}
         @(subscribe [:lang/get :reactions.details.conditions.reference])]
        [:option
         {:value "val"}
         @(subscribe [:lang/get :reactions.details.conditions.value])]]
       [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.comparator])}]]
      (if ref
        [:dd [ref-edit
              (conj reaction-path :ref)
              ref]]
        [:dd [val-edit
              (conj reaction-path :val)
              path
              val]])]
     [delete-icon
      :to-delete-desc "Statement Criteria"
      :on-click
      (fn []
        (dispatch
         [:reaction/delete-clause reaction-path]))]]))

(defn- clause-focus
  [reaction-path
   {and-clauses :and
    or-clauses  :or
    not-clause  :not
    :keys [op sort-idx]
    :as clause}]
  (cond
    and-clauses
    [and-clauses-focus reaction-path and-clauses]
    or-clauses
    [or-clauses-focus reaction-path or-clauses]
    (find clause :not)
    [not-clause-focus reaction-path not-clause]
    op
    [logic-clause-focus reaction-path clause]
    ;; if it is top-level & empty, do not render
    sort-idx nil))

(defn- clause-edit
  [reaction-path
   {and-clauses :and
    or-clauses  :or
    not-clause  :not
    :keys [op sort-idx]
    :as clause}]
  (cond
    and-clauses
    [and-clauses-edit reaction-path and-clauses]
    or-clauses
    [or-clauses-edit reaction-path or-clauses]
    (find clause :not)
    [not-clause-edit reaction-path not-clause]
    op
    [logic-clause-edit reaction-path clause]
    ;; if it is top-level & empty, do not render
    sort-idx nil))

(defn- condition-name-errors
  [condition-name]
  (when (not (rse/keywordizable-string? condition-name))
    [:ul.reaction-error-list
     [:li @(subscribe [:lang/get :reactions.errors.invalid-condition-name])]]))

(defn- condition-name-focus
  [condition-name]
  [:div.condition-name
   condition-name
   [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.condition-title])}]])

(defn- condition-name-edit
  [condition-path condition-name]
  [:div.condition-name
   [:input
    {:type      "text"
     :class     "round"
     :value     (name condition-name)
     :on-change (fn [e]
                  (dispatch [:reaction/set-condition-name
                             (conj condition-path :name)
                             (fns/ps-event-val e)]))}]
   [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.condition-title])}]])

(defn- condition-errors
  "Render individual condition errors."
  [condition]
  (when (empty? (select-keys condition [:and :or :not :path]))
    [:ul.reaction-error-list
     [:li @(subscribe [:lang/get :reactions.errors.one-clause])]]))

(defn- conditions-focus
  [conditions]
  (into [:div.conditions]
        (map-indexed
         (fn [idx condition*]
           (let [condition-name (first condition*)
                 condition      (second condition*)
                 condition-path [:ruleset :conditions idx]]
             [:div.condition
              [condition-name-focus condition-name]
              [:div.condition-body
               [clause-focus condition-path condition]]]))
         conditions)))

(defn- conditions-errors
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

(defn- conditions-edit*
  [conditions]
  (into [:div.conditions]
        (map-indexed
         (fn [idx condition*]
           (let [condition-name (get condition* :name)
                 condition      condition*
                 condition-path [:ruleset :conditions idx]]
             [:div.condition
              [condition-name-errors condition-name]
              [condition-name-edit condition-path condition-name]
              [condition-errors condition]
              [:div.condition-body
               (when condition ; condition can be nil during edit
                 [clause-edit condition-path condition])]
              [delete-icon
               :to-delete-desc "Condition"
               :on-click
               (fn []
                 (dispatch [:reaction/delete-condition idx]))]
              (when (= condition {:name condition-name}) ; when empty
                [add-clause condition-path])]))
         conditions)))

(defn- conditions-edit
  [conditions]
  [:<>
   [conditions-errors conditions]
   [conditions-edit* conditions]
   [add-condition :to-add-desc ""]])

(defn- identity-paths-focus*
  [identity-paths]
  (into
   [:ul.identity-paths {:class "view"}]
   (map (fn [path]
          [:li [path-focus path]])
        identity-paths)))

(defn- identity-paths-edit*
  [identity-paths]
  [:<>
   (into
    [:ul.identity-paths]
    (map-indexed
     (fn [idx path]
       (let [path-path [:ruleset :identityPaths idx]]
         [:li
          [path-edit
           path-path
           path
           :remove-fn
           #(dispatch [:reaction/delete-identity-path idx])
           :open-next? true]]))
     identity-paths))
   [:span.add-identity-path
    [:a {:href "#"
         :on-click (fn [e]
                     (fns/ps-event e)
                     (dispatch [:reaction/add-identity-path]))}
     @(subscribe [:lang/get :reactions.identity-paths.add])
     [:img {:src @(subscribe [:resources/icon "add.svg"])}]]]])

(defn- identity-paths-view
  [_ _]
  (let [open? (r/atom false)]
    (fn [identity-paths-view* identity-paths]
      [:<>
       [:label {:for "reaction-identity-paths"}
        [:span {:on-click #(swap! open? not)
                :class    (str "pane-collapse" (when @open? " expanded"))}
         @(subscribe [:lang/get :reactions.identity-paths])
         [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.identity-path])}]]]
       [:div {:id "reaction-identity-paths"}
        (when @open?
          [identity-paths-view* identity-paths])]])))

(defn- identity-paths-focus
  [identity-paths]
  [identity-paths-view identity-paths-focus* identity-paths])

(defn- identity-paths-edit
  [identity-paths]
  [identity-paths-view identity-paths-edit* identity-paths])

(defn- reaction-ruleset-view
  [conditions-view template-view identity-paths-view
   {:keys [conditions template identityPaths]}]
  [:div.reaction-ruleset {:id "ruleset-view"}
   ;; TODO: Properly redo divs to remove extraneous nesting
   [:hr]
   [:label {:for "reaction-ruleset-conditions"}
    @(subscribe [:lang/get :reactions.details.ruleset.conditions])
    [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.ruleset.conditions])}]]
   [:div {:id "reaction-ruleset-conditions"}
    [conditions-view conditions]]
   [:hr]
   [:label {:for "reaction-ruleset-templates"}
    @(subscribe [:lang/get :reactions.template.title])
    [tooltip-info {:value @(subscribe [:lang/get :tooltip.reactions.template])}]]
   [:div {:id "reaction-ruleset-templates"}
    [template-view template]]
   [:hr]
   [identity-paths-view identityPaths]])

(defn- reaction-ruleset-focus
  [ruleset]
  [reaction-ruleset-view
   conditions-focus
   t/template-focus
   identity-paths-focus
   ruleset])

(defn- reaction-ruleset-edit
  [ruleset]
  [reaction-ruleset-view
   conditions-edit
   t/template-edit
   identity-paths-edit
   ruleset])

(defn- reaction-error
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

(defn- reaction-actions-focus
  [id]
  [:div {:class "api-keys-table-actions"}
   [back-button]
   [edit-button id]
   [download-button id]])

(defn- reaction-actions-edit
  [error?]
  [:div {:class "api-keys-table-actions"}
   [back-button]
   (when (not error?)
     [save-button])
   [revert-button]])

(defn- reaction-actions-new
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
   [:dd [reaction-error error]]])

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
      [reaction-actions-focus id]
      [reaction-info-panel-focus reaction]  
      [reaction-ruleset-focus ruleset]
      [reaction-actions-focus id]]]))

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
      [reaction-actions-edit error?]
      (when error? [reaction-edit-invalid])
      [reaction-info-panel-edit reaction]
      [reaction-ruleset-edit ruleset]
      [reaction-actions-edit error?]]]))

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
      [reaction-actions-new error?]
      (when error? [reaction-edit-invalid])
      [reaction-info-panel-new reaction]
      [reaction-ruleset-edit ruleset]
      [reaction-actions-new error?]]]))

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
