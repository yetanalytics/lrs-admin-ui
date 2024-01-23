;; This test runner is intended to be run from the command line
(ns com.yetanalytics.lrs-admin-ui.test-runner
  (:require
   #_clojure.test.check
   #_clojure.test.check.properties
   ;; require all the namespaces that you want to test
   [com.yetanalytics.lrs-admin-ui.functions.reaction-test]
   [figwheel.main.testing :refer-macros [run-tests-async]]))

(defn -main [& args]
  (run-tests-async 120000))
