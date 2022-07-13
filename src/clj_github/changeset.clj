(ns clj-github.changeset
  "Provides a functional api to make commits. It is centered around the concept of a changeset.

  A changeset is an object that accumulates a series of changes to the repository that will eventually generate a commit.

  A changeset can be created by calling `from-revision` or `from-branch!`.

  A changeset can be \"changed\" by using `put-content`, `update-content`, `delete`, etc. Those functions don't actually
  change the changeset but return a new one.

  `get-content` will return the content of a file correspondent to the state of the provided changeset.

  Once the necessary changes have been made, `commit!` will generate a commit having the changeset base revision as its parent.

  `commit!` will not update the head of the branch, but `create-branch!` and `update-branch!` can be used to do that.
  That allows one to make a series of commits and only change the branch when they are ready to, which makes it easier
  to make processes idempontent, since if anything wrong happens the intermediary commits can just be ignored.

  Note: The internal format of the changeset is considered an implementation detail and should not be relied upon.
  Always create a changeset using one of the factory functions (e.g. `from-revision`, `from-branch`)."
  (:require [clj-github.repository :as repository]))

(defn orphan [client org repo]
  {:client client
   :org org
   :repo repo})

(defn from-revision
  "Creates a new changeset based on a given git revision (hexadecimal sha)."
  [client org repo revision]
  {:client client
   :org org
   :repo repo
   :base-revision revision})

(defn from-branch!
  "Creates a new changeset based on the current HEAD of a given branch."
  [client org repo branch]
  {:client client
   :org org
   :repo repo
   :branch branch
   :base-revision (-> (repository/get-branch! client org repo branch)
                      :commit
                      :sha)})

(defn get-content
  "Returns the content of a file (as a string) for a given changeset."
  [{:keys [client org repo base-revision changes]} path]
  (let [content (get changes path)]
    (case content
      ::deleted nil
      (or content
          (repository/get-content! client org repo path {:ref base-revision})))))

(defn put-content
  "Returns a new changeset with the file under path with new content.
  `content` must be a string (only text files are supported).
  It creates a new file if it does not exist yet."
  [changeset path content]
  (assoc-in changeset [:changes path] content))

(defn update-content
  "Returns a new changeset with the file under path with new content return by `update-fn`.
  `update-fn` should be an 1-arg function that receives the current content of the file."
  [revision path update-fn]
  (let [old-content (get-content revision path)
        new-content (update-fn old-content)]
    (if (= old-content new-content)
      revision
      (put-content revision path new-content))))

(defn delete
  "Returns a new changeset with the file under path deleted."
  [revision path]
  (assoc-in revision [:changes path] ::deleted))

(defn dirty?
  "Returns true if changes were made to the given changeset"
  [{:keys [changes]}]
  (not (empty? changes)))

(defn- deleted? [content]
  (#{::deleted} content))

(defn- change->tree-object [{:keys [client org repo]} [path content]]
  (let [base-object {:path path
                     :mode "100644"
                     :type "blob"}]
    (condp apply [content]
      deleted? (assoc base-object :sha nil)
      bytes? (assoc base-object :sha (-> (repository/create-blob! client org repo content)
                                         :sha))
      (assoc base-object :content content))))

(defn- changes->tree [{:keys [changes] :as changeset}]
  (mapv (partial change->tree-object changeset) changes))

(defn commit!
  "Commits the changeset returning a new changeset based on the new commit revision.
  If the changeset does not contain any changes, calling this function is a no-op.
  This function does not update the HEAD of a branch."
  [{:keys [client org repo base-revision changes] :as changeset} message]
  (if (empty? changes)
    changeset
    (let [{:keys [sha]} (repository/commit! client org repo base-revision {:message  message
                                                                           :tree     (changes->tree changeset)})]
      (-> changeset
          (dissoc :changes)
          (assoc :base-revision sha)))))

(defn- branch-ref [branch]
  (format "heads/%s" branch))

(defn- assert-not-dirty [changeset]
  (when (dirty? changeset)
    (throw (ex-info "Changeset cannot contain changes" {}))))

(defn create-branch!
  "Creates a branch that points to the base revision of the given changeset. Returns a new changeset with the branch associated.

  The changeset should not contain any changes."
  [{:keys [client org repo base-revision] :as changeset} branch]
  (assert-not-dirty changeset)
  (repository/create-reference! client org repo {:ref (branch-ref branch)
                                                 :sha base-revision})
  (assoc changeset :branch branch))

(defn update-branch!
  "Updates a branch to point to the base revision of the given changeset. Returns the changeset unchanged.

  The changeset should not contain any changes."
  [{:keys [client org repo base-revision branch] :as changeset}]
  (assert-not-dirty changeset)
  (repository/update-reference! client org repo (branch-ref branch) {:sha base-revision})
  changeset)

(defn delete-branch!
  "Deletes the branch associated with the changeset, returning the changeset with the branch excluded.

  The changeset should not contain any changes."
  [{:keys [client org repo branch] :as changeset}]
  (assert-not-dirty changeset)
  (repository/delete-reference! client org repo (branch-ref branch))
  (dissoc changeset :branch))
