(ns com.yetanalytics.lrs-admin-ui.views.credentials.api-key
  (:require
   [reagent.core :as r]))

(defn api-key
  [{:keys [credential]}]
  (let [expanded (r/atom false)
        show-secret (r/atom false)]
    (fn []
      [:li {:class "mb-2"}
       [:div {:class "accordion-container"}
        [:div {:class "api-key-row"
               :aria-label "Show/Hide Api Key Details"
               :on-click #(swap! expanded not)}
         [:div {:class "api-key-col"}
          [:span {:class (str "collapse-sign"
                              (when @expanded " expanded"))}
           (:api-key credential)]]
         [:div {:class "api-key-col"} "Permissions: "
          (map (fn [scope]
                 [:span scope])
               (:scopes credential))]]
        (when @expanded
          [:div {:class "api-key-expand"}
           [:div {:class "api-key-col"}
            [:p {:class "api-key-col-header"}
             "API Key Secret"]
            [:div {:class "action-row"}
             [:div {:class "action-label"}
              (cond
                @show-secret [:span (:secret-key credential)]
                :else "Redacted")]
             [:ul {:class "action-icon-list"}
              [:li
               [:a {:href "#!",
                    :class "icon-secret"
                    :on-click #(swap! show-secret not)}
                (str (cond
                       @show-secret "Hide"
                       :else "Show")
                     " Secret")]]]]]
           [:div {:class "api-key-col"}
            [:p {:class "api-key-col-header"}
             "Permissions"]
            [:div {:class "action-row"}
             [:div {:class "action-label"}
              "All"]
             [:ul {:class "action-icon-list"}
              [:li
               [:a {:href "#!", :class "icon-edit"} "Edit"]]
              [:li
               [:a {:href "#!", :class "icon-delete"} "Delete"]]]]]])]])))
