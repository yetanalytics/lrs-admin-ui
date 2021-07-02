(ns com.yetanalytics.lrs-admin-ui.views.footer)

(defn footer []
  [:footer {:class "home-footer"}
   [:div {:class "footer-wrapper"}
    [:span "@Copyright 2021"]"   |  "
    [:a {:class "text-white", :href "#"} "Terms and Conditions"]"   |  "
    [:a {:class "text-white", :href "#"} "Privacy Policy"]]])
