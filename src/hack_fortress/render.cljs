(ns hack-fortress.render
  (:require [hack-fortress.path]
            [hack-fortress.ai :as ai]
            [hack-fortress.game-state :as s]
            [clojure.string]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(def block [32 32])

(def block-x (nth block 0))
(def block-y (nth block 1))

(defn by-id [id]
  (.getElementById js/document (name id)))

(def canvas# (by-id "canvas"))
(def c-ctx (.getContext canvas# "2d"))

;(def info# (by-id "info"))
(def state# (by-id "state"))
;(def messages# (by-id "messages"))

(def toggle-state# (by-id "toggle-state"))


(set! (.-textAlign c-ctx) "center")
(set! (.-textBaseline c-ctx) "middle")
(set! (.-font c-ctx) (str block-x "px sans-serif"))




;
;(defn map! [{m :nodes}]
;  (doseq [[[i j] thing] m]
;    (render-thing! thing [i j])))

(def thing-captions {:person "a"
                     :pc "P"
                     :provider "@"
                     :wall :w})

(defn draw-line! [from to color]
  (.save c-ctx)
  (set! (.-strokeStyle c-ctx) color)
  (.beginPath c-ctx)
  (let [[x y] (map #(* %1 (+ 0.5 %2)) block from)]
    (.moveTo c-ctx x y))
  (let [[x y] (map #(* %1 (+ 0.5 %2)) block to)]
    (.lineTo c-ctx x y))
  (.stroke c-ctx)
  (.closePath c-ctx)
  (.restore c-ctx))

(defn plan! [start plan]
  ;(println plan)
  (let [moves (map second (filter #(= (first %) :move) plan))
        s-ds (map vector (cons start moves) moves)]
    (doseq [[s d] s-ds]
      (draw-line! s d "#673ab7"))))

(defmulti render-trait! first)
(defmethod render-trait! :default [arg thing] nil)
(defmethod render-trait! :internet [[_ provider-id] thing]
  (let [provider (s/get-thing-by-id provider-id)]
    (draw-line! (:pos thing) (:pos provider) "green")))


(defmethod render-trait! :moving-to [[_ goal] thing]
  (draw-line! (:pos thing) goal "blue"))

(defmethod render-trait! :caption [[_ caption] {[x y] :pos}]
  (.save c-ctx)
  (set! (.-font c-ctx) (str (/ block-x 3) "px sans-serif"))
  (.fillText c-ctx caption (+ (* x block-x) (/ block-x 2) 20) (+ (* y block-y) (/ block-y 20)))
  (.restore c-ctx))

(defn to-coords [x y]
  [(int (/ x block-x)) (int (/ y block-y))])


(defn thing! [type pos]
  (let [[x y] (map * block pos)]
    (case type
      :w (do
              (.fillRect c-ctx x y block-x block-y))
      :s nil
      (do
        ;(.strokeRect c-ctx x y (- block-x 1) (- block-y 1))

        (.fillText c-ctx type (+ x (/ block-x 2)) (+ y (/ block-y 2)))))))


(defn things! [things state]
  (doseq [[id thing] things]
    (let [color (if (:player thing)
                  (:color ((:player thing) (:players state)))
                  "black")]
      (.save c-ctx)

      (set! (.-fillStyle c-ctx) color)
      (set! (.-strokeStyle c-ctx) color)

      (thing! (thing-captions (:type thing)) (:pos thing))
      (doseq [trait thing] (render-trait! trait thing))

      (.restore c-ctx))))



;(defn update-trait-by-type)




(defn game-state-info [{:keys [age things]}]
  (clojure.string/join "\n" [(str "age: " age)
                             (str "btc: " (apply + (for [[_ ts] things
                                                         [t v] ts
                                                         :when (= t :btc)]
                                                     v)))]))

(defn things-info [{:keys [things]}]
  (clojure.string/join (for [t things] (str "<li>" t "</li>"))))

(defn players-info [{:keys [players]}]
  (clojure.string/join (for [p players] (str "<li>" p "</li>"))))


(defn clear! []
  ;(.clearRect c-ctx 0 0 (.-width canvas#) (.-height canvas#))
  (.save c-ctx)
  (set! (.-fillStyle c-ctx) "#e6e6e6")
  (.fillRect c-ctx 0 0 (.-width canvas#) (.-height canvas#))
  (.restore c-ctx))

(defn redraw! []
  (println @s/game-state)

  (clear!)
  ;(map! s/game-map)

  (things! (@s/game-state :things) @s/game-state)
  (set! (.-innerHTML toggle-state#) (if (= (:state @s/game-state) :running) "PAUSE" "RUN"))
  ;(set! (.-innerHTML info#) (game-state-info @s/game-state))
  (set! (.-innerHTML state#) (str
                               "<ul>" (players-info @s/game-state) "</ul>"
                               "<hr>"
                               "<ul>" (things-info @s/game-state) "</ul>"
                               "<hr>"
                               "<ul>" (clojure.string/join (map #(str "<li>" % "</li>") (:messages @s/game-state))) "</ul>"
                               )))


;
;(redraw!)
;(defonce interval (js/setInterval #(make-move!) 1000))
;
;(set! (.-onclick toggle-state#)
;      #(do (println "println?") (swap! s/game-state assoc :state (if (= (:state @s/game-state) :running) :paused :running))))
;

