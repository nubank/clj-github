(ns clj-github.token
  (:require [cheshire.core :as cheshire]
            [clj-github-app.token-manager :as token-manager]
            [clojure.java.io :as io]
            [org.httpkit.client :as httpkit])
  (:import (java.io File IOException)))

(set! *warn-on-reflection* true)

(defn- file-exists-or-nil [^File file]
  (when (.exists file)
    file))

(defn- parse-yaml [s]
  (let [parse-string (requiring-resolve 'clj-yaml.core/parse-string)]
    (parse-string s)))

(def hub-config
  "Read token from `~/.config/hub` if the file exists.

  This credentials file is managed by https://github.com/mislav/hub.

  Users must provide their own dependency on `clj-commons/clj-yaml`."
  (memoize
   (fn []
     (some-> (io/file (System/getProperty "user.home") ".config/hub")
             file-exists-or-nil
             (slurp)
             parse-yaml
             (get :github.com)
             first
             (get :oauth_token)))))

(def env-var
  "Get token from the GITHUB_TOKEN environment variable."
  (memoize
   (fn [] (System/getenv "GITHUB_TOKEN"))))

(def github-url "https://api.github.com")

(def gh-cli
  "Get token by invoking `gh auth token` if command is available.

  Requires https://github.com/cli/cli."
  (memoize
    (fn []
      (try
        (let [process (.start (ProcessBuilder. ["gh" "auth" "token"]))
              output (with-open [stdout (.getInputStream process)]
                       (String. (.readAllBytes stdout)))]
          (when (zero? (.waitFor process))
            (.trim ^String output)))
        (catch IOException _
          ; gh cli not available
          nil)))))

(def ^:private get-token-manager
  (memoize
   (fn [{:keys [github-app-id github-private-key]}]
     (when (and github-app-id github-private-key)
       (token-manager/make-token-manager github-url github-app-id github-private-key)))))

(def ^:private get-installation-id
  (memoize
   (fn
     [{:keys [github-org] :as opts}]
     (let [token-manager (get-token-manager opts)
           result        @(httpkit/request {:url     (str github-url "/app/installations")
                               :method  :get
                               :headers {"Authorization" (str "Bearer " (token-manager/get-app-token token-manager))
                                         "Accept"        "application/vnd.github.machine-man-preview+json"}})]
       (->> (cheshire/parse-string (:body result) keyword)
            (filter #(= (:login (:account %)) (or github-org "nubank")))
            (first)
            (:id))))))

(defn github-app-token-manager [opts]
  (fn []
    (when-let [token-manager (get-token-manager opts)]
      (token-manager/get-installation-token token-manager
                                            (get-installation-id opts)))))

(defn chain [providers]
  (fn []
    (loop [providers providers]
      (when (seq providers)
        (if-let [token (-> (first providers)
                           (apply nil))]
          token
          (recur (rest providers)))))))
