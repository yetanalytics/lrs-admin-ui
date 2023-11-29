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

(defn parent-paths
  "Given a path vector, return a seq of parent paths in reverse order.
  Does not return the root path []."
  [path]
  (lazy-seq
   (when-let [ppath (not-empty
                     (-> path
                         butlast
                         vec))]
     (cons
      ppath
      (parent-paths ppath)))))

(defn- zero-indices
  "Replace index integers with 0 in path"
  [path]
  (mapv (fn [seg]
          (if (number? seg)
            0
            seg))
        path))

(defn analyze-path*
  [path]
  (let [ret       (get-in pathmap-statement
                          (zero-indices path))
        ;; lmaps and extensions
        [?p-idx ?p-leaf-type]
        (or (some
             (fn [[idx ppath]]
               (let [ret (get-in pathmap-statement
                                 (zero-indices ppath))]
                 (when (symbol? ret)
                   [idx ret])))
             (map-indexed vector (parent-paths path)))
            [])
        next-keys (cond
                    (map? ret)    (into [] (keys ret))
                    (vector? ret) ['idx]
                    :else         [])
        leaf-type (if (symbol? ret)
                    (when-not (contains? #{'lmap 'extensions} ret)
                      ret)
                    (when ?p-leaf-type
                      (cond
                        (and (= 'lmap ?p-leaf-type)
                             (= 0 ?p-idx))           'string
                        (= 'extensions ?p-leaf-type) 'json)))
        valid?    (or
                   (some? ret)
                   (= 'extensions ?p-leaf-type)
                   (and (= 'lmap ?p-leaf-type)
                        (= 0 ?p-idx)))]
    {:next-keys next-keys
     :leaf-type leaf-type
     :valid?    valid?
     :complete?
     (cond
       ;; Invalid paths are always complete
       (not valid?)                      true
       ;; extensions are never complete
       (or (= 'extensions ret)
           (= 'extensions ?p-leaf-type)) false
       ;; lmaps w/o ltag are not complete
       (= 'lmap ret)                     false
       :else                             (empty? next-keys))}))

(def analyze-path
  "Given a (possibly) partial xapi path:
    Return a map with keys:
      :valid? Is the path valid per xapi?
      :next-keys - Coll of possible further keys. May contain the special key
        `'idx` to denote that the current structure is an array.
      :leaf-type - One of the following symbols:
        'string
        'number
        'boolean
        'null
        'json - In the case of extensions, might be any JSON scalar!
      :valid? - Is the path a valid xapi path?
      :complete? - For the purposes of UI, is the path complete (ie. no further
        segments should be offered)? Will be false for extension paths."
  (memoize analyze-path*))

(defn val-type
  "Get a value type as a string"
  [val]
  (cond
    (string? val)
    "string"
    (number? val)
    "number"
    (boolean? val)
    "boolean"
    (nil? val)
    "null"))

(defn index-conditions
  "Provide sort-indices for reaction conditions editing. Base order on prior
  indices if available."
  [reaction]
  (update-in
   reaction
   [:ruleset :conditions]
   (fn [conditions]
     (reduce
      (fn [m
           [idx
            [condition-key
             condition-val]]]
        (assoc
         m
         condition-key
         (assoc condition-val :sort-idx idx)))
      {}
      (map-indexed
       vector
       (sort-by
        (comp :sort-idx val)
        conditions))))))

(defn strip-condition-indices
  "Remove all indices from conditions."
  [reaction]
  (update-in
   reaction
   [:ruleset :conditions]
   (fn [conditions]
     (reduce-kv
      (fn [m k v]
        (assoc m k (dissoc v :sort-idx)))
      {}
      conditions))))
