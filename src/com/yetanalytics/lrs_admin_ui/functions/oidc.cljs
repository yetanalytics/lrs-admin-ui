(ns com.yetanalytics.lrs-admin-ui.functions.oidc
  (:require [com.yetanalytics.re-oidc :as re-oidc]))

(defn push-state
  "Push history state to clean up on login/logout"
  [path]
  (.pushState js/window.history
              (clj->js {})
              js/document.title
              path))

(def static-config
  {:auto-login false
   :on-login-success [:oidc/login-success]
   ;; Keep the token up to date
   :on-user-loaded [:oidc/user-loaded]
   ;; remove OIDC creds if unloaded
   :on-user-unloaded [:oidc/user-unloaded]
   :on-logout-success [:notification/notify false "You have logged out."]
   :user-store :local-storage})

(defn init-config
  "Combine the OIDC client config from the server with static config.
  Set redirect uis based on SPA origin."
  [oidc-config]
  (let [origin js/window.location.origin
        pathname js/window.location.pathname
        spa-uri (str origin pathname)]
    (assoc static-config
           :oidc-config
           (merge oidc-config
                  {"redirect_uri" spa-uri
                   "post_logout_redirect_uri" spa-uri}))))

(defn logged-in?
  "Is there an active OIDC login?"
  [{status ::re-oidc/status}]
  (= :loaded status))
