;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns cambium.codec-util-test
  (:require [cambium.codec.util :as ccu]
            [clojure.test :refer [deftest is testing]]))


(deftest dissoc-in-test
  (testing "top-level"
    (is (= {}
          (ccu/dissoc-in {:foo 10} [:foo])) "present key")
    (is (= {}
          (ccu/dissoc-in {:foo 10
                          :bar 20} [:foo] [:bar])) "multiple present keys")
    (is (= {:foo 10}
          (ccu/dissoc-in {:foo 10} [:bar])) "missing key"))
  (testing "nested level"
    (is (= {:foo {}}
          (ccu/dissoc-in {:foo {:bar 10}} [:foo :bar])) "present key")
    (is (= {:foo {:bar 10}}
          (ccu/dissoc-in {:foo {:bar 10}} [:baz :foo])) "missing path")
    (is (= {:foo {:bar 10}}
          (ccu/dissoc-in {:foo {:bar 10}} [:foo :baz])) "missing key")))
