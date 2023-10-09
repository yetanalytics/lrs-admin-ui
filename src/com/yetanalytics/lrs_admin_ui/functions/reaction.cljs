(ns com.yetanalytics.lrs-admin-ui.functions.reaction
  (:require [goog.string :refer [format]]
            [goog.string.format]))

(defn path->string
  "Given a vector of keys and/or indices, return a JSONPath string suitable for
  SQL JSON access."
  ([path]
   (path->string path "$"))
  ([[seg & rpath] s]
   (if seg
     (recur rpath
            (cond
              (string? seg)
              ;; Unlike on the backend, these don't need to be valid to parse
              (format "%s.%s" s seg)

              (nat-int? seg)
              (format "%s[%d]" s seg)

              :else
              (throw (ex-info "Invalid path segement"
                              {:type    ::invalid-path-segment
                               :segment seg}))))
     s)))

;; (def types #{'string 'number 'boolean 'null 'lmap 'extensions})

(def pathmap-agent
  {"account"      {"homePage" 'string,
                   "name"     'string},
   "name"         'string,
   "mbox"         'string,
   "openid"       'string,
   "mbox_sha1sum" 'string,
   "objectType"   'string})

(def pathmap-group
  {"objectType"   'string,
   "member"       [pathmap-agent],
   "account"      {"homePage" 'string, "name" 'string},
   "mbox"         'string,
   "name"         'string,
   "mbox_sha1sum" 'string})

(def pathmap-actor
  (merge pathmap-agent
         pathmap-group))

(def pathmap-interaction-component
  {"id"          'string,
   "description" 'lmap})

(def pathmap-activity
  {"definition"
   {"source"                  [pathmap-interaction-component],
    "extensions"              'extensions,
    "description"             'lmap,
    "steps"                   [pathmap-interaction-component],
    "target"                  [pathmap-interaction-component],
    "moreInfo"                'string,
    "correctResponsesPattern" ['string],
    "interactionType"         'string,
    "choices"                 [pathmap-interaction-component],
    "scale"                   [pathmap-interaction-component],
    "name"                    'lmap,
    "type"                    'string},
   "id"         'string,
   "objectType" 'string})

(def pathmap-statement-ref
  {"id" 'string, "objectType" 'string})

(def pathmap-verb
  {"display" 'lmap, "id" 'string})

(def pathmap-attachment
  {"description" 'lmap,
   "display"     'lmap,
   "usageType"   'string,
   "contentType" 'string,
   "length"      'number,
   "fileUrl"     'string,
   "sha2"        'string})

(def pathmap-context
  {"language"     'string,
   "extensions"   'extensions,
   "team"         pathmap-group,
   "contextActivities"
   {"category" [pathmap-activity],
    "grouping" [pathmap-activity],
    "other"    [pathmap-activity],
    "parent"   [pathmap-activity]},
   "instructor"   pathmap-actor,
   "registration" 'string,
   "platform"     'string,
   "statement"    pathmap-statement-ref,
   "revision"     'string})

(def pathmap-result
  {"score"
   {"raw" 'number, "max" 'number, "min" 'number, "scaled" 'number},
   "extensions" 'extensions,
   "response"   'string,
   "duration"   'string,
   "completion" 'boolean,
   "success"    'boolean})

(def pathmap-sub-statement
  {"verb"        pathmap-verb,
   "objectType"  'string,
   "attachments" [pathmap-attachment],
   "context"     pathmap-context,
   "result"      pathmap-result,
   "timestamp"   'string,
   "object"      (merge pathmap-activity
                        pathmap-actor
                        pathmap-statement-ref),
   "actor"       pathmap-actor})

(def pathmap-statement
  (merge pathmap-sub-statement
         {"id"        'string,
          "version"   'string,
          "object"    (merge
                       pathmap-activity
                       pathmap-actor
                       pathmap-sub-statement
                       pathmap-statement-ref),
          "stored"    'string,
          "authority" pathmap-actor}))

(defn analyze-path*
  [pathmap path]
  (let [ret          (get-in pathmap
                             ;; zero out path indices
                             (mapv (fn [seg]
                                     (if (number? seg)
                                       0
                                       seg))
                                   path))
        ;; lmaps and extensions
        ?p-leaf-type (when-let [prev-path (not-empty (butlast path))]
                       (:leaf-type (analyze-path* pathmap prev-path)))]
    {:next-keys
     (cond
       (map? ret)    (into [] (keys ret))
       (vector? ret) ['idx]
       :else         [])
     :leaf-type (if (symbol? ret)
                  ret
                  (when ?p-leaf-type
                    (cond
                      (= 'lmap ?p-leaf-type)       'string
                      (= 'extensions ?p-leaf-type) 'json)))
     :valid?    (or
                 (some? ret)
                 (some? ?p-leaf-type))}))

(defn analyze-path
  "Given pathmap and a (possibly) partial path:
    Return a map with keys:
      :valid? Is the path valid per xapi?
      :next-keys - Coll of possible further keys. May contain the special key
        `'idx` to denote that the current structure is an array. When this
        coll is empty the path is complete.
      :leaf-type - If the path is complete, one of the following symbols:
        'string
        'number
        'boolean
        'null
        'lmap
        'extensions
      :valid? - Is the path a valid xapi path?"
  [pathmap path]
  (let [{:keys [leaf-type] :as ret} (analyze-path* pathmap path)]
    (merge ret
           {:leaf-type (when-not (contains? #{'lmap 'extensions} leaf-type)
                         leaf-type)})))
