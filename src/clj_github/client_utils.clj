(ns clj-github.client-utils
  (:require [clj-github.httpkit-client :as client]))

(defn fetch-body! [client request]
  (:body (client/request client request)))
