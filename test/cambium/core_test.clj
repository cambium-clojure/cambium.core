;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns cambium.core-test
  (:require
    [clojure.test :refer :all]
    [cambium.codec :as codec]
    [cambium.core :as c]
    [cambium.test-util :as tu]))


(deftest log-test
  (testing "Normal scenarios"
    (c/info "hello")
    (c/info {:foo "bar" :baz 10 :qux true} "hello with context")
    (c/with-logging-context {:extra "context" "data" [1 2 :three 'four]}
      (is (= (c/get-context) {"extra" "context" "data" "[1 2 :three four]"}))
      (is (= (c/context-val :extra) "context"))
      (is (nil? (c/context-val "foo")))
      (c/info {:foo "bar"} "hello with wrapped context"))
    (c/error {} (ex-info "some error" {:data :foo}) "internal error"))
  (testing "custom loggers"
    (tu/metrics {:latency-ns 430 :module "registration"} "op.latency")
    (tu/metrics {:module "registration"} (ex-info "some error" {:data :foo}) "internal error")
    (tu/txn-metrics {:module "order-fetch"} "Fetched order #4568")))


(deftest test-context-propagation
  (let [context-old {:foo :bar
                     :baz :quux}
        context-new {:foo 10
                     :bar :baz}
        f (fn
            ([]
              (is (= "bar"  (c/context-val :foo)))
              (is (= "quux" (c/context-val :baz)))
              (is (nil? (c/context-val :bar))))
            ([dummy arg]))]
    (testing "with-raw-mdc"
      (is (nil? (c/context-val :foo)) "Attribute not set must be absent before override")
      (c/with-logging-context context-old
        (f)
        (c/with-logging-context context-new
          (is (= "10" (c/context-val :foo)))
          (is (= "quux" (c/context-val :baz)) "Delta context override must not remove non-overridden attributes")
          (is (= "baz" (c/context-val :bar)))))
      (c/with-logging-context context-old
        (f)
        (c/with-logging-context context-new
          (is (= "10" (c/context-val :foo)))
          (is (= "quux" (c/context-val :baz)) "Delta context override must not remove non-overridden attributes")
          (is (= "baz" (c/context-val :bar)))))
      (is (nil? (c/context-val :foo)) "Attribute not set must be absent after restoration"))
    (testing "deletion via nil values"
      (c/with-logging-context context-old
        (c/with-logging-context {:foo nil}
          (is (not (contains? (c/get-context) (codec/stringify-key :foo)))))))
    (testing "wrap-raw-mdc"
      (is (nil? (c/context-val :foo)))
      ((c/wrap-logging-context context-old f))
      ((c/wrap-logging-context context-old f) :dummy :arg)
      ((comp (partial c/wrap-logging-context context-new) (c/wrap-logging-context context-old f)))
      (is (nil? (c/context-val :foo))))))
