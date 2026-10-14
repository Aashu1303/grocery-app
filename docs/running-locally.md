# Running Rasoi locally, and what production needs

Everything below was checked against this repository's configuration files. The server runs and is tested end to end
(`./gradlew :server:app:test` drives the real HTTP API on PostgreSQL). The Android and iOS apps are compiled and
tested in CI; they have not been run on a device in the environment that built them, so treat the first device run as
the real test.

## 1. Run the backend and the app on your machine

**You need:** JDK 17 or newer, Docker, and for the apps Android Studio (SDK and an emulator) and/or Xcode with
`xcodegen` (`brew install xcodegen`).

1. **Start PostgreSQL**
   ```sh
   docker run --name rasoi-db -e POSTGRES_USER=rasoi -e POSTGRES_PASSWORD=rasoi -e POSTGRES_DB=rasoi \
     -p 5432:5432 -d postgres:16
   ```
2. **Start the server in dev mode.** Dev mode accepts the app's built-in `dev:<phone>` tokens, so no Firebase project
   is needed. The schema is created by Flyway on start.
   ```sh
   export DATABASE_URL=jdbc:postgresql://localhost:5432/rasoi DATABASE_USER=rasoi DATABASE_PASSWORD=rasoi
   export RASOI_AUTH_MODE=dev PORT=8080
   ./gradlew :server:app:run
   curl http://localhost:8080/health      # {"status":"ok", ...}
   ```
   Invoice PDFs are stored under `data/documents` (set `RASOI_STORAGE_DIR` to change it; the folder is git-ignored).
3. **Optional: seed a demo flat** (needs an empty database). It runs the whole demo scenario against your server:
   ```sh
   RASOI_DEMO_URL=http://localhost:8080 ./gradlew :server:app:seedDemo
   ```
   Then sign in to the app as **+91 98000 00001**, code **123456** (the dev provider accepts that code for any
   number). The seed also closes the current month, so the Balances tab shows a closed ledger with a paid transfer.
4. **Optional: let unknown invoice formats be read by an LLM.** Set all three or none:
   `RASOI_LLM_BASE_URL` (OpenAI-compatible, e.g. `https://api.openai.com/v1`), `RASOI_LLM_API_KEY`, `RASOI_LLM_MODEL`.
   Only the invoice's line-item table is sent, with personal details removed.
5. **Android**
   - Emulator: `./gradlew :androidApp:installDebug`. The default API address `http://10.0.2.2:8080` is the emulator's
     name for your machine.
   - Physical phone on the same Wi-Fi: `./gradlew :androidApp:installDebug -Prasoi.apiBaseUrl=http://<your-LAN-IP>:8080`.
   Debug builds allow plain HTTP.
6. **iOS** (macOS only): `cd iosApp && xcodegen generate && open iosApp.xcodeproj`, run on a simulator. The API
   address is the `RASOI_API_BASE_URL` build setting in `iosApp/project.yml` (default `http://localhost:8080`).
7. **First run:** sign in with any phone number and code `123456`, create a household, invite flatmates (share the
   code; sign in as another number on a second emulator or just use the seeded users), then import an invoice PDF.

On a machine without Google Maven or Kotlin/Native access, see "Platform switches" in `AGENTS.md`.

## 2. Configuration for a production-like deployment

### Server (environment variables only; nothing secret is committed)
| Variable | Production value |
| --- | --- |
| `RASOI_AUTH_MODE` | `firebase` (the default). Never `dev` outside your laptop: dev mode lets anyone sign in as anyone. |
| `FIREBASE_PROJECT_ID` | Your Firebase project id; the server verifies ID tokens against Google's public keys. |
| `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` | A managed PostgreSQL (16 recommended) with backups. Migrations run automatically on start. |
| `PORT` | The port your reverse proxy forwards to. |
| `RASOI_STORAGE_DIR` | A directory on a **persistent volume** (invoice PDFs live there). S3-compatible storage is in the design plan but not built yet. |
| `SESSION_TTL_DAYS` | Session lifetime, default 30. |
| `RASOI_VERSION` | Shown by `/health`; set it to your release. |
| `RASOI_LLM_*` | Optional, as above. The same chat model also powers the Kitchen tab's dish ideas; without it that feature says it is off. |
| `RASOI_SWEEP_TOKEN` | Optional, at least 32 characters. When set, an external scheduler runs the 15-minute sweeps through `POST /internal/sweeps` with this value in `X-Rasoi-Sweep-Token`, and the in-process timer is off. Needed on hosts that pause idle CPU (Cloud Run; ADR 0028). |
| `FCM_SERVICE_ACCOUNT_FILE` | Optional path to a Firebase service-account JSON file (keep it outside the repo). With it the server sends approval and expiry push notifications; without it nothing is sent. |

