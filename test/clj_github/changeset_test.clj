(ns clj-github.changeset-test
  (:require [clojure.test :refer :all]
            [clj-github-mock.core :as mock.core]
            [clj-github.changeset :as sut]
            [clj-github.httpkit-client :as client]
            [org.httpkit.fake :as fake]
            [clojure.java.io :as io]))

(defmacro with-client [[client initial-state] & body]
  `(fake/with-fake-http
     [#"^https://api.github.com/repos/.*" (mock.core/httpkit-fake-handler {:initial-state initial-state})]
     (let [~client (client/new-client {:token-fn (constantly "token")})]
       ~@body)))

(def initial-state {:orgs [{:name "nubank" :repos [{:name           "repo"
                                                    :default_branch "master"}]}]})

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
                    (sut/get-content "file")))))))

(deftest visit-test
  (testing "it changes the content of a file"
    (with-client [client initial-state]
      (-> (sut/orphan client "nubank" "repo")
          (sut/put-content "file" "content")
          (sut/commit! "initial commit")
          (sut/create-branch! "master"))
      (-> (sut/from-branch! client "nubank" "repo" "master")
          (sut/visit (fn [{:keys [content]}]
                       (str "changed " content)))
          (sut/commit! "change")
          (sut/update-branch!))
      (is (= "changed content"
             (-> (sut/from-branch! client "nubank" "repo" "master")
                 (sut/get-content "file")))))))

(deftest visit-fn-test
  (testing "it changes the content of a file"
    (with-client [client initial-state]
      (-> (sut/orphan client "nubank" "repo")
          (sut/put-content "file" "content")
          (sut/commit! "initial commit")
          (sut/create-branch! "master"))
      (-> (sut/from-branch! client "nubank" "repo" "master")
          (sut/visit-fs (fn [dir]
                          (let [file (io/file dir "file")]
                            (spit file "changed content"))))
          (sut/commit! "change")
          (sut/update-branch!))
      (is (= "changed content"
             (-> (sut/from-branch! client "nubank" "repo" "master")
                 (sut/get-content "file"))))))
  (testing "it adds new files"
    (with-client [client initial-state]
      (-> (sut/orphan client "nubank" "repo")
          (sut/put-content "file" "content")
          (sut/commit! "initial commit")
          (sut/create-branch! "master"))
      (-> (sut/from-branch! client "nubank" "repo" "master")
          (sut/visit-fs (fn [dir]
                          (let [file (io/file dir "new-file")]
                            (spit file "new content"))))
          (sut/commit! "change")
          (sut/update-branch!))
      (is (= "new content"
             (-> (sut/from-branch! client "nubank" "repo" "master")
                 (sut/get-content "new-file"))))))
  (testing "it deletes a file"
    (with-client [client initial-state]
      (-> (sut/orphan client "nubank" "repo")
          (sut/put-content "file" "content")
          (sut/commit! "initial commit")
          (sut/create-branch! "master"))
      (-> (sut/from-branch! client "nubank" "repo" "master")
          (sut/visit-fs (fn [dir]
                          (let [file (io/file dir "file")]
                            (.delete file))))
          (sut/commit! "change")
          (sut/update-branch!))
      (is (nil? (-> (sut/from-branch! client "nubank" "repo" "master")
                    (sut/get-content "file")))))))
