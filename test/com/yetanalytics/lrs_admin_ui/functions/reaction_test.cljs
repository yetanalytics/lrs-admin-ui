(ns com.yetanalytics.lrs-admin-ui.functions.reaction-test
  (:require [clojure.test :refer-macros [deftest are testing]]
            [com.yetanalytics.lrs-admin-ui.functions.reaction
             :refer [path->string]]))

(deftest path->string-test
  (testing "converts paths to easily-readable JSONPath strings"
    (are [input output]
        (= output
           (path->string input))
      []
      "$"

      ["object" "id"]
      "$.object.id"

      ["context" "contextActivities" "parent" 0 "id"]
      "$.context.contextActivities.parent[0].id"

      ["context" "extensions" "https://www.google.com/array"]
      "$.context.extensions.https://www.google.com/array")))
