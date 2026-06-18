(ns clj-github.test-helpers
  (:require [clj-github-app.token-manager]
            [clj-github.httpkit-client :refer [github-url]]
            [org.httpkit.fake :as fake])
  (:import (java.util.regex Pattern)))

(defn- request-spec [request]
  (cond
    (string? request)                    (str github-url request)
    (instance? Pattern request)          {:url request}
    (and (map? request) (:path request)) (assoc request :url (str github-url (:path request)))
    :else                                request))

(defn- response-spec [response]
  (let [responder  (fake/responder response)
        force-json #(assoc-in % [:headers :content-type] "application/json")]
    (fn [orig-fn opts callback]
      ((or callback identity)
       (responder orig-fn opts force-json)))))

(defn ^:internal build-spec
  "Converts a `with-fake-github`-style spec (a sequence of request/response
   pairs) into a value suitable for `org.httpkit.fake/with-fake-http`.

   Do not call directly."
  [spec]
  (into [(str github-url "app/installations") "{}"]
        (mapcat (fn [[req resp]] [(request-spec req) (response-spec resp)]))
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
  `(fake/with-fake-http
     (build-spec ~(mapv #(if (-> % meta :path) `{:path ~%} %) spec))
     ~@body))
