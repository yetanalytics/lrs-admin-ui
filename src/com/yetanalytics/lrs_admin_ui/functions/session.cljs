(ns com.yetanalytics.lrs-admin-ui.functions.session
  "Functions for session/login management"
  (:require [com.yetanalytics.re-route :as re-route]
            [com.yetanalytics.lrs-admin-ui.db :as db]
            [com.yetanalytics.lrs-admin-ui.functions.oidc :as oidc]))

(defn- logged-in?
  [db]
  (or (some? (get-in db [::db/session :token]))
      (oidc/logged-in? db)))

(defn login-dispatch*
  "Do nothing if logged in, otherwise redirect to the homepage.
   Returns an `{:fx ...}` map."
  [db]
  (if (logged-in? db)
    {}
    {:fx [[:dispatch [::re-route/navigate :home]]]}))

(defn login-dispatch
  "Dispatch `dispatch-vec` if logged in, otherwise redirect to the homepage.
   Returns an `{:fx ...}` map."
  [db dispatch-vec]
  (if (logged-in? db)
    {:fx [[:dispatch dispatch-vec]]}
    {:fx [[:dispatch [::re-route/navigate :home]]]}))
