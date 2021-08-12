(ns com.yetanalytics.lrs-admin-ui.functions.http
  (:require [re-frame.core     :refer [subscribe dispatch]]
            [clojure.pprint    :refer [pprint]]
            [ajax.interceptors :refer [to-interceptor]]
            [lambdaisland.uri  :as uri]))


;;URL Manipulation
(def server-host "http://localhost:8080")

(def default-xapi-path "/xapi/statements")

(defn serv-uri
  [path]
  (str server-host path))

(defn build-xapi-url
  [path params]
  (let [path' (or path default-xapi-path)
        param-map (cond-> {:unwrap_html true}
                    (some? params)
                    (merge (uri/query-map params)))
        params' (uri/map->query-string param-map)]
    (str server-host path' "?" params')))

(defn is-rel?
  [url]
  (= (:host (uri/uri url))
     (. (. js/window -location) -hostname)))


;;JWTs
(defn add-jwt [{:keys [headers] :as request}]
  (assoc request :headers
         (conj headers {"Authorization"
                        (str "Bearer " @(subscribe [:session/get-token]))})))

(def add-jwt-interceptor
  (to-interceptor {:name "JWT Authentication Interceptor"
                   :request add-jwt}))

(defn make-basic-auth
  [credential]
  (let [unencoded (str (:api-key credential) ":" (:secret-key credential))]
    (print unencoded)
    (js/btoa unencoded)))

;;Basic Auth and html format for xAPI
(defn format-html [{:keys [headers] :as request}]
  (assoc request :headers
         {"Accept" "text/html"
          "Authorization" (str "Basic " (make-basic-auth
                                         @(subscribe [:browser/get-credential])))
          "X-Experience-API-Version" "1.0.3"}))

(def format-html-interceptor
  (to-interceptor {:name "HTML Interceptor"
                   :request format-html}))
