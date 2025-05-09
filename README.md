# clj-github

A Clojure library for interacting with the GitHub REST API.

## Httpkit client


*Example*:
```clojure
(require '[clj-github.httpkit-client :as github-client])

(def client (github-client/new-client {:app-id "app-id" :private-key "private-key"}))

(github-client/request client {:path "/api/github/..."
                                :method :get})
```

The client will automatically get the token necessary to call github. The
request map passed to the client must follow http-kit's format (see: http://http-kit.github.io/).
There is a special `:path` attribute that can be used instead of `:url`
that the client will automatically convert to a url with the github address.

### Credentials options

When creating a client you can use a number of options to determine how it will obtain the app
credentials.

#### `:app-id` + `:private-key`

The client uses the provided app ID and private key to generate an [installation access token](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-an-installation-access-token-for-a-github-app)
for a GitHub App.

The generated token is cached and will be automatically refreshed when needed.

`:private-key` must be a PEM encoded string.

#### `:token`

The client uses the provided hardcoded token string. Useful for experimentation, but not recommended for production 
workloads.

#### `:token-fn`

You can provide an arbitrary zero-argument function that when invoked returns a valid token string.

Some common token functions are available in `clj-github.token`, and `clj-github.token/chain` can
be used to try multiple token functions in order.

In the example below, the chain will look for:
1. an environment variable named `GITHUB_TOKEN`.
2. a token managed by `gh` CLI (by running `gh auth token`)
3. a personal token at `~/.config/hub` (this is the configuration file of `hub` tool)

```clojure
(require '[clj-github.token :as token])

(github-client/new-client {:token-fn (token/chain [token/env-var
                                                   token/gh-cli
                                                   token/hub-config])})
```

### Managing repositories

The `clj-github.repository` namespace provides auxiliary functions to call the api to manage repositories.

Additionally the `clj-github.changeset` namespace provides a functional api to make commits.

For example to change the contents of a file and commit them to a new branch, one would could do:

```clojure
(-> (changeset/from-branch! client "nubank" "some-repo" "main")
    (changeset/put-content "some-file" "some-content")
    (changeset/commit! "commit message")
    (changeset/update-branch!))
```

`clj-github.changeset` docstring provides more details on how to use it.

## Testing

### Helpers

The `clj-github.test-helpers` provides a macro that can be used to mock
calls to the GitHub REST API. 

Example:
```clojure
(with-fake-github ["/repos/nubank/clj-github/pulls/1" {:body (cheshire.core/generate-string {:attr "value"})}]
  (github-client/request (github-client/new-client) {:path "/repos/nubank/clj-github/pulls/1"}))
```
 
`with-fake-github` is a wrapper around the [`httpkit.fake`](https://github.com/d11wtq/http-kit-fake) library, 
specifically the `org.httpkit.fake/with-fake-http` macro.

As with `with-fake-http` the behavior is specified as request and response pairs.
The request forms are used to identify which requests are matched against which response forms.

A request form may be one of:

* `String`: sets the _path_ of the endpoint to match
* `Map`: a complete request map specificiation; all keys in the map must exactly match the corresponding keys of the request map
* `regex`: matches if the reqex matches the :url of the request
* `function`: matches if the function, passed the request map, returns truthy

With a Map, the :path (if present) is prefixed to form the final :url attribute.

Note: normally, if you compute the value of the path, e.g., `(str "/repos/" repo-name "/pulls/" pull-number)`, the
computed value will be passed to `with-fake-http` and interpretted as the _URL_.  Apply the meta-data :path to the
expression so that `with-fake-github` can treat the computed value the same as a literal string: as the path from which
the URL is computed.  Example: `^:path (str "/repos/" repo-name "/pulls/" pull-number)`.

Provided a request is matched, a full response is generated from the corresponding response form.

A response form can be one of:

* `String`: it will be returned as the body of the response, the response will have a status 200
* `Map`: it will be returned as the response, some values are automatically added as default
(e.g. status will be 200 if not specified)
* `Integer`: a response with the given status code will be returned
* `function`: See the `with-fake-http` macro documentation for this advanced usage

In addition, the values :allow and :deny are supported.

The response content type `application/json` is automatically applied.
Response bodies must be JSON values encoded as strings, which will be parsed back to EDN data.

Example:
```clojure
(deftest response-may-be-a-JSON-encoded-string
  (is (match? {:status 200
               :body   {:solution 42}}
              (with-fake-github ["/api/answer" (json/generate-string {:solution 42})]
                                (request "/api/answer")))))
```


### Running tests

To run tests you can use

```
lein test
```

