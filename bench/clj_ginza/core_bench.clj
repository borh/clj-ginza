(ns clj-ginza.core-bench
  (:require [libra.bench :refer :all]
            [libra.criterium :as c]
            [clj-ginza.core :refer :all]
            [clojure.string :as string]))

(defbench text->tokens-bench
  (is (dur 1 (text->tokens (string/join (repeat 1000 "銀河鉄道の夜に。")))))
  (is (dur 1 (text->tokens (repeat 1000 "銀河鉄道の夜に。")))))
