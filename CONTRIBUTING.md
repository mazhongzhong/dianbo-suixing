# Contributing

Thank you for contributing to UK Radio Companion.

## Submitting changes

1. Fork the repository and create a feature branch from the default branch.
2. Keep each change focused. Keep build artifacts, signing keys, credentials, account details, and local SDK paths out of commits.
3. Run the following checks before submitting:

   ```bash
   ./gradlew test lintDebug assembleDebug
   ```

4. In the pull request, explain what changed, why it changed, how it was verified, and any compatibility considerations. Include screenshots for interface changes.

## Code and content guidelines

- Follow the existing Kotlin and Android resource structure.
- Add meaningful tests for stream parsing, time handling, and fallback behavior.
- Review service terms, distribution rights, and naming requirements before adding a station.
- Use artwork, programme images, audio, and other media only when their licenses permit redistribution.
- Keep signing keys and real credentials out of the repository.

## Reporting issues

Use GitHub Issues for reproducible defects. Include the Android version, app version, affected station, reproduction steps, and relevant logs. Remove account details, IP addresses, tokens, and other sensitive information from logs before posting.
