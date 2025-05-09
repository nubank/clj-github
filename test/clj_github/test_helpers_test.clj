(ns clj-github.test-helpers-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is]]
            [clj-github.httpkit-client :as httpkit-client]
            [clj-github.test-helpers :refer [with-fake-github]]
            [matcher-combinators.test]))

(def ^:private client (httpkit-client/new-client {:token-fn (fn [] "token")}))

(defn- request [path]
  (httpkit-client/request client {:path   path
                                  :throw? false}))

(deftest appends-github-url-when-request-is-string
  (is (match? {:status 200
               :opts   {:url "https://api.github.com/api/repos"}}
              (with-fake-github ["/api/repos" 200]
                                (request "/api/repos")))))

(deftest response-may-be-a-number
  (is (match? {:status 418}
              (with-fake-github ["/api/teapot" 418]
                                (request "/api/teapot")))))

(deftest response-may-be-a-JSON-encoded-string
  ;; Because we force the response content type to application/json, the JSON is
  ;; parsed back to EDN in the response body.
  (is (match? {:status 200
               :body   {:solution 42}}
              (with-fake-github ["/api/answer" (json/generate-string {:solution 42})]
                                (request "/api/answer")))))

(deftest supports-computed-string
  (let [last-term "bar"]
    (is (match? {:status 200
                 :body   1234}
                (with-fake-github [^:path (str "/api/repos/" last-term) "1234"]
                                  (request "/api/repos/bar"))))))

(deftest response-may-be-a-map
  (is (match? {:status 201
               :body   {:solution 42}}
              (with-fake-github ["/api/answer" {:status 201
                                                :body   (json/generate-string {:solution 42})}]
                                (request "/api/answer")))))

(deftest supports-computed-path-key
  (let [last-term "foo"]
    (is (match? {:status 200}
                (with-fake-github [{:path (str "/api/repos/" last-term)} 200]
                                  (request "/api/repos/foo"))))))

(deftest supports-path-attribute-in-request-map
  (is (match? {:status 200}
              (with-fake-github [{:path "/api/repos"} 200]
                                (request "/api/repos")))))

(deftest supports-regexps-as-spec
  (is (match? {:status 200}
              (with-fake-github [#"/api/repos" 200]
                                (request "/api/repos")))))

(deftest supports-computed-regexps-as-spec
  (let [last-term "fred"]
    (is (match? {:status 200}
                (with-fake-github [(re-pattern (str ".api/repos/" last-term)) 200]
                                  (request "/api/repos/fred/and-more"))))))

(deftest supports-functions-as-spec
  (let [request-fn          (fn [request]
                              (re-find #"/api/repos" (:url request)))
        request-fn-with-arg (fn [url-regex]
                              (fn [request]
                                (re-find url-regex (:url request))))]
    (is (match? [{:status 418}
                 {:status 200}]
                (with-fake-github ["/other" 418
                                   (request-fn-with-arg #"/api/whatever") 200]
                                  [(request "/other")
                                   (request "/api/whatever")])))

    (is (match? [{:status 418}
                 {:status 200}]
                (with-fake-github ["/other" 418
                                   request-fn 200]
                                  [(request "/other")
                                   (request "/api/repos")])))))
