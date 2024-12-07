(ns clj-github.changeset-test
  (:require [clj-github-mock.core :as mock.core]
            [clj-github.changeset :as sut]
            [clj-github.httpkit-client :as client]
            [clojure.test :refer :all]
            [org.httpkit.fake :as fake])
  (:import (java.util Arrays)))

(defmacro with-client [[client initial-state] & body]
  `(fake/with-fake-http
     [#"^https://api.github.com/repos/.*" (mock.core/httpkit-fake-handler {:initial-state initial-state})]
     (let [~client (client/new-client {:token-fn (constantly "token")})]
       ~@body)))

(def initial-state {:orgs [{:name "nubank" :repos [{:name           "repo"
                                                    :default_branch "master"}]}]})

(def string-content-with-special-chars
  "This is a string with special characters: \uD83C\uDF89\uD83C\uDF89\uD83C\uDF89\uD83D\uDD25\uD83D\uDD25\uD83D\uDD25")

(def binary-content
  (byte-array 55 (unchecked-byte 255)))

(def binary-content-2
  (byte-array 7 (unchecked-byte 0)))

(deftest get-content-test
  (testing "get content from client if there is no change"
    (with-client [client initial-state]
      (-> (sut/orphan client "nubank" "repo")
          (sut/put-content "file" "content")
          (sut/commit! "initial commit")
          (sut/create-branch! "master"))
      (let [revision (sut/from-branch! client "nubank" "repo" "master")]
        (is (= "content"
               (sut/get-content revision "file"))))))
  (testing "get string content with special characters"
    (with-client [client initial-state]
      (-> (sut/orphan client "nubank" "repo")
          (sut/put-content "file" string-content-with-special-chars)
          (sut/commit! "initial commit")
          (sut/create-branch! "master"))
      (let [revision (sut/from-branch! client "nubank" "repo" "master")]
        (is (= string-content-with-special-chars
               (sut/get-content revision "file"))))))
  (testing "binary contents"
    (with-client [client initial-state]
      (-> (sut/orphan client "nubank" "repo")
          (sut/put-content "file" binary-content)
          (sut/commit! "initial commit")
          (sut/create-branch! "master"))
      (let [revision (sut/from-branch! client "nubank" "repo" "master")]
        (testing "read remote contents"
          (is (Arrays/equals ^bytes binary-content
                             (sut/get-content-raw revision "file"))))
        (testing "read local changes"
          (is (Arrays/equals ^bytes binary-content-2
                             (-> (sut/put-content revision "file" binary-content-2)
                                 (sut/get-content-raw "file"))))))))
  (testing "get changed content"
    (with-client [client initial-state]
      (-> (sut/orphan client "nubank" "repo")
          (sut/put-content "file" "content")
          (sut/commit! "initial commit")
          (sut/create-branch! "master"))
      (is (= "changed content"
             (-> (sut/from-branch! client "nubank" "repo" "master")
                 (sut/put-content "file" "changed content")
                 (sut/get-content "file"))))))
  (testing "get deleted content"
    (with-client [client initial-state]
      (-> (sut/orphan client "nubank" "repo")
          (sut/put-content "file" "content")
          (sut/commit! "initial commit")
          (sut/create-branch! "master"))
      (is (nil? (-> (sut/from-branch! client "nubank" "repo" "master")
                    (sut/delete "file")
                    (sut/get-content "file"))))))
  (testing "get missing file"
    (with-client [client initial-state]
      (-> (sut/orphan client "nubank" "repo")
          (sut/put-content "file" "content")
          (sut/commit! "initial commit")
          (sut/create-branch! "master"))
      (let [revision (sut/from-branch! client "nubank" "repo" "master")]
        (is (nil? (sut/get-content revision "different_file")))
        (is (nil? (sut/get-content-raw revision "different_file")))))))
