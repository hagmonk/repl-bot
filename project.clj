(defproject repl-bot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "3.5.0"]
                 [stylefruits/gniazdo "1.0.0"]
                 [environ/environ.core "0.3.1"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [clojail "1.0.6"]]
  :main ^:skip-aot repl-bot.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
