(ns borh.clj-ginza.utils
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [clojure.core.memoize :as m]))

(def ->python-case
  (m/fifo csk/->snake_case_string {} :fifo/threshold 512))

(def ->clojure-case
  (m/fifo csk/->kebab-case-keyword {} :fifo/threshold 512))

(defn ->clojure-case-map [m]
  (cske/transform-keys ->clojure-case m))