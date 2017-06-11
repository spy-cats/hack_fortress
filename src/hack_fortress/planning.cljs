(ns hack-fortress.planning
  (:require [hack-fortress.map-tools :as mt]
            [hack-fortress.path :as path]
            [hack-fortress.game-state :as s]
            [clojure.set :as set]))

(defn passable [{:keys [type]}]
  (not (contains? #{:wall :door} type)))

(defn compile-edges-for-planning [{:keys [things] [mi mj] :map-size :as state}]
  (let [impassable-spaces (set (map :pos (filter (complement passable) (map second things))))
        ;empty-spaces (set (map :pos (filter passable (map second things))))
        empty-spaces (set/difference
                       (set (for [i (range 0 mi)
                                  j (range mj)]
                              [i j]))
                       impassable-spaces)]
    ;(println impassable-spaces)
    (into {}
          (for [[i j] empty-spaces
                [i' j'] (mt/around [mi mj] [i j])
                :when (contains? empty-spaces [i' j'])]
            [[[i j] [i' j']] 1                              ;(path/euclidian-distance [i j] [i' j']) (diagonals are just as wll)
             ]))))


(defn make-plan [player action {:keys [things]}]
  (let [guys (filter #(= (:player %) player) (map second things))
        goal (-> action second things :pos)
        compiled-map (compile-edges-for-planning @s/game-state)
        path (hack-fortress.path/A* compiled-map (:pos (first guys)) goal)]
    ;(println guys)
    ;(println compiled-map)
    ;(println (:pos (first guys)) goal path)
    (map #(vector :move %) (rest path))))
  ;[[:move [4 4]]
  ; [:move [2 3]]
  ; [:move [5 6]]]
  ;)

