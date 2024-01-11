(ns com.yetanalytics.lrs-admin-ui.views.reactions
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.reaction :as rfns]
            [com.yetanalytics.lrs-reactions.path :as rpath]
            [com.yetanalytics.lrs-admin-ui.functions.time :refer [iso8601->local-display]]
            [com.yetanalytics.lrs-admin-ui.functions.tooltip :refer [tooltip-info]]
            [com.yetanalytics.lrs-admin-ui.views.form :as form]
            [com.yetanalytics.lrs-admin-ui.views.reactions.path :as p]
            [com.yetanalytics.lrs-admin-ui.views.reactions.template :as t]
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
     [:th {:scope "col"} "Title"]
     [:th {:scope "col"} "# of Conditions"]
     [:th {:scope "col"} "Created"]
     [:th {:scope "col"} "Modified"]
     [:th {:scope "col"} "Status"]
     [:th {:scope "col"} "Error"]
     [:th {:scope "col" :class "action"} "Action"]]]
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
            [:td {:data-label "Title"} title]
            [:td {:data-label "# of Conditions"} (count (:conditions ruleset))]
            [:td {:data-label "Created"} (iso8601->local-display created)]
            [:td {:data-label "Modified"} (iso8601->local-display modified)]
            [:td {:data-label "Status"} (if (true? active) "Active" "Inactive")]
            [:td {:data-label "Error"} (if error (short-error error) "[None]")]
            [:td {:data-label "Action"}
             [:ul {:class "action-icon-list"}
              [:li 
               [:a {:href "#!"
                    :class "icon-edit"
                    :on-click (fn [e]
                                (fns/ps-event e)
                                (dispatch [:reaction/edit id]))}
                "Edit"]]
              [:li
               [:a {:href "#!"
                    :class "icon-delete"
                    :on-click (fn [e]
                                (fns/ps-event e)
                                (dispatch [:reaction/delete-confirm id]))}
                "Delete"]]]]]))])

(defn- reactions-list
  []
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    "Reactions"
    [tooltip-info {:value "Reactions is a new functionality for SQL LRS that allows for the generation of custom xAPI statements triggered by other statements posted to the LRS. An administrator can configure rulesets that match one or more incoming xAPI statement(s), based on conditions, and generate a custom statement which is added to the LRS. -- This can be used for statement transformation (e.g. integration with systems expecting a certain statement format the provider does not make) and statement aggregation (e.g. generate summary statements or assertions about groups of statements)."}]
    " (Beta)"]
   [:p ]
   [:div {:class "tenant-wrapper"}
    [:div {:class "api-keys-table-actions"}
     [:input {:type "button",
              :class "btn-blue-bold",
              :on-click #(dispatch [:reaction/new])
              :value "ADD NEW REACTION"}]]
    [reactions-table]
    [:div {:class "api-keys-table-actions"}
     [:input {:type "button",
              :class "btn-blue-bold",
              :on-click #(dispatch [:reaction/new])
              :value "ADD NEW REACTION"}]]]])

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
                                 spec-valid?]
                          :or {spec-valid? true}}]
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
                             seg-val]))
     :remove-fn remove-fn
     :spec-valid? spec-valid?]
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

(def clause-type-tooltips
  {:and   "AND Clause: All sub-clauses must be true for the statement to match this clause. Requires at least 1 sub-clause."
   :or    "OR Clause: One of the sub-clauses must be true for the statement to match this clause. Requires at leat 1 sub-clause."
   :not   "NOT Clause: The single sub-clause must return false for the statement to match this clause. Requires one sub-clause."
   :logic "Logic Clause: The comparison detailed in this clause must resolve to true for the statement to match this clause."})

(defn- clause-label
  [mode
   reaction-path
   type-key]
  [:div.clause-type-label
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
        {:value "and"}
        "AND"]
       [:option
        {:value "or"}
        "OR"]
       [:option
        {:value "not"}
        "NOT"]
       [:option
        {:value "logic"}
        "Logic"]]
      [tooltip-info {:value (get clause-type-tooltips type-key)}]]
     
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
    (gstr/format "Delete %s " to-delete-desc)
    [:img {:src "/images/icons/icon-delete-blue.svg"}]]])

(defn- add-condition
  []
  [:div.add-icon
   [:a {:href "#"
        :on-click (fn [e]
                    (fns/ps-event e)
                    (dispatch [:reaction/add-condition]))}
    "Add New Condition "
    [:img {:src "/images/icons/add.svg"}]]])

(defn- add-clause
  [parent-path]
  [:div.add-clause
   [form/action-dropdown
    {:options     [{:value :and
                    :label "AND"}
                   {:value :or
                    :label "OR"}
                   {:value :not
                    :label "NOT"}
                   {:value :logic
                    :label "Logic"}]
     :label       "Add Clause"
     :label-left? true
     :class       "round"
     :select-fn   (fn [v]
                    (dispatch [:reaction/add-clause
                               parent-path
                               v]))}]])

(declare render-clause)

