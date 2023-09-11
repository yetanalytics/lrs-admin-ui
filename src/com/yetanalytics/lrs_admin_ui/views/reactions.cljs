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

(defn- ruleset-view
  [{:keys [identityPaths
           conditions
           template]}]
  [:dl.reaction-ruleset
   [:dt "Identity Paths"]
   [:dd
    (into [:ul.identity-paths]
          (for [path identityPaths]
            [render-path path]))]])

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
