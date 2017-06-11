(ns hack-fortress.map-tools)

(def blocks {:w {:type :wall}})

(defn map-to-things [m]
  (into {} (for [[i row] (map-indexed vector m)
                 [j thing] (map-indexed vector row)
                 :when (contains? blocks thing)]
             [(gensym) (into {:pos [j i]} (thing blocks))])))



(defn around [[mi mj] [i j]]
  (for [i' (map #(+ i %) [-1 0 1])
        j' (map #(+ j %) [-1 0 1])
        :when (not= [i j] [i' j'])
        :when (< i mi)
        :when (< j mj)]
    [i' j']))


