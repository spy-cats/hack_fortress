(ns hack-fortress.sim.core
  (:require [hack-fortress.sim.render :as render]
            [hack-fortress.sim.util :as util :refer [log values]]))


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
  (let [workers (util/find :worker state)
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

(defn perform-tasks-system [state]
  (let [workers (util/find :worker state)
        with-tasks (filter :_task (map #(assoc % :_task (-> state :ui :todo-list (get (:task %))))
                                       workers))
        {to-move false
         to-work true} (group-by #(= (-> % :_task :pos) (:pos %)) with-tasks)
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

(defn apply-work-system [state]
  (let [events (-> state :_work_events_bag :events)

        ; find all already existing entities (or entities-in-progress)
        ; find all entities that should be created
        ; discard conflicting

        ]
    (-> state
        (ulog (str "work done: " events))
        (dissoc :_work_events_bag))))

(def systems [#(update-in % [:world :age] inc)
              assign-tasks-system
              perform-tasks-system
              apply-work-system])

(def all-systems (apply comp (reverse systems)))

(defn evolve-state [state]
  (if (-> state :ui :running?)
    (try
      (all-systems state)
      (catch js/Error e
        (js/console.error e)
        (update-in state [:ui :running?] false)))
    state))

(render/init! (js/document.getElementById "app") ui-evolve!)

(defonce _ [
            (js/setInterval #(swap! state evolve-state) 1000)
            (render/every-animation-frame! state)
            ])







