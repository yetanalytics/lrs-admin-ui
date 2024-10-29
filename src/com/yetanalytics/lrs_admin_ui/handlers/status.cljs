(ns com.yetanalytics.lrs-admin-ui.handlers.status
  (:require [re-frame.core                                    :as re-frame]
            [com.yetanalytics.lrs-admin-ui.db                 :as db]
            [day8.re-frame.http-fx]
            [com.yetanalytics.lrs-admin-ui.functions.http     :as httpfn]
            [com.yetanalytics.lrs-admin-ui.functions.time     :as t]
            [ajax.core                                        :as ajax]
            [clojure.walk                                     :as w]
            [com.yetanalytics.lrs-admin-ui.handlers.util :refer [global-interceptors
                                                                 page-fx]]))

(re-frame/reg-event-fx
 :status/get-data
 global-interceptors
 (fn [{{server-host      ::db/server-host
        proxy-path       ::db/proxy-path
        {:keys [params]} ::db/status
        :as              db}
       :db}
      [_ include]]
   {;; Set loading state
    :db (reduce
         (fn [db' vis-k]
           (assoc-in db' [::db/status :loading vis-k] true))
         db
         include)
    ;; make request
    :fx [[:http-xhrio
          {:method          :get
           :uri             (httpfn/serv-uri
                             server-host
                             "/admin/status"
                             proxy-path)
           :params          (reduce-kv
                             (fn [m k v]
                               (assoc m (name k) v))
                             {"include" include}
                             params)
           :format          (ajax/json-request-format)
           :response-format (ajax/json-response-format {:keywords? false})
           :on-success      [:status/set-data include]
           :on-failure      [:status/server-error include]
           :interceptors    [httpfn/add-jwt-interceptor]}]]}))

(re-frame/reg-event-fx
 :status/server-error
 global-interceptors
 (fn [{:keys [db]} [_ include error]]
   {:db (reduce
         (fn [db' k]
           (update-in db' [::db/status :loading] dissoc k))
         db
         include)
    :fx [[:dispatch
          [:server-error error]]]}))

(def status-dispatch-all
  (into []
        (for [status-query ["statement-count"
                            "actor-count"
                            "last-statement-stored"
                            "timeline"
                            "platform-frequency"]]
          [:dispatch [:status/get-data [status-query]]])))

(defmethod page-fx :status [_]
  status-dispatch-all)

(re-frame/reg-event-fx
 :status/get-all-data
 global-interceptors
 (fn [_ _]
   {:fx status-dispatch-all}))

(defn- coerce-status-data
  "Convert string keys in status data to keyword where appropriate."
  [status-data]
  (reduce-kv
   (fn [m k v]
     (assoc m
            (keyword k)
            (if (= k "timeline")
              (w/keywordize-keys v)
              v)))
   {}
   status-data))

(re-frame/reg-event-fx
 :status/set-data
 global-interceptors
 (fn [{:keys [db]} [_ include status-data]]
   {:db (reduce
         (fn [db' k]
           (update-in db'
                      [::db/status :loading]
                      dissoc
                      k))
         (update-in db [::db/status :data] merge (coerce-status-data status-data))
         include)}))

(def timeline-control-fx
  "Fx call that will refesh the timeline for a control change, debounced."
  [:debounce-dispatch-later
   {:key :status/timeline-control
    :dispatch [:status/get-data ["timeline"]]
    :ms 1000}])

(re-frame/reg-event-fx
 :status/set-timeline-unit
 global-interceptors
 (fn [{:keys [db]} [_ unit]]
   {:db (assoc-in db [::db/status :params :timeline-unit] unit)
    :fx [timeline-control-fx]}))

(re-frame/reg-event-fx
 :status/set-timeline-since
 global-interceptors
 (fn [{{{{:keys [timeline-until]
          :or   {timeline-until (t/timeline-until-default)}}
         :params} ::db/status
        :as       db} :db} [_ since-datetime-str]]
   (try
     (let [timeline-since (t/local-datetime->utc since-datetime-str)]
       (if (< (js/Date. timeline-since) (js/Date. timeline-until))
         {:db (assoc-in db
                        [::db/status :params :timeline-since]
                        timeline-since)
          :fx [timeline-control-fx]}
         (.log js/console
               "New timeline-since ignored, must be before timeline-until.")))
     (catch js/Error _
       (.log js/console
             (str "Invalid timestamp " since-datetime-str " was ignored"))))))

(re-frame/reg-event-fx
 :status/set-timeline-until
 global-interceptors
 (fn [{{{{:keys [timeline-since]
          :or   {timeline-since (t/timeline-since-default)}}
         :params} ::db/status
        :as       db} :db} [_ until-datetime-str]]
   (try
     (let [timeline-until (t/local-datetime->utc until-datetime-str)]
       (if (< (js/Date. timeline-since) (js/Date. timeline-until))
         {:db (assoc-in db
                        [::db/status :params :timeline-until]
                        timeline-until)
          :fx [timeline-control-fx]}
         (.log js/console
               "New timeline-until ignored, must be after timeline-since.")))
     (catch js/Error _
       (.log js/console
             (str "Invalid timestamp " until-datetime-str " was ignored"))))))
