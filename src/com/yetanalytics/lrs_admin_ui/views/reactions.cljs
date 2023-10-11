(ns com.yetanalytics.lrs-admin-ui.views.reactions
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.reaction :as rfns]
            [com.yetanalytics.lrs-admin-ui.views.reactions.path :as p]
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
              "Edit"]]]))])

(defn- reactions-list
  []
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    "Reactions"]
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
  (if (= :edit mode)
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
  (if (= :edit mode)
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

(defn- render-clause
  [mode
   reaction-path
   {and-clauses :and
    or-clauses  :or
    not-clause  :not
    :as clause}]
  (cond
    and-clauses
    [:div.boolean.and
     [:div.boolean-label "AND"]
     (into [:div.boolean-body]
           (map-indexed
            (fn [idx clause]
              [render-clause
               mode
               (into reaction-path [:and idx])
               clause])
            and-clauses))]
    or-clauses
    [:div.boolean.or
     [:div.boolean-label "OR"]
     (into [:div.boolean-body]
           (map-indexed
            (fn [idx clause]
              [render-clause
               mode
               (into reaction-path [:or idx])
               clause])
            or-clauses))]
    not-clause
    [:div.boolean.not
     [:div.boolean-label "NOT"]
     [:div.boolean-body
      [render-clause mode (conj reaction-path :not) not-clause]]]
    :else
    (let [{:keys [path op val ref]} clause]
      [:div.clause.op
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
                      op]]]
         val (conj [:dt "Val"]
                   [:dd [render-val val]])
         ref (conj [:dt "Ref"]
                   [:dd
                    [:dl.ref
                     [:dt "Condition"]
                     [:dd (:condition ref)]
                     [:dt "Path"]
                     [:dd
                      [render-or-edit-path
                       mode
                       (conj reaction-path :ref :path)
                       (:path ref)]]]]))])))

(defn- render-conditions
  [mode conditions]
  (into [:div.reaction-conditions]
        (for [[condition-name condition] conditions]
          [:div.condition
           [:div.condition-name condition-name]
           [:div.condition-body [render-clause
                                 mode
                                 [:ruleset :conditions condition-name]
                                 condition]]])))

(defn- render-template
  [mode template]
  [:pre.template
   (.stringify js/JSON (clj->js template) nil 2)])

(defn- render-identity-paths
  [mode identity-paths]
  [:<>
   [:dt "Identity Paths"
    (when (= :edit mode)
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
   [:dd [render-conditions mode conditions]]
   [:dt "Template"]
   [:dd [render-template mode template]]])

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
  [mode id]
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
                      (dispatch [:reaction/edit id]))}
      "Edit"])
   (when (and (= :edit mode)
              @(subscribe [:reaction/edit-dirty?]))
     [:a {:href "#!"
          :on-click (fn [e]
                      (fns/ps-event e)
                      (dispatch [:reaction/edit id]))}
      "Revert Changes"])])

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
                                           :edit [:reaction/editing]))]
    [:div {:class "left-content-wrapper"}
     [:h2 {:class "content-title"}
      (case mode
        :focus "Reaction Details"
        :edit "Edit Reaction")]
     [reaction-actions mode id]
     [:div {:class "tenant-wrapper"}
      [:dl.reaction-view
       [:dt "Title"]
       [:dd
        (case mode
          :focus title
          :edit [edit-title title])]

       [:dt "ID"]
       [:dd id]

       [:dt "Status"]
       [:dd
        (case mode
          :focus (if active "Active" "Inactive")
          :edit [edit-status active])]

       [:dt "Created"]
       [:dd created]

       [:dt "Modified"]
       [:dd modified]

       [:dt "Error"]
       [:dd [render-error error]]

       [:dt "Ruleset"]
       [:dd [ruleset-view mode ruleset]]]]]))

(defn reactions
  []
  (let [mode @(subscribe [:reaction/mode])]
    (if (= :list mode)
      [reactions-list]
      [reaction-view mode])))
