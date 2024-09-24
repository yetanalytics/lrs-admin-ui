(ns com.yetanalytics.lrs-admin-ui.functions.download)

(defn download-json
  "Download `json-data` as `file-name.json`."
  [json-data file-name]
  (let [data-blob  (js/Blob. #js[json-data] #js{:type "application/json"})
        object-url (.createObjectURL js/URL data-blob)
        link-elem  (.createElement js/document "a")]
    (set! (.-href link-elem) object-url)
    (.setAttribute link-elem "download" file-name)
    (.appendChild (.-body js/document) link-elem)
    (.click link-elem)
    (.removeChild (.-body js/document) link-elem)
    (.revokeObjectURL js/URL object-url)))

(defn download-edn
  "Download `edn-data` as `file-name.json`. NOTE: Converts the EDN data
   into JSON."
  [edn-data file-name]
  (download-json (js/JSON.stringify (clj->js edn-data)) file-name))
