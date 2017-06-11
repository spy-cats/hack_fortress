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
    (render/plan! (:pos (:guy2 (:things @s/game-state))) @cur-plan))
  (set! (.-innerHTML render/state#) (str
                               ;"<ul>" (players-info @s/game-state) "</ul>"
                               ;"<hr>"
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

(defn onclick! [e]
  (let [pos (render/to-coords (.-offsetX e) (.-offsetY e))
        thing (case (.-value (.querySelector js/document "input[type=radio]:checked"))
                "wall" :wall
                "door" :door
                ;"remove" nil
                )]
    (swap! s/game-state update-in [:things] into {(gensym) {:type thing :pos pos}}))
  (new-plan!))

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