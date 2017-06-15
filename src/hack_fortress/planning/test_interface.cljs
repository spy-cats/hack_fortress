(ns hack-fortress.planning.test-interface
  (:require [hack-fortress.game-state :as s]
            [hack-fortress.render :as render]
            [hack-fortress.planning :as planning]))


(defonce render-int (atom nil))
(defonce t-int (atom nil))
(defonce cur-plan (atom nil))



(defn render! []
  (render/clear!)
  (render/things! (:things @s/game-state) @s/game-state)
  (if @cur-plan
    (render/plan! @cur-plan))
  (set! (.-innerHTML render/state#) (str
                               "<ul>" (apply str (map #(str "<li>" % "</li>") @cur-plan)) "</ul>"
                               "<hr>"
                               "<ul>" (render/things-info @s/game-state) "</ul>"
                               ;"<hr>"
                               ;"<ul>" (clojure.string/join (map #(str "<li>" % "</li>") (:messages @s/game-state))) "</ul>"
                               )))


(defn profiling [label fn & rest]
  (.time js/console label)
  (let [res (apply fn rest)]
    (.timeEnd js/console label)
    (println res)
    res))


(defn new-plan! []
  (reset! cur-plan (profiling "plan" planning/make-plan :ai0 [:hacking :pc1] @s/game-state)))

(defn thing-at [pos]
  (first (filter #(= pos (:pos (second %))) (:things @s/game-state))))

(defn remove-at! [pos]
  (if-let [thing-id (first (thing-at pos))]
    (swap! s/game-state update-in [:things] dissoc thing-id)))

(defn onclick! [e]
  (let [pos (render/to-coords (.-offsetX e) (.-offsetY e))
        thing (case (.-value (.querySelector js/document "input[type=radio]:checked"))
                "wall" :wall
                "door" :door
                "remove" nil
                )]
    (if (nil? thing)
      (remove-at! pos)
      (swap! s/game-state update-in [:things] into {(gensym) {:type thing :pos pos}}))
  (new-plan!)))

(defn test-interface []
  ;(println "wtf")
  (set! (.-onclick render/canvas#) #(onclick! %))


  (new-plan!)

  (when (nil? @render-int)
    (render!)
    (reset! render-int (js/setInterval #(render!) 500)))

  ;(when (nil? @t-int)
  ;  (tick!)
  ;  (reset! t-int (js/setInterval #(tick!) 2000)))
  )