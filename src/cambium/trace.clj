;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns cambium.trace
  "Tracing utility functions."
  (:require
    [clojure.string :as string]
    [cambium.core :as log])
  (:import
    [java.util UUID]))


(def ^:const default-trace-id-key  :log-trace-id)
(def ^:const default-parent-id-key :log-parent-id)
(def ^:const default-span-id-key   :log-span-id)


(defrecord TraceInfo [trace-id parent-id span-id])


;; ----- common utility functions -----


(defn current-trace-info
  "Return the current trace info as a TraceInfo record. Options:
  | Key          | Description                                      |
  |--------------|--------------------------------------------------|
  |:trace-id-key |Key to find trace ID at from logging context      |
  |:parent-id-key|Key to find parent span ID at from logging context|
  |:span-id-key  |Key to find span ID at from logging context       |"
  ([]
    (current-trace-info {}))
  ([{:keys [trace-id-key
            parent-id-key
            span-id-key]
     :or {trace-id-key  default-trace-id-key
          parent-id-key default-parent-id-key
          span-id-key   default-span-id-key}}]
    (->TraceInfo
      (log/context-val trace-id-key)
      (log/context-val parent-id-key)
      (log/context-val span-id-key))))


(defn make-trace-extractor
  "Given trace-ID and span-ID key paths in argument map, return a fn `(fn [m]) -> TraceInfo` that extracts trace info
  from map argument."
  [trace-id-keypath span-id-keypath]
  (fn [m]
    (let [uuid-str  (str (UUID/randomUUID))
          trace-id  (get-in m trace-id-keypath uuid-str)
          ;; parent ID is nil/absent unless supplied
          parent-id (get-in m span-id-keypath)]
      (->TraceInfo trace-id parent-id uuid-str))))


(defn make-trace-producer
  "Given trace-ID and span-ID key paths in argument map, return a fn `(fn [m] [m TraceInfo]) -> m` that populates the
  map argument with trace info. Options:
  | Key         | Description                                |
  |-------------|--------------------------------------------|
  |:trace-id-key|Key to find trace ID at from logging context|
  |:span-id-key |Key to find span ID at from logging context |"
  ([trace-id-keypath span-id-keypath]
    (make-trace-producer trace-id-keypath span-id-keypath {}))
  ([trace-id-keypath span-id-keypath {:keys [trace-id-key
                                             span-id-key]
                                      :or {trace-id-key default-trace-id-key
                                           span-id-key  default-span-id-key}
                                      :as options}]
    (fn trace-producer
      ([m]
        (trace-producer m (current-trace-info options)))
      ([m ^TraceInfo trace-info]
        (let [trace-id (.-trace-id trace-info)
              span-id  (.-span-id trace-info)]
          (cond-> m
            trace-id (assoc-in trace-id-keypath trace-id)
            span-id  (assoc-in span-id-keypath span-id)))))))


(def default-msg-trace-extractor  (make-trace-extractor [:trace-id] [:span-id]))
(def default-ring-trace-extractor (make-trace-extractor [:headers "trace-id"] [:headers "span-id"]))
(def default-ring-trace-producer  (make-trace-producer  [:headers "Trace-ID"] [:headers "Span-ID"]))
(def identity-trace-producer      (fn ([m] m) ([m _] m)))


;; ----- middleware functions -----


(defn map-input-middleware
  "Instrument a function that accepts a message (map) as first argument such
  that the trace information is extracted and the function is invoked in the
  context of the trace information.
  Options:
  | Key             | Description                                               |
  |-----------------|-----------------------------------------------------------|
  |:trace-id-key    |Key to log trace ID under                                  |
  |:parent-id-key   |Key to log parent span ID under                            |
  |:span-id-key     |Key to log span ID under                                   |
  |:trace-extractor |Fn `(fn [m])->TraceInfo` to extract trace info from message|"
  ([f]
   (map-input-middleware f {}))
  ([f {:keys [trace-id-key
              parent-id-key
              span-id-key
              trace-extractor]
       :or {trace-id-key     default-trace-id-key
            parent-id-key    default-parent-id-key
            span-id-key      default-span-id-key
            trace-extractor  default-msg-trace-extractor}}]
   (fn [m & args]
     (let [^TraceInfo trace-info (trace-extractor m)]
       (log/with-logging-context {trace-id-key  (.-trace-id  trace-info)
                                  parent-id-key (.-parent-id trace-info)
                                  span-id-key   (.-span-id   trace-info)}
         (apply f m args))))))


(defn ring-trace-middleware
  "Extract or augment the Trace-ID/Span-ID from Ring request and propagate it
  to MDC. Log request and response attributes. Options:
  |Key               |Description                                                   |
  |------------------|--------------------------------------------------------------|
  |:trace-id-key     |Key to log trace ID under                                     |
  |:parent-id-key    |Key to log parent span ID under                               |
  |:span-id-key      |Key to log span ID under                                      |
  |:trace-extractor  |Fn `(fn [m])->TraceInfo` to extract trace info from request   |
  |:trace-producer   |Fn `(fn [m] [m TraceInfo]) -> m` to add trace info to response|
  |:request-log-keys |Default [:request-method :uri], nil disables request log      |
  |:response-log-keys|Default [:status], nil disables response log                  |"
  ([handler]
   (ring-trace-middleware handler {}))
  ([handler {:keys [trace-id-key
                    parent-id-key
                    span-id-key
                    trace-extractor
                    trace-producer
                    request-log-keys
                    response-log-keys]
             :or {trace-id-key      default-trace-id-key
                  parent-id-key     default-parent-id-key
                  span-id-key       default-span-id-key
                  trace-extractor   default-ring-trace-extractor
                  trace-producer    default-ring-trace-producer
                  request-log-keys  [:request-method :uri :headers]
                  response-log-keys [:status :headers]}}]
   (let [uuid-str      (fn [] (str (UUID/randomUUID)))
         log-request?  (boolean request-log-keys)
         log-response? (boolean response-log-keys)]
     (fn ring-trace
       ([request]
         (let [^TraceInfo trace-info (trace-extractor request)]
           (log/with-logging-context {trace-id-key  (.-trace-id  trace-info)
                                      parent-id-key (.-parent-id trace-info)
                                      span-id-key   (.-span-id   trace-info)}
             (when log-request?
               (log/info (select-keys request request-log-keys)
                 "ring.request.received"))
             (let [start-ns (System/nanoTime)
                   find-dur (fn [] (-> (System/nanoTime)
                                     (unchecked-subtract start-ns)
                                     (/ 1e6)  ; nanos to millis
                                     double))
                   [response
                    thrown
                    dur-ms] (try
                              (let [res (handler request)] [res nil (find-dur)])
                              (catch Throwable ex          [nil ex  (find-dur)]))]
               (when log-response?
                 (if thrown
                   (log/info {:duration-ms dur-ms} thrown "ring.request.failed")
                   (log/info (if (map? response)
                               (-> response
                                 (select-keys response-log-keys)
                                 (assoc :duration-ms dur-ms))
                               {:duration-ms dur-ms})
                     "ring.response.sent")))
               (if thrown
                 (throw thrown)
                 (if (map? response)
                   (trace-producer response trace-info)
                   response))))))
       ([request respond raise]
         (respond (ring-trace request)))))))
