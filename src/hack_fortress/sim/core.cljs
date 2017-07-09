(ns hack-fortress.sim.core
  (:require [hack-fortress.sim.render :as render]
            [hack-fortress.sim.util :as util :refer [log values]]
            [clojure.set :as set]))


(def default-state [{:id    :world
                     :types #{:world}
                     :size  [60 22]
                     :age   0}

                    {:id    :mob1
                     :types #{:being :worker}
                     :pos   [5 6]}

                    {:id    :mob2
                     :types #{:being :worker}
                     :pos   [3 2]}

                    {:id    :mob3
                     :types #{:being :worker}
                     :pos   [8 1]}

                    {:id           :wall1
                     :types        #{:construction :impassable}
                     :construction :wall
                     :pos          [2 2]}

                    {:id            :ui
                     :types         #{:ui}
                     :current-build :wall
                     :todo-list     {}
                     :log           [[0 "game begins"]]
                     :highlight     nil}])


(defonce state (atom (util/indexed-by :id default-state)))

(defn step-toward [[fx fy] [tx ty]]
  (cond
    (< fx tx) [(inc fx) fy]
    (> fx tx) [(dec fx) fy]
    (< fy ty) [fx (inc fy)]
    (> fy ty) [fx (dec fy)]
    :default [fx fy]))

(defn ulog [state msgs]
  (let [msgs (if (string? msgs) [msgs] msgs)
        age (-> state :world :age)]
    (update-in state [:ui :log] into (for [m msgs]
                                       [age m]))))



(defn run-by-types [fn entity]
  (loop [e entity
         t (seq (:types entity))]
    (if (empty? t)
      e
      (recur (fn (first t) e)
             (rest t)))))



(defn ui-evolve! [fn & args]
  (apply swap! state update :ui fn args))

(defn assign-tasks-system [state]
  (let [workers (util/all-typed :worker state)
        free-workers (map :id (filter (complement :task) workers))
        unassigned-tasks (map :id (filter #(nil? (:assignee %)) (values (-> state :ui :todo-list))))
        assignations (map vector free-workers unassigned-tasks)
        as-fns (apply comp (for [[wid tid] assignations]
                             #(-> %
                                  (assoc-in [:ui :todo-list tid :assignee] wid)
                                  (assoc-in [wid :task] tid))))]
    (-> state
        (ulog (for [[w t] assignations]
                (str w " starts " t)))
        as-fns)))


(defn close? [a b]
  (let [[x1 y1] (:pos a)
        [x2 y2] (:pos b)]
    (>= 1 (apply + (map Math/abs [(- x1 x2) (- y1 y2)])))))

(defn perform-tasks-system [state]
  (let [workers (util/all-typed :worker state)
        with-tasks (filter :_task (map #(assoc % :_task (-> state :ui :todo-list (get (:task %))))
                                       workers))
        [to-work to-move] (util/split-by-pred #(close? (:_task %) %) with-tasks)
        to-move-upds (apply comp
                            (for [w to-move]
                              #(assoc-in %
                                         [(:id w) :pos]
                                         (step-toward (:pos w) (-> w :_task :pos)))))
        work-events (for [w to-work]
                      (dissoc (:_task w) :id))]
    (-> state
        to-move-upds
        (conj [:_work_events_bag
               {:id     :_work_events_bag
                :types  #{:events_bag}
                :events work-events}]))))

; we have "spaces" (should not conflict)

(defn apply-builds-system [state]
  (let [[builds other-events] (util/split-by-pred #(= (:type %) :build) (-> state :_work_events_bag :events))
        pos-types (set (map #(map % [:pos :construction]) builds))
        already-in-progress (map #(map % [:id :pos :construction])
                                 (filter
                                   (util/of? :under-construction)
                                   (util/values state)))
        to-create (map #(cons (gensym) %)
                       (set/difference pos-types (set (map rest already-in-progress))))
        ; TODO: check conflicts with existing and new entities
        created-entities (for [[id pos construction] to-create]
                           [id {:id           id
                                :pos          pos
                                :construction construction
                                :types        #{:under-construction :construction}
                                :progress     0}])

        to-update (into
                    (filter #(contains? pos-types (rest %)) already-in-progress)
                    to-create)

        to-update-fns (apply comp
                             (for [[id pos construction] to-update]
                               #(update-in % [id :progress] inc)))

        ; TODO: check whether constuction is done
        ]
    (-> state
        (ulog (str "build events done: " (count builds) ", created: " (count created-entities)))
        (into created-entities)
        to-update-fns
        (assoc-in [:_work_events_bag :events] other-events))))

(def systems [#(update-in % [:world :age] inc)
              assign-tasks-system
              perform-tasks-system
              apply-builds-system])

(def all-systems (apply comp (reverse systems)))

(defn evolve-state [state]
  (if (-> state :ui :running?)
    (try
      (doall (all-systems state))
      (catch js/Error e
        (js/console.error e)
        (assoc-in state [:ui :running?] false)))
    state))

(render/init! (js/document.getElementById "app") ui-evolve!)

(defonce _ [
            (js/setInterval #(swap! state evolve-state) 1000)
            (render/every-animation-frame! state)
            ])







