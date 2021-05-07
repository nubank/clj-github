(ns clj-github.httpkit-client-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-github.httpkit-client :as sut]
            [matcher-combinators.clj-test]
            [org.httpkit.fake :refer [with-fake-http]]))

(deftest request-test
  (let [client (sut/new-client {:token-fn (fn [] "token")})]
    (testing "default method is get"
      (with-fake-http [{:method :get}
                       {:status 200}]
        (is (match? {:status 200} (sut/request client {})))))
    (testing "body is converted to clojure data if content type is json"
      (with-fake-http [{:body "{\"key\":\"value\"}"}
                       {:headers {:content-type "application/json; charset=utf-8"}
                        :body "{\"value\":\"result\"}"}]
        (is (match? {:body {:value "result"}} (sut/request client {:body {:key "value"}})))))
    (testing "body is not converted if content type is not json"
      (with-fake-http [{:body "{\"key\":\"value\"}"}
                       {:headers {:content-type "some-other-content-type"}
                        :body "{\"value\":\"result\"}"}]
        (is  (match? {:body "{\"value\":\"result\"}"} (sut/request client {:body {:key "value"}})))))
    (testing "path is appended to url"
      (with-fake-http [{:url "https://api.github.com/path"}
                       {:status 200}]
        (is (match? {:status 200} (sut/request client {:path "/path"})))))
    (testing "github token is added to authorization header"
      (with-fake-http [{:headers {"Authorization" "Bearer token"
                                  "Content-Type" "application/json"}}
                       {:status 200}]
        (is (match? {:status 200} (sut/request client {})))))
    (testing "it throws an exception when status code is not succesful"
      (with-fake-http [{} {:status 400}]
        (is (thrown? clojure.lang.ExceptionInfo #"Request to GitHub failed"
                     (sut/request client {})))))))
