(ns com.yetanalytics.lrs-admin-ui.server
  "Ring handler only for dev/figwheel to serve multiple paths"
  (:require [ring.util.request  :as req]
            [ring.util.response :as resp]))

(defn handler [request]
  (cond
    (not= :get (:request-method request))
    {:status  405
     :headers {"Context-Type" "text/plain"}
     :body    "Unsupported Operation!"}
    ;; Imitate backend redirects
    ;; NOTE: "/" will actually not get redirected since Figwheel handles
    ;; that separately.
    (#{"/" "/admin" "/admin/"} (req/path-info request))
    (resp/redirect "/admin/ui")
    :else
    (resp/file-response "resources/public/")))
