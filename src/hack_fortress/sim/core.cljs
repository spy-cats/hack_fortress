(ns hack-fortress.sim.core
  (:require [hack-fortress.sim.render :as render]
            [hack-fortress.sim.util :as util :refer [log values]]
            [hack-fortress.sim.content :as content]
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
                     :selected      nil    ; [:entity pos] | [:build type]
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

(defn update-in-many [state path-fns]
  ((apply comp (for [[p fn] path-fns]
                 #(update-in % p fn)))
    state))

(defn check-tasks-system [state]
  (let [tasks (util/values (-> state :ui :todo-list))
        constructions (util/indexed-by :pos (util/all-typed :construction state))
        classify-task #(let [c (constructions (:pos %))]
                         (cond
                           (nil? c) :possible
                           (not= (:construction c) (:construction %)) :impossible
                           (util/of? :under-construction c) :in-progress
                           :default :done))
        ; TODO: task conflicts
        tasks (map #(assoc % :state (classify-task %)) tasks)]
    (assoc-in state [:ui :todo-list] (util/indexed-by :id tasks))))



(defn assign-tasks-system [state]
  (let [tasks (-> state :ui :todo-list)
        cancelled? #(contains? #{:impossible :done} (:state %))
        workers (util/all-typed :worker state)
        cancel-worker-tasks (map :id (filter #(-> % :task tasks cancelled?) workers))

        free-workers (into
                       (map :id (filter (complement :task) workers))
                       cancel-worker-tasks)
        unassigned-tasks (map :id (filter #(nil? (:assignee %)) (filter (complement cancelled?) (values tasks))))
        assignations (map vector free-workers unassigned-tasks)
        as-fns (apply comp (for [[wid tid] assignations]
                             #(-> %
                                  (assoc-in [:ui :todo-list tid :assignee] wid)
                                  (assoc-in [wid :task] tid))))]
    (-> state
        (ulog (for [[w t] assignations]
                (str w " starts " t)))
        (update-in-many (for [id cancel-worker-tasks]
                          [[id :task] (constantly nil)]))
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

; we have "spaces" (should not conflict:)
; e.g. #impassable & new constructions
;      #impassable & #being
;      new constructions & #being (how????)
;      #being & #being
;      ........

(defn apply-builds-system [state]
  (let [[builds other-events] (util/split-by-pred #(= (:type %) :build) (-> state :_work_events_bag :events))
        pos-types (set (map #(map % [:pos :construction]) builds))
        already-in-progress (map #(map % [:id :pos :construction])
                                 (filter
                                   (util/of? :under-construction)
                                   (util/values state)))
        to-create (map #(cons (gensym) %)
                       (set/difference pos-types (set (map rest already-in-progress))))

        constructions (set (map :pos (util/all-typed :construction state)))
        [denied to-create] (util/split-by-pred #(contains? constructions (second %)) to-create)

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
                               #(update-in % [id :progress] inc)))]
    (-> state
        (ulog (for [c created-entities]
                (str "started " c)))
        (ulog (for [d denied]
                (str "denied " d)))
        (into created-entities)
        to-update-fns
        (assoc-in [:_work_events_bag :events] other-events))))

(defn apply-finish-build-system [state]
  (let [finished (filter #(>= (:progress %) 10) (util/all-typed :under-construction state))
        updates (for [{:keys [id]} finished]
                  [[id] #(-> %
                             (dissoc :progress)
                             (update :types disj :under-construction)
                             (update :types into (-> % :construction content/constructions :types)))])]
    (-> state
        (ulog (for [d finished]
                (str "finished " finished)))
        (update-in-many updates))))

(def systems [#(update-in % [:world :age] inc)
              ; TODO: check done and impossible tasks
              check-tasks-system
              assign-tasks-system
              perform-tasks-system
              apply-builds-system
              apply-finish-build-system])

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
            (js/setInterval #(swap! state evolve-state) 300)
            (render/every-animation-frame! state)
            ])







