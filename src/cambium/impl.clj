;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns cambium.impl
  (:require
    [cambium.codec    :as codec]
    [cambium.internal :as i]
    [cambium.type     :as t])
  (:import
    [java.util ArrayList HashMap Map$Entry]
    [org.slf4j MDC]))


;; ----- MDC read/write -----


(extend-protocol t/IMutableContext
  HashMap  ; and LinkedHashMap, which extends HashMap
  (get-val [this k]   (.get this k))
  (put!    [this k v] (.put this k v))
  (remove! [this k]   (.remove this k)))


(def current-mdc-context
  (reify t/IMutableContext
    (get-val [_ k]   (MDC/get k))
    (put!    [_ k v] (MDC/put k v))
    (remove! [_ k]   (MDC/remove k))))


;; ----- flat impl -----


(defn flat-context-val
  "Return the value of the specified key from the current context; behavior for non-existent keys would be
  implementation dependent - it may return nil or may throw exception."
  ([k]
    (flat-context-val current-mdc-context codec/stringify-key codec/destringify-val k))
  ([repo stringify-key destringify-val k]
    (destringify-val (t/get-val repo (stringify-key k)))))


(defn merge-flat-context!
  "Merge given context map into the current MDC using the following constraints:
  * Nil keys are ignored
  * Nil values are considered as deletion-request for corresponding keys
  * Keys are converted to string
  * Keys in the current context continue to have old values unless they are overridden by the specified context map
  * Keys in the context map may not be nested (for nesting support consider 'cambium.nested/merge-nested-context!')"
  ([context]
    (merge-flat-context! current-mdc-context codec/stringify-key codec/stringify-val codec/destringify-val context))
  ([dest stringify-key stringify-val destringify-val context]
    (doseq [^Map$Entry entry (seq context)]
      (let [k (.getKey entry)
            v (.getValue entry)]
        (when-not (nil? k)
          (if (nil? v)  ; consider nil values as deletion request
            (t/remove! dest (stringify-key k))
            (t/put! dest (stringify-key k) (stringify-val v))))))))


;; ----- nested impl -----


(defn nested-context-val
  "Return the value of the specified key (or keypath in nested structure) from the current context; behavior for
  non-existent keys would be implementation dependent - it may return nil or may throw exception."
  ([k]
    (nested-context-val current-mdc-context codec/stringify-key codec/destringify-val k))
  ([repo stringify-key destringify-val k]
    (let [mdc-val #(destringify-val (t/get-val repo (stringify-key %)))]
      (if (coll? k)
        (get-in (mdc-val (first k)) (map stringify-key (next k)))
        (mdc-val k)))))


(defn merge-nested-context!
  "Merge given 'potentially-nested' context map into the current MDC using the following constraints:
  * Entries with nil key are ignored
  * Nil values are considered as deletion-request for corresponding keys
  * Collection keys are treated as key-path (all tokens in a key path are turned into string)
  * Keys are converted to string"
  ([context]
    (merge-nested-context! current-mdc-context codec/stringify-key codec/stringify-val codec/destringify-val context))
  ([dest stringify-key stringify-val destringify-val context]
    (let [^HashMap delta (HashMap. (count context))
          deleted-keys   (ArrayList.)
          remove-key     (fn [^String str-k] (.remove delta str-k) (.add deleted-keys str-k))]
      ;; build up a delta with top-level stringified keys and original vals
      (doseq [^Map$Entry entry (seq context)]
        (let [k (.getKey entry)
              v (.getValue entry)]
          (when-not (nil? k)
            (if (coll? k)
              (when (and (seq k) (every? #(not (nil? %)) k))
                (let [k-path (map stringify-key k)
                      k-head (first k-path)
                      k-next (next k-path)]
                  (if (and (nil? v) (not k-next))  ; consider nil values as deletion request
                    (remove-key k-head)
                    (.put delta k-head (let [value-map (or (get delta k-head)
                                                         (when-let [oldval (t/get-val dest k-head)]
                                                           (let [oldmap (destringify-val oldval)]
                                                             (if (map? oldmap) oldmap {}))))]
                                         (if (nil? v)  ; consider nil values as deletion request
                                           (if (next k-next)
                                             (update-in value-map (butlast k-next) dissoc (last k-next))
                                             (dissoc value-map (first k-next)))
                                           (assoc-in value-map k-next (i/stringify-nested-keys stringify-key v))))))))
              (if (nil? v)  ; consider nil values as deletion request
                (remove-key (stringify-key k))
                (.put delta (stringify-key k) (i/stringify-nested-keys stringify-key v)))))))
      ;; set the pairs from delta into the MDC
      (doseq [^Map$Entry pair (.entrySet delta)]
        (let [str-k (.getKey pair)
              v     (.getValue pair)]
          (t/put! dest str-k (stringify-val v))))
      ;; remove keys identified for deletion
      (doseq [^String str-k deleted-keys]
        (t/remove! dest str-k)))))
