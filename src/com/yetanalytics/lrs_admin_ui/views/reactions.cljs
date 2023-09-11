(ns com.yetanalytics.lrs-admin-ui.views.reactions
  (:require [re-frame.core :refer [dispatch subscribe]]
            [com.yetanalytics.lrs-admin-ui.functions :as fns]
            [goog.string :refer [format]]
            [goog.string.format]))

(defn- path->string
  "Given a vector of keys and/or indices, return a JSONPath string suitable for
  SQL JSON access."
  ([path]
   (path->string path "$"))
  ([[seg & rpath] s]
   (if seg
     (recur rpath
            (cond
              (string? seg)
              ;; Unlike on the backend, these don't need to be valid to parse
              (format "%s.%s" s seg)

              (nat-int? seg)
              (format "%s[%d]" s seg)

              :else
              (throw (ex-info "Invalid path segement"
                              {:type ::invalid-path-segment
                               :segment seg}))))
     s)))

(defn- render-path
  [path]
  [:code (path->string path)])

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
              "View"]]]))])

(defn- reactions-list
  []
  [:div {:class "left-content-wrapper"}
   [:h2 {:class "content-title"}
    "Reactions"]
   [:div {:class "tenant-wrapper"}
    [reactions-table]]])

(defn- reaction-info
  [{:keys [id
           title
           active
           created
           modified]}]
  [:dl.reaction-info
   [:dt "ID"]
   [:dd id]

   [:dt "Status"]
   [:dd (if active "Active" "Inactive")]

   [:dt "Created"]
   [:dd created]

   [:dt "Modified"]
   [:dd modified]])

(defn- reaction-error
  [?error]
  (when-let [{:keys [type message]} ?error]
    [:dl.reaction-error
     [:dt "Error Type"]
     [:dd
      (case type
        "ReactionQueryError" "Query Error"
        "ReactionTemplateError" "Template Error"
        "ReactionInvalidStatementError" "Invalid Statement Error")]
     [:dt "Error Message"]
     [:dd message]]))

(defn- render-clause
  [{and-clauses :and
    or-clauses  :or
    not-clause  :not
    :as clause}]
  (cond
    and-clauses
    (into [:div.clause.and
           [:div.clause-label "AND"]]
          (for [clause and-clauses]
            [render-clause clause]))
    or-clauses
    (into [:div.clause.or
           [:div.clause-label "OR"]]
          (for [clause or-clauses]
            [render-clause clause]))
    not-clause
    [:div.clause.not
     [:div.clause-label "NOT"]
     [render-clause not-clause]]
    :else
    (let [{:keys [path op val ref]} clause]
      [:div.clause.op
       (cond-> [:dl
                [:dt "Path"]
                [:dd [render-path path]]
                [:dt "Op"]
                [:dd [:code op]]]
         val (conj [:dt "Val"]
                   [:dd [render-val val]])
         ref (conj [:dt "Ref"]
                   [:dd
                    [:dl
                     [:dt "Condition"]
                     [:dd (:condition ref)]
                     [:dt "Path"]
                     [:dd [render-path (:path ref)]]]]))])))

(defn- render-conditions
  [conditions]
  (into [:div.reaction-conditions]
        (for [[condition-name condition] conditions]
          [:div.clause.condition
           condition-name
           [render-clause condition]])))

(defn- render-template
  [template]
  [:pre
   (.stringify js/JSON (clj->js template) nil 2)])

(defn- ruleset-view
  [{:keys [identityPaths
           conditions
           template]}]
  [:dl.reaction-ruleset
   [:dt "Identity Paths"]
   [:dd
    (into [:ul.identity-paths]
          (for [path identityPaths]
            [render-path path]))]
   [:dt "Conditions"]
   [:dd [render-conditions conditions]]
   [:dt "Template"]
   [:dd [render-template template]]])

(defn- reaction-view
  []
  (let [{:keys [title
                error
                ruleset] :as reaction} @(subscribe [:reaction/focus])]
    [:div {:class "left-content-wrapper"}
     [:h2 {:class "content-title"}
      title]
     [:a {:href "#!"
          :on-click (fn [e]
                      (fns/ps-event e)
                      (dispatch [:reaction/unset-focus]))}
      "< Back"]
     [:div {:class "tenant-wrapper"}
      [reaction-info reaction]
      [reaction-error error]
      [ruleset-view ruleset]]]))

(defn reactions
  []
  (if @(subscribe [:reaction/focus-id])
    [reaction-view]
    [reactions-list]))
