(ns hack-fortress.sim.render
  (:require [rum.core :as rum]
            [hack-fortress.sim.util :as util :refer [log]]
            [hack-fortress.sim.content :as content]))

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
                 :on-click #(ui-evolve! (fn [st] (assoc st :running? (not running?))))}
        "pause"]]))


(rum/defc bottom-menu < rum/reactive [ui-state-cursor ui-evolve!]
  (let [{:keys [current-build]} (rum/react ui-state-cursor)]
    [:div
     (for [[id c] content/constructions
           :let [chosen? (= id current-build)]]
       [:button {:key      id
                 :style    (if chosen? chosen-button-style {})
                 :on-click #(ui-evolve! (fn [st] (assoc st :current-build id)))}
        [:b (:char c)] " " (:name c)])]))

(rum/defc game-screen < rum/reactive [renderer-state ui-evolve!]
  (let [{[width height]                    :size
         [tw th]                           :tile
         {:keys [current-build highlight]} :ui
         :keys                             [characters]} (rum/react renderer-state)]
    [:svg {:width (* width tw) :height (* height th)}
     [:rect {:width "100%" :height "100%" :fill "black"}]
     [:g {:fill "white" :font-family "Courier New" :font-size "16px" :text-anchor "end"}
      (for [i (range width)
            j (range height)
            :let [char (or (characters [i j])
                           ".")]]
        [:text
         {:key      [i j]
          :x        (* tw i)
          :y        (* th j)
          :fill     (if (= highlight [i j]) "red" "white")
          :on-click #(let [t (gensym)] (ui-evolve! update :todo-list conj [t {:id           t
                                                                              :type         :build
                                                                              :pos          [i j]
                                                                              :construction current-build}]))}
         char])]]))

(rum/defc todo-list < rum/reactive [ui-state-cursor ui-evolve!]
  [:ul
   (for [[tid {:keys [type pos] :as t}] (:todo-list (rum/react ui-state-cursor))]
     [:li
      {:key           t
       :on-mouse-over #(ui-evolve! assoc :highlight pos)
       :on-mouse-out  #(ui-evolve! update :highlight (fn [p] (if (= p pos) nil p)))}
      (str t)])])

(rum/defc history < rum/reactive [log-cursor]
  [:ul
   (for [[age m] (take 20 (reverse (rum/react log-cursor)))]
     [:li
      {:key [age m]}
      (str age ": " m)])])

(rum/defc game [ui-evolve!]
  [:div
   [:div {:style {:display "flex"}}
    [:div {:style {:margin "10px"}}
     (top-menu renderer-state ui-evolve!)
     (game-screen renderer-state ui-evolve!)
     (bottom-menu (rum/cursor renderer-state :ui) ui-evolve!)]

    [:div {:style {:flex-grow "1" }}
     [:div {:style {:height "200px" :overflow-y "scroll"}}
      (todo-list (rum/cursor renderer-state :ui) ui-evolve!)]
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
    :world (:world state)
    :ui (:ui state)
    :characters (into {} (map #(vector (:pos %) "B")
                              (filter (util/of? :being)
                                      (map second state))))))

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

