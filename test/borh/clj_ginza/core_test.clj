(ns clj-ginza.core-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [orchestra.spec.test :as st]
            [expound.alpha :as expound]
            [clj-ginza.core :refer :all]))

(st/instrument)
(stest/check (stest/enumerate-namespace 'clj-ginza.core))
(alter-var-root #'s/*explain-out* (constantly (expound/custom-printer {:show-valid-values? true :print-specs? false :theme :figwheel-theme})))

(deftest model-info-test
  (is (model-info)))

(deftest text->tokens-test
  (let [tokens (text->tokens "銀座鉄道の夜")]
    (is (= 4 (count tokens)))
    (is (= ["PROPN" "NOUN" "ADP" "NOUN"]
           (->> tokens (map token->map) (map :pos))))))