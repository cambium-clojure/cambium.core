;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns cambium.mdc-test
  (:require
    [clojure.test :refer :all]
    [cambium.core        :as c]
    [cambium.mdc         :as m]
    [cambium.test-util   :as tu]))


(deftest test-mdc
  (let [context-old {"foo" "bar"
                     "baz" "quux"}
        context-new {"foo" "10"
                     "bar" "baz"}
        f (fn
            ([]
              (is (= context-old (m/get-raw-mdc)))  ; raw MDC is same as context due to string K/V pairs
              (is (= "bar"  (c/context-val "foo")))
              (is (= "quux" (c/context-val "baz")))
              (is (nil? (c/context-val "bar"))))
            ([dummy arg]))]
    (testing "get-raw-mdc"
      (is (empty? (m/get-raw-mdc))))
    (testing "with-raw-mdc"
      (is (nil? (c/context-val "foo")) "Attribute not set must be absent before override")
      (m/with-raw-mdc nil
        (do :nothing))
      (m/with-raw-mdc context-old
        (f)
        (m/with-raw-mdc context-new
          (is (= "10" (c/context-val "foo")))
          (is (nil? (c/context-val "baz")) "Wholesale MDC replacement must remove non-overridden attributes")
          (is (= "baz" (c/context-val "bar")))))
      (is (nil? (c/context-val "foo")) "Attribute not set must be absent after restoration"))
    (testing "wrap-raw-mdc"
      (is (nil? (c/context-val "foo")))
      ((m/wrap-raw-mdc context-old f))
      ((m/wrap-raw-mdc context-old f) :dummy :arg)
      ((comp (partial m/wrap-raw-mdc context-new) (m/wrap-raw-mdc context-old f)))
      (is (nil? (c/context-val "foo"))))))
