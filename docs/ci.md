# CI runners

CI runs on **GitHub-hosted runners**. Only when those cannot start (for example the account's Actions minutes are used
up) do the jobs fall back to a **self-hosted macOS (Apple silicon) runner** with the labels `self-hosted`, `macOS`,
`ARM64` (ADR 0025). The first job, `choose-runner`, decides: if it fails to start on `ubuntu-latest`, the others pick
the self-hosted labels automatically.

To force a choice, set the repository variables `RASOI_RUNNER` and `RASOI_IOS_RUNNER` to JSON label lists, for example
`["self-hosted","macOS","ARM64"]` for both (Settings, Secrets and variables, Actions, Variables). Do this if hosted jobs
queue forever instead of failing, or while hosted runners are unavailable for a while: a `choose-runner` job that cannot
start shows as a red failed check on every commit, even though the run as a whole passes. With the variables set it is
skipped instead (`Lint` needs only `RASOI_RUNNER`). Delete the variables to return to the default.

CI runs on `push` (and manually). It does not also listen to `pull_request`: the open pull request shows the same checks
for its head commit, and a second trigger would run every job twice.

## Secrets

| Secret | Used by | Content |
| --- | --- | --- |
| `GOOGLE_SERVICES_JSON` | CI Android builds, Lint | The Firebase Console's `google-services.json` for `app.rasoi`, written to `androidApp/` before Gradle runs. Optional: without it the app builds with Firebase unconfigured. Set it with `gh secret set GOOGLE_SERVICES_JSON < androidApp/google-services.json`. |
| `RASOI_KEYSTORE_BASE64` | CI Android release | The Android upload keystore, base64: `base64 -i ~/rasoi-upload.jks \| gh secret set RASOI_KEYSTORE_BASE64`. Optional: without it the release is unsigned. |
| `RASOI_KEYSTORE_PASSWORD`, `RASOI_KEY_ALIAS`, `RASOI_KEY_PASSWORD` | CI Android release | The keystore password, key alias (`rasoi-upload`) and key password (the same as the keystore's for a PKCS12 keystore). |

Repository variable `RASOI_API_BASE_URL` (an `https://` address): the CI release build points at it and is kept as the
`rasoi-release-apk` artifact for 14 days. Without it the release points at a placeholder and is not kept.

## Screenshots

Goldens are Linux renderings. The recorder workflow always runs on `ubuntu-latest`, and the self-hosted fallback skips
screenshot verification (it prints a notice) because macOS draws text differently. New goldens therefore need hosted
runners.

## One-time setup of the Mac

This only matters for the fallback. The workflows install Java 21 and the Android SDK themselves. Everything else must exist on the machine:

| Needed | Why | How |
| --- | --- | --- |
| Xcode, with the iOS simulator runtime, and its licence accepted | iOS tests and app build | App Store or developers.apple.com, then `sudo xcodebuild -license accept` and `xcodebuild -downloadPlatform iOS` |
| Homebrew | installs `xcodegen` on first iOS run | https://brew.sh |
| Docker that the runner user can reach | server tests start PostgreSQL with Testcontainers | Docker Desktop (start it and let it launch at login), or Colima: `brew install colima docker && colima start`, then give the runner `DOCKER_HOST=unix://$HOME/.colima/default/docker.sock` and `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` in the runner's `.env` file |
| Python 3 | resource guard script | comes with the Xcode command line tools |
| Git | checkout | comes with the Xcode command line tools |

Run the runner as a normal user session (`./run.sh`, or `./svc.sh install` as a LaunchAgent) so Docker and Xcode are
available to it. Jobs run one at a time on one runner, so a full CI run takes the sum of its three jobs. A job fails
fast with a clear message if Docker or Xcode is missing.

## Security

A self-hosted runner executes whatever a workflow runs. Keep the repository private, or require approval for workflow
runs from outside contributors (Settings, Actions, General), and do not keep personal files or credentials on the
machine.
