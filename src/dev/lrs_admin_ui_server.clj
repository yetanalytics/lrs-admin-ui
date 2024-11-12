(ns dev.lrs-admin-ui-server
  "Ring handler only for dev/figwheel to serve multiple paths"
  (:require [ring.util.response :refer [file-response]]))

(defn handler [request]
  (if (= :get (:request-method request))
    (file-response "resources/public/")
    {:status  405
     :headers {"Context-Type" "text/plain"}
     :body    "Unsupported Operation!"}))
