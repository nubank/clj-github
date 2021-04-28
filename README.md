# clj-github
A Clojure library for interacting with the github developer API.

Note: while this library is being used for several production use cases, we're still ironing out the APIs, so they are subject to change.

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

When create a client you can use a number options to determine how it will obtain the app
credentials.

When looking for credentials the client will by default first:

 1. Look for your personal token at `~/.config/hub` (this is the configuration file of `hub` tool).
 2. Look for an environment variable named `GITHUB_TOKEN`.

#### `:app-id` + `:private-key`

If you have the credentials stored in a different place, you can pass them directly to the client.
The client will always use them and will not fallback to local configurations.

#### `:token`

For quick test you can also pass a token directly to the client. The client will use it and not
try to fetch one from the app.

#### `:token-fn`

If you have special needs, you can pass a function without parameters, the client will always
call that function when it makes a request.

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
calls to github.

Example:
```clojure
(with-fake-github ["/repos/nubank/clj-github/pulls/1" {:body (misc/write-json {:attr "value"})}]
  (github-client/request (github-client/new-client) {:path "/repos/nubank/clj-github/pulls/1"}))
```

The macro receives pairs of `request` and `response` forms.

A request can be a:

* `String`: it should contain an endpoint that you want to match. The value should only
contain the path of the endpoint, without the `https://api.github.com` url.
* `Map`: it can contain a complete request map specification.
* `regex`: the request will match if its url matches the request.
* `function`: a function with one argument, the request map is passed to the function,
if the function returns a truthy value, the request will match.

Provided a request is matched, the corresponding response will be returned. A response can
be a:

* `String`: it will be returned as the body of the response, the response will have a status 200.
* `Map`: it will be returned as the response, some values are automatically added as default
(e.g. status will be 200 if not specified).
* `Integer`: a response with the given status code will be returned.

Notes:

* The macro does not work with the http client component. You can just its own mocking facility.
* The macro is based on [`httpkit.fake`](https://github.com/d11wtq/http-kit-fake) library, to make it work you need to add it as a development dependency of your project. You can look at `httpkit.fake documentation` for more clear explanation of how to specify requests and responses.

### Running tests

To run tests you can use

```
lein test
```

## examples

If you'd like an example of this library in action, check out [`ordnungsamt`](https://github.com/nubank/ordnungsamt), which is a tool for applying ad-hoc code migrations to a set of github repositories and subsequently opening pull requests for them.
