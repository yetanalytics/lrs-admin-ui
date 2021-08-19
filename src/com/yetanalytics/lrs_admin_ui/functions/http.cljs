(ns com.yetanalytics.lrs-admin-ui.functions.http
  (:require [re-frame.core     :refer [subscribe dispatch]]
            [clojure.pprint    :refer [pprint]]
            [ajax.interceptors :refer [to-interceptor]]
            [lambdaisland.uri  :as uri]
            [goog.string       :refer [format]]))


;;URL Manipulation
;;TODO: feed from config OR possibly assume relative path depending on bundle
;;choices
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
    (format "%s%s?%s" server-host path' params')))

(defn is-rel?
  [url]
  (= (:host (uri/uri url))
     (. (. js/window -location) -hostname)))


;;JWTs
(defn add-jwt [{:keys [headers] :as request}]
  (assoc request :headers
         (conj headers {"Authorization"
                        (format "Bearer %s" @(subscribe [:session/get-token]))})))

(def add-jwt-interceptor
  (to-interceptor {:name "JWT Authentication Interceptor"
                   :request add-jwt}))

(defn make-basic-auth
  [credential]
  (js/btoa (format "%s:%s" (:api-key credential) (:secret-key credential))))

;;Basic Auth and html format for xAPI
(defn format-html [{:keys [headers] :as request}]
  (assoc request :headers
         {"Accept" "text/html"
          "Authorization" (format "Basic %s" (make-basic-auth
                                         @(subscribe [:browser/get-credential])))
          "X-Experience-API-Version" "1.0.3"}))

(def format-html-interceptor
  (to-interceptor {:name "HTML Interceptor"
                   :request format-html}))
