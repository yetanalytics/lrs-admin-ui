(ns com.yetanalytics.lrs-admin-ui.functions.password)

;; Character classes
(def digit-chars (map char (range 48 58)))  ; 0-9
(def upper-chars (map char (range 65 91)))  ; A-Z
(def lower-chars (map char (range 97 123))) ; a-z
(def special-chars [\! \@ \# \$ \% \^ \& \* \_ \- \+ \= \?])

(defn pass-gen
  "Given length `n`, generate a password with random uppercase and lowercase
   letters, numbers, and specials. At least one of each character category will
   be included in the generated password. Note that `n` should be at at least
   4 or else undefined behavior will occur."
  [n]
  (let [;; Password
        dcount (inc (rand-int (- n 3)))
        ucount (inc (rand-int (- n 2 dcount)))
        lcount (inc (rand-int (- n 1 dcount ucount)))
        scount (- n dcount ucount lcount)
        ;; Password chars
        nseq (take dcount (repeatedly (partial rand-nth digit-chars)))
        useq (take ucount (repeatedly (partial rand-nth upper-chars)))
        lseq (take lcount (repeatedly (partial rand-nth lower-chars)))
        sseq (take scount (repeatedly (partial rand-nth special-chars)))]
    (->> (concat nseq useq lseq sseq)
         shuffle
         (reduce str))))
