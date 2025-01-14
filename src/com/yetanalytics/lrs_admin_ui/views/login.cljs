(ns com.yetanalytics.lrs-admin-ui.views.login
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [com.yetanalytics.lrs-admin-ui.functions :as fns]))

(defn login []
  (let [show-local-login? @(subscribe [:oidc/show-local-login?])
        show-oidc-login? @(subscribe [:oidc/login-available?])]
    [:main {:class "page-login"}
     [:div {:class "opacity-wrapper"}]
     [:div {:class "main-wrapper"}
      [:div {:class "logo-image"}
       [:i
        [:img {:src @(subscribe [:resources/image "logo.png"])
               :alt "LRS Logo"}]]]
      [:h1 @(subscribe [:lang/get :login.title])]
      [:div {:class "form-wrapper"}
       [:form {:id "login-form"}
        (when show-local-login?
          [:<>
           [:div {:class "form-group"}
            [:label {:class "field-label",
                     :for "username"}
             @(subscribe [:lang/get :login.username])]
            [:input {:type "text",
                     :class "form-control",
                     :name "username",
                     :value @(subscribe [:login/get-username])
                     :on-change #(dispatch [:login/set-username
                                            (fns/ps-event-val %)])
                     :id "username"}]]
           [:div {:class "form-group"}
            [:label {:class "field-label",
                     :for "password"}
             @(subscribe [:lang/get :login.password])]
            [:input {:type "password",
                     :class "form-control",
                     :name "password",
                     :value @(subscribe [:login/get-password])
                     :on-change #(dispatch [:login/set-password
                                            (fns/ps-event-val %)])
                     :id "password"}]]])
        [:div {:class "login-button-wrapper"}
         (when show-local-login?
           [:div
            [:button {:class "login-button"
                      :on-click (fn [e]
                                  (fns/ps-event e)
                                  (dispatch [:session/authenticate]))} @(subscribe [:lang/get :login.login-button])]])
         (when show-oidc-login?
           [:div
            [:button {:class "login-button"
                      :on-click (fn [e]
                                  (fns/ps-event e)
                                  (dispatch [:com.yetanalytics.re-oidc/login]))}
             @(subscribe [:lang/get :login.oidc-button])]])
         [:p {:class "login-separator"} @(subscribe [:lang/get :login.trouble])]]]]]]))
