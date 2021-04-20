(ns clj-github.httpkit-client-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-github.httpkit-client :as sut]
            [matcher-combinators.test :refer [match?]]
            [org.httpkit.fake :refer [with-fake-http]]))

(deftest request-test
  (let [client (sut/new-client {:token-fn (fn [] "token")})]
    (testing "default method is get"
      (with-fake-http [{:method :get}
                       "\"result\""]
        (is "result" (sut/request client {}))))
    (testing "body is converted to json"
      (with-fake-http [{:body "{\"key\":\"value\"}"}
                       "\"result\""]
        (is "result" (sut/request client {:body {:key "value"}}))))
    (testing "path is appended to url"
      (with-fake-http [{:url "https://api.github.com/path"}
                       "\"result\""]
        (is "result" (sut/request client {:path "/path"}))))
    (testing "github token is added to authorization header"
      (with-fake-http [{:headers {"Authorization" "Bearer token"
                                  "Content-Type" "application/json"}}
                       "\"result\""]
        (is "result" (sut/request client {}))))
    (testing "it converts response to clojure data"
      (with-fake-http [{} "{\"key\":\"value\"}"]
        (is {:key "value"} (sut/request client {}))))
    (testing "it throws an exception when status code is not succesful"
      (with-fake-http [{} {:status 400}]
        (is (thrown? clojure.lang.ExceptionInfo #"Request to GitHub failed"
                     (sut/request client {})))))))
