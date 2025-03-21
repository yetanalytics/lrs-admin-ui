(ns com.yetanalytics.lrs-admin-ui.functions.http
  (:require [re-frame.core     :refer [subscribe]]
            [ajax.interceptors :refer [to-interceptor]]
            [lambdaisland.uri  :as uri]
            [goog.string       :refer [format]]
            goog.string.format))

(defn serv-uri
  ([server-host path proxy-path]
   (str server-host proxy-path path))
  ([server-host path proxy-path params]
   (let [uri       (serv-uri server-host path proxy-path)
         param-map (if (string? params)
                     (uri/query-map params)
                     params)
         params'   (uri/map->query-string param-map)]
     (format "%s?%s" uri params'))))

(defn build-xapi-url
  [server-host xapi-prefix path params proxy-path]
  (let [path' (or path
                  (format "%s%s/statements"
                          (if (some? proxy-path) proxy-path "")
                          xapi-prefix))
        param-map (cond-> {:limit @(subscribe [:browser/get-batch-size])}
                    (some? params)
                    (merge
                     (if (string? params)
                       (uri/query-map params)
                       params)))
        params' (uri/map->query-string param-map)]
    (format "%s%s?%s" server-host path' params')))

(defn extract-params
  "return a map of parameters, unencoded and cleaned, from an xapi url,
  excluding limit"
  [address]
  (-> (uri/query-map (uri/uri address) {:keywordize? false})
      (dissoc "limit")))

(defn is-rel?
  [url]
  (= (:host (uri/uri url))
     (. (. js/window -location) -hostname)))


;;JWTs

(defn add-jwt* [token {:keys [headers] :as request}]
  (assoc request
         :headers
         (conj headers {"Authorization" (format "Bearer %s" token)})))

(defn add-jwt [request]
  (let [token @(subscribe [:session/get-token])]
    (add-jwt* token request)))

(defn add-jwt-interceptor* [token]
  (to-interceptor {:name "JWT Authentication Interceptor"
                   :request (partial add-jwt* token)}))

(def add-jwt-interceptor
  (to-interceptor {:name "JWT Authentication Interceptor"
                   :request add-jwt}))

(defn make-basic-auth
  [credential]
  (js/btoa (format "%s:%s" (:api-key credential) (:secret-key credential))))

;;Basic Auth and json format for xAPI
(defn req-xapi [request]
  (assoc request :headers
         {"Accept" "application/json"
          "Authorization" (format "Basic %s" (make-basic-auth
                                              @(subscribe [:browser/get-credential])))
          "X-Experience-API-Version" "1.0.3"}))

(def req-xapi-interceptor
  (to-interceptor {:name "xAPI Interceptor"
                   :request req-xapi}))
