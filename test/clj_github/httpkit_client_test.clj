(ns clj-github.httpkit-client-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [clj-github.httpkit-client :as sut]
    [matcher-combinators.clj-test]
    [org.httpkit.fake :refer [with-fake-http]]
    [ring.util.codec :as encode]))

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
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"(?i)Request to GitHub failed"
                              (sut/request client {})))))
    ;; in case of lower level errors, e.g. DNS lookup failue there will not be a status code
    (testing "it throws an exception when there is no status code"
      (with-fake-http [{} {:status nil}]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"(?i)Request to GitHub failed"
                              (sut/request client {})))))
    (testing "it throws an exception with the root cause"
      (let [cause (Exception. "Exception for testing purpose")]
        (with-fake-http [{} {:error cause :status nil}]
          (let [e (try (sut/request client {}) (catch Exception e e))]
            (is (re-matches #"(?i)Request to GitHub failed" (.getMessage e)))
            (is (= cause (.getCause e)))))))
    (testing "url path contains special character `|`"
      (with-fake-http [{:url "https://api.github.com/test%7Ctest"}
                       {:status 200}]
                      (is (match? {:status 200} (sut/request client {:path (str "/" (encode/form-encode "test|test"))})))))))
