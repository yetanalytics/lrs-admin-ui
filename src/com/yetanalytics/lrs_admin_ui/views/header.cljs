(ns com.yetanalytics.lrs-admin-ui.views.header)

(defn header []
  [:header {:class "container-fluid"}
   [:div {:class "header-wrapper"}
    [:div {:class "post-image"}
     [:i
      [:img {:src "/images/logo.png", :alt "logo", :class "logo-img"}]]]
    [:div {:class "text-right"}
     [:div {:class "user-name"} "Welcome Mike"]
     [:div {:class "header-actions-wrapper"}
      [:a {:class "fg-primary", :href "#"} "Logout"]]]]])
