(ns com.yetanalytics.lrs-admin-ui.functions.reaction-test
  (:require [clojure.test :refer-macros [deftest is are]]
            [com.yetanalytics.lrs-admin-ui.functions.reaction
             :refer [parent-paths analyze-path pathmap-statement]]))

(deftest parent-paths-test
  (is (= [["context" "extensions" "https://foo.bar/baz" "foo"]
          ["context" "extensions" "https://foo.bar/baz"]
          ["context" "extensions"]
          ["context"]]
         (parent-paths
          ["context"
           "extensions"
           "https://foo.bar/baz"
           "foo"
           "bar"])))
  (is (= []
         (parent-paths
          ["context"]))))

(deftest analyze-path-test
  (are [path result]
      (= (analyze-path pathmap-statement path)
         result)
    []                      {:next-keys
                             ["object"
                              "authority"
                              "verb"
                              "id"
                              "timestamp"
                              "context"
                              "version"
                              "stored"
                              "attachments"
                              "actor"
                              "objectType"
                              "result"],
                             :leaf-type nil,
                             :valid?    true
                             :complete?  false}
    ["actor"]               {:next-keys
                             ["account"
                              "name"
                              "mbox"
                              "openid"
                              "mbox_sha1sum"
                              "objectType"
                              "member"],
                             :leaf-type nil,
                             :valid?    true
                             :complete? false}
    ["actor"
     "member"]              {:next-keys ['idx],
                             :leaf-type nil,
                             :valid?    true
                             :complete? false}
    ["actor"
     "member"
     3]                     {:next-keys
                             ["account"
                              "name"
                              "mbox"
                              "openid"
                              "mbox_sha1sum"
                              "objectType"],
                             :leaf-type nil,
                             :valid?    true
                             :complete? false}
    ["actor"
     "member"
     3
     "mbox"]                {:next-keys [],
                             :leaf-type 'string,
                             :valid?    true
                             :complete? true}
    ["foo"]                 {:next-keys [],
                             :leaf-type nil,
                             :valid?    false
                             :complete? true}
    ["context"
     "extensions"]          {:next-keys [],
                             :leaf-type nil,
                             :valid?    true
                             :complete? false}
    ["context"
     "extensions"
     "https://foo.bar/baz"] {:next-keys [],
                             :leaf-type 'json,
                             :valid?    true
                             :complete? false}
    ["context"
     "extensions"
     "https://foo.bar/baz"
     "foo"]                 {:next-keys [],
                             :leaf-type 'json,
                             :valid?    true
                             :complete? false}
    ["context"
     "extensions"
     "https://foo.bar/baz"
     "foo"
     "bar"]                 {:next-keys [],
                             :leaf-type 'json,
                             :valid?    true
                             :complete? false}
    ["object"
     "definition"
     "name"]                {:next-keys [],
                             :leaf-type nil,
                             :valid?    true
                             :complete? false}
    ["object"
     "definition"
     "name"
     "en-US"]               {:next-keys [],
                             :leaf-type 'string,
                             :valid?    true
                             :complete? true}
    ["object"
     "definition"
     "name"
     "en-US"
     "foo"]                 {:next-keys [],
                             :leaf-type nil,
                             :valid?    false
                             :complete? true}))
