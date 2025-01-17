(ns com.yetanalytics.lrs-admin-ui.views.footer
  (:require [com.yetanalytics.lrs-admin-ui.functions :refer [ps-event]]
            [re-frame.core :refer [subscribe dispatch-sync]]
            [goog.string   :refer [format unescapeEntities]]
            goog.string.format))


(defn yet-copy []
  [:span {:class "text-white"}
   ;; Unescape needed to render special chars.
   (format "%s2021-2025 " (unescapeEntities "&copy;"))
   [:a {:href "https://www.yetanalytics.com"
        :target "_blank"
        :rel "noopener noreferrer"
        :class "text-white"}
    @(subscribe [:lang/get :footer.attribution])]])

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
         [:span {:class "font-condensed font-10 fg-primary"} @(subscribe [:lang/get :footer.nav.credentials])]]]
       [:div {:class "col-3 text-center footer-icon pointer"}
        [:a {:href "#"
             :on-click #(dispatch-sync [:session/set-page :accounts])}
         [:i
          [:img {:src "images/icons/icon-mobile-profle.svg" :alt "" :width "10"}]]
         [:span {:class "font-condensed font-10 fg-primary"} @(subscribe [:lang/get :footer.nav.accounts])]]]
       [:div {:class "col-3 text-center footer-icon pointer"}
        [:a {:href "#"
             :on-click #(dispatch-sync [:session/set-page :browser])}
         [:i
          [:img {:src "images/icons/icon-mobile-search.svg" :alt "" :width "16"}]]
         [:span {:class "font-condensed font-10 fg-primary"} @(subscribe [:lang/get :footer.nav.browser])]]]
       [:div {:class "col-3 text-center footer-icon pointer"}
        [:a {:href "#"
             :on-click #(do (ps-event %)
                            (dispatch-sync [:session/logout]))}
         [:i
          [:img {:src "images/icons/icon-mobile-logout.svg" :alt "Logout" :width "16"}]]
         [:span {:class "font-condensed font-10 fg-primary"} @(subscribe [:lang/get :footer.nav.logout])]]]])
    [:div {:class "mobile-footer-links"}
     [yet-copy]]]
   [:footer {:class "home-footer"}
    [:div {:class "footer-wrapper"}
     [yet-copy]"   |   "
     [:a {:class "text-white", :rel "noopener noreferrer" :href "https://github.com/yetanalytics/lrsql/blob/main/LICENSE"}
      @(subscribe [:lang/get :footer.license])]"   |   "
     [:a {:class "text-white", :rel "noopener noreferrer" :href "https://github.com/yetanalytics/lrsql"}
      @(subscribe [:lang/get :footer.contribute])
      [:img {:src "images/icons/github.png"
             :alt "Github Logo"}]]
     [:span {:class "support-note"}
      @(subscribe [:lang/get :footer.contact-note])]]]])
