(ns clj-github.token
  (:require [cheshire.core :as cheshire]
            [clj-github-app.token-manager :as token-manager]
            [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [org.httpkit.client :as httpkit]))

(defn- file-exists-or-nil [file]
  (when (.exists file)
    file))

(def hub-config
  (memoize
   (fn []
     (some-> (io/file (System/getenv "HOME") ".config/hub")
             file-exists-or-nil
             (slurp)
             yaml/parse-string
             (get :github.com)
             first
             (get :oauth_token)))))

(def env-var
  (memoize
   (fn [] (System/getenv "GITHUB_TOKEN"))))

(def github-url "https://api.github.com")

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
