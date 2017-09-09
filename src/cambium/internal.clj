;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns cambium.internal
  (:require
    [clojure.tools.logging      :as ctl]
    [clojure.tools.logging.impl :as ctl-impl]))


(defn expected
  "Throw IllegalArgumentException indicating what was expected and what was found instead."
  ([expectation found]
    (throw (IllegalArgumentException.
             (format "Expected %s, but found (%s) %s" expectation (class found) (pr-str found)))))
  ([pred expectation found]
    (when-not (pred found)
      (expected expectation found))))


(defn scalar-literal?
  "Return true if argument is a program literal, false otherwise."
  [x]
  (or (string? x)
    (instance? Boolean x)
    (number? x)
    (keyword? x)
    (vector? x)
    (set? x)))


(defn mdc-literal?
  "Return true if potential MDC literal/expression, false otherwise."
  [x]
  (and (not (scalar-literal? x))
    (not (vector? x))
    (not (set? x))))


(defn throwable-literal?
  "Return true if potential throwable exception literal/expression, false otherwise."
  [x]
  (and (not (scalar-literal? x))
    (not (vector? x))
    (not (set? x))
    (not (map? x))))


(defmacro strcat
  "Stripped down impl of Stringer/strcat: https://github.com/kumarshantanu/stringer"
  ([]
    "")
  ([token]
    (if (or (string? token)
          (keyword? token)
          (number? token))
      (str token)
      `(let [x# ~token]
         (if (nil? x#)
           ""
           (String/valueOf x#))))))


(defn as-str
  "Turn anything into string"
  ^String [x]
  (cond
    (instance? clojure.lang.Named x) (name x)
    (string? x)                      x
    :otherwise                       (strcat x)))


(defn stringify-nested-keys
  "Given a potentially nested structure, turn all map keys to string using the stringify-key argument."
  [stringify-key v]
  (if (map? v)
    (zipmap
      (map stringify-key (keys v))
      (map #(stringify-nested-keys stringify-key %) (vals v)))
    v))
