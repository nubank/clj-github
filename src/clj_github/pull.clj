(ns clj-github.pull
  "Provides functions to work with pull requests via github api."
  (:require [clj-github.client-utils :refer [fetch-body!]]
            [clj-github.httpkit-client :as github-client]))

(defn- pull-url
  ([org repo]
   (format "/repos/%s/%s/pulls" org repo))
  ([org repo number]
   (format "%s/%s" (pull-url org repo) number)))

(defn get-pulls! [client org repo params]
  (fetch-body! client {:path         (pull-url org repo)
                       :query-params params}))

(defn get-pull! [client org repo pull-number]
  (fetch-body! client
               {:method :get
                :path   (pull-url org repo pull-number)}))

(defn update-pull! [client org repo pull-number params]
  (fetch-body! client {:path   (pull-url org repo pull-number)
                       :body   params
                       :method :post}))

(defn close-pull! [client org repo pull-number]
  (update-pull! client org repo pull-number {:state :closed}))

(defn get-open-pulls! [client org repo]
  (get-pulls! client org repo {"state" "open"}))

(defn create-pull!
  [client org {:keys [repo title branch body base] :or {base "master"}}]
  (fetch-body! client {:method :post
                       :path   (pull-url org repo)
                       :body   {:title title
                                :head  branch
                                :base  base
                                :body  body}}))

(defn pull-closed? [pull]
  (= "closed" (:state pull)))

(defn has-label?
  "does a pull request have the given label?"
  [{:keys [labels] :as _pull} label]
  ((set (map :name labels)) label))

(defn pull->branch-name
  "get branch name associated with a pull request"
  [pull]
  (get-in pull [:head :ref]))

(defn merge!
  "Merge a pull request.

  For details about the parameters and response format, look at https://docs.github.com/en/rest/reference/pulls#merge-a-pull-request"
  [client org repo pull-number params]
  (fetch-body! client {:path   (str (pull-url org repo pull-number) "/merge")
                       :method :put
                       :body   params}))

(defn get-pull-files!
  "Get files changed in a pull request. For more details, look at https://docs.github.com/en/rest/reference/pulls#list-pull-requests-files"
  [client org repo pull-number]
  (fetch-body! client {:path   (str (pull-url org repo pull-number) "/files")
                       :method :get}))
