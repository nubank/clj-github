(ns clj-github.test-helpers
  (:require [clj-github-app.token-manager]
            [clj-github.httpkit-client :refer [github-url]]
            [org.httpkit.fake :as fake])
  (:import (java.util.regex Pattern)))


(defn- spec-type [spec]
  (cond
    (-> spec meta :path) :path
    (string? spec) :string
    (map? spec) :map
    (instance? Pattern spec) :pattern
    :else :form))

(defmulti spec-builder spec-type)

(defmethod spec-builder :string [request]
  (str github-url request))

(defmethod spec-builder :map [request]
  (if (:path request)
    `(let [request# ~request
           path#    (:path request#)]
       (assoc request# :url (str github-url path#)))
    request))

(defmethod spec-builder :form [request]
  request)

(defmethod spec-builder :path [form]
  `(spec-builder ~form))

(defmethod spec-builder :pattern [pattern]
  {:url pattern})

(defn -add-default-content-type
  [response]
  (assoc-in response [:headers :content-type] "application/json"))

(defn add-default-content-type [response]
  `(let [responder# (fake/responder ~response)]
     (fn [origin-fn# opts# callback#]
       ((or callback# identity)
        (responder# origin-fn# opts# -add-default-content-type)))))

(defn build-spec [spec]
  (reduce (fn [processed-fakes [request response]]
            (-> processed-fakes
                (conj (spec-builder request))
                (conj (add-default-content-type response))))
          [(str github-url "app/installations") "{}"]
          (partition 2 spec)))

(defmacro with-fake-github
  "A wrapper around `with-fake-http` that sets up some defaults for GitHub access.

  `with-fake-http` is organized with the expectation that request and response specs
  are values; any function calls used to generate the specs look to it as if they are
  function values that will (for requests) match or (for responses) build the response from
  the request map.

  The ^:path metadata may precede a form to indicate that the form is an expression
  that computes the path.

  The response may be any value supported by `with-fake-http` (map, integer, string, function, etc.).
  However, the response Content-Type header is forced to \"application/json\", so the body (if provided)
  must be a JSON value encoded as string."
  [spec & body]
  `(fake/with-fake-http ~(build-spec spec)
                        ~@body))
