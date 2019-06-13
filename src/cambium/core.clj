;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns cambium.core
  (:require
    [clojure.tools.logging      :as ctl]
    [clojure.tools.logging.impl :as ctl-impl]
    [cambium.codec              :as codec]
    [cambium.internal           :as i]
    [cambium.impl               :as impl]
    [cambium.mdc                :as m]))


;; ----- MDC handling -----


(defn get-context
  "Return a copy of the current context containing string keys and original values."
  ^java.util.Map []
  (let [cm (m/get-raw-mdc)
        ks (keys cm)]
    (zipmap ks (map #(codec/destringify-val (get cm %)) ks))))


(defn context-val
  "Return the value of the specified key from the current context; behavior for non-existent keys would be
  implementation dependent - it may return nil or may throw exception. Nested keys are handled subject to
  `cambium.codec/nested-nav?`."
  ([k]
    (if codec/nested-nav?
      (impl/nested-context-val k)
      (impl/flat-context-val k)))
  ([repo stringify-key destringify-val k]
    (if codec/nested-nav?
      (impl/nested-context-val repo stringify-key destringify-val k)
      (impl/flat-context-val repo stringify-key destringify-val k))))


(defn merge-logging-context!
  "Merge given context map into the current MDC using the following constraints:
  * Nil keys are ignored
  * Nil values are considered as deletion-request for corresponding keys
  * Keys are converted to string
  * Keys in the current context continue to have old values unless they are overridden by the specified context map
  * Nested keys are handled subject to `cambium.codec/nested-nav?`."
  ([context]
    (if codec/nested-nav?
      (impl/merge-nested-context! context)
      (impl/merge-flat-context! context)))
  ([dest stringify-key stringify-val destringify-val context]
    (if codec/nested-nav?
      (impl/merge-nested-context! dest stringify-key stringify-val destringify-val context)
      (impl/merge-flat-context! dest stringify-key stringify-val destringify-val context))))


(defmacro with-logging-context
  "Given context map data, merge it into the current MDC and evaluate the body of code in that context. Restore
  original context in the end.
  See: cambium.core/merge-logging-context!, cambium.mdc/with-raw-mdc
       http://logback.qos.ch/manual/mdc.html"
  [context & body]
  `(m/preserving-mdc
     (merge-logging-context! ~context)
     ~@body))


(defn wrap-logging-context
  "Wrap function f with the specified logging context.
  See: cambium.core/with-logging-context, cambium.mdc/wrap-raw-mdc
       http://logback.qos.ch/manual/mdc.html"
  [context f]
  (fn
    ([]
      (with-logging-context context
        (f)))
    ([x]
      (with-logging-context context
        (f x)))
    ([x y]
      (with-logging-context context
        (f x y)))
    ([x y & args]
      (with-logging-context context
        (apply f x y args)))))


;; ----- logging calls -----


(def ^:redef caller-meta-in-context?
  "Boolean (default true) - whether include caller form metadata in the logging context. May be disabled by setting
  system property 'cambium.caller.meta.in.context' or by applying 'alter-var-root' (overrides the system property)
  before loading caller namespaces."
  (if-let [^String cmic (System/getProperty "cambium.caller.meta.in.context")]
    (if (= "false" (.toLowerCase cmic))
      false
      true)
    true))


(defmacro log
  "Log an event or message under specified logger and log-level."
  ([level msg-or-throwable]
   (with-meta `(log (ctl-impl/get-logger ctl/*logger-factory* ~*ns*) ~level ~msg-or-throwable) (meta &form)))
  ([level mdc throwable msg]
    (with-meta `(log (ctl-impl/get-logger ctl/*logger-factory* ~*ns*) ~level ~mdc ~throwable ~msg) (meta &form)))
  ([logger level msg-or-throwable]
   (let [log-expr `(let [mot# ~msg-or-throwable]
                     (if (instance? Throwable mot#)
                       (ctl-impl/write! ~logger ~level mot# (.getMessage ^Throwable mot#))
                       (ctl-impl/write! ~logger ~level nil mot#)))]
     (with-meta `(when (ctl-impl/enabled? ~logger ~level)
                   ~(if caller-meta-in-context?
                      `(with-logging-context ~(assoc (meta &form) :ns (name (ns-name *ns*)))
                         ~log-expr)
                      `~log-expr))
       (meta &form))))
  ([logger level mdc-or-throwable throwable msg]
    (i/expected i/mdc-literal? "context (MDC) map" mdc-or-throwable)
    (i/expected i/throwable-literal? "exception object" throwable)
    (let [m-sym (gensym)
          t-sym (gensym)]
      (with-meta `(when (ctl-impl/enabled? ~logger ~level)
                    (let [~m-sym ~mdc-or-throwable
                          ~t-sym ~throwable]
                      (if (and (nil? ~t-sym) (instance? Throwable ~m-sym))
                        (with-logging-context ~(assoc (meta &form) :ns (name (ns-name *ns*)))
                          (ctl-impl/write! ~logger ~level ~m-sym ~msg))
                        (with-logging-context ~(if caller-meta-in-context?
                                                 `(conj ~(assoc (meta &form) :ns (name (ns-name *ns*))) ~m-sym)
                                                 `~m-sym)
                          (ctl-impl/write! ~logger ~level ~t-sym ~msg)))))
        (meta &form)))))


(def level-keys
  #{:trace :debug :info :warn :error :fatal})


(def level-syms
  "Symbols levels"
  '#{trace debug info warn error fatal})


(defmacro ^:private deflevel
  "This macro is used internally to only define normal namespace-based level loggers."
  [level-sym]
  (i/expected level-syms (str "a symbol for level name " level-syms) level-sym)
  (let [level-key (keyword level-sym)
        level-doc (str "Similar to clojure.tools.logging/" level-sym ".")
        arglists  ''([msg-or-throwable] [mdc-or-throwable msg] [mdc throwable msg])]
    `(defmacro ~level-sym
       ~level-doc
       {:arglists ~arglists}
       ([msg-or-throwable#]      (with-meta `(log ~~level-key ~msg-or-throwable#)           ~'(meta &form)))
       ([mdc-or-throwable# msg#] (with-meta `(log ~~level-key ~mdc-or-throwable# nil ~msg#) ~'(meta &form)))
       ([mdc# throwable# msg#]   (with-meta `(log ~~level-key ~mdc# ~throwable# ~msg#)      ~'(meta &form))))))


(deflevel trace)
(deflevel debug)
(deflevel info)
(deflevel warn)
(deflevel error)
(deflevel fatal)


;; ----- making custom logger -----


(defmacro deflogger
  "Define a custom logger with spcified logger name. You may optionally specify normal (:info by default) and error
  (:error by default) log levels."
  ([logger-sym logger-name]
    `(deflogger ~logger-sym ~logger-name :info :error))
  ([logger-sym logger-name log-level error-log-level]
    (i/expected symbol? "a symbol for logger var name" logger-sym)
    (i/expected string? "a string logger name" logger-name)
    (i/expected level-keys (str "log-level keyword " level-keys) log-level)
    (i/expected level-keys (str "error-log-level keyword " level-keys) error-log-level)
    (let [docstring (str logger-name " logger.")
          arglists  ''([msg-or-throwable] [mdc-or-throwable msg] [mdc throwable msg])]
      `(defmacro ~logger-sym
         ~docstring
         {:arglists ~arglists}
         ([msg-or-throwable#]      (with-meta `(log (ctl-impl/get-logger ctl/*logger-factory* ~~logger-name)
                                                 ~~log-level ~msg-or-throwable#)            ~'(meta &form)))
         ([mdc-or-throwable# msg#] (with-meta `(log (ctl-impl/get-logger ctl/*logger-factory* ~~logger-name)
                                                 ~~log-level ~mdc-or-throwable# nil ~msg#)  ~'(meta &form)))
         ([mdc# throwable# msg#]   (with-meta `(log (ctl-impl/get-logger ctl/*logger-factory* ~~logger-name)
                                                 ~~error-log-level ~mdc# ~throwable# ~msg#) ~'(meta &form)))))))
