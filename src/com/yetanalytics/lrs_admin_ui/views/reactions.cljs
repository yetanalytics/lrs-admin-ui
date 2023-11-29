(ns com.yetanalytics.lrs-admin-ui.views.reactions
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.reaction :as rfns]
            [com.yetanalytics.lrs-admin-ui.views.form :as form]
            [com.yetanalytics.lrs-admin-ui.views.reactions.path :as p]
            [com.yetanalytics.lrs-admin-ui.views.reactions.template :as t]
            [com.yetanalytics.lrs-admin-ui.views.reactions.errors :as e]
            [goog.string :refer [format]]
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

(defn- render-val
  [val]
  [:div.val
   (str (val-type val) ": ")
   [:code (str val)]])


(defn- render-or-edit-path
  [mode path-path path & {:keys [remove-fn]}]
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
     :remove-fn remove-fn]
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
  (let [{:keys [leaf-type]} (rfns/analyze-path path)]
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
  (let [{:keys [leaf-type]} (rfns/analyze-path path)
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

(defn- and-or-label
  [mode
   reaction-path
   bool-key]
  [:div.boolean-label
   (if (contains? #{:edit :new} mode)
     [:select
      {:value (name bool-key)
       :on-change
       (fn [e]
         (dispatch [:reaction/and-or-toggle
                    reaction-path
                    (keyword (fns/ps-event-val e))]))}
      [:option
       {:value "and"}
       "AND"]
      [:option
       {:value "or"}
       "OR"]]
     (case bool-key :and "AND" :or "OR"))])

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

(defn- render-clause
  [mode
   reaction-path
   {and-clauses :and
    or-clauses  :or
    not-clause  :not
    :as clause}]
  (let [attrs {:class
               (if (and (contains? #{:edit :new} mode)
                        (not-empty
                         @(subscribe
                           [:reaction/edit-ruleset-spec-errors-at-path
                            (rest reaction-path)])))
                 "reaction-invalid"
                 "")}]
    (-> (cond
          and-clauses
          [:div.clause.boolean.and
           attrs
           [and-or-label mode reaction-path :and]
           (into [:div.boolean-body]
                 (map-indexed
                  (fn [idx clause]
                    [render-clause
                     mode
                     (into reaction-path [:and idx])
                     clause])
                  and-clauses))
           (when (contains? #{:edit :new} mode)
             [add-clause
              (conj reaction-path :and)])]
          or-clauses
          [:div.clause.boolean.or
           attrs
           [and-or-label mode reaction-path :or]
           (into [:div.boolean-body]
                 (map-indexed
                  (fn [idx clause]
                    [render-clause
                     mode
                     (into reaction-path [:or idx])
                     clause])
                  or-clauses))
           (when (contains? #{:edit :new} mode)
             [add-clause
              (conj reaction-path :or)])]
          (find clause :not)
          [:div.clause.boolean.not
           attrs
           [:div.boolean-label "NOT"]
           [:div.boolean-body
            (when not-clause
              [render-clause mode (conj reaction-path :not) not-clause])]
           (when (and (contains? #{:edit :new} mode)
                      (nil? not-clause))
             [add-clause
              (conj reaction-path :not)])]
          :else
          (let [{:keys [path op val ref]} clause]
            [:div.clause.op
             attrs
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
                           ref]]))]))
        (cond->
            (contains? #{:edit :new} mode)
          (conj [delete-icon
                 :on-click
                 (fn []
                   (dispatch
                    [:reaction/delete-clause reaction-path]))])))))

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

(defn- render-conditions
  [mode conditions]
  (into [:div.reaction-conditions]
        (for [[condition-name condition]
              (sort-by
               (comp :sort-idx val)
               conditions)]
          [:div.condition
           [render-or-edit-condition-name
            mode condition-name]
           [:div.condition-body
            ;; condition can be nil during edit
            (when condition
              [render-clause
               mode
               [:ruleset :conditions condition-name]
               condition])]
           (when (contains? #{:edit :new} mode)
             [delete-icon
              :on-click
              (fn []
                (dispatch [:reaction/delete-condition condition-name]))])
           (when (and (contains? #{:edit :new} mode)
                      (nil? condition))
             [add-clause
              [:ruleset :conditions condition-name]])])))

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
             [render-or-edit-path
              mode
              [:ruleset :identityPaths idx]
              path
              :remove-fn (fn []
                           (dispatch [:reaction/delete-identity-path idx]))])
           identity-paths))]])

(defn- ruleset-view
  [mode
   {:keys [identityPaths
           conditions
           template]}]
  [:dl.reaction-ruleset
   [render-identity-paths
    mode identityPaths]
   [:dt "Conditions"]
   [:dd [render-conditions mode conditions]
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
                       (dispatch [:reaction/save-edit]))
           :disabled true}
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
                ruleset] :as reaction} @(subscribe
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
     [:div {:class "tenant-wrapper"}
      [:dl.reaction-view
       [:dt "Title"]
       [:dd
        (case mode
          :focus title
          [edit-title title])]

       [:dt "ID"]
       [:dd (or id "[New]")]

       [:dt "Status"]
       [:dd
        (case mode
          :focus (if active "Active" "Inactive")
          [edit-status active])]

       [:dt "Created"]
       [:dd (or created "[New]")]

       [:dt "Modified"]
       [:dd (or modified "[New]")]

       [:dt "Error"]
       [:dd [render-error error]]

       [:dt "Ruleset"]
       [:dd
        [e/render-ruleset-errors [:conditions]]
        [ruleset-view mode ruleset]]]]]))

(defn reactions
  []
  (let [mode @(subscribe [:reaction/mode])]
    (if (= :list mode)
      [reactions-list]
      [reaction-view mode])))
