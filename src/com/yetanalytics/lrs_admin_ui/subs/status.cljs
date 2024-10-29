(ns com.yetanalytics.lrs-admin-ui.subs.status
  (:require [re-frame.core :refer [reg-sub]]
            [com.yetanalytics.lrs-admin-ui.db             :as db]
            [com.yetanalytics.lrs-admin-ui.functions.time :as t]))

(reg-sub
 :status/enabled?
 (fn [db _]
   (::db/enable-admin-status db false)))

(reg-sub
 :db/status
 (fn [db _]
   (::db/status db)))

(reg-sub
 :status/params
 :<- [:db/status]
 (fn [status _]
   (:params status)))

(reg-sub
 :status.params/timeline-unit
 :<- [:status/params]
 (fn [params _]
   (:timeline-unit params "day")))

(reg-sub
 :status.params/timeline-since
 :<- [:status/params]
 (fn [params _]
   (:timeline-since params (t/timeline-since-default))))

(reg-sub
 :status.params/timeline-since-local
 :<- [:status.params/timeline-since]
 (fn [since _]
   (t/utc->local-datetime since)))

(reg-sub
 :status.params/timeline-until
 :<- [:status/params]
 (fn [params _]
   (:timeline-until params (t/timeline-until-default))))

(reg-sub
 :status.params/timeline-until-local
 :<- [:status.params/timeline-until]
 (fn [until _]
   (t/utc->local-datetime until)))

(reg-sub
 :status/data
 :<- [:db/status]
 (fn [status _]
   (:data status)))

(reg-sub
 :status.data/statement-count
 :<- [:status/data]
 (fn [data _]
   (:statement-count data)))

(reg-sub
 :status.data/actor-count
 :<- [:status/data]
 (fn [data _]
   (:actor-count data)))

(reg-sub
 :status.data/last-statement-stored
 :<- [:status/data]
 (fn [data _]
   (:last-statement-stored data)))

(reg-sub
 :status.data/last-statement-stored-locale
 :<- [:status.data/last-statement-stored]
 (fn [last-stored _]
   (when last-stored
     (.toLocaleString (js/Date. last-stored)))))

(def bar-colors
  ["#137BCE"
   "#BAD54C"
   "#20956A"
   "#405EA7"
   "#D88638"
   "#6D4B9B"])

(reg-sub
 :status.data/platform-frequency
 :<- [:status/data]
 (fn [data _]
   (let [freqs (:platform-frequency data {})
         total (reduce + 0 (vals freqs))]
     (mapv
      (fn [[[platform count] color]]
        {:x platform
         :y (-> count
                (/ total)
                (* 100)
                int)
         :fill color})
      (map vector freqs (cycle bar-colors))))))

(reg-sub
 :status.data.timeline/data
 :<- [:status/data]
 (fn [data _]
   (mapv
    (fn [{:keys [stored count]}]
      {:x (js/Date. stored)
       :y count})
    (:timeline data))))

(reg-sub
 :status.data.timeline/domain
 :<- [:status.params/timeline-since]
 :<- [:status.params/timeline-until]
 :<- [:status.data.timeline/data]
 (fn [[since until data] _]
   {:x [;; Make sure the domain min includes all data present
        ;; this prevents a blank timeline in some cases
        (js/Date.
         (if (not-empty data)
           (min
            (-> data first :x .getTime)
            (.parse js/Date since))
           since))
        (js/Date. until)]}))

(reg-sub
 :status/loading-map
 :<- [:db/status]
 (fn [status _]
   (:loading status {})))

(reg-sub
 :status/loading?
 :<- [:status/loading-map]
 (fn [loading-map [_ loading-k]]
   (true? (get loading-map loading-k))))
