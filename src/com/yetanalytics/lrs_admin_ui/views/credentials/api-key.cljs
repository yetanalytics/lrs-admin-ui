(ns ^:figwheel-hooks com.yetanalytics.lrs-admin-ui.views.credentials.api-key
  (:require
   [reagent.core :as r]))

(defn api-key []
  (let [expanded (r/atom false)]
    (fn []
      (println @expanded)
      [:li {:class "mb-2"}
       [:div {:class "accordion-contianer"}
        [:div {:class "row no-gutters accordion-header bg-gray fg-dark-blue"
               :on-click (fn [_]
                           (swap! expanded not)
                           (println @expanded))}
         [:div {:class "api-key-col"}
          [:span {:aria-label "toggle accordion"}
           "3c71cea45c6915ba33fad3b8965226f3"]]
         [:div {:class "api-key-col"} "Permissions: "
          [:span "All"]]]
        (cond @expanded
              [:div {:class "api-key-expand"}
               [:div {:class "api-key-col"}
                [:div {:class "form-group"}
                 [:p {:class "font-condensed-bold mb-2"}
                  "API Key Secret"]
                 [:div {:class "action-row"}
                  [:div {:class "permissions-content"}
                   "Redacted"]
                  [:ul {:class "action-icon-list"}
                   [:li
                    [:a {:href "#!", :class "icon-secret"} "Show Secret"]]]]]]
               [:div {:class "api-key-col"}
                [:div {:class "form-group"}
                 [:p {:class "font-condensed-bold mb-2"} "Permissions"]
                 [:div {:class "action-row"}
                  [:div {:class "action-label"}
                   "All"]
                  [:ul {:class "action-icon-list"}
                   [:li
                    [:a {:href "#!", :class "icon-edit"} "Edit"]]
                   [:li
                    [:a {:href "#!", :class "icon-delete"} "Delete"]]
                   ]]]]])]])))
