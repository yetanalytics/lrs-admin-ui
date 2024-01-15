(ns com.yetanalytics.lrs-admin-ui.views.footer
  (:require [com.yetanalytics.lrs-admin-ui.functions :refer [ps-event]]
            [re-frame.core :refer [subscribe dispatch-sync]]
            [goog.string   :refer [format unescapeEntities]]
            goog.string.format))


(defn yet-copy []
  [:span {:class "text-white"}
   ;; Unescape needed to render special chars.
   (format "%s2021-2023 " (unescapeEntities "&copy;"))
   [:a {:href "https://www.yetanalytics.com"
        :target "_blank"
        :rel "noopener noreferrer"
        :class "text-white"}
    "Yet Analytics Inc."]])

(defn footer []
  [:div {:class "mobile-footer"}
   [:div {:class "footer-menu-box"}
    (when (not= nil @(subscribe [:session/get-token]))
      [:div {:class "row no-gutters"}
       [:div {:class "col-3 text-center footer-icon pointer"}
        [:a {:href "#"
             :on-click #(dispatch-sync [:session/set-page :credentials])}
         [:i
          [:img {:src "images/icons/icon-mobile-credentials.svg" :alt "" :width "16"}]]
         [:span {:class "font-condensed font-10 fg-primary"} "CREDENTIALS"]]]
       [:div {:class "col-3 text-center footer-icon pointer"}
        [:a {:href "#"
             :on-click #(dispatch-sync [:session/set-page :accounts])}
         [:i
          [:img {:src "images/icons/icon-mobile-profle.svg" :alt "" :width "10"}]]
         [:span {:class "font-condensed font-10 fg-primary"} "ACCOUNTS"]]]
       [:div {:class "col-3 text-center footer-icon pointer"}
        [:a {:href "#"
             :on-click #(dispatch-sync [:session/set-page :browser])}
         [:i
          [:img {:src "images/icons/icon-mobile-search.svg" :alt "" :width "16"}]]
         [:span {:class "font-condensed font-10 fg-primary"} "BROWSER"]]]
       [:div {:class "col-3 text-center footer-icon pointer"}
        [:a {:href "#"
             :on-click #(do (ps-event %)
                            (dispatch-sync [:session/logout]))}
         [:i
          [:img {:src "images/icons/icon-mobile-logout.svg" :alt "Logout" :width "16"}]]
         [:span {:class "font-condensed font-10 fg-primary"} "LOG OUT"]]]])
    [:div {:class "mobile-footer-links"}
     [yet-copy]]]
   [:footer {:class "home-footer"}
    [:div {:class "footer-wrapper"}
     [yet-copy]"   |   "
     [:a {:class "text-white", :rel "noopener noreferrer" :href "https://github.com/yetanalytics/lrsql/blob/main/LICENSE"}
      "Licensed under the Apache 2.0 License"]"   |   "
     [:a {:class "text-white", :rel "noopener noreferrer" :href "https://github.com/yetanalytics/lrsql"}
      "Contribute on "
      [:img {:src "images/icons/github.png"
             :alt "Github Logo"}]]
     [:span {:class "support-note"}
      "Contact us to learn about Enterprise Support options."]]]])
