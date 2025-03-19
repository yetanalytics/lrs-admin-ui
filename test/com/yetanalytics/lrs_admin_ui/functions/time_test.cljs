(ns com.yetanalytics.lrs-admin-ui.functions.time-test
  (:require [clojure.test :refer-macros [deftest is testing]]
            [com.yetanalytics.lrs-admin-ui.functions.time
             :refer [tz-offset-mins*
                     utc->local-datetime]]))

(deftest tz-offset-mins*-test
  (testing "yields different offsets for different dates"
    (is (not=
         (tz-offset-mins* (new js/Date "2025-03-04T00:00:00Z"))
         (tz-offset-mins* (new js/Date "2025-03-19T00:00:00Z"))))))

(deftest utc->local-datetime-test
  (testing "derives timezone offset appropriate to the passed-in date"
    (is (= "2025-03-04T14:48:53"
           (utc->local-datetime "2025-03-04T19:48:53Z"
                                :tz-offset-mins 300)))
    (is (= "2025-03-19T15:48:53"
           (utc->local-datetime "2025-03-19T19:48:53Z"
                                :tz-offset-mins 240)))))
