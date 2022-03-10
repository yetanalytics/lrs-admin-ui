(ns cljsjs.oidc-client
  "A shim so we can run w/o the cljsjs dep and pass analyses"
  (:require [oidc-client :as oidc]))

(def UserManager oidc/UserManager)
(def Log oidc/Log)
(def WebStorageStateStore oidc/WebStorageStateStore)
