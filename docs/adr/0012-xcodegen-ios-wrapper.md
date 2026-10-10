# 0012. iOS wrapper generated with XcodeGen

Status: Accepted
Date: 2026-10-10

## Context
`iosApp/` must host the shared Compose framework. A hand-maintained `project.pbxproj` is large, merge-hostile and
cannot be authored or reviewed meaningfully outside Xcode.

## Decision
`iosApp/project.yml` describes the Xcode project for XcodeGen; the generated `iosApp.xcodeproj` and `Info.plist` are
git-ignored. A pre-build phase runs `:composeApp:embedAndSignAppleFrameworkForXcode`. CI on macOS generates the
project and runs `xcodebuild` for the simulator on every push.

## Consequences
Developers run `xcodegen generate` once after cloning or after editing `project.yml`. Project changes are small,
readable YAML diffs.