Run it behind a TLS-terminating proxy (HTTPS only), run one instance (the 15-minute sweeps that auto-approve bills and
close months are safe to repeat, but only one process is tested), and back up the database and the storage folder.

### Cloud Run (the hosted setup, ADR 0028)
One-time: install the Google Cloud CLI, `gcloud auth login`, put the Firebase project on the Blaze plan, and store
the Neon password with `gcloud secrets create rasoi-database-password --data-file=<file with only the password>`.
Then every deploy is:
```sh
RASOI_DATABASE_URL='jdbc:postgresql://<neon host>/rasoi?sslmode=require' RASOI_DATABASE_USER=neondb_owner \
  ./scripts/deploy-cloud-run.sh
```
It builds the image with Cloud Build, deploys the current commit, schedules the sweeps and checks `/health`. To turn on the AI fallback, create a Groq key at console.groq.com and store it once with
`gcloud secrets create rasoi-llm-api-key --data-file=<file with only the key>`; the next deploy picks it up.

### Server image
`docker build -t rasoi-server .` builds the server only. Run it with the variables above, for example
`docker run -p 8080:8080 -v rasoi-data:/data -e DATABASE_URL=... -e FIREBASE_PROJECT_ID=... rasoi-server`. It runs as a
non-root user and keeps invoice PDFs in the `/data` volume.

### Apps
- **API address:** Android `-Prasoi.apiBaseUrl=https://api.example.com` at build time; iOS `RASOI_API_BASE_URL`. A
  release build refuses any address that is not `https://` (ADR 0024).
- **Android release.** Create a Firebase project, enable **Phone** sign-in, add the Android app (`app.rasoi`) with your
  upload key's SHA-1 and SHA-256 (`keytool -list -v -keystore ~/rasoi-upload.jks`), and download its
  `google-services.json` into `androidApp/` (it is git-ignored; the google-services plugin reads it). Create the upload
  key once with `keytool -genkeypair -keystore ~/rasoi-upload.jks -alias rasoi-upload -keyalg RSA -keysize 4096
  -validity 10000`, keep it outside the repo and back it up with its password. Then put this in
  `~/.gradle/gradle.properties` (never in the repo):
  ```properties
  rasoi.signing.storeFile=/Users/you/rasoi-upload.jks
  rasoi.signing.storePassword=...
  rasoi.signing.keyAlias=rasoi-upload
  rasoi.signing.keyPassword=...
  ```
  and build with `./gradlew :androidApp:bundleRelease -Prasoi.apiBaseUrl=https://api.example.com`. The
  `RASOI_KEYSTORE_FILE`, `RASOI_KEYSTORE_PASSWORD`, `RASOI_KEY_ALIAS` and `RASOI_KEY_PASSWORD` environment variables
  work too (CI uses them). Without a key the release is unsigned; without `google-services.json` sign-in reports an
  error. The release build has no development sign-in.
- **iOS release.** In the same Firebase project add the iOS app (`app.rasoi`), upload an APNs key (Project settings,
  Cloud Messaging) and download `GoogleService-Info.plist` into `iosApp/iosApp/` (it is git-ignored). Put its
  `REVERSED_CLIENT_ID` in the `RASOI_FIREBASE_REVERSED_CLIENT_ID` build setting. Then build the Release configuration
  with your team and `RASOI_API_BASE_URL=https://api.example.com`:
  `xcodebuild -scheme iosApp -configuration Release RASOI_API_BASE_URL=https://api.example.com DEVELOPMENT_TEAM=... CODE_SIGNING_ALLOWED=YES`.
  Debug builds keep the development sign-in.
- **Server for real sign-in:** `RASOI_AUTH_MODE=firebase` and `FIREBASE_PROJECT_ID` as above.

### What is not production-ready yet
1. **Real phone sign-in is built but unproven.** It compiles in CI, and the unconfigured paths are tested, but it has
   never run against a Firebase project (there is none in this repository). Expect to debug the first real sign-in.
2. **Push on the phones.** The server side is built (`POST /devices`, FCM sender, expiry sweep), but the apps do not
   yet obtain an FCM token or register it, so no phone receives pushes until that client work is done (ADR 0024).
3. **Shrinking (R8)** is off in release builds until keep rules are tested on a device.
4. **File storage** is a local folder, not S3.
