(ns clj-ginza.core
  (:require [libpython-clj.python :as py]
            [libpython-clj.jna.base]
            [clj-ginza.utils :refer [->python-case ->clojure-case ->clojure-case-map]]
            [aero.core :as aero]
            [clojure.spec.alpha :as s])
  (:import (clojure.lang IPersistentCollection)))

(def config (aero/read-config (clojure.java.io/resource "ginza.edn")))

(alter-var-root #'libpython-clj.jna.base/*python-library* (constantly (:python-interpreter config)))

(py/initialize!)

(defonce spacy (py/import-module "spacy"))

(defonce nlp (py/call-attr spacy "load" (:model config)))

(defn model-info
  "Return the spaCy (GiNZA) model information."
  []
  (py/get-attr nlp "meta"))

(defn get-nested
  "Helper function to return nested attributes in Python objects. attrs must be a vector of string attributes."
  [obj attrs]
  (loop [a attrs
         o obj]
    (if (seq a)
      (recur (rest a) (py/get-attr o (first a)))
      o)))

(defn text->doc
  ([text]
   (py/call nlp text))
  ([text {:keys [batch-size] :or {batch-size 100}}]
   (s/assert (s/coll-of string?) text)
   (py/call-attr-kw nlp "pipe" [text] {"batch_size" batch-size})))

(defprotocol IDoc
  (nlp-doc [x]))

(extend-type String
  IDoc
  (nlp-doc [text] (text->doc text)))

(extend-type IPersistentCollection
  IDoc
  (nlp-doc [text] (mapcat identity (text->doc text {}))))

(def ginza-token-kw-map
  {:i                     "i"
   :orth                  "orth_"
   :lemma                 "lemma_"
   :pos                   "pos_"
   :tag                   "tag_"
   :tag-extra             ["_" "inf"]
   :head-i                ["head" "i"]
   :bunsetu-label         ["_" "bunsetu_bi_label"]
   :bunsetu-position-type ["_" "bunsetu_position_type"]
   :dep                   "dep_"})

(defn token->map
  ([token]
   (token->map token [:i :orth :lemma :pos :tag :tag-extra :head-i :bunsetu-label :bunsetu-position-type :dep]))
  ([token kws]
   (reduce
     (fn [m kw]
       (let [spacy-attr (kw ginza-token-kw-map)]
         (assoc m kw (if (vector? spacy-attr)
                       (get-nested token spacy-attr)
                       (py/get-attr token spacy-attr)))))
     {}
     kws)))

(defn similarity [token-a token-b]
  (py/call-attr token-a "similarity" token-b))

(defn embedding-vector [token]
  (py/get-attr token "vector"))

(defn embedding-vector-norm [token]
  (py/get-attr token "vector_norm"))

(defn doc->sentences [doc]
  (py/get-attr doc "sents"))

(defn sentence->tokens [sentence]
  (->> sentence nlp-doc doc->sentences (mapcat identity)))

(defn text->tokens [text]
  (->> text nlp-doc (into [])))

(s/fdef text->tokens
  :args (s/cat :text (s/or :text string? :texts (s/coll-of string?)))
  :ret (s/coll-of any? #_#(instance? % :pyobject)))

(defn by-bunsetu
  "Given a sequence of tokens, returns those tokens partitioned into vectors of tokens each representing one bunsetsu
  unit. If passing in multiple sentences, sentence boundaries will not result in additional nesting."
  [tokens]
  (loop [tokens* tokens
         b []]
    (if-not (seq tokens*)
      b
      (let [token (first tokens*)
            rest-tokens (rest tokens*)
            bi (get-nested token ["_" "bunsetu_bi_label"])]
        (recur rest-tokens
               (case bi
                 "B" (conj b [token])
                 "I" (update b (dec (count b)) conj token)))))))