# Changelog

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
