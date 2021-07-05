(ns clj-github.repository
  "Provides auxiliary functions to work with repositories via github api."
  (:require [clj-github.client-utils :refer [fetch-body!]]
            [clj-github.httpkit-client :as client]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [me.raynes.fs :as fs]
            [me.raynes.fs.compression :as fs-compression])
  (:import [clojure.lang ExceptionInfo]
           [java.util Base64]))

(defn- base64->string
  ([base64] (base64->string base64 (Base64/getDecoder)))
  ([base64 decoder] (String. (.decode decoder ^String base64) "UTF-8")))

(defn- split-lines [content]
  (string/split content #"\r?\n" -1)) ; make sure we don't lose \n at the end of the string

(defn- base64-lines->string [content]
  (->> (split-lines content)
       (map base64->string)
       (string/join)))

(defn get-contents!
  "Returns the list of contents of a repository default branch (usually `master`).
  An optional `:ref` parameter can be used to fetch content from a different commit/branch/tag."
  ([client org repo]
   (get-contents! client org repo {}))
  ([client org repo {:keys [ref branch]}]
   (try
     (-> (fetch-body! client (merge {:method :get
                                     :path (format "/repos/%s/%s/contents" org repo)}
                                    (cond
                                      ref    {:query-params {"ref" ref}}
                                      branch {:query-params {"branch" branch}}
                                      :else  {}))))
     (catch ExceptionInfo e
       (if (= 404 (-> (ex-data e) :response :status))
         nil
         (throw e))))))

(defn get-content!
  "Returns the content of a text file from the repository default branch (usually `master`).
  An optional `:ref` parameter can be used to fetch content from a different commit/branch/tag.
  If the file does not exist, nil is returned.

  Note 1: it currently does not work for directories, symlinks and submodules.
  Note 2: it only works for text files"
  ([client org repo path]
   (get-content! client org repo path {}))
  ([client org repo path {:keys [ref branch]}]
   (try
     (-> (fetch-body! client (merge {:method :get
                                     :path (format "/repos/%s/%s/contents/%s" org repo path)}
                                    (cond
                                      ref    {:query-params {"ref" ref}}
                                      branch {:query-params {"branch" branch}}
                                      :else  {})))
         :content
         base64-lines->string)
     (catch ExceptionInfo e
       (if (= 404 (-> (ex-data e) :response :status))
         nil
         (throw e))))))

(defn get-repo!
  [client org repo]
  (fetch-body! client {:method :get
                       :path (format "/repos/%s/%s" org repo)}))

(defn get-branch!
  "Returns information about a branch.

  Look at https://developer.github.com/v3/repos/branches/#get-a-branch for details about the response format."
  [client org repo branch]
  (fetch-body! client {:method :get
                       :path (format "/repos/%s/%s/branches/%s" org repo branch)}))

(defn get-tree!
  "Returns information about a tree.

  Look at https://developer.github.com/v3/git/trees/#get-a-tree for details about the response format.

  Note: it currently does not support the `recursive` option."
  [client org repo tree-sha]
  (fetch-body! client {:path (format "/repos/%s/%s/git/trees/%s" org repo tree-sha)}))

(defn create-tree!
  "Creates a new tree.

  Look at https://developer.github.com/v3/git/trees/#create-a-tree for details about the parameters and response format"
  [client org repo params]
  (fetch-body! client {:method :post
                       :path (format "/repos/%s/%s/git/trees" org repo)
                       :body params}))

(defn get-commit!
  "Returns information about a commit.

  Look at https://developer.github.com/v3/git/commits/#get-a-commit for details about the response format."
  [client org repo ref]
  (fetch-body! client {:path (format "/repos/%s/%s/git/commits/%s" org repo ref)}))

(defn create-commit!
  "Creates a new commit.

  Look at https://developer.github.com/v3/git/commits/#create-a-commit for details about the parameters and response format."
  [client org repo params]
  (fetch-body! client {:method :post
                       :path (format "/repos/%s/%s/git/commits" org repo)
                       :body params}))

(defn commit!
  "Auxiliary function that combines the creation of a tree and a commit.
  Returns the information of the new commit."
  [client org repo base-revision {:keys [message tree]}]
  (let [{{base-tree-sha :sha} :tree} (when base-revision (get-commit! client org repo base-revision))
        {tree-sha :sha} (create-tree! client org repo {:base_tree base-tree-sha
                                                       :tree tree})]
    (create-commit! client org repo (merge {:message message
                                            :tree tree-sha}
                                           (when base-revision
                                             {:parents [base-revision]})))))

(defn get-reference!
  "Returns information about a reference.
  The `ref` parameter should be in the format \"heads/<branch>\" or \"tags/<tag>\".

  Look at https://developer.github.com/v3/git/refs/#get-a-reference for details about the reponse format."
  [client org repo ref]
  (fetch-body! client {:path (format "/repos/%s/%s/git/ref/%s" org repo ref)}))

(defn create-reference!
  "Creates a new reference.

  Look at https://developer.github.com/v3/git/refs/#create-a-reference for details about the parameters and response format.
  Note that the `ref` attribute inside params should be in the format \"heads/<branch>\" or \"tags/<tag>\".
  This function will do the transformation to the format expected by the api."
  [client org repo params]
  (fetch-body! client {:path (format "/repos/%s/%s/git/refs" org repo)
                       :method :post
                       :body (update params :ref #(str "refs/" %))}))

(defn update-reference!
  "Updates a reference.
  The `ref` parameter should be in the format \"heads/<branch>\" or \"tags/<tag>\".

  Look at https://developer.github.com/v3/git/refs/#update-a-reference for details about the parameters and response format."
  [client org repo ref params]
  (fetch-body! client {:path (format "/repos/%s/%s/git/refs/%s" org repo ref)
                       :headers {"Accept" "application/vnd.github.v3+json"}
                       :method :patch
                       :body params}))

(defn delete-reference!
  "Deletes a reference.
  The `ref` parameter should be in the format \"heads/<branch>\" or \"tags/<tag>\".

  Look at https://developer.github.com/v3/git/refs/#delete-a-reference for details about the parameters and response format."
  [client org repo ref]
  (fetch-body! client {:path   (format "/repos/%s/%s/git/refs/%s" org repo ref)
                       :method :delete}))

(defn create-org-repository!
  "Creates a new github repository.
  This function supports using the `visibility` and `is-template`, params by automatically setting the appropriate `Accept` header.

  For details about the parameters, response format, and preview headers, look at https://docs.github.com/en/rest/reference/repos#create-an-organization-repository."
  [client org params]
  (fetch-body! client {:path    (format "/orgs/%s/repos" org)
                       :headers {"Accept" (cond
                                            (:visibility params)
                                            "application/vnd.github.nebula-preview+json"

                                            (:template-repository params)
                                            "application/vnd.github.baptiste-preview+json"

                                            :else
                                            "application/vnd.github.v3+json")}
                       :method :post
                       :body   params}))

(defn get-repository!
  "Get information from github repository.

  For details about the parameters and response format, look at https://docs.github.com/en/rest/reference/repos#get-a-repository."
  [client org repo]
  (let [url (format "/repos/%s/%s" org repo)]
    (fetch-body! client {:path   url
                         :method :get})))

(defn upsert-permissions!
  "Add or update team repository permissions.

  For details about the parameters and response format, look at https://docs.github.com/en/rest/reference/teams#add-or-update-team-repository-permissions."
  [client org team owner repo params]
  (let [url (format "/orgs/%s/teams/%s/repos/%s/%s" org team owner repo)]
    (fetch-body! client {:path   url
                         :method :put
                         :body   params})))

(defn clone
  "Download github repository and put content on destination path"
  ([client org repo dest]
   (clone client org repo "" dest))
  ([client org repo tag dest]
   (let [clone-path (str (fs/temp-dir "clone-repo") "/")
         url (format "/repos/%s/%s/zipball/%s" org repo tag)
         git-response (client/request client {:path   url
                                              :method :get
                                              :as     :byte-array})
         filename (->> git-response :headers :content-disposition (re-find #"filename=(.*).zip") second)]
     (-> git-response
         :body
         (io/input-stream)
         (io/copy (io/file clone-path "git-response.zip")))
     (fs-compression/unzip (str clone-path "git-response.zip") clone-path)
     (fs/move (str clone-path filename) dest))))
