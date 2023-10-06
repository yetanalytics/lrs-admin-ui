(ns com.yetanalytics.lrs-admin-ui.functions.reaction-test
  (:require [clojure.test :refer-macros [deftest are]]
            [com.yetanalytics.lrs-admin-ui.functions.reaction
             :refer [analyze-path pathmap-statement]]))

(deftest analyze-path-test
  (are [path result]
      (= (analyze-path pathmap-statement path)
         result)
    []                                                   {:next-keys
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
                                                          :valid?    true}
    ["actor"]                                            {:next-keys
                                                          ["account"
                                                           "name"
                                                           "mbox"
                                                           "openid"
                                                           "mbox_sha1sum"
                                                           "objectType"
                                                           "member"],
                                                          :leaf-type nil,
                                                          :valid?    true}
    ["actor" "member"]                                   {:next-keys ['idx],
                                                          :leaf-type nil,
                                                          :valid?    true}
    ["actor" "member" 3]                                 {:next-keys
                                                          ["account"
                                                           "name"
                                                           "mbox"
                                                           "openid"
                                                           "mbox_sha1sum"
                                                           "objectType"],
                                                          :leaf-type nil,
                                                          :valid?    true}
    ["actor" "member" 3 "mbox"]                          {:next-keys [],
                                                          :leaf-type 'string,
                                                          :valid?    true}
    ["foo"]                                              {:next-keys [],
                                                          :leaf-type nil,
                                                          :valid?    false}
    ["context" "extensions"]                             {:next-keys [],
                                                          :leaf-type nil,
                                                          :valid?    true}
    ["context" "extensions" "https://foo.bar/baz"]       {:next-keys [],
                                                          :leaf-type 'json,
                                                          :valid?    true}
    ["context" "extensions" "https://foo.bar/baz" "foo"] {:next-keys [],
                                                          :leaf-type nil,
                                                          :valid?    true}
    ;; ["context" "extensions" "https://foo.bar/baz" "foo" "bar"]
    ["object" "definition" "name"]                       {:next-keys [],
                                                          :leaf-type nil,
                                                          :valid?    true}
    ["object" "definition" "name" "en-US"]               {:next-keys [],
                                                          :leaf-type 'string,
                                                          :valid?    true}
    ))
