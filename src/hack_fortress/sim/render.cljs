(ns hack-fortress.sim.render
  (:require [rum.core :as rum]
            [hack-fortress.sim.util :as util :refer [log]]
            [hack-fortress.sim.content :as content]
            [clojure.pprint]))

(defonce renderer-state
         (atom {
                ;:tick 0
                }))


(def chosen-button-style {:backgroundColor "black"
                          :color           "white"})

(rum/defc top-menu < rum/reactive [ui-state-cursor ui-evolve!]
  (let [{:keys [world ui]} (rum/react ui-state-cursor)
        running? (:running? ui)]
    [:div {:style {:margin-bottom "5px"}}
     "Age: " (:age world) " "
     [:button {:style    (if (not running?) chosen-button-style {})
               :on-click #(ui-evolve! assoc :running? (not running?))}
      "pause"]
     [:span {:on-click #(ui-evolve! assoc :selected [:entity :ui])}
      " Todo: "
      (for [[st vs] (group-by :state (util/values (:todo-list ui)))]
        (str (count vs) " " st "; "))]]))


(rum/defc bottom-menu < rum/reactive [ui-state-cursor ui-evolve!]
  (let [{:keys [selected]} (rum/react ui-state-cursor)]
    [:div
     (for [[id c] content/constructions
           :let [chosen? (= [:build id] selected)]]
       [:button {:key      id
                 :style    (if chosen? chosen-button-style {})
                 :on-click #(ui-evolve! assoc :selected (if chosen? nil [:build id]))}
        [:b (:char c)] " " (:name c)])]))

(defn on-click! [ui-evolve! [i j] id selected]
  (case (first selected)
    :build (let [t (gensym)]
             (ui-evolve! update :todo-list conj
                         [t {:id           t
                             :type         :build
                             :pos          [i j]
                             :construction (second selected)}]))
    (ui-evolve! assoc :selected (if id [:entity id] nil))))

(rum/defc game-screen < rum/reactive [renderer-state ui-evolve!]
  (let [{[width height]               :size
         [tw th]                      :tile
         {:keys [selected highlight]} :ui
         :keys                        [entities]} (rum/react renderer-state)]
    [:svg {:width (* width tw) :height (* height th)}
     [:rect {:width "100%" :height "100%" :fill "black"}]
     [:g {:fill "white" :font-family "Courier New" :font-size "16px" :text-anchor "end"}
      (for [i (range width)
            j (range height)
            :let [char (entities [i j])]]
        [:text
         {:key      (or (:id char) [i j])
          :x        (* tw i)
          :y        (* th j)
          :fill     (cond (= highlight [i j]) "red"
                          (= selected [:entity (:id char)]) "yellow"
                          (util/of? :under-construction char) "green"
                          :default "white")
          :on-click #(on-click! ui-evolve! [i j] (:id char) selected)}
         (content/get-render-character char)])]]))

(rum/defc todo-list < rum/reactive [ui-state-cursor ui-evolve!]
  [:ul
   (for [[tid {:keys [type pos] :as t}] (:todo-list (rum/react ui-state-cursor))]
     [:li
      {:key           t
       :on-mouse-over #(ui-evolve! assoc :highlight pos)
       :on-mouse-out  #(ui-evolve! update :highlight (fn [p] (if (= p pos) nil p)))}
      (str t)])])

(rum/defc entity-details < rum/reactive [entity-state-cursor]
  [:pre (with-out-str (clojure.pprint/pprint (rum/react entity-state-cursor)))]
    ;[:ul
    ; (for [[tid {:keys [type pos] :as t}] (:todo-list (rum/react ui-state-cursor))]
    ;   [:li
    ;    {:key           t
    ;     :on-mouse-over #(ui-evolve! assoc :highlight pos)
    ;     :on-mouse-out  #(ui-evolve! update :highlight (fn [p] (if (= p pos) nil p)))}
    ;    (str t)])]
  )

(rum/defc history < rum/reactive [log-cursor]
  [:ul
   (for [[age m] (take 20 (reverse (rum/react log-cursor)))]
     [:li
      {:key [age m]}
      (str age ": " m)])])

(rum/defc details-box < rum/reactive [ui-evolve!]
  (if (-> (rum/react renderer-state) :ui :selected first (= :entity))
    (entity-details (rum/cursor-in renderer-state [:state (-> (rum/react renderer-state) :ui :selected second)]))
    (todo-list (rum/cursor renderer-state :ui) ui-evolve!)))

(rum/defc game [ui-evolve!]
  [:div
   [:div {:style {:display "flex"}}
    [:div {:style {:margin "10px"}}
     (top-menu renderer-state ui-evolve!)
     (game-screen renderer-state ui-evolve!)
     (bottom-menu (rum/cursor renderer-state :ui) ui-evolve!)]

    [:div {:style {:flex-grow "1"}}
     [:div {:style {:height "200px" :overflow-y "scroll"}}
      (details-box ui-evolve!)]
     [:div {:style {:height "200px" :overflow-y "scroll"}}
      (history (rum/cursor-in renderer-state [:ui :log]))]]]])

(defn init! [el ui-evolve!]
  (rum/mount (game ui-evolve!) el))

(defn recalc-render [state renderer-state]
  (assoc
    renderer-state
    ;(update renderer-state :tick inc)
    :size (:size (:world state))
    :tile [10 16]
    :state state
    :world (:world state)
    :ui (:ui state)
    :entities (into
                (util/indexed-by :pos (util/all-typed :construction state))
                (util/indexed-by :pos (util/all-typed :being state)))))

(defn every-animation-frame! [state-atom]
  (swap! renderer-state (partial recalc-render @state-atom))

  (js/window.requestAnimationFrame
    #(do
       (swap! renderer-state (partial recalc-render @state-atom))
       (every-animation-frame! state-atom)))


  ;(js/window.setInterval
  ;  #(do
  ;     (swap! renderer-state (partial recalc-render @state-atom)))
  ;  ;(every-animation-frame! state-atom))
  ;  100)

  )

