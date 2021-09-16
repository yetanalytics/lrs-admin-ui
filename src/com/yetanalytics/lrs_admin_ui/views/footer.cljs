(ns com.yetanalytics.lrs-admin-ui.views.footer
  (:require [goog.string :refer [format unescapeEntities]]
            goog.string.format))

(defn footer []
  [:footer {:class "home-footer"}
   [:div {:class "footer-wrapper"}
    [:span {:class "text-white"}
     (format "%s2021 " (unescapeEntities "&copy;"))
     [:a {:href "https://www.yetanalytics.com"
          :target "_blank"
          :class "text-white"}
      "Yet Analytics Inc."]]"   |   "
    [:a {:class "text-white", :href "https://github.com/yetanalytics/lrsql/blob/main/LICENSE"}
     "Licensed under the Apache 2.0 License"]"   |   "
    [:a {:class "text-white", :href "https://github.com/yetanalytics/lrsql"}
     "Contribute on "
     [:img {:src "images/icons/github.png"
            :alt "Github Logo"}]]
    [:span {:class "support-note"}
     "Contact us to learn about Enterprise Support options."]]])
