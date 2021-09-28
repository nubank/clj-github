(ns clj-github.test-helpers-test
  (:require [clojure.test :refer :all]
            [clj-github.httpkit-client :as httpkit-client]
            [clj-github.test-helpers :as sut]
            [matcher-combinators.test]))

(deftest with-fake-github-test
  (let [client (httpkit-client/new-client {:token-fn (fn [] "token")})]
    (testing "it appends github url when request is a string"
      (is (match? {:status 200}
                  (sut/with-fake-github ["/api/repos" {:status 200}]
                    (httpkit-client/request client {:path "/api/repos"})))))
    (testing "it supports a path attribute in a request map"
      (is (match? {:status 200} 
                  (sut/with-fake-github [{:path "/api/repos"} {:status 200}]
                    (httpkit-client/request client {:path "/api/repos"})))))
    (testing "it supports regexes"
      (is (match? {:status 200} 
                  (sut/with-fake-github [#"/api/repos" {:status 200}]
                    (httpkit-client/request client {:path "/api/repos"})))))

    (testing "it supports functions as request spec"
      (let [request-fn          (fn [request]
                                  (re-find #"/api/repos" (:url request)))
            request-fn-with-arg (fn [url-regex]
                                  (fn [request]
                                    (re-find url-regex (:url request))))]
        (is (match? {:status 200} 
                    (sut/with-fake-github ["/other" "{}"
                                           (request-fn-with-arg #"/api/whatever") {:status 200}]
                      (httpkit-client/request client {:path "/other"})
                      (httpkit-client/request client {:path "/api/whatever"}))))

        (is (match? {:status 200}
                    (sut/with-fake-github ["/other" "{}"
                                           request-fn {:status 200}]
                      (httpkit-client/request client {:path "/other"})
                      (httpkit-client/request client {:path "/api/repos"}))))))

    (testing "it adds content-type application/json by default if no content-type is provided"
      (is (match? {:status 200 :body {:number 2}}
                  (sut/with-fake-github ["/other" "{\"number\": 2}"]
                                        (httpkit-client/request client {:path "/other"})))))

    (testing "it maintains the content-type if one is provided"
      (is (match? {:status 200 :body "{\"number\": 2}"}
                  (sut/with-fake-github ["/other" {:body "{\"number\": 2}"
                                                   :headers {:content-type "text/html"}}]
                                        (httpkit-client/request client {:path "/other"})))))))

