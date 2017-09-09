;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns cambium.codec.util
  (:require
    [clojure.edn :as edn]))


(defn as-str
  "Turn given argument into string."
  ^String [^Object x]
  (cond
    (instance?
      clojure.lang.Named x) (if-let [^String the-ns (namespace x)]
                              (let [^StringBuilder sb (StringBuilder. the-ns)]
                                (.append sb \/)
                                (.append sb (name x))
                                (.toString sb))
                              (name x))
    (instance? String x)    x
    (nil? x)                ""
    :otherwise              (.toString x)))


;; ----- Codec (default: EDN codec) helper -----


(defn encode-val
  "Encode MDC value as string such that it retains type information. May be used to redefine cambium.core/stringify-val.
  See: decode-val"
  (^String [object-encoder v]
    (let [hint-str (fn ^String [^String hint v]
                     (let [^StringBuilder sb (StringBuilder. 15)]
                       (.append sb hint)
                       (.append sb v)
                       (.toString sb)))]
      (cond
        (string? v)  v  ; do not follow escape-safety due to performance
        (instance?
          clojure.lang.Named v) (name v)
        (integer? v) (hint-str "^long "    v)
        (float? v)   (hint-str "^double "  v)
        (instance?
          Boolean v) (hint-str "^boolean " v)
        :otherwise   (hint-str "^object "  (try (object-encoder v) (catch Exception e (str v)))))))
  (^String [v]
    (encode-val pr-str v)))


(defn decode-val
  "Decode MDC string value into the correct original type. May be used to redefine cambium.core/destringify-val.
  See: encode-val"
  ([object-decoder ^String s]
    (cond
      (nil? s)             s
      (= 0 (.length s))    s
      (= \^ (.charAt s 0)) (cond
                             (.startsWith s "^long ")    (try (Long/parseLong     (subs s 6)) (catch Exception e 0))
                             (.startsWith s "^double ")  (try (Double/parseDouble (subs s 8)) (catch Exception e 0.0))
                             (.startsWith s "^boolean ") (Boolean/parseBoolean    (subs s 9))
                             (.startsWith s "^object ")  (try (object-decoder (subs s 8)) (catch Exception e (str e)))
                             :otherwise                  s)
      :otherwise           s))
  ([^String s]
    (decode-val edn/read-string s)))
