(ns com.yetanalytics.lrs-admin-ui.functions.download)

(defn download
  "Create a link of the form
   ```
   <a href=[object-url] download=[file-name].[extension] target=\"_blank\" />
   ```
   then append it to the DOM, \"click\" on it, and it will download `data-blob`
   as a file. The `object-url` can either be a HTTP endpoint on a server, or
   a URL representation of an object returned by `URL.createObjectURL()`."
  [url file-name]
  (let [link-element (.createElement js/document "a")]
    (set! (.-href link-element) url)
    (set! (.-target link-element) "_blank")
    (.setAttribute link-element "download" file-name)
    (.appendChild (.-body js/document) link-element)
    (.click link-element)
    (.removeChild (.-body js/document) link-element)
    (.revokeObjectURL js/URL url)))

;; TODO: Test out refactored download-json button
(defn download-json
  "Download `json-data` as `file-name.json`."
  [json-data file-name]
  (let [data-blob  (js/Blob. #js[json-data] #js{:type "application/json"})
        object-url (.createObjectURL js/URL data-blob)
        #_#_link-elem  (.createElement js/document "a")]
    (download object-url file-name)
    #_(set! (.-href link-elem) object-url)
    #_(.setAttribute link-elem "download" file-name)
    #_(.appendChild (.-body js/document) link-elem)
    #_(.click link-elem)
    #_(.removeChild (.-body js/document) link-elem)
    #_(.revokeObjectURL js/URL object-url))
    )

(defn download-edn
  "Download `edn-data` as `file-name.json`. NOTE: Converts the EDN data
   into JSON."
  [edn-data file-name]
  (download-json (js/JSON.stringify (clj->js edn-data)) file-name))
