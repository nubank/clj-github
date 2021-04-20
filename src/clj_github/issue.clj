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
