# TeaDroid

A Material 3 Expressive Android client for Gitea. You choose your server on first launch.
Built with Kotlin, Jetpack Compose and Material3 1.5 (expressive APIs), against the Gitea REST API (/api/v1).

## Features
- Sign in with an access token, or username + password (creates a device token; 2FA supported)
- Home: stats, recent repos, activity feed
- Repos: mine / starred, create, search repos and users; browse code by branch, view files (text, images, Markdown), README
- Issues and pull requests: cross-repo filters (assigned, created, mentioned, review requested), detail with comments, comment, close/reopen, merge, create issue
- Commits and releases per repo; inbox (notifications) with mark-as-read
- Settings: theme (system/light/dark), dynamic color; token stored encrypted with an Android Keystore key

## Build
Needs JDK 17+ and the Android SDK (platform 37). Set sdk.dir in local.properties, then:

    gradlew assembleDebug

APK: app/build/outputs/apk/debug/app-debug.apk
