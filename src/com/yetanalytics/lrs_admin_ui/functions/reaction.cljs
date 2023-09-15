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

(def pathmap-agent
  #:agent{:account
          #:account{:homePage 'string,
                    :name     'string},
          :name         'string,
          :mbox         'string,
          :openid       'string,
          :mbox_sha1sum 'string,
          :objectType   'string})

(def pathmap-group
  #:group{:objectType   'string,
          :member       pathmap-agent,
          :account
          #:account{:homePage 'string, :name 'string},
          :mbox         'string,
          :name         'string,
          :mbox_sha1sum 'string})

(def pathmap-actor
  (merge pathmap-agent
         pathmap-group))

(def pathmap-interaction-component
  #:interaction-component{:id          'string,
                          :description 'lmap})

(def pathmap-activity
  #:activity{:definition
             #:definition{:source                  pathmap-interaction-component,
                          :extensions              'extensions,
                          :description             'lmap,
                          :steps                   pathmap-interaction-component,
                          :target                  pathmap-interaction-component,
                          :moreInfo                'string,
                          :correctResponsesPattern 'string,
                          :interactionType         'string,
                          :choices                 pathmap-interaction-component,
                          :scale                   pathmap-interaction-component,
                          :name                    'lmap,
                          :type                    'string},
             :id         'string,
             :objectType 'string})

(def pathmap-statement-ref
  #:statement-reference{:id 'string, :objectType 'string})

(def pathmap-verb
  #:verb{:display 'lmap, :id 'string})

(def pathmap-attachment
  #:attachment{:description 'lmap,
               :display     'lmap,
               :usageType   'string,
               :contentType 'string,
               :length      'number,
               :fileUrl     'string,
               :sha2        'string})

(def pathmap-context
  #:context{:language     'string,
            :extensions   'extensions,
            :team         pathmap-group,
            :contextActivities
            #:context-activities{:category pathmap-activity,
                                 :grouping pathmap-activity,
                                 :other    pathmap-activity,
                                 :parent   pathmap-activity},
            :instructor   pathmap-actor,
            :registration 'string,
            :platform     'string,
            :statement    pathmap-statement-ref,
            :revision     'string})

(def pathmap-result
  #:result{:score
           #:score{:raw 'number, :max 'number, :min 'number, :scaled 'number},
           :extensions 'extensions,
           :response   'string,
           :duration   'string,
           :completion 'boolean,
           :success    'boolean})

(def pathmap-sub-statement
  #:sub-statement{:verb        pathmap-verb,
                  :objectType  'string,
                  :attachments pathmap-attachment,
                  :context     pathmap-context,
                  :result      pathmap-result,
                  :timestamp   'string,
                  :object      (merge pathmap-activity
                                 pathmap-actor
                                 pathmap-statement-ref),
                  :actor       pathmap-actor})

(def pathmap
  #:statement{:result      pathmap-result,
              :id          'string,
              :context     pathmap-context,
              :version     'string,
              :timestamp   'string,
              :object      (merge
                       pathmap-activity
                       pathmap-actor
                       pathmap-sub-statement
                       pathmap-statement-ref),
              :actor       pathmap-actor,
              :stored      'string,
              :verb        pathmap-verb,
              :attachments pathmap-attachment,
              :authority   pathmap-actor})

(def card-many-attrs
  #{:context-activities/category
    :context-activities/grouping
    :context-activities/other
    :context-activities/parent
    :definition/choices
    :definition/correctResponsesPattern
    :definition/scale
    :definition/source
    :definition/steps
    :definition/target
    :group/member
    :statement/attachments
    :sub-statement/attachments})