(defn clause-nest-class
  [reaction-path]
  (if (-> (split-at 3 reaction-path)
          second
          count
          (/ 2) even?)
    "even" "odd"))

(defn- render-and
  [mode reaction-path and-clauses]
  [:div.clause.boolean.and
   {:class (clause-nest-class reaction-path)}
   [clause-label mode reaction-path :and]
   (when (empty? and-clauses)
     [:ul.reaction-error-list
      [:li
       "AND must have at least one clause."]])
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
       :to-delete-desc "'AND' Clause"
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
      [:li
       "OR must have at least one clause."]])
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
       :to-delete-desc "'OR' Clause"
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
      [:li
       "NOT must specify a clause."]])
   [:div.boolean-body
    (when not-clause
      [render-clause mode (conj reaction-path :not) not-clause])]
   (when (and (contains? #{:edit :new} mode)
              (nil? not-clause))
     [add-clause
      (conj reaction-path :not)])
   (when (contains? #{:edit :new} mode)
     [delete-icon
      :to-delete-desc "'NOT' Clause"
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
          "Incomplete path."])
        (pred-set
         'com.yetanalytics.lrs-reactions.spec/valid-like-val?)
        (conj [:li
               "The 'like' op only supports string values."])))))

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
                    [:dt "Statement Path"
                     [tooltip-info {:value "Path is how you identify which part of a matching statement you are comparing. For instance `$.object.id` means we are comparing the statement object's id field. These are limited to xAPI specification except for extensions where you can write in the variable part of the path directly."}]]
                    [:dd
                     [render-or-edit-path
                      mode
                      (conj reaction-path :path)
                      path]]
                    [:dt "Operation"
                     [tooltip-info {:value "Operation represents the method with which to compare the values. For instance `Equals` means the value at the statement path above must exactly match the Value or Reference below."}]]
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
                         "Reference"]
                        [:option
                         {:value "val"}
                         "Value"]]
                       (if ref
                         "Reference"
                         "Value"))
                     [tooltip-info {:value "This field determines what kind of data we are comparing the statement field to. It can either be a literal `Value` manually entered here or a `Reference` to a field in another matching condition to produce interdependent conditions. For `Value` entries, the data type may be automatically assigned based on xAPI Specification."}]]]
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
              :to-delete-desc "Logic Clause"
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
    :else
    [render-logic
     mode reaction-path clause]))

