(defproject rest-testing-one "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ^:replace []
  :dependencies
        [[org.clojure/clojure "1.5.1"]
         [commons-codec/commons-codec "1.4"]
         [org.clojure/core.async "0.1.267.0-0d7780-alpha"]]
  :main nioserver.core)
