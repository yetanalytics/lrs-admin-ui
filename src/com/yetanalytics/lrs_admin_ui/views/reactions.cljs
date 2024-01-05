(ns com.yetanalytics.lrs-admin-ui.views.reactions
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.reaction :as rfns]
            [com.yetanalytics.lrs-reactions.path :as rpath]
            [com.yetanalytics.lrs-admin-ui.views.form :as form]
            [com.yetanalytics.lrs-admin-ui.views.reactions.path :as p]
            [com.yetanalytics.lrs-admin-ui.views.reactions.template :as t]))

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
     [:th {:scope "col"} "ID"]
     [:th {:scope "col"} "Title"]
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
                       error]} @(subscribe [:reaction/list])]
           [:tr
            [:td {:data-label "ID"} id]
            [:td {:data-label "Title"} title]
            [:td {:data-label "Created"} created]
            [:td {:data-label "Modified"} modified]
            [:td {:data-label "Status"} (if (true? active) "Active" "Inactive")]
            [:td {:data-label "Error"} (if error (short-error error) "[None]")]
            [:td {:data-label "Action"}
             [:a {:href "#!"
                  :on-click (fn [e]
                              (fns/ps-event e)
                              (dispatch [:reaction/set-focus id]))}
              "View"]
             [:a {:href "#!"
                  :on-click (fn [e]
                              (fns/ps-event e)
                              (dispatch [:reaction/edit id]))}
              "Edit"]
             [:a {:href "#!"
                  :on-click (fn [e]
                              (fns/ps-event e)
                              (dispatch [:reaction/delete-confirm id]))}
              "Delete"]]]))])

(defn- reactions-list
  []
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    "Reactions"]
   [:a {:href "#"
        :on-click (fn [e]
                    (fns/ps-event e)
                    (dispatch [:reaction/new]))}
    "New"]
   [:div {:class "tenant-wrapper"}
    [reactions-table]]])

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

