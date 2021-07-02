(ns com.yetanalytics.lrs-admin-ui.views.credentials.api-key
  (:require
   [reagent.core :as r]))

(defn api-key []
  (let [expanded (r/atom false)]
    (fn []
      [:li {:class "mb-2"}
       [:div {:class "accordion-container"}
        [:div {:class "api-key-row"
               :aria-label "Show/Hide Api Key Details"
               :on-click #(swap! expanded not)}
         [:div {:class "api-key-col"}
          [:span {:class (str "collapse-sign"
                              (cond @expanded " expanded"))}
           "3c71cea45c6915ba33fad3b8965226f3"]]
         [:div {:class "api-key-col"} "Permissions: "
          [:span "All"]]]
        (cond @expanded
              [:div {:class "api-key-expand"}
               [:div {:class "api-key-col"}
                [:p {:class "api-key-col-header"}
                 "API Key Secret"]
                [:div {:class "action-row"}
                 [:div {:class "action-label"}
                  "Redacted"]
                 [:ul {:class "action-icon-list"}
                  [:li
                   [:a {:href "#!", :class "icon-secret"} "Show Secret"]]]]]
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
                   [:a {:href "#!", :class "icon-delete"} "Delete"]]
                  ]]]])]])))
