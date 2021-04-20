(ns clj-github.test-helpers
  (:require [clj-github-app.token-manager]
            [org.httpkit.fake :refer [with-fake-http]]))

(defn- spec-type [spec]
  (cond
    (string? spec) :string
    (map? spec) :map
    :else :form))

(defmulti spec-builder spec-type)

(defmethod spec-builder :string [request]
  (str "https://api.github.com" request))

(defmethod spec-builder :map [{:keys [path] :as request}]
  (if path
    (assoc request :url (str "https://api.github.com" path))
    request))

(defmethod spec-builder :form [request]
  request)

(defn build-spec [spec]
  (reduce (fn [processed-fakes [request response]]
            (-> processed-fakes
                (conj (spec-builder request))
                (conj response)))
          ["https://api.github.com/app/installations" "{}"]
          (partition 2 spec)))

(defmacro with-fake-github [spec & body]
  `(with-fake-http ~(build-spec spec)
     ~@body))
