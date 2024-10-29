(ns com.yetanalytics.lrs-admin-ui.handlers.browser
  (:require [re-frame.core                                    :as re-frame]
            [com.yetanalytics.lrs-admin-ui.db                 :as db]
            [day8.re-frame.http-fx]
            [com.yetanalytics.lrs-admin-ui.functions.http     :as httpfn]
            [ajax.core                                        :as ajax]
            [com.yetanalytics.lrs-admin-ui.handlers.util :refer [global-interceptors]]))

(re-frame/reg-event-fx
 :browser/load-xapi
 global-interceptors
 (fn [{{server-host ::db/server-host
        proxy-path  ::db/proxy-path
        xapi-prefix ::db/xapi-prefix} :db} [_ {:keys [path params]}]]
   (let [xapi-url (httpfn/build-xapi-url
                   server-host xapi-prefix path params proxy-path)]
     {:dispatch   [:browser/set-address xapi-url]
      :http-xhrio {:method          :get
                   :uri             xapi-url
                   :response-format (ajax/json-response-format {:keywords? false})
                   :on-success      [:browser/load-stmts-success]
                   :on-failure      [:server-error]
                   :interceptors    [httpfn/req-xapi-interceptor]}})))

(re-frame/reg-event-db
 :browser/set-address
 (fn [db [_ address]]
   (assoc-in db [::db/browser :address] address)))

(re-frame/reg-event-db
 :browser/load-stmts-success
 global-interceptors
 (fn [db [_ {:strs [statements more]}]]
   (update-in db [::db/browser] assoc
              :content   statements
              :more-link more)))

(re-frame/reg-event-fx
 :browser/more
 global-interceptors
 (fn [{:keys [db]} _]
   ;; Convert more link into params and request new data.
   (let [more-params
         (httpfn/extract-params (get-in db [::db/browser :more-link]))
         address (get-in db [::db/browser :address])]
     ;; Push current address into stack
     {:db (update-in db [::db/browser :back-stack] conj address)
      :dispatch [:browser/load-xapi {:params more-params}]})))

(re-frame/reg-event-fx
 :browser/back
 global-interceptors
 (fn [{:keys [db]} _]
   ;; Pop most recent from stack
   (let [back-stack (get-in db [::db/browser :back-stack])
         back-params (httpfn/extract-params (peek back-stack))]
     {:db (update-in db [::db/browser :back-stack] pop)
      :dispatch [:browser/load-xapi {:params back-params}]})))

(re-frame/reg-event-fx
 :browser/add-filter
 global-interceptors
 (fn [{:keys [db]} [_ param-key param-value]]
   (let [address (get-in db [::db/browser :address])
         params (-> (httpfn/extract-params address)
                    (dissoc "from" "limit")
                    (assoc param-key param-value))]
     ;; Clear back-stack
     {:db (assoc-in db [::db/browser :back-stack] [])
      :dispatch [:browser/load-xapi {:params params}]})))

(re-frame/reg-event-fx
 :browser/clear-filters
 global-interceptors
 (fn [{:keys [db]} _]
   {;; Clear back-stack and reset query
    :db (assoc-in db [::db/browser :back-stack] [])
    :dispatch [:browser/load-xapi]}))

(re-frame/reg-event-fx
 :browser/update-credential
 global-interceptors
 (fn [{:keys [db]} [_ key]]
   (let [credential (first (filter #(= key (:api-key %))
                                   (::db/credentials db)))]
     (when credential
       ;; Clear backstack and limit and filters
       {:db (update-in db [::db/browser] assoc
                       :credential credential
                       :back-stack []
                       :batch-size 10)
        :dispatch [:browser/load-xapi]}))))

(re-frame/reg-event-fx
 :browser/refresh
 global-interceptors
 (fn [{:keys [db]} _]
   (when (get-in db [::db/browser :credential])
     ;; Clear backstack
     {:db (assoc-in db [::db/browser :back-stack] [])
      :dispatch [:browser/load-xapi]})))

(re-frame/reg-event-fx
 :browser/update-batch-size
 global-interceptors
 (fn [{:keys [db]} [_ batch-size]]
   ;; Clear from and limit
   (let [address (get-in db [::db/browser :address])
         params (-> (httpfn/extract-params address)
                    (dissoc "from" "limit"))]
     ;; update batch to new size and clear back-stack
     {:db (update-in db [::db/browser] assoc
                     :batch-size batch-size
                     :back-stack [])
      :dispatch [:browser/load-xapi {:params params}]})))
