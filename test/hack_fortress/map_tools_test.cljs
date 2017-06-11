(ns hack-fortress.map-tools-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [hack-fortress.map-tools]
            [hack-fortress.path]))



(deftest make-map-test
   (is (= (hack-fortress.map-tools/make-map [[:s :s]
                                             [:w :w]])
          {:nodes {[0 0] :s, [1 0] :s, [0 1] :w, [1 1] :w}})))

(def test-map (hack-fortress.map-tools/make-map
                [[:b :h :s]
                 [:s :w :s]
                 [:w :w :s]
                 [:s :s :s]
                 [:s :s :s]]))


(deftest around-test
         (is (= (hack-fortress.map-tools/around test-map [0 1])
                [[0 0 :b] [0 2 :w] [1 0 :h] [1 1 :w] [1 2 :w]])))

;(println (hack-fortress.map-tools/around test-map [0 1]))

;(println (for [[[i j] t] (:nodes test-map)] [i j t]))

;(println (hack-fortress.path/compile-map-for-path-finding test-map))

(deftest path-test
  (is (= (hack-fortress.path/A*
           (hack-fortress.path/compile-map-for-path-finding test-map)
           [0 0]
           [0 3])
         [[0 0] [1 0] [2 1] [2 2] [1 3] [0 3]])))


(run-tests)