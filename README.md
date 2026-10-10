# Mira for Android

An independent, native Android music companion for Spotify with a Material Expressive interface. It is the phone version of [Mira](https://github.com/LilaccFlowerr/Mira-player), rewritten in Kotlin with Jetpack Compose.

Mira does not play audio itself. The Spotify app on your phone plays the music in the background and Mira is the interface: browse, search, control playback and read lyrics.

## Features

- **Library and playlists**: Liked songs, your playlists and albums, with play, shuffle, filter and "load more".
- **Search** for songs, artists, albums and playlists at once, with recent searches.
- **Player bar and full player** with a wavy progress line that waves while music plays and settles when paused. Tap or drag to seek.
- **Lyrics**, synced line by line, from [LRCLIB](https://lrclib.net). Tap a line to jump to it.
- **Playback without opening Spotify**: Mira talks to the Spotify app through the Spotify App Remote SDK.
- **Phone volume slider** that follows the volume buttons.
- **Offline notice** when the phone has no internet, and automatic recovery afterwards.
- **Customisation**: system, light or dark theme, wallpaper colours (Material You), true black for AMOLED, seven accent colours or any hue, corner roundness, font, text size, and switches for the wavy progress, the blurred player background, lyrics and the genre shortcuts.

## What you need

- An Android phone (Android 8.0 / API 26 or newer).
- The **Spotify app**, installed and logged in.
- **Spotify Premium**. Spotify only allows remote control of playback for Premium accounts.
- Your **own free Spotify Developer app** (below). Spotify limits apps in development mode to a few users, so every person uses their own Client ID.

## Setup

### 1. Create your Spotify app

1. Go to the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard), log in and click **Create app**.
2. Fill in any name and description.
3. Add this **Redirect URI** and click **Add**:

   ```
   mira://callback
   ```
4. Under **Which API/SDKs are you planning to use?** tick **Web API** and **Android**.
5. Open the app's **Settings** and, under **Android Packages**, add the package name and the SHA-1 fingerprint of the key that signs your build:

   | Field | Value |
   |---|---|
   | Package name | `com.izuuhh.mira` |
   | SHA-1 | see below |

   For a debug build, print the fingerprint with:

   ```sh
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
   ```
6. Under **User Management**, add the email address of your Spotify account (needed while the app is in development mode).
7. Copy the **Client ID**. You do not need the client secret; never put it in Mira.

### 2. Build and install

You need [Android Studio](https://developer.android.com/studio) or just a JDK 17+ and the Android SDK.

```sh
git clone git@github.com:LilaccFlowerr/Mira-Android.git
cd Mira-Android
./gradlew installDebug      # builds and installs on a connected phone
```

Without Android Studio, point Gradle at its JDK or any JDK 17+:

```sh
export JAVA_HOME=/opt/android-studio/jbr
```

To connect your phone, turn on **Developer options** and **USB debugging** (on Samsung: Settings → About phone → Software information → tap *Build number* seven times), plug it in and check `adb devices`.

### 3. Connect Mira

1. Open Mira and go to **Settings**.
2. Paste your **Client ID** and tap **Connect Spotify**. Log in in the browser and agree.
3. Pick a song. The first time, Spotify shows a screen asking you to **allow Mira**; tap *Agree*. After that, music starts without Spotify coming to the front.

## How it works

| Part | Used for |
|---|---|
| Spotify **Web API** (PKCE sign-in, no client secret) | Library, playlists, search, now-playing info, devices |
| Spotify **App Remote SDK** | Starting and controlling playback in the background |
| Spotify **Auth library** | The one-time "allow Mira" screen |
| [LRCLIB](https://lrclib.net) | Lyrics (receives the song and artist name; can be switched off) |

The refresh token is stored encrypted with a key in the Android Keystore. Mira has no server and no analytics.

If App Remote is not available (for example the Spotify app is missing), Mira falls back to controlling the Spotify app as a Connect device through the Web API.

## Project layout

```
app/src/main/java/com/izuuhh/mira/
├── MainActivity.kt          sign-in redirect, Spotify consent, edge to edge
├── MiraViewModel.kt         state and actions
├── Navigation.kt            pages and one-off events
├── data/                    Spotify API, auth, token store, App Remote, lyrics, prefs
└── ui/
    ├── MiraApp.kt           main layout and page transitions
    ├── theme/               colours derived from one hue, corners, fonts
    ├── components/          cover, buttons, wavy progress, loading indicator, shapes
    ├── screens/             home, playlists, track lists, settings
    └── player/              player bar, full player, lyrics, device sheet
app/libs/                    Spotify App Remote and Auth libraries (.aar)
```

## Not (yet) included

- Own lock screen and notification controls. Spotify's own media notification already controls playback.
- The session timer from the desktop version.
- iOS.

## Notices

Independent project, not made, endorsed or sponsored by Spotify. Spotify is a trademark of Spotify AB. Album art and metadata are provided by Spotify. Using the Spotify SDKs is subject to the [Spotify Developer Terms](https://developer.spotify.com/terms).

Built with Jetpack Compose, [OkHttp](https://square.github.io/okhttp/), [Coil](https://coil-kt.github.io/coil/), [androidx.graphics:graphics-shapes](https://developer.android.com/jetpack/androidx/releases/graphics) and the [Spotify Android SDK](https://github.com/spotify/android-sdk) (Apache 2.0). The loading indicator is adapted from the one in the desktop version.
