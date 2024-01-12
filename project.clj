(defproject cambium/cambium.core "1.1.1"
  :description "Core module for the Cambium logging API"
  :url "https://github.com/cambium-clojure/cambium.core"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.2.0"
  :pedantic? :warn
  :dependencies [[org.slf4j/slf4j-api       "1.7.32"]
                 [org.clojure/tools.logging "1.2.2" :exclusions [org.clojure/clojure]]]
  :global-vars {*assert* true
                *warn-on-reflection* true
                *unchecked-math* :warn-on-boxed}
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :codec-simple {:dependencies [[cambium/cambium.codec-simple "1.0.0"]]}
             :logback {:dependencies [[ch.qos.logback/logback-classic "1.2.13"]
                                      [ch.qos.logback/logback-core    "1.2.13"]]}
             :log4j12 {:dependencies [[org.slf4j/slf4j-log4j12 "1.7.30"]
                                      [log4j/log4j "1.2.17"]]}
             :log4j2  {:dependencies [[org.apache.logging.log4j/log4j-api  "2.16.0" :exclusions [org.slf4j/slf4j-api]]
                                      [org.apache.logging.log4j/log4j-core "2.16.0" :exclusions [org.slf4j/slf4j-api]]
                                      [org.apache.logging.log4j/log4j-slf4j-impl "2.13.3" :exclusions [org.slf4j/slf4j-api]]]}
             :c06 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :c07 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :c08 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :c09 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :c10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
             :dln {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :aliases {"test-all-logback" ["with-profile" "logback,c06:logback,c07:logback,c08:logback,c09:logback,c10" "test"]
            "test-all-log4j12" ["with-profile" "log4j12,c06:log4j12,c07:log4j12,c08:log4j12,c09:log4j12,c10" "test"]
            "test-all-log4j2"  ["with-profile" "log4j2,c06:log4j2,c07:log4j2,c08:log4j2,c09:log4j2,c10" "test"]})
