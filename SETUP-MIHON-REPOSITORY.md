# Set up signing and install the Mihon repository

This project builds a signed extension APK and publishes a Mihon v2 repository index from GitHub Actions.

## 1. Create a permanent signing key

Install a JDK with keytool, then run this on your own computer:

\`\`\`bash
keytool -genkeypair -v \\
  -keystore junji-ito-mihon.jks \\
  -alias junji-ito \\
  -keyalg RSA -keysize 2048 -validity 10000
\`\`\`

Choose and securely store the keystore and key passwords. **Keep this keystore permanently**: future updates must be signed with the same key or Mihon will reject them as a different publisher. Never commit the keystore or passwords to Git.

## 2. Add GitHub Actions secrets

Open this repository's **Settings → Secrets and variables → Actions → New repository secret** and add:

| Secret name | Value |
|---|---|
| SIGNING_KEY | Base64 encoding of the complete junji-ito-mihon.jks file |
| ALIAS | junji-ito (or the alias you chose) |
| KEY_STORE_PASSWORD | Keystore password |
| KEY_PASSWORD | Key password |

On macOS, copy the Base64 value without line breaks:

\`\`\`bash
base64 < junji-ito-mihon.jks | tr -d '\\n' | pbcopy
\`\`\`

Paste the copied value as the SIGNING_KEY secret. On Linux, use:

\`\`\`bash
base64 -w 0 junji-ito-mihon.jks
\`\`\`

Do not paste secrets into chat, issues, commits, or workflow logs.

## 3. Run the build

After all four secrets exist, open **Actions → Build and publish Mihon extension → Run workflow**. The workflow checks out the current Keiyoushi build system, overlays this source module, runs release lint and assembles the signed release APK/JAR, then publishes a versioned GitHub Release and regenerates index.pb, index.json, and repo.json.

The first successful run is required before the repository URL can be added to Mihon.

## 4. Add the repository to Mihon

After the workflow succeeds, add this repository metadata URL in Mihon:

\`\`\`
https://raw.githubusercontent.com/saefulrohman/junji-ito-mihon-extension/main/repo.json
\`\`\`

If your Mihon build does not accept the repo.json URL directly, use the raw v2 index URL:

\`\`\`
https://raw.githubusercontent.com/saefulrohman/junji-ito-mihon-extension/main/index.pb
\`\`\`

Mihon repository support and UI can vary by app version. Follow Mihon's repository-add flow and approve the signing fingerprint shown for this repository when prompted.

## 5. Updates

Increment versionCode in src/id/junjito/build.gradle.kts whenever you change extension behavior. Push the change to main; GitHub Actions builds and publishes the next version automatically. Keep using the exact same signing key.

## Current limitations

- The source module and workflow are committed, but a successful Android build and device test are not yet confirmed.
- The first workflow run requires the four signing secrets above.
- Do not delete or replace the signing keystore after publishing the first release.
