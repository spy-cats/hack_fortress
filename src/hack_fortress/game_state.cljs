(ns hack-fortress.game-state
  (:require [hack-fortress.map-tools :as mt]))

(def things (mt/map-to-things
              [[:s :s :s :s :s :s :s :s]
               [:s :s :s :s :s :s :s :s]
               [:s :s :s :s :w :w :s :s]
               [:s :s :w :s :w :s :s :s]
               [:s :s :w :s :w :s :s :s]
               [:s :s :w :s :s :s :s :s]
               [:s :s :w :s :s :s :s :s]
               [:s :s :s :s :s :s :s :s]]))

(defonce game-state (atom
                      {:map-size [8 8]
                       :things   (into things
                                       {:guy1  {:type   :person
                                                :pos    [1 6]
                                                ;:action [:wandering]
                                                :player :human
                                                }

                                        :guy2  {
                                                :type   :person
                                                :pos    [1 1]
                                                :player :ai0


                                                ;:action [:hacking]
                                                }

                                        :pc1   {:type     :pc
                                                :pos      [5 3]
                                                :btc      100
                                                :internet :prov1
                                                :player   :human
                                                :action   [:mining]}

                                        :prov1 {:type :provider
                                                :pos  [7 0]}})

                       :players  {:human {:type :human
                                          :todo []
                                          :color "blue"}

                                  :ai0   {:type  :ai
                                          :goals []
                                          :todo  []
                                          :color "red"}}
                       :age      0
                       :state    :running

                       :messages []}))


;(def compiled-map (hack-fortress.path/compile-map-for-path-finding game-map))

(defn get-thing-by-id [id]
  (second (first (filter #(= (first %) id) (:things @game-state)))))
