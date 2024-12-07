(defproject dev.nubank/clj-github "0.7.1"
  :description "A Clojure library for interacting with the github developer API"
  :url "https://github.com/nubank/clj-github"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :repositories [["publish" {:url "https://clojars.org/repo"
                             :username :env/clojars_username
                             :password :env/clojars_passwd
                             :sign-releases false}]]

  :plugins [[lein-cljfmt "0.9.2"]
            [lein-nsorg "0.3.0"]
            [lein-ancient "0.7.0"]]

  :dependencies [[org.clojure/clojure "1.12.0"]
                 [cheshire "5.13.0"]
                 [http-kit "2.8.0"]
                 [nubank/clj-github-app "0.3.0"]
                 [clj-commons/fs "1.6.311"]
                 [ring/ring-codec "1.2.0"]
                 ; Optional dependency used by clj-github.token/hub-config
                 [clj-commons/clj-yaml "1.0.29" :scope "provided"]
                 ; Dependencies required by clj-github.test-helpers and clj-github.state-flow-helper.
                 ; Must be provided by the user (typically only used in tests)
                 [http-kit.fake "0.2.2" :scope "provided"]
                 [nubank/state-flow "5.18.0" :scope "provided"]
                 [dev.nubank/clj-github-mock "0.4.0" :scope "provided"]]

  :cljfmt {:indents {flow       [[:block 1]]
                     assoc-some [[:block 0]]}}

  :profiles {:dev {:plugins [[lein-project-version "0.1.0"]]
                   :dependencies [[ch.qos.logback/logback-classic "1.5.12"]
                                  [nubank/matcher-combinators "3.9.1"]]}}

  :aliases {"coverage" ["cloverage" "-s" "coverage"]
            "lint"     ["do" ["cljfmt" "check"] ["nsorg"]]
            "lint-fix" ["do" ["cljfmt" "fix"] ["nsorg" "--replace"]]
            "loc"      ["vanity"]}

  :min-lein-version "2.4.2"
  :resource-paths ["resources"]
  :test-paths ["test/"])
