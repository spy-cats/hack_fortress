(ns hack-fortress.sim.content
  (:require [hack-fortress.sim.util :as util :refer [log of?]]))

(def constructions
  (util/indexed-by :id
    [{:id :wall
      :name "Wall"
      :types #{:impassable}
      :char "X"}

     {:id :door
      :name "Door"
      :char "="}]))

(defn get-render-character [e]
  (cond
    (nil? e) "."
    (of? :being e) "b"
    (of? :construction e) (-> e :construction constructions :char)
    :default "?"))
