(ns app.view.board
  (:require
    [taoensso.timbre :as timbre]
    ["@material-ui/core" :as mui]
    ["@material-ui/core/styles" :as styles
     :refer [makeStyles]]
    ["@material-ui/icons/FileCopy" :default CopyIcon]
    ["@material-ui/core/Grid" :default Grid]
    ["@material-ui/core/Hidden" :default Hidden]
    ["react" :as react]
    [re-frame.core :as rf]
    [reagent.core :as reagent]
    [app.view.invoice :as invoice-view]
    [app.view.identity
     :refer [identity-card]]
    [app.view.entry :as entry
     :refer [profile-card]]))

(def debug (rf/subscribe [:debug]))

(defmulti pane (fn [{:keys [stage] :as session}]
                  (if stage [@stage])))

(defn error-boundary [{:as props} & children]
  "Catch errors to limit the effect on rendering"
  (let [error-status (reagent/atom nil)]
    (reagent/create-class
     {:get-derived-state-from-error
      (fn [error]
        #js{:hasError true})
      :component-did-catch
      (fn [error info]
        (timbre/warn error info)
        (reset! error-status error))
      :reagent-render
      (fn [& children]
        (if-not @error-status
          (into [:<>] children)
          [:div.error-boundary]))})))

(defn board-listing [{:keys [items]}]
  (timbre/debug "Show as list")
  (into
    [:> mui/List]
    (for [{:keys [id selected] :as item} items]
       ^{:key id}
       [:> mui/ListItem
        {:selected (boolean selected)}
        [entry/profile-panel {:item item}]])))

(defn mui-styles [f]
  (styles/makeStyles (fn [theme] (clj->js (f theme)))))

(def use-styles (mui-styles (fn [theme]
                              {:grid-bg {:padding (.spacing theme 1)}})))

(defn Canvas [props extra]
  ;; React function component aka hook - Can't use Ratoms here!
  (let [; [count set-count] (react/useState 0)
        classes (js->clj (use-styles) :keywordize-keys true)]
    (timbre/debug "Canvas with class name:" classes)
    (reagent/as-element
     [:div {:class-name (:grid-bg classes)}
       (.-children props)])))

(def canvas (reagent/adapt-react-class Canvas))

(defn board-grid [{:keys [items]}]
  (timbre/debug "Show as grid" #_(js-keys classes))
  [canvas {}
         (into
          [:> Grid {:container true :spacing 1}]
          (for [{:keys [id selected] :as item} items]
            ^{:key id}
           [:> Grid {:item true :xs 12 :sm 6 :md 4}
             #_{:selected (boolean selected)}
             [error-boundary {}
              [entry/pick-zone {:item item
                                :style {:width "100%"
                                        :height "100%"}}
               [entry/profile-card {:item item}]]]]))])

(def requesting-funds (rf/subscribe [:requesting-funds]))

(defn board-pane [items]
  (let [on-dialog-close #(rf/dispatch [:request-funds false])]
    (fn [items]
      [error-boundary {}
       [:> mui/Dialog {:open (boolean @requesting-funds)
                       :on-close on-dialog-close}
        [invoice-view/funding-request-card]]
       [:> mui/Hidden {:sm-up true}
        [board-listing {:items items}]]
       [:> mui/Hidden {:xs-down true}
        [board-grid {:items items}]]])))

(defmethod pane :default [{:keys [mobile profile] :as session}]
  [board-pane (:fields @profile)])
