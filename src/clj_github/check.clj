(ns clj-github.check
  "Provides auxiliary functions to work with checks via github api."
  (:require [clj-github.client-utils :refer [fetch-body!]]))

(defn- check-run-url [org repo]
  (format "/repos/%s/%s/check-runs" org repo))

(defn get-check-run!
  "Gets a single check run using its id.

  For details about the response format, look at https://docs.github.com/en/rest/reference/checks#get-a-check-run."
  [client org repo id]
  (fetch-body! client {:path (str (check-run-url org repo) "/" id)
                       :headers {"Accept" "application/vnd.github.v3+json"}
                       :method :get}))

(defn create-check-run!
  "Creates a new check run for a specific commit in a repository.

  For details about the parameters and response format, look at https://docs.github.com/en/rest/reference/checks#create-a-check-run."
  ([client org repo params]
   (fetch-body! client {:path (check-run-url org repo)
                        :headers {"Accept" "application/vnd.github.v3+json"}
                        :method :post
                        :body params}))
  ([client org repo name sha]
   (create-check-run! client org repo {:name name :head_sha sha})))

(defn update-check-run!
  "Updates a check run using its id.

  For details about the parameters and response format, look at https://docs.github.com/en/rest/reference/checks#update-a-check-run."
  [client org repo id params]
  (fetch-body! client {:path (str (check-run-url org repo) "/" id)
                       :headers {"Accept" "application/vnd.github.v3+json"}
                       :method :patch
                       :body params}))
