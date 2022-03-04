(ns com.yetanalytics.lrs-admin-ui.functions.oidc)

(defn- push-state
  "Push history state to clean up on login/logout"
  [path]
  (.pushState js/window.history
              (clj->js {})
              js/document.title
              path))

(def static-config
  {:auto-login false
   :on-login-success #(push-state "/")
   :user-store :local-storage})

(defn init-config
  [oidc-config]
  (assoc static-config
         :oidc-config
         (merge oidc-config
                {"redirect_uri"             js/window.location.origin
                 "post_logout_redirect_uri" js/window.location.origin})))
