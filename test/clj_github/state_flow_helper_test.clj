(ns clj-github.state-flow-helper-test
  (:require [cheshire.core :as json]
            [clj-github.httpkit-client :as httpkit-client]
            [clj-github.state-flow-helper :as sfh]
            [clojure.test :refer [deftest is]]
            [matcher-combinators.test]
            [state-flow.core :as state-flow]))

(defn- init-system []
  {:system {:github-client (httpkit-client/new-client {:token-fn (constantly "token")})}})

(defn- run-flow [a-flow]
  (state-flow/run* {:init init-system} a-flow))

(deftest mock-github-flow-honors-responses
  (let [response-body (json/generate-string {:id 1 :mergeable_state "clean"})
        [result _] (run-flow
                    (sfh/mock-github-flow
                     {:initial-state {:orgs [{:name  "nubank"
                                              :repos [{:name           "dummy-lib"
                                                       :default_branch "main"}]}]}
                      :responses     [{:path "/repos/nubank/dummy-lib/pulls/1" :method :get}
                                      {:status 200
                                       :body   response-body}]}
                     (sfh/with-github-client
                       (fn [client]
                         (httpkit-client/request client {:path "/repos/nubank/dummy-lib/pulls/1"})))))]
    (is (match? {:status 200
                 :body   {:id 1 :mergeable_state "clean"}}
                result))))
