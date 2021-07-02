(ns com.yetanalytics.lrs-admin-ui.views.menu)

(defn menu []
  [:div {:class "banner-link-box container-fluid bg-black d-none d-md-flex"}
   [:ul {:class "d-none d-md-flex"}
    [:li {:class "banner-link-item font-condensed-bold"}
     [:a {:class "active", :href "#"} "Credentials Management"]]
    [:li {:class "banner-link-item font-condensed-bold ml-md-3 ml-lg-5 pl-md-3 pl-lg-5"}
     [:a {:href "#"} "Statement Browser"]]]])
