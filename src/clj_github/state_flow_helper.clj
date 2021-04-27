(ns clj-github.state-flow-helper
  (:require [clj-github-mock.core :as mock.core]
            [clj-github.test-helpers :as test-helpers]
            [org.httpkit.fake :as fake]
            [state-flow.api :as flow :refer [flow]]
            [state-flow.labs.state :as state]))

(def ^:private get-github-client (comp :github-client :system))

(defmacro mock-github-flow
  "Runs a sequence of flows mocking calls to github api. Behavior of the mock
  can be setup via the following attributes:

  - `:initial-state`: a map containing information about organizations
  and repositories that will form the initial state of the emulator.

  Example:
  ```
  {:orgs [{:name \"nubank\" :repos [{:name \"some-repo\" :default_branch \"master\"}]}]}
  ```

  `default_branch` is optional and will default to \"main\".

  - `:responses`: allow mocking specific calls that are not supported out of the box.
  Responses are a vector of request and response pairs.

  Example:

  ```
  [\"/repos/nubank/some-repo/pulls/1\" {:status 200 :body {:id 1}}]
  ```

  This is backed up by `http-kit-fake`, for more details on how to declare requests and responses
  see https://github.com/d11wtq/http-kit-fake."
  [seed-data-expr & flows]
  `(state/wrap-with
    (fn [f#]
      (fake/with-fake-http
        (into
          []
          (let [seed-data#       ~seed-data-expr
                faked-responses# (test-helpers/build-spec (or (:responses seed-data#) []))
                mocked-handler#  (mock.core/httpkit-fake-handler
                                  {:initial-state (:initial-state seed-data#)})]
            (concat faked-responses#
                    [#"^https://api.github.com/.*" mocked-handler#])))
        (f#)))
    (flow/flow "mock-github"
               ~@flows)))

(defn- with-resource
  "Gets a value from the state using resource-fetcher and passes it as an
   argument to <user-fn>."
  [resource-fetcher user-fn]
  (assert (ifn? resource-fetcher) "First argument must be callable")
  (assert (ifn? user-fn) "Second argument must be callable")
  (flow "run function with resource retreived state"
    [resource (flow/get-state resource-fetcher)]
    (flow/return (user-fn resource))))

(defn with-github-client
  "State monad that returns the result of executing github-client-fn with github-client as argument"
  [github-client-fn]
  (with-resource get-github-client github-client-fn))
