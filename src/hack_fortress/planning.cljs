(ns hack-fortress.planning
  (:require [hack-fortress.map-tools :as mt]
            [hack-fortress.path :as path]
            [hack-fortress.game-state :as s]
            [clojure.set :as set]))


(defn passable [{:keys [type]}]
  (not (contains? #{:wall :door} type)))


(def difficulties {:wall 10
                   :door 5
                   :person :ignore})

(defn compile-geo-edges-for-planning [{:keys [things] [mi mj] :map-size :as state}]
  (let [all-spaces (set (for [i (range 0 mi)
                              j (range mj)]
                          [i j]))
        pos-to-difficulty (into {} (for [[_ {:keys [pos type]}] things
                                         :let [d (difficulties type)]
                                         :when (not= d :ignore)] [pos d]))

        ]
    ;(println impassable-spaces)
    ;(println pos-to-difficulty things)
    (into {}
          (for [[i j] all-spaces
                [i' j'] (mt/around [mi mj] [i j])
                ;:when (contains? empty-spaces [i' j'])
                ]
            [[[i j] [i' j']] (or (pos-to-difficulty [i' j']) 1)
             ]))))


(defn compile-internet-edges-for-planning [state]
  {[[4 0] [5 3]] 2})


(defn compile-map-for-planning [state]
  (let [geo (compile-geo-edges-for-planning state)
        net (compile-internet-edges-for-planning state)]
    (into {}
          (for [edge (set/union (keys geo) (keys net))
                :let [g-v (geo edge)
                      n-v (net edge)]]
            [edge
             (cond
               (not g-v) [:net n-v]
               (not n-v) [:geo g-v]
               (<= g-v n-v) [:geo g-v]
               :else [:net n-v])]))))

(defn map-values [m f]
  (into {} (for [[k v] m] [k (f v)])))

(defn make-plan [player action {:keys [things]}]
  (let [guys (filter #(= (:player %) player) (map second things))              ; {:pos [3 4]}
        goal (-> action second things :pos)                                    ; [1 2]
        compiled-map (compile-map-for-planning @s/game-state)                ;
        compiled-edges (map-values compiled-map second)
        path (hack-fortress.path/A* compiled-edges (:pos (first guys)) goal)
        ;plan (zipmap path (map compiled-map path))
        steps (map vector path (rest path))
        plan (zipmap steps (map compiled-map steps))
        ]
    (println compiled-map)
    (println path)
    (println steps)
    (println plan)
    ;(println compiled-edges)
    ;(println compiled-map)
    ;(println (:pos (first guys)) goal path)
    plan))
  ;[[:move [4 4]]
  ; [:move [2 3]]
  ; [:move [5 6]]]
  ;)

