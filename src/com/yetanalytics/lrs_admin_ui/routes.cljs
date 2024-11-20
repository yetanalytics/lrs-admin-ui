(ns com.yetanalytics.lrs-admin-ui.routes
  (:require
   [re-frame.core :refer [dispatch]]
   [com.yetanalytics.lrs-admin-ui.views.accounts        :refer [accounts]]
   [com.yetanalytics.lrs-admin-ui.views.browser         :refer [browser]]
   [com.yetanalytics.lrs-admin-ui.views.credentials     :refer [credentials]]
   [com.yetanalytics.lrs-admin-ui.views.data-management :refer [data-management]]
   [com.yetanalytics.lrs-admin-ui.views.not-found       :refer [not-found]]
   [com.yetanalytics.lrs-admin-ui.views.reactions       :refer [reactions]]
   [com.yetanalytics.lrs-admin-ui.views.status          :refer [status]]
   [com.yetanalytics.lrs-admin-ui.views.update-password :refer [update-password]]))

(defn routes [{:keys [enable-admin-delete-actor
                      enable-admin-status
                      enable-reactions]}]
  (cond-> ["/admin/ui"
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
             :controllers [{:start
                            (fn [_]
                              (dispatch [:accounts/load-accounts]))}]}]
           ["/accounts/password"
            {:name        :update-password
             :view        update-password
             :controllers [{:stop
                            (fn [_]
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
            :controllers [{:start
                           (fn [_]
                             (dispatch [:status/get-all-data]))}]}])
    ;; TODO: Add `/reactions/new`, `/reactions/view`, and `/reactions/edit`
    ;; routes for their respective pages.
    enable-reactions
    (conj ["/reactions"
           {:name        :reactions
            :view        reactions
            :controllers [{:start
                           (fn [_]
                             (dispatch [:reaction/back-to-list])
                             (dispatch [:reaction/load-reactions]))}]}])))
