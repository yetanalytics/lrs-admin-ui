(ns io.github.cljsjs.oidc-client-ts
  ;; TODO: Move this file to an src/dev directory.
  "A shim so we can run w/o the cljsjs dep and pass analyses"
  (:require [oidc-client-ts :as oidc]))

(def UserManager oidc/UserManager)
(def Log oidc/Log)
(def WebStorageStateStore oidc/WebStorageStateStore)
