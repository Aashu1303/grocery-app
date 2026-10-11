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
| `RASOI_LLM_*` | Optional, as above. |

Run it behind a TLS-terminating proxy (HTTPS only), run one instance (the 15-minute sweeps that auto-approve bills and
close months are safe to repeat, but only one process is tested), and back up the database and the storage folder.

### Apps
- **API address:** Android `-Prasoi.apiBaseUrl=https://api.example.com` at build time; iOS `RASOI_API_BASE_URL` in
  `iosApp/project.yml`. Use HTTPS; plain HTTP is only meant for local development.
- **Android release:** add your own signing configuration and build a release bundle. The repository does not hold
  keystores.
- **iOS release:** set your team, bundle id and signing in Xcode.

### What is not production-ready yet
These are the gaps between "runs locally" and "ships", all from milestones not built yet:
1. **Real phone sign-in.** The apps only contain the development sign-in provider (ADR 0016), which the production
   server rejects. You need a Firebase project with Phone sign-in enabled, `google-services.json` (Android) and
   `GoogleService-Info.plist` (iOS), release SHA-1/APNs/reCAPTCHA setup, and the two production `PhoneAuthProvider`
   implementations (planned for M7). Release builds must not ship the dev provider.
2. **Push notifications** (approval and expiry alerts) need FCM and are planned for M6.
3. **Expiry alerts, use-first list and dish suggestions** are M6; the final accessibility pass and release polish are M7.
4. **File storage** is a local folder, not S3.
