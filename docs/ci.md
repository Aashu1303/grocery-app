# CI on a self-hosted runner

GitHub Actions runs on a self-hosted **macOS (Apple silicon)** runner with the labels `self-hosted`, `macOS`, `ARM64`
(ADR 0025). Workflows pick their runner from the repository variables `RASOI_RUNNER` (guard, check and screenshot
recording) and `RASOI_IOS_RUNNER` (iOS job); both default to those labels. To use GitHub-hosted runners again, set
`RASOI_RUNNER` to `["ubuntu-latest"]` and `RASOI_IOS_RUNNER` to `["macos-latest"]` (Settings, Secrets and variables,
Actions, Variables), then re-record the screenshots (see below).

## One-time setup of the Mac

The workflows install Java 21 and the Android SDK themselves. Everything else must exist on the machine:

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

## Screenshot goldens belong to the runner type

Roborazzi compares pixels, and text rendering differs between Linux and macOS. Goldens are recorded and verified on the
same kind of runner. After changing `RASOI_RUNNER` to a different OS, push a commit whose message contains
`[record screenshots]` and review the diff.

## Security

A self-hosted runner executes whatever a workflow runs. Keep the repository private, or require approval for workflow
runs from outside contributors (Settings, Actions, General), and do not keep personal files or credentials on the
machine.
