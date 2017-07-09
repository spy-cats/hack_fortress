(ns hack-fortress.sim.util)


(defn log [& r]
  (apply js/console.log r))

(defn values [m]
  (map second m))

(defn of?
  ([type]
   #(of? type %))
  ([type entity]
   (if (vector? entity)
     (of? type (second entity))
     (contains? (get entity :types #{}) type))))

(defn find [type state]
  (filter (of? type) (map second state)))

(defn indexed-by [key forms]
  (into {} (for [f forms]
             [(key f) f])))

(defn map-values [f m]
  (into {} (for [[k v] m]
             [k (f v)])))
