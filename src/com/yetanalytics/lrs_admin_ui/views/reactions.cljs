(ns com.yetanalytics.lrs-admin-ui.views.reactions
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [com.yetanalytics.lrs-admin-ui.functions.reaction :as rfns]
            [com.yetanalytics.lrs-admin-ui.views.reactions.path :as p]
            [goog.string :refer [format]]
            [goog.string.format]))

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

(defn- render-clause
  [{and-clauses :and
    or-clauses  :or
    not-clause  :not
    :as clause}]
  (cond
    and-clauses
    [:div.boolean.and
     [:div.boolean-label "AND"]
     (into [:div.boolean-body]
           (for [clause and-clauses]
             [render-clause clause]))]
    or-clauses
    [:div.boolean.or
     [:div.boolean-label "OR"]
     (into [:div.boolean-body]
           (for [clause or-clauses]
             [render-clause clause]))]
    not-clause
    [:div.boolean.not
     [:div.boolean-label "NOT"]
     [:div.boolean-body
      [render-clause not-clause]]]
    :else
    (let [{:keys [path op val ref]} clause]
      [:div.clause.op
       (cond-> [:dl.op-list
                [:dt "Path"]
                [:dd [render-path path]]
                [:dt "Op"]
                [:dd [:code op]]]
         val (conj [:dt "Val"]
                   [:dd [render-val val]])
         ref (conj [:dt "Ref"]
                   [:dd
                    [:dl.ref
                     [:dt "Condition"]
                     [:dd (:condition ref)]
                     [:dt "Path"]
                     [:dd [render-path (:path ref)]]]]))])))

(defn- render-conditions
  [conditions]
  (into [:div.reaction-conditions]
        (for [[condition-name condition] conditions]
          [:div.condition
           [:div.condition-name condition-name]
           [:div.condition-body [render-clause condition]]])))

(defn- render-template
  [template]
  [:pre.template
   (.stringify js/JSON (clj->js template) nil 2)])

(defn- ruleset-view
  [mode
   {:keys [identityPaths
           conditions
           template]}]
  [:dl.reaction-ruleset
   [:dt "Identity Paths"]
   [:dd
    (into [:ul.identity-paths]
          (for [path identityPaths]
            (if (= :edit mode)
              [p/path-input path
               :remove-fn (fn [_] (println 'remove))]
              [render-path path])))]
   [:dt "Conditions"]
   [:dd [render-conditions conditions]]
   [:dt "Template"]
   [:dd [render-template template]]])

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
   {:on-change (fn [e]
                 (dispatch
                  [:reaction/edit-status
                   (fns/ps-event-val e)]))}
   [:option
    {:value "active"
     :selected (if (true? active) "selected" "")}
    "Active"]
   [:option
    {:value "inactive"
     :selected (if (false? active) "selected" "")}
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
