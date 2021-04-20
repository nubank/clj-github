(ns clj-github.test-helpers-test
  (:require [clojure.test :refer :all]
            [clj-github.httpkit-client :as httpkit-client]
            [clj-github.test-helpers :as sut]
            [matcher-combinators.test]))

(deftest with-fake-github-test
  (let [client (httpkit-client/new-client {:token-fn (fn [] "token")})]
    (testing "it appends github url when request is a string"
      (is (match? {:body {}}
                  (sut/with-fake-github ["/api/repos" "{}"]
                    (httpkit-client/request client {:path "/api/repos"})))))
    (testing "it supports a path attribute in a request map"
      (is (match? {:body {}}
                  (sut/with-fake-github [{:path "/api/repos"} "{}"]
                    (httpkit-client/request client {:path "/api/repos"})))))
    (testing "it supports regexes"
      (is (match? {:body {}}
                  (sut/with-fake-github [#"/api/repos" "{}"]
                    (httpkit-client/request client {:path "/api/repos"})))))

    (testing "it supports functions as request spec"
      (let [request-fn          (fn [request]
                                  (re-find #"/api/repos" (:url request)))
            request-fn-with-arg (fn [url-regex]
                                  (fn [request]
                                    (re-find url-regex (:url request))))]
        (is (match? {:body {}}
                    (sut/with-fake-github ["/other" "{}"
                                           (request-fn-with-arg #"/api/whatever") {:body "{}"}]
                      (httpkit-client/request client {:path "/other"})
                      (httpkit-client/request client {:path "/api/whatever"}))))

        (is (match? {:body {}}
                    (sut/with-fake-github ["/other" "{}"
                                           request-fn {:body "{}"}]
                      (httpkit-client/request client {:path "/other"})
                      (httpkit-client/request client {:path "/api/repos"}))))))))
