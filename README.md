<p align="center">
  <img src="docs/logo.svg" width="128" alt="TeaDroid logo">
</p>

<h1 align="center">TeaDroid</h1>

<p align="center">
  A Material 3 Expressive Android client for <a href="https://about.gitea.com">Gitea</a>.<br>
  Built with Kotlin and Jetpack Compose. Point it at your own server on first launch.
</p>

> **Status: alpha.** Expect rough edges. Bug reports and feedback are welcome via issues.

## Features

- **Sign in** with username and password (creates a device token, 2FA supported) or an access token
- **Home** with your stats, recently updated repos and activity feed
- **Repositories**: mine and starred, create new, search repos and users
- **Code browser**: branches, files (text, images, Markdown) and rendered READMEs
- **Issues and pull requests**: filter across repos, read and post comments, close or reopen, merge, create issues
- **Commits and releases** for every repo
- **Inbox** for notifications, with mark as read
- **Material 3 Expressive** UI with dynamic color and light, dark or system themes

Your token is stored encrypted with an Android Keystore key. TeaDroid only talks to the server you enter.

## Install

Download the latest APK from [Releases](https://github.com/colebolebole/TeaDroid/releases) and open it on your phone.
Requires Android 8.0 or newer (Android 12 or newer for dynamic color).

## Build

You need JDK 17+ and the Android SDK (platform 37). Put the SDK path in `local.properties`:

```properties
sdk.dir=/path/to/Android/sdk
```

then run:

```sh
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Tech

Kotlin, Jetpack Compose, Material 3 (expressive APIs), Navigation Compose, OkHttp, kotlinx.serialization and Coil.
It uses the Gitea REST API (`/api/v1`).