(defn- render-or-edit-condition-name
  [mode condition-name]
  [:div.condition-name
   (if (contains? #{:edit :new} mode)
     [:input
      {:type  "text"
       :class "round"
       :value condition-name
       :on-change
       (fn [e]
         (dispatch [:reaction/set-condition-name
                    condition-name (keyword (fns/ps-event-val e))]))}]
     condition-name)
   [tooltip-info {:value "This is the title of the Condition. It is given a generated name on creation but can be customized. It may be used in `Logic Clauses` to reference between Conditions."}]])

(defn- render-condition-errors
  "Render individual condition errors."
  [condition]
  (when (empty? (select-keys condition [:and :or :not :path]))
    [:ul.reaction-error-list
     [:li
      "Condition must have at least one clause."]]))

(defn- render-conditions
  [mode conditions]
  (into [:div.reaction-conditions]
        (for [[condition-name condition]
              (sort-by
               (comp :sort-idx val)
               conditions)
              :let [condition-path [:ruleset :conditions condition-name]]]
          [:div.condition
           [render-or-edit-condition-name
            mode condition-name]
           (when (contains? #{:edit :new} mode)
             [render-condition-errors condition])
           [:div.condition-body
            ;; condition can be nil during edit
            (when condition
              [render-clause
               mode
               condition-path
               condition
               1])]
           (when (contains? #{:edit :new} mode)
             [delete-icon
              :to-delete-desc "Condition"
              :on-click
              (fn []
                (dispatch [:reaction/delete-condition condition-name]))])
           (when (and (contains? #{:edit :new} mode)
                      (nil? condition))
             [add-clause
              condition-path])])))

(defn- render-identity-paths
  [mode identity-paths]
  (let [open? (r/atom false)
        edit? (contains? #{:edit :new} mode)]
    (fn [mode identity-paths]
      [:<>
       [:dt 
        {:on-click #(swap! open? not)
         :class (str "paths-collapse" (when @open? " expanded"))}
        "Identity Paths (Advanced)"
        [tooltip-info {:value "USE WITH CAUTION. Identity Paths are a method of grouping statements for which you are attempting to match conditions. Typically, Reactions may revolve around actor, e.g. `$.actor.mbox` or `$.actor.account.name` which is equivalent to saying \"For a given Actor, look for statements that match the Conditions below\". This is what the default is set to. Alternative approaches to Identity Path may be used by modifying this section, for instance `$.context.registration` to group statements by learning session."}]]
       [:dd
        (when @open?
          [:<>
           (into [:ul.identity-paths]
                 (map-indexed
                  (fn [idx path]
                    (let [path-path [:ruleset :identityPaths idx]]
                      [:<>
                       [render-or-edit-path
                        mode
                        path-path
                        path
                        :remove-fn (fn []
                                     (dispatch [:reaction/delete-identity-path idx]))
                        :spec-valid? (if (contains? #{:edit :new} mode)
                                       (if (not-empty
                                            @(subscribe
                                              [:reaction/edit-spec-errors-in path-path]))
                                         false
                                         true)
                                       true)]
                       (when (not edit?) [:br])]))
                  identity-paths))
           (when edit?
            [:span.add-identity-path
             [:a {:href "#"
                  :on-click (fn [e]
                              (fns/ps-event e)
                              (dispatch [:reaction/add-identity-path]))}
              "Add New Identity Path "
              [:img {:src "/images/icons/add.svg"}]]])])]])))

(defn- render-conditions-errors
  "Render out top-level conditions errors, currently there is only one, an empty
  conditions map."
  [conditions]
  (when (empty? conditions)
    [:ul.reaction-error-list
     [:li
      "Ruleset must specify at least one condition."]]))

(defn- ruleset-view
  [mode
   {:keys [identityPaths
           conditions
           template]}]
  [:dl.reaction-ruleset
   [:dt "Conditions"
    [tooltip-info {:value "This part of a ruleset controls the criteria for which statements match in a Reaction."}]]
   [:dd
    (when (contains? #{:edit :new} mode)
      [render-conditions-errors conditions])
    [render-conditions mode conditions]
    (when (contains? #{:edit :new} mode)
      [add-condition :to-add-desc "Weeeee"])]
   [:dt "Template"
    [tooltip-info {:value "This is where you design the custom statement to be generated and stored in the event of matching statements for this Reaction. Variables from the statements matching individual conditions can be injected into the custom statement."}]]
   [:dd [t/render-or-edit-template mode template]]
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

(defn- reaction-actions
  [mode ?id error?]
  [:<>
   [:div {:class "api-keys-table-actions"}
    [:input {:type "button",
             :class "btn-blue-bold",
             :on-click #(dispatch [:reaction/back-to-list])
             :value "BACK"}] 
    (when (= :focus mode)
      [:input {:type "button",
               :class "btn-blue-bold",
               :on-click #(dispatch [:reaction/edit ?id])
               :value "EDIT"}] )
    (when (and (= :edit mode)
               @(subscribe [:reaction/edit-dirty?]))
      [:<>
       (when (not error?)
         [:input {:type "button",
                  :class "btn-blue-bold",
                  :on-click #(dispatch [:reaction/save-edit])
                  :value "SAVE"}]) 
       [:input {:type "button",
                :class "btn-blue-bold",
                :on-click #(dispatch [:reaction/revert-edit])
                :value "REVERT CHANGES"}]])
    (when (and (= :new mode) (not error?))
      [:input {:type "button",
               :class "btn-blue-bold",
               :on-click #(dispatch [:reaction/save-edit])
               :value "CREATE"}])]])

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

(defn- reaction-view
  [mode]
  (let [{:keys [id
                title
                active
                created
                modified
                error
                ruleset]} @(subscribe
                            (case mode
                              :focus [:reaction/focus]
                              [:reaction/editing]))
        error?  (and (contains? #{:edit :new} mode)
                     (some? @(subscribe [:reaction/edit-spec-errors])))]
    [:div {:class "left-content-wrapper"}
     [:h2 {:class "content-title"}
      (case mode
        :focus "Reaction Details"
        :edit "Edit Reaction"
        :new "New Reaction")]
     [:div {:class "tenant-wrapper"}
      [reaction-actions mode id error?]
      (when error?
        [:div.reaction-edit-invalid
         "Reaction is invalid, see below."])
      [:dl.reaction-view
       [:div {:class "reaction-info-panel"}
        (when (contains? #{:focus :edit} mode)
          [:<>
           [:dt "Created"]
           [:dd (or (iso8601->local-display created) "[New]")]
       
           [:dt "Modified"]
           [:dd (or (iso8601->local-display modified) "[New]")]
       
           [:dt "Error"]
           [:dd [render-error error]]])]
       [:dt "Title"
        [tooltip-info {:value "This is the title of the Reaction you are creating/editing. It has no effect on Reaction functionality."}]]
       [:dd
        (case mode
          :focus title
          [edit-title title])]

       (when (contains? #{:focus :edit} mode)
         [:<>
          [:dt "ID"
           [tooltip-info {:value "This is the system ID of the Reaction you are creating/editing. It has no effect on Reaction functionality, but may be useful for error tracing."}]]
          [:dd id]])

       [:dt "Status"
        [tooltip-info {:value "This field sets whether the Reaction is turned on or not. If set to Active it will generate statements based on the rulesets provided."}]]
       [:dd
        (case mode
          :focus (if active "Active" "Inactive")
          [edit-status active])]

       [:dt "Ruleset"]
       [:dd [ruleset-view mode ruleset]]]
      [reaction-actions mode id error?]]]))

(defn reactions
  []
  (let [mode @(subscribe [:reaction/mode])]
    (if (= :list mode)
      [reactions-list]
      [reaction-view mode])))
