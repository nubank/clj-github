(defproject dev.nubank/clj-github "0.2.1"
  :description "A Clojure library for interacting with the github developer API"
  :url "https://github.com/nubank/clj-github"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :repositories [["publish" {:url "https://clojars.org/repo"
                             :username :env/clojars_username
                             :password :env/clojars_passwd
                             :sign-releases false}]]

  :plugins [[lein-cljfmt "0.6.4" :exclusions [org.clojure/clojure]]
            [lein-nsorg "0.3.0" :exclusions [org.clojure/clojure]]
            [lein-ancient "0.6.14" :exclusions [commons-logging com.fasterxml.jackson.core/jackson-databind com.fasterxml.jackson.core/jackson-core]]]

  :dependencies [[org.clojure/clojure "1.10.3"]

                 [cheshire "5.10.0"]
                 [clj-commons/clj-yaml "0.7.106"]
                 [http-kit "2.5.3"]
                 [nubank/clj-github-app "0.1.4"]
                 [nubank/state-flow "5.11.3"]]

  :cljfmt {:indents {flow       [[:block 1]]
                     assoc-some [[:block 0]]}}

  :profiles {:dev {:plugins [[lein-project-version "0.1.0"]]
                   :dependencies [[ch.qos.logback/logback-classic "1.3.0-alpha4" :exclusions [com.sun.mail/javax.mail]]
                                  [org.clojure/test.check "1.1.0"]
                                  [nubank/matcher-combinators "3.1.4" :exclusions [mvxcvi/puget commons-codec]]
                                  [tortue/spy "2.4.0"]
                                  [http-kit.fake "0.2.2"]
                                  [metosin/reitit-core "0.5.13"]
                                  [dev.nubank/clj-github-mock "0.2.0"]]}}

  :aliases {"coverage" ["cloverage" "-s" "coverage"]
            "lint"     ["do" ["cljfmt" "check"] ["nsorg"]]
            "lint-fix" ["do" ["cljfmt" "fix"] ["nsorg" "--replace"]]
            "loc"      ["vanity"]}

  :min-lein-version "2.4.2"
  :resource-paths ["resources"]
  :test-paths ["test/"])
