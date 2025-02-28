(ns cljsjs.codemirror
  "A shim so we can run cm w/o the cljsjs dep and pass analyses"
  (:require [codemirror]))

;; Force a global export so re-codemirror's usage style works
(set! js/window.CodeMirror codemirror)
