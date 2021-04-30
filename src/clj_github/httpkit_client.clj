(ns clj-github.httpkit-client
  (:require [cheshire.core :as cheshire]
            [clj-github.token :as token]
            [clj-github.utils :refer [assoc-some]]
            [org.httpkit.client :as httpkit]))

(def ^:private success-codes #{200 201 202 204})

(def github-url "https://api.github.com")

(defn get-installation-token [{:keys [token-fn]}]
  (token-fn))

(defn- prepare
  [{:keys [token-fn]} {:keys [path method body] :or {method :get} :as request}]
  (-> request
      (assoc :method method)
      (assoc-some :body (and body (cheshire/generate-string body)))
      (assoc :url (str github-url path))
      (assoc-in [:headers "Content-Type"] "application/json")
      (assoc-in [:headers "Authorization"] (str "Bearer " (token-fn)))))

(defn- parse-body [content-type body]
  (if (and content-type (re-find #"application/json" content-type))
    (cheshire/parse-string body true)
    body))

(defn- content-type [response]
  (or (get-in response [:headers :content-type])
      (get-in response [:headers "Content-Type"])))

(defn request [client req-map]
  (let [response @(httpkit/request (prepare client req-map))]
    (if (success-codes (:status response))
      (update response :body (partial parse-body (content-type response)))
      (throw (ex-info "Request to GitHub failed" {:response response})))))

(defn new-client [{:keys [app-id private-key token org] :as opts}]
  (cond
    token
    {:token-fn (constantly token)}

    app-id
    {:token-fn (token/github-app-token-manager
                 (assoc-some {:github-app-id      app-id
                              :github-private-key private-key}
                             :github-org org))}

    :else
    opts))
