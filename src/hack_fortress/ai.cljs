(ns hack-fortress.ai
  (:require [hack-fortress.path]
            [hack-fortress.map-tools :as mt]
            [hack-fortress.game-state :as s]
            [clojure.string]))


;
;(defn ?!assoc
;  "Same as assoc, but dissoc if v is nil"
;  [m & kvs]
;  (let [{diss true ass false} (group-by #(nil? (second %)) (partition 2 kvs))]
;    (into (apply dissoc m (map first diss))
;          (map vec ass))))
;
;(defmulti process-player #(:type (second %)))
;(defmethod process-player :human [[id player] state] [])
;(defmethod process-player :ai [[id {:keys [goals todo] :as player}] state]
;  (let [goals' (if (empty? goals)
;                 [[:hacking (first (rand-nth (filter #(= (:type (second %)) :pc) (:things state))))]]
;                 goals)
;        todo' (if (empty? todo)
;                [(first goals')]
;                todo)]
;    [#(assoc-in % [:players id :goals] goals')
;     #(assoc-in % [:players id :todo] todo')
;     ;#(update-in % [:messages] conj (str (:age state) ": computer to be hacked"))
;     ]))
;
;
;(defmulti process-action first)
;(defmethod process-action :default [arg thing state] thing)
;(defmethod process-action :mining [arg thing state]
;  (update thing :btc inc))
;(defmethod process-action :wandering [_ {:keys [pos moving-to] :as thing} state]
;  (let [moving-to' (if (or (= moving-to pos) (nil? moving-to))
;                     (first (rand-nth (filter #(= (second %) :s) (:nodes s/game-map))))
;                     moving-to)]
;    (assoc thing :moving-to moving-to')))
;(defmethod process-action :hacking [[_ pc-id] {:keys [pos] :as thing} state]
;  (let [pc (s/get-thing-by-id pc-id)]
;    (if (not= pos (:pos pc))
;      (assoc thing :moving-to (:pos pc))
;
;      (if (< (rand) 0.9)
;        (assoc thing :caption "hacking...")
;        (dissoc thing :caption :action)))))
;
;
;(defmulti process-trait first)
;(defmethod process-trait :default [[trait-kind arg] thing state] thing)
;(defmethod process-trait :moving-to [_ {:keys [pos moving-to] :as thing} _]
;  (if (or (= moving-to pos) (nil? moving-to))
;    (dissoc thing :moving-to)
;    (assoc thing :pos )))
;(defmethod process-trait :action [[_ action-kind] thing state]
;  (process-action action-kind thing state))
;;(defmethod process-trait :player [[_ id] {:keys [player pos action type] :as thing}]
;;  (if (and (empty? action) (= type :person))
;;    (assoc thing :action )
;;    thing))
;
;(defn process-thing [[id thing] state]
;  (apply concat (map #(process-trait % thing state) thing)))
;
;
;(defn collect-and-apply-changes [state coll-key fn]
;  (let [changes (apply concat (map #(fn % state) (coll-key state)))]
;    (loop [s state
;           chs changes]
;      ;(println chs)
;      (if (empty? chs)
;        s
;          (recur ((first chs) s)
;                 (rest chs))))))
;
;
;(defn active? [thing]
;  (contains #{:pc :person} (:type thing)))
;
;
;(defn choose-action [thing state]
;  (if (nil? (:action thing)) ; TODO: check possibility
;    (first (:todo (player (:players @s/game-state))))  ; TODO: check if someone else took it
;    (:action thing)))
;
;(defn choose-destination [action actor state]
;  (case (first action)
;    :hacking nil
;    :wandering (first (rand-nth (filter #(= (second %) :s) (:nodes s/game-map))))))
;
;(defn step-towards [pos goal]
;  (second (hack-fortress.path/A* s/compiled-map pos moving-to)))
;
;(defn thing-move [[id {:keys [pos] :as thing}] state]
;  (if (active? thing)
;
;    (let [action (choose-action thing state)
;          destination (choose-destination thing state)]
;
;      (if (and destination (not= destination pos))
;
;        [#(assoc-in % [:things id :moving-to] destination)
;         #(assoc-in % [:things id :pos] (step-towards pos destination))]
;
;        (conj #(update-in % [:things id] dissoc :moving-to) (perform action thing state))))))
;
;
;
;
;
;(defn make-move [{:keys [age things players] :as state}]
;  (-> state
;      (update :age inc)
;      (collect-and-apply-changes :players process-player)
;      (collect-and-apply-changes :things process-thing)))