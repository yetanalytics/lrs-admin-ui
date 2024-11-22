(ns com.yetanalytics.lrs-admin-ui.views.footer
  (:require [com.yetanalytics.lrs-admin-ui.functions :refer [ps-event]]
            [com.yetanalytics.re-route :as re-route]
            [re-frame.core :refer [subscribe dispatch-sync]]
            [goog.string   :refer [format unescapeEntities]]
            goog.string.format))


(defn yet-copy []
  [:span {:class "text-white"}
   ;; Unescape needed to render special chars.
   (format "%s2021-2024 " (unescapeEntities "&copy;"))
   [:a {:href "https://www.yetanalytics.com"
        :target "_blank"
        :rel "noopener noreferrer"
        :class "text-white"}
    @(subscribe [:lang/get :footer.attribution])]])

(defn footer []
  [:div {:class "mobile-footer"}
   [:div {:class "footer-menu-box"}
    (when (not= nil @(subscribe [:session/get-token]))
      ;; TODO: All menu items, including LRS monitor, data mgmt, and reactions
      [:div {:class "row no-gutters"}
       [:div {:class "col-3 text-center footer-icon pointer"}
        [:a {:href @(subscribe [::re-route/href :credentials])}
         [:i
          [:img {:src @(subscribe [:resources/icon "icon-mobile-credentials.svg"]) :alt "" :width "16"}]]
         [:span {:class "font-condensed font-10 fg-primary"} @(subscribe [:lang/get :footer.nav.credentials])]]]
       [:div {:class "col-3 text-center footer-icon pointer"}
        [:a {:href @(subscribe [::re-route/href :accounts])}
         [:i
          [:img {:src @(subscribe [:resources/icon "icon-mobile-profle.svg"]) :alt "" :width "10"}]]
         [:span {:class "font-condensed font-10 fg-primary"} @(subscribe [:lang/get :footer.nav.accounts])]]]
       [:div {:class "col-3 text-center footer-icon pointer"}
        [:a {:href @(subscribe [::re-route/href :browser])}
         [:i
          [:img {:src @(subscribe [:resources/icon "icon-mobile-search.svg"]) :alt "" :width "16"}]]
         [:span {:class "font-condensed font-10 fg-primary"} @(subscribe [:lang/get :footer.nav.browser])]]]
       [:div {:class "col-3 text-center footer-icon pointer"}
        [:a {:href "#"
             :on-click #(do (ps-event %)
                            (dispatch-sync [:session/logout]))}
         [:i
          [:img {:src @(subscribe [:resources/icon "icon-mobile-logout.svg"]) :alt "Logout" :width "16"}]]
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
      [:img {:src @(subscribe [:resources/icon "github.png"])
             :alt "Github Logo"}]]
     [:span {:class "support-note"}
      @(subscribe [:lang/get :footer.contact-note])]]]])
