;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns cambium.mdc
  (:import
    [java.util Map]
    [org.slf4j MDC]))


(defn get-raw-mdc
  "Return a raw copy of the current MDC. Empty MDC may be nil or empty map, subject to the underlying implementation."
  ^java.util.Map []
  (MDC/getCopyOfContextMap))


(defmacro preserving-mdc
  "Execute body of code such that the MDC found at the start of execution is restored at the end of execution."
  [& body]
  `(let [mdc# (get-raw-mdc)]
     (try
       ~@body
       (finally
         (if mdc#
           (MDC/setContextMap mdc#)
           (MDC/clear))))))


(defmacro with-raw-mdc
  "Given raw MDC map with string key/value pairs (which need no transformation), execute the body of code in the
  specified logging context. Faster than cambium.core/with-logging-context but replaces entire context at once instead
  of individual key/value pairs.
  See also: http://logback.qos.ch/manual/mdc.html"
  [mdc & body]
  `(preserving-mdc
     (if-let [mdc# ~mdc]
       (MDC/setContextMap mdc#)
       (MDC/clear))
     ~@body))


(defn wrap-raw-mdc
  "Wrap function f such that it is executed with the specified MDC. When no MDC is specified, the MDC at the time of
  wrapping is used. The MDC is considered raw, i.e. is not converted to string key/value pairs. Faster than
  cambium.core/wrap-logging-context but replaces entire context at once instead of individual key/value pairs.
  See also: http://logback.qos.ch/manual/mdc.html"
  ([f]
    (wrap-raw-mdc (get-raw-mdc) f))
  ([mdc f]
    (fn
      ([]
        (with-raw-mdc mdc
          (f)))
      ([x]
        (with-raw-mdc mdc
          (f x)))
      ([x y]
        (with-raw-mdc mdc
          (f x y)))
      ([x y & args]
        (with-raw-mdc mdc
          (apply f x y args))))))
