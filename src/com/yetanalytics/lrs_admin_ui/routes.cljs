(ns com.yetanalytics.lrs-admin-ui.routes
  (:require
   [re-frame.core :refer [dispatch]]
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
                              (dispatch [:credentials/load-credentials]))}]}]
           ["/credentials"
            {:name        :credentials
             :view        credentials
             :controllers [{:start
                            (fn [_]
                              (dispatch [:credentials/load-credentials]))}]}]
           ["/accounts"
            {:name        :accounts
             :view        accounts
             :controllers [{:start (fn [_]
                                     (dispatch [:accounts/load-accounts]))}]}]
           ["/accounts/password"
            {:name        :update-password
             :view        update-password
             :controllers [{:stop (fn [_]
                                    (dispatch [:update-password/clear]))}]}]
           ["/browser"
            {:name :browser
             :view browser}]
           ["/not-found"
            {:name :not-found
             :view not-found}]]
    enable-admin-delete-actor
    (conj ["/data-management"
           {:name :data-management
            :view data-management}])
    enable-admin-status
    (conj ["/status"
           {:name        :status
            :view        status
            :controllers [{:start (fn [_]
                                    (dispatch [:status/get-all-data]))}]}])
    enable-reactions
    (conj
     ["/reactions"
      {:name        :reactions
       :view        reactions-list
       :controllers [{:start (fn [_]
                               (dispatch [:reaction/back-to-list])
                               (dispatch [:reaction/load-reactions]))}]}]
     ["/reactions/new"
      {:name        :reactions/new
       :view        reaction-new
       :controllers [{:start (fn [_]
                               (dispatch [:reaction/new]))
                      :stop  (fn [_]
                               (dispatch [:reaction/clear-edit]))}]}]
     ["/reactions/:id/view"
      {:name        :reactions/focus
       :view        reaction-focus
       :parameters  {:path [:id]}
       :controllers [{:identity (fn [params]
                                  (get-in params [:path-params :id]))
                      :start    (fn [id]
                                  (dispatch [:reaction/set-focus id]))}]}]
     ["/reactions/:id/edit"
      {:name        :reactions/edit
       :view        reaction-edit
       :parameters  {:path [:id]}
       :controllers [{:identity (fn [params]
                                  (get-in params [:path-params :id]))
                      :start    (fn [id]
                                  (dispatch [:reaction/edit id]))
                      :stop     (fn [_]
                                  (dispatch [:reaction/clear-edit]))}]}])))
