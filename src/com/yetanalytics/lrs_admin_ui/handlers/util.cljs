(ns com.yetanalytics.lrs-admin-ui.handlers.util
  (:require [com.yetanalytics.lrs-admin-ui.db :as db]))

(def global-interceptors
  [db/check-spec-interceptor])

(defmulti page-fx
  "Given a set-page event query vector, adds any effects of moving to that page.
  Note that you can use overloads beyond just the page keyword in your methods."
  (fn [[_ page]]
    page))

(defmethod page-fx :default [_] [])
