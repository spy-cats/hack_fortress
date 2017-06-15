(ns hack-fortress.path
  (:require [tailrecursion.priority-map :refer [priority-map priority-map-by]]
            [hack-fortress.map-tools :as mt]))


(defn euclidian-distance [a b] ; multidimensional
  (Math/sqrt (reduce + (map #(let [c (- %1 %2)] (* c c)) a b))))

(def estimate euclidian-distance)

(defn compile-map-for-path-finding [mp]
  (into {}
        (for [[[i j] t] (:nodes mp)
              :when (not= t :w)
              [i' j' t] (mt/around mp [i j])
              :when (not= t :w)]
          [[[i j] [i' j']] (euclidian-distance [i j] [i' j'])])))


(defn A*
  "Finds a path between start and goal inside the graph described by edges
   (a map of edge to distance); estimate is an heuristic for the actual
   distance. Accepts a named option: :monotonic (default to true).
   Returns the path if found or nil."
  [edges  ;; edges: {[x y] dist ...}
   ;estimate
    start goal & {mono :monotonic :or {mono true}}]
  (let [f (memoize #(estimate % goal)) ; unsure the memoization is worthy
        neighbours (reduce (fn [m [a b]] (assoc m a (conj (m a #{}) b)))
                           {} (keys edges))]
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