(defn- render-or-edit-op
  [mode op-path op]
  (if (contains? #{:edit :new} mode)
    (into [:select
           {:value op
            :on-change (fn [e]
                         (dispatch
                          [:reaction/set-op
                           op-path
                           (fns/ps-event-val e)]))}]
          (for [op ["gt"
                    "lt"
                    "gte"
                    "lte"
                    "eq"
                    "noteq"
                    "like"
                    "contains"]]
            [:option
             {:value op}
             op]))
    [:code op]))

(defn- select-val-type
  [val-path path val]
  (let [{:keys [leaf-type]} (rpath/analyze-path path)]
    (into [:select
           {:value (rfns/val-type val)
            :on-change (fn [e]
                         (dispatch [:reaction/set-val-type
                                    val-path
                                    (fns/ps-event-val e)]))}]
          (for [t (if leaf-type
                    (if (= 'json leaf-type)
                      ["string" "number" "boolean" "null"]
                      [(name leaf-type)])
                    [(rfns/val-type val)])]
            [:option
             {:value t}
             t]))))

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
         :value "null"}]
       "boolean"
       [:select
        {:value (str val)
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
         :on-change
         (fn [e]
           (dispatch [:reaction/set-val
                      val-path
                      (js/parseFloat
                       (fns/ps-event-val e))]))}]
       "string"
       [:input
        {:type "text"
         :value @(subscribe [:test/test-str])
         :on-change
         (fn [e]
           (dispatch [:test/set-test-str (fns/ps-event-val e)])
           #_(dispatch [:reaction/set-val
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

(defn- clause-label
  [mode
   reaction-path
   type-key]
  [:div.boolean-label
   (if (contains? #{:edit :new} mode)
     [:select
      {:value (name type-key)
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
     (case type-key :and "AND" :or "OR" :not "NOT" ""))])

(defn- delete-icon
  [& {:keys [on-click]
      :or {on-click (fn [] (println 'delete))}}]
  [:div.delete-icon
   [:a {:href "#"
        :on-click (fn [e]
                    (fns/ps-event e)
                    (on-click))}
    [:img {:src "/images/icons/icon-delete-blue.svg"}]]])

(defn- add-condition
  []
  [:div.add-icon
   [:a {:href "#"
        :on-click (fn [e]
                    (fns/ps-event e)
                    (dispatch [:reaction/add-condition]))}
    [:img {:src "/images/icons/add.svg"}]]])

(defn- add-clause
  [parent-path]
  [:div.add-clause
   [form/action-dropdown
    {:options [{:value :and
                :label "AND"}
               {:value :or
                :label "OR"}
               {:value :not
                :label "NOT"}
               {:value :logic
                :label "Logic"}]
     :select-fn (fn [v]
                  (dispatch [:reaction/add-clause
                             parent-path
                             v]))}]])

(declare render-clause)

(defn- render-and
  [mode reaction-path and-clauses]
  [:div.clause.boolean.and
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
       :on-click
       (fn []
         (dispatch
          [:reaction/delete-clause reaction-path]))]])])

(defn- render-or
  [mode reaction-path or-clauses]
  [:div.clause.boolean.or
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
       :on-click
       (fn []
         (dispatch
          [:reaction/delete-clause reaction-path]))]])])

(defn- render-not
  [mode reaction-path not-clause]
  [:div.clause.boolean.not
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
           [clause-label mode reaction-path :logic]
           (when (contains? #{:edit :new} mode)
             [render-logic-errors
              reaction-path])
           (cond-> [:dl.op-list
                    [:dt "Path"]
                    [:dd
                     [render-or-edit-path
                      mode
                      (conj reaction-path :path)
                      path]]
                    [:dt "Op"]
                    [:dd [render-or-edit-op
                          mode
                          (conj reaction-path :op)
                          op]]
                    [:dt
                     (if (contains? #{:edit :new} mode)
                       [:select
                        {:value (if ref "ref" "val")
                         :on-change
                         (fn [e]
                           (dispatch [:reaction/set-val-or-ref
                                      reaction-path
                                      (fns/ps-event-val e)]))}
                        [:option
                         {:value "ref"}
                         "Ref"]
                        [:option
                         {:value "val"}
                         "Val"]]
                       (if ref
                         "Ref"
                         "Val"))]]
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
  ^{:key (pr-str reaction-path)}
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
      {:type "text"
       :value condition-name
       :on-change
       (fn [e]
         (dispatch [:reaction/set-condition-name
                    condition-name (keyword (fns/ps-event-val e))]))}]
     condition-name)])

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
               condition])]
           (when (contains? #{:edit :new} mode)
             [delete-icon
              :on-click
              (fn []
                (dispatch [:reaction/delete-condition condition-name]))])
           (when (and (contains? #{:edit :new} mode)
                      (nil? condition))
             [add-clause
              condition-path])])))

(defn- render-identity-paths
  [mode identity-paths]
  [:<>
   [:dt "Identity Paths"
    (when (contains? #{:edit :new} mode)
      [:span.add-identity-path
       [:a {:href "#"
            :on-click (fn [e]
                        (fns/ps-event e)
                        (dispatch [:reaction/add-identity-path]))}
        [:img {:src "/images/icons/add.svg"}]]])]
   [:dd
    (into [:ul.identity-paths]
          (map-indexed
           (fn [idx path]
             (let [path-path [:ruleset :identityPaths idx]]
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
                               true)]))
           identity-paths))]])

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
   [render-identity-paths
    mode identityPaths]
   [:dt "Conditions"]
   [:dd
    (when (contains? #{:edit :new} mode)
      [render-conditions-errors conditions])
    [render-conditions mode conditions]
    (when (contains? #{:edit :new} mode)
      [add-condition])]
   [:dt "Template"]
   [:dd [t/render-or-edit-template mode template]]])

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
  [mode ?id]
  [:div.reaction-actions
   [:a {:href "#!"
        :on-click (fn [e]
                    (fns/ps-event e)
                    (dispatch [:reaction/back-to-list]))}
    "Back"]
   (when (= :focus mode)
     [:a {:href "#!"
          :on-click (fn [e]
                      (fns/ps-event e)
                      (dispatch [:reaction/edit ?id]))}
      "Edit"])
   (when (and (= :edit mode)
              @(subscribe [:reaction/edit-dirty?]))
     [:<>
      [:a {:href "#!"
           :on-click (fn [e]
                       (fns/ps-event e)
                       (dispatch [:reaction/save-edit]))}
       "Save"]
      [:a {:href "#!"
           :on-click (fn [e]
                       (fns/ps-event e)
                       (dispatch [:reaction/revert-edit]))}
       "Revert Changes"]])
   (when (= :new mode)
     [:a {:href "#!"
          :on-click (fn [e]
                      (fns/ps-event e)
                      (dispatch [:reaction/save-edit]))}
      "Create"])])

(defn- edit-title
  [title]
  [:input
   {:type "text"
    :value title
    :on-change (fn [e]
                 (dispatch
                  [:reaction/edit-title
                   (fns/ps-event-val e)]))}])

(defn- edit-status
  [active]
  [:select
   {:value (if (true? active) "active" "inactive")
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
                              [:reaction/editing]))]
    [:div {:class "left-content-wrapper"}
     [:h2 {:class "content-title"}
      (case mode
        :focus "Reaction Details"
        :edit "Edit Reaction"
        :new "New Reaction")]
     [reaction-actions mode id]
     (when (and (contains? #{:edit :new} mode)
                (some? @(subscribe [:reaction/edit-spec-errors])))
       [:div.reaction-edit-invalid
        "Reaction is invalid, see below."])
     [:div {:class "tenant-wrapper"}
      [:dl.reaction-view
       [:dt "Title"]
       [:dd
        (case mode
          :focus title
          [edit-title title])]

       (when (contains? #{:focus :edit} mode)
         [:<>
          [:dt "ID"]
          [:dd id]])

       [:dt "Status"]
       [:dd
        (case mode
          :focus (if active "Active" "Inactive")
          [edit-status active])]

       (when (contains? #{:focus :edit} mode)
         [:<>
          [:dt "Created"]
          [:dd (or created "[New]")]

          [:dt "Modified"]
          [:dd (or modified "[New]")]

          [:dt "Error"]
          [:dd [render-error error]]])

       [:dt "Ruleset"]
       [:dd [ruleset-view mode ruleset]]]]]))

(defn reactions
  []
  (let [mode @(subscribe [:reaction/mode])]
    (if (= :list mode)
      [reactions-list]
      [reaction-view mode])))
