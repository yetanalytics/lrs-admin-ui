(ns com.yetanalytics.lrs-admin-ui.functions
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client                 :as http]
            [cljs.core.async                  :refer [<!]]
            [re-frame.core                    :refer [subscribe dispatch-sync]]
            [com.yetanalytics.lrs-admin-ui.db :as db]
            [clojure.pprint :refer [pprint]]))


(defn ps-event
  "Helper function that will prevent default action
   and stop propagation for an event, a common task."
  [e]
  (.preventDefault e)
  (.stopPropagation e))

(defn ps-event-val
  "Not only pevents defaults but extracts value for form elements"
  [e]
  (ps-event e)
  (.. e -target -value))

(defn load-creds
  "Get and load user credentials"
  []
  (go (let [{:keys [status success body]}
            (<! (http/get "https://localhost:8443/admin/creds"
                          {:with-credentials? false
                           :oauth-token @(subscribe [:session/get-token])}))]
        (when
          (= status 200) (dispatch-sync [:credentials/set-credentials body])))))

(defn authenticate
  "Get and store token or error on authentication."
  []
  (go (let [{:keys [status success body]}
            (<! (http/post "https://localhost:8443/admin/account/login"
                           {:with-credentials? false
                            :json-params @(subscribe [:db/get-login])}))]
        (cond
          (= status 200) (do
                           (dispatch-sync [:session/set-token
                                           (:json-web-token body)])
                           (load-creds))
          :else (dispatch-sync [:login/set-error body])))))
