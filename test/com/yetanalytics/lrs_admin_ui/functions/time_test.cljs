(ns com.yetanalytics.lrs-admin-ui.functions.time-test
  "Test time utility functions. Note that any headless browser running this
  should have TZ=America/New_York set."
  (:require [clojure.test :refer-macros [deftest is testing]]
            [com.yetanalytics.lrs-admin-ui.functions.time
             :refer [tz-offset-mins*
                     utc->local-datetime]]))

(deftest tz-offset-mins*-test
  (testing "Yields different offsets for different dates on either side of the EDT/EST boundary"
    (is (not=
         (tz-offset-mins* (new js/Date "2025-03-04T00:00:00Z"))
         (tz-offset-mins* (new js/Date "2025-03-19T00:00:00Z"))))))

(deftest utc->local-datetime-test
  (testing "derives timezone offset appropriate to the passed-in date"
    (is (= "2025-03-04T14:48:53"
           (utc->local-datetime "2025-03-04T19:48:53Z")))
    (is (= "2025-03-19T15:48:53"
           (utc->local-datetime "2025-03-19T19:48:53Z")))))
