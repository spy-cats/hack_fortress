(ns hack-fortress.sim.systems.pathfinding
  (:require [hack-fortress.sim.util :as util :refer [log]]
            [tailrecursion.priority-map :refer [priority-map priority-map-by]]))

; :pos
; ::goal
; ::path (?)

(declare A* compile-edges euclidian-distance around)

(defn make-path [compiled-map pos goal]
  ;(js* "debugger;")
  ;(log pos goal compiled-map)
  (rest (A* (:edges compiled-map) pos goal)))

(defn compile-map [state]
  (let [size (-> state :world :size)
        impassable (set (map :pos (util/all-typed :impassable state)))
        edges (compile-edges size impassable)]
    {:size       size
     :impassable impassable
     :edges      edges}))

(defn add-edges-from-impassable [{:keys [impassable size edges] :as compiled-map} [i j]]
  (if (contains? impassable [i j])
    (assoc compiled-map
      :edges
      (into edges
            (for [[i' j'] (around size [i j])
                  :when (not (contains? impassable [i' j']))]
              [[[i j] [i' j']] 1])))
    compiled-map))

(defn process-moving [compiled-map entity]
  (let [[kind goal-pos] (::goal entity)
        goal-test (delay (case kind
                           ::near #(>= 1 (euclidian-distance %1 %2))
                           ::exact =))
        pos (:pos entity)

        goal (if (or (nil? goal) (@goal-test goal-pos pos))
               nil
               [kind goal-pos])
        path (if (nil? goal)
               nil
               (make-path (add-edges-from-impassable compiled-map pos) pos goal-pos))]
    (log "?" entity pos goal path)
    (assoc entity
      ::goal goal
      ::path path
      :pos (or (first path) pos))))

(defn path-finding-system [state]
  (let [compiled-map (compile-map state)]
    (util/map-values
      #(if (util/of? :moving %)
         (process-moving compiled-map %)
         %) state)))


; --------------------


(defn euclidian-distance [a b]                              ; multidimensional
  (Math/sqrt (reduce + (map #(let [c (- %1 %2)] (* c c)) a b))))

(def estimate euclidian-distance)

(defn around [[mi mj] [i j]]
  (for [i' (map #(+ i %) [-1 0 1])
        j' (map #(+ j %) [-1 0 1])
        :when (not= [i j] [i' j'])
        :when (< i mi)
        :when (< j mj)
        :when (or (= i i') (= j j'))]                       ; FIXME optimize?
    [i' j']))

; state -> [(from to dist)]
(defn compile-edges [[width height] impassable]
  (into {}
        (for [i (range width)
              j (range height)
              :when (not (contains? impassable [i j]))
              [i' j'] (around [width height] [i j])
              :when (not (contains? impassable [i' j']))]
          [[[i j] [i' j']] 1])))


(defn A*
  "Finds a path between start and goal inside the graph described by edges
   (a map of edge to distance); estimate is an heuristic for the actual
   distance. Accepts a named option: :monotonic (default to true).
   Returns the path if found or nil."
  [edges                                                    ;; edges: {[x y] dist ...}
   ;estimate
   start goal & {mono :monotonic :or {mono true}}]
  (let [f (memoize #(estimate % goal))                      ; unsure the memoization is worthy
        neighbours (reduce (fn [m [a b]] (assoc m a (conj (m a #{}) b)))
                           {} (keys edges))]                ; FIXME neighbours -> move to compile
    (loop [q (priority-map start (f start))
           preds {}
           shortest {start 0}
           done #{}]
      (when-let [[x hx] (peek q)]
        (if (= goal x)
          (reverse (take-while identity (iterate preds goal)))
          (let [dx (- hx (f x))
                bn (for [n (remove done (neighbours x))
                         :let [hn (+ dx (edges [x n]) (f n))
                               sn (shortest n (.-POSITIVE_INFINITY js/Number))]
                         :when (< hn sn)]
                     [n hn])]
            (recur (into (pop q) bn)
                   (into preds (for [[n] bn] [n x]))
                   (into shortest bn)
                   (if mono (conj done x) done))))))))

