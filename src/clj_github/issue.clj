(ns clj-github.issue
  "Provides auxiliary functions to work with issues via github api."
  (:require [clj-github.client-utils :refer [fetch-body!]]))

(defn- issue-url [org repo issue-number]
  (format "/repos/%s/%s/issues/%s" org repo issue-number))

(defn update-issue!
  [client org repo issue-number params]
  (fetch-body! client {:path   (issue-url org repo issue-number)
                       :method :patch
                       :body   params}))

(defn add-label! [client org repo issue-number label]
  (update-issue! client org repo issue-number {:labels [label]}))

(defn delete-label!
  "Delete a label from an issue.

  For details about the response format, look at https://docs.github.com/en/rest/reference/issues#remove-a-label-from-an-issue."
  [client org repo issue-number label]
  (fetch-body! client {:path (str (issue-url org repo issue-number) "/labels/" label)
                       :method :delete}))

(defn comment!
  "Create an issue comment.

  For details about the response format, look at https://docs.github.com/en/rest/reference/issues#create-an-issue-comment."
  [client org repo issue-number message]
  (fetch-body! client {:path (str (issue-url org repo issue-number) "/comments")
                       :method :post
                       :body {:body message}}))
