# UK Radio Companion

A lightweight Android player for BBC radio, designed for listeners outside the UK. It currently supports BBC World Service, Radio 1, Radio 2, Radio 3, Radio 4, Radio 5 Live, and Radio 6 Music.

> UK Radio Companion is an independent, unofficial open-source project. It is not affiliated with, authorized by, or endorsed by the BBC.

## Features

- Live playback for major BBC radio stations.
- Current programme details, start time, expected end time, and synopsis on the home screen.
- Upcoming schedules displayed in the device's local time zone.
- An animated equalizer that reflects playing, paused, connecting, and error states.
- Background playback, lock-screen controls, headset controls, and system media notifications.
- An optional setting that keeps the screen awake during playback.

## Technology

- Kotlin
- AndroidX Media3 / ExoPlayer
- `MediaSessionService`
- BBC Media Selector and RMS schedule endpoints
- DASH and HLS live streaming

The player resolves current stream endpoints dynamically and uses fallback endpoints when needed.

## Build

Requirements:

- JDK 17
- Android SDK Platform 36
- Android SDK Build-Tools 36.0.0

The Gradle Wrapper is included. Open the project directory in Android Studio, or run:

```powershell
./gradlew.bat test lintDebug assembleDebug
```

On Linux or macOS:

```bash
./gradlew test lintDebug assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy

The selected station and keep-screen-on preference are stored locally on the device. Streaming and schedule requests connect directly to BBC services and their content delivery networks. See [PRIVACY.md](PRIVACY.md) for details.

## Contributing

Issues and pull requests are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) before contributing. Report security issues privately as described in [SECURITY.md](SECURITY.md).

## License and third-party content

The project source code is available under the [Apache License 2.0](LICENSE).

The license applies only to source code contributed to this repository. BBC programmes, audio, schedule metadata, names, logos, trademarks, and third-party services remain the property of their respective owners. Users and distributors are responsible for following applicable laws, BBC terms, content licenses, and app-store policies.
