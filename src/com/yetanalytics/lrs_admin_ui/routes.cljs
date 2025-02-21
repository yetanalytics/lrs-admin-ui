(ns com.yetanalytics.lrs-admin-ui.routes
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [com.yetanalytics.re-route :as re-route]
   [com.yetanalytics.lrs-admin-ui.views.accounts        :refer [accounts]]
   [com.yetanalytics.lrs-admin-ui.views.browser         :refer [browser]]
   [com.yetanalytics.lrs-admin-ui.views.credentials     :refer [credentials]]
   [com.yetanalytics.lrs-admin-ui.views.data-management :refer [data-management]]
   [com.yetanalytics.lrs-admin-ui.views.not-found       :refer [not-found]]
   [com.yetanalytics.lrs-admin-ui.views.status          :refer [status]]
   [com.yetanalytics.lrs-admin-ui.views.update-password :refer [update-password]]
   [com.yetanalytics.lrs-admin-ui.views.reactions       :refer [reactions-list
                                                                reaction-focus
                                                                reaction-edit
                                                                reaction-new]]))

(defn routes [{:keys [proxy-path
                      enable-admin-delete-actor
                      enable-admin-status
                      enable-reactions]}]
  (cond-> [(str proxy-path "/admin/ui")
           [""
            {:name        :home
             :view        credentials
             :controllers [{:start
                            (fn [_]
                              (dispatch [::re-route/on-start :home]))}]}]
           ["/credentials"
            {:name        :credentials
             :view        credentials
             :controllers [{:start
                            (fn [_]
                              (dispatch [::re-route/on-start :credentials]))}]}]
           ["/accounts"
            {:name        :accounts
             :view        accounts
             :controllers [{:start
                            (fn [_]
                              (dispatch [::re-route/on-start :accounts]))}]}]
           ["/accounts/password"
            {:name        :update-password
             :view        update-password
             :controllers [{:start
                            (fn [_]
                              (dispatch [::re-route/on-start :update-password]))
                            :stop
                            (fn [_]
                              (dispatch [::re-route/on-stop :update-password]))}]}]
           ["/browser"
            {:name :browser
             :view browser
             :controllers [{:start
                            (fn [_]
                              (dispatch [::re-route/on-start :browser]))}]}]
           ["/not-found"
            {:name :not-found
             :view not-found
             :controllers [{:start
                            (fn [_]
                              (dispatch [::re-route/on-start :not-found]))}]}]]
    enable-admin-delete-actor
    (conj ["/data-management"
           {:name :data-management
            :view data-management
            :controllers [{:start
                           (fn [_]
                             (dispatch [::re-route/on-start :data-management]))}]}])
    enable-admin-status
    (conj ["/status"
           {:name        :status
            :view        status
            :controllers [{:start
                           (fn [_]
                             (dispatch [::re-route/on-start :status]))}]}])
    enable-reactions
    (conj
     ["/reactions"
      {:name        :reactions
       :view        reactions-list
       :controllers [{:start (fn [_]
                               (dispatch [::re-route/on-start :reactions]))}]}]
     ["/reactions/new"
      {:name        :reactions/new
       :view        reaction-new
       :controllers [{:start (fn [_]
                               (dispatch [::re-route/on-start :reactions/new]))
                      :stop  (fn [_]
                               (dispatch [::re-route/on-stop :reactions/new]))}]}]
     ["/reactions/:id/view"
      {:name        :reactions/focus
       :view        reaction-focus
       :parameters  {:path [:id]}
       :controllers [{:identity (fn [params]
                                  (get-in params [:path-params :id]))
                      :start    (fn [id]
                                  (dispatch [::re-route/on-start :reactions/focus id]))
                      :stop     (fn [_]
                                  (dispatch [::re-route/on-stop :reactions/focus]))}]}]
     ["/reactions/:id/edit"
      {:name        :reactions/edit
       :view        reaction-edit
       :parameters  {:path [:id]}
       :controllers [{:identity (fn [params]
                                  (get-in params [:path-params :id]))
                      :start    (fn [id]
                                  (dispatch [::re-route/on-start :reactions/edit id]))
                      :stop     (fn [_]
                                  (dispatch [::re-route/on-stop :reactions/edit]))}]}])))
