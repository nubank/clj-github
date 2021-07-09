# Releasing

Anybody with write access to this repository can release a new version and deploy it to Clojars. To do this, first make sure your local main branch is sync'd with main on github:

```bash
git checkout main
git pull
```

Now run this command:
```
./release.sh
```

The `release.sh` script creates a git tag with the project's current version and pushes it
to github. This will trigger a GithubAction that tests and uploads JAR files to
Clojars.

### Credentials

There is a [github secret][ghsecret] named `CLOJARS_DEPLOY_TOKEN` that contains a [clojars token][clojars_token].

[ghsecret]: https://github.com/nubank/clj-github/settings/secrets/actions
[clojars_token]: https://github.com/clojars/clojars-web/issues/726
