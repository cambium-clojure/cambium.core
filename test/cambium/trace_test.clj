;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns cambium.trace-test
  (:require
    [clojure.test :refer :all]
    [cambium.core :as log]
    [cambium.trace :as t]))


(deftest test-make-trace-extractor
  (testing "custom trace extractor"
    (let [ti-keypath [:foo :trace-id]
          si-keypath [:bar :span-id]
          extractorf (t/make-trace-extractor ti-keypath si-keypath)
          orig-data  {:foo {:trace-id "sample-trace-id"}
                      :bar {:span-id "sample-span-id"}}
          trace-info (extractorf orig-data)]
      (is (string? (:span-id trace-info)))
      (is (not= (:span-id trace-info) (get-in orig-data si-keypath)))
      (is (= {:trace-id "sample-trace-id"
              :parent-id "sample-span-id"}
            (dissoc trace-info :span-id)))))
  (testing "msg trace extractor"
    (let [orig-data  {:trace-id "sample-trace-id"
                      :span-id  "sample-span-id"
                      :foo 10
                      :bar 20}
          trace-info (t/default-msg-trace-extractor orig-data)]
      (is (string? (:span-id trace-info)))
      (is (not= (:span-id trace-info) "sample-span-id"))
      (is (= {:trace-id "sample-trace-id"
              :parent-id "sample-span-id"}
            (dissoc trace-info :span-id)))))
  (testing "ring trace extractor"
    (let [ring-req   {:request-method :get
                      :headers {"trace-id" "sample-trace-id"
                                "span-id"  "sample-span-id"}
                      :uri "/"}
          trace-info (t/default-ring-trace-extractor ring-req)]
      (is (string? (:span-id trace-info)))
      (is (not= (:span-id trace-info) "sample-span-id"))
      (is (= {:trace-id "sample-trace-id"
              :parent-id "sample-span-id"}
            (dissoc trace-info :span-id))))))


(deftest test-make-trace-producer
  (testing "custom trace producer"
    (let [ti-keypath [:foo :trace-id]
          si-keypath [:bar :span-id]
          producerfn (t/make-trace-producer ti-keypath si-keypath)
          orig-data  {:baz 10}
          processed  (log/with-logging-context {t/default-trace-id-key "sample-trace-id"
                                                t/default-span-id-key  "sample-span-id"}
                       (producerfn orig-data))]
      (is (= {:baz 10
              :foo {:trace-id "sample-trace-id"}
              :bar {:span-id "sample-span-id"}}
            processed))))
  (testing "ring trace producer"
    (let [response {:status 200
                    :headers {"Content-type" "application/json"}
                    :body "{\"foo\": 10}"}
          post-res (log/with-logging-context {t/default-trace-id-key "sample-trace-id"
                                              t/default-span-id-key  "sample-span-id"}
                     (t/default-ring-trace-producer response))]
      (is (= {:status 200
              :headers {"Content-type" "application/json"
                        "Trace-ID" "sample-trace-id"
                        "Span-ID"  "sample-span-id"}
              :body "{\"foo\": 10}"}
            post-res))))
  (testing "no-op trace producer"
    (let [response {:status 200
                    :headers {"Content-type" "application/json"}
                    :body "{\"foo\": 10}"}
          post-res (log/with-logging-context {t/default-trace-id-key "sample-trace-id"
                                              t/default-span-id-key  "sample-span-id"}
                     (t/identity-trace-producer response))]
      (is (= response post-res)))))


(deftest test-map-input-middleware
  (let [orig-fn (fn [m]
                  (is (= "sample-trace-id"
                        (log/context-val t/default-trace-id-key)))
                  (is (= "sample-span-id"
                        (log/context-val t/default-parent-id-key)))
                  (is (string? (log/context-val t/default-span-id-key)))
                  (is (not= "sample-span-id"
                        (log/context-val t/default-span-id-key)))
                  m)
        wrapped (t/map-input-middleware orig-fn)
        input-m {:trace-id "sample-trace-id"
                 :span-id  "sample-span-id"}]
    (is (= input-m (wrapped input-m)))))


(deftest test-ring-trace-middleware
  (let [request  {:request-method :get
                  :headers {"trace-id" "sample-trace-id"
                            "span-id"  "sample-span-id"}
                  :uri "/"}
        span-id  (atom nil)
        handler  (fn [request]
                   (is (= "sample-trace-id" (log/context-val t/default-trace-id-key)))
                   (is (= "sample-span-id"  (log/context-val t/default-parent-id-key)))
                   (reset! span-id (log/context-val t/default-span-id-key))
                   (is (string? (log/context-val t/default-span-id-key)))
                   (is (not= "sample-span-id" (log/context-val t/default-span-id-key) ))
                   {:status 200
                    :body "hello"})
        wrapped  (t/ring-trace-middleware handler)
        response (wrapped request)]
    (is (= "sample-trace-id" (get-in response [:headers "Trace-ID"])))
    (is (= @span-id (get-in response [:headers "Span-ID"])))))
