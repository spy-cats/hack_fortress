(ns hack-fortress.sim.util
  (:require [clojure.set :as set]))


(defn log [& r]
  (apply js/console.log r))

(defn values [m]
  (map second m))

(defn of?
  ([type]
   (let [types (if (coll? type)
                 (set type)
                 #{type})]
    #(of? types %)))
  ([types entity]
   (if (vector? entity)
     (of? types (second entity))
     (if (not (coll? types))
       (of? #{types} entity)
       (seq (set/intersection types (:types entity)))))))


(defn all-typed [type state]
  (filter (of? type) (map second state)))

(defn indexed-by [key forms]
  (into {} (for [f forms]
             [(key f) f])))

(defn map-values [f m]
  (into {} (for [[k v] m]
             [k (f v)])))

(defn split-by-pred [pred coll]
  (let [{truey true
         falsey false} (group-by (some-fn pred) coll)]
    [truey falsey]))
