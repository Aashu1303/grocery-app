# CI runners

CI runs on **GitHub-hosted runners**. Only when those cannot start (for example the account's Actions minutes are used
up) do the jobs fall back to a **self-hosted macOS (Apple silicon) runner** with the labels `self-hosted`, `macOS`,
`ARM64` (ADR 0025). The first job, `choose-runner`, decides: if it fails to start on `ubuntu-latest`, the others pick
the self-hosted labels automatically.

To force a choice, set the repository variables `RASOI_RUNNER` and `RASOI_IOS_RUNNER` to JSON label lists, for example
`["self-hosted","macOS","ARM64"]` for both (Settings, Secrets and variables, Actions, Variables). Do this if hosted jobs
queue forever instead of failing. Delete the variables to return to the default.

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
