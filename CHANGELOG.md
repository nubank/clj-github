# Changelog

## 0.8.0

- Revise the `with-fake-github` macro to support all the options of the underlying `with-fake-http` macro

## 0.7.1

- Fix decoding bug introduced in 0.7.0
  - Don't try to base64 decode content if nil

## 0.7.0

- Fix decoding bug in `get-content!`
- Add `get-content-raw` function that decodes file's content to a byte array
- Support providing a token via `gh auth token`
- Upgrade dependencies
  - Upgrade nubank/clj-github-app from 0.2.1 to 0.3.0
- Mark optional dependencies with scope provided
  - clj-commons/clj-yaml
  - http-kit.fake
  - dev.nubank/clj-github-mock

## 0.6.5

- Non-success status codes should not always result in a thrown exception

## 0.6.4
- Encode ref parameter in update-reference! from repository.clj namespace

## 0.6.3
- Fix special characters on branch name

## 0.6.2
- Bump `clj-github-app` version

## 0.6.1
- Bump `clj-github-app` version

## 0.6.0
- Function to create blob objects and add in the commit other files than text files

## 0.5.1

- Fix `clj-github.check/update-check-run!` overloads with same arity

## 0.5.0

- Functions to create and delete a label from an issue, merge a pull request, and new namespace to work with check runs via GitHub API.

## 0.4.2

- Add application/json content-type to mocked responses if none is provided.

## 0.4.1

- Include httpkit error in exception when available

## 0.4.0
- Add function that downloads repository content

## 0.3.0
- Support content types other than json

## 0.2.2
- Fix breaking change in exception result

## 0.2.1
- Avoid that exceptions leak token

## 0.2.0
- Bump some libs

## 0.1.1
- Allow passing expressions (along with map values) to the `clj-github.state-flow-helper/mock-github-flow` macro

## 0.1.0
- Initial version
