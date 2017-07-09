(ns hack-fortress.sim.content
  (:require [hack-fortress.sim.util :as util :refer [log]]))

(def constructions
  (util/indexed-by :id
    [{:id :wall
      :name "Wall"
      :char "X"}

     {:id :door
      :name "Door"
      :char "="}]))