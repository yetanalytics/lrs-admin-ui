(ns com.yetanalytics.lrs-admin-ui.functions.http
  (:require [re-frame.core     :refer [subscribe dispatch]]
            [clojure.pprint    :refer [pprint]]
            [ajax.interceptors :refer [to-interceptor]]))

(def server-host "https://localhost:8443")

(defn serv-uri
  [path]
  (str server-host path))

(defn add-jwt [{:keys [headers] :as request}]
  (assoc request :headers
         (conj headers {"Authorization"
                        (str "Bearer " @(subscribe [:session/get-token]))})))

(def add-jwt-interceptor
  (to-interceptor {:name "JWT Authentication Interceptor"
                   :request add-jwt}))
