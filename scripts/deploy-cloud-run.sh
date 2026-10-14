#!/usr/bin/env bash
# Deploys the Rasoi server to Cloud Run (ADR 0028). Safe to re-run: every step creates what is missing and updates the
# rest. Needs an authenticated gcloud with Owner on the project, which must be on the Blaze plan.
#
# Required once, before the first run (the password never touches this script, the shell history or the repo):
#   gcloud secrets create rasoi-database-password --data-file=<file holding only the Neon password>
#
# Environment (defaults in brackets):
#   RASOI_GCP_PROJECT [grocery-app-2c112]   RASOI_GCP_REGION [asia-southeast1]
#   RASOI_DATABASE_URL (required)  e.g. jdbc:postgresql://ep-xxx.ap-southeast-1.aws.neon.tech/rasoi?sslmode=require
#   RASOI_DATABASE_USER (required)
#   RASOI_LLM_BASE_URL [https://api.groq.com/openai/v1]   RASOI_LLM_MODEL [llama-3.3-70b-versatile]
#
# Optional AI fallback for unknown invoice formats and dish ideas: store the provider's API key once as the secret
# rasoi-llm-api-key (gcloud secrets create rasoi-llm-api-key --data-file=<file with only the key>). Without it the
# server runs with the fallback off.
set -euo pipefail

PROJECT="${RASOI_GCP_PROJECT:-grocery-app-2c112}"
REGION="${RASOI_GCP_REGION:-asia-southeast1}"
DATABASE_URL="${RASOI_DATABASE_URL:?set RASOI_DATABASE_URL (jdbc:postgresql://…?sslmode=require)}"
DATABASE_USER="${RASOI_DATABASE_USER:?set RASOI_DATABASE_USER}"
LLM_BASE_URL="${RASOI_LLM_BASE_URL:-https://api.groq.com/openai/v1}"
LLM_MODEL="${RASOI_LLM_MODEL:-llama-3.3-70b-versatile}"
SERVICE=rasoi-api
REPO=rasoi
BUCKET="${PROJECT}-rasoi-documents"
RUNTIME_SA="rasoi-api@${PROJECT}.iam.gserviceaccount.com"
VERSION="$(git rev-parse --short HEAD)"
IMAGE="${REGION}-docker.pkg.dev/${PROJECT}/${REPO}/server:${VERSION}"

gcloud() { command gcloud --project "$PROJECT" --quiet "$@"; }
exists() { "$@" >/dev/null 2>&1; }

echo "==> APIs"
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com \
  secretmanager.googleapis.com cloudscheduler.googleapis.com storage.googleapis.com iam.googleapis.com

echo "==> Image registry (keeps the 2 newest images, inside the 0.5 GB free tier)"
exists gcloud artifacts repositories describe "$REPO" --location "$REGION" ||
  gcloud artifacts repositories create "$REPO" --repository-format docker --location "$REGION"
POLICY="$(mktemp)"
cat >"$POLICY" <<'JSON'
[{"name": "keep-newest", "action": {"type": "Keep"}, "mostRecentVersions": {"keepCount": 2}},
 {"name": "delete-rest", "action": {"type": "Delete"}, "condition": {"tagState": "ANY"}}]
JSON
gcloud artifacts repositories set-cleanup-policies "$REPO" --location "$REGION" --policy "$POLICY" --no-dry-run
rm -f "$POLICY"

echo "==> Invoice storage bucket"
exists gcloud storage buckets describe "gs://${BUCKET}" ||
  gcloud storage buckets create "gs://${BUCKET}" --location "$REGION" --uniform-bucket-level-access \
    --public-access-prevention

echo "==> Runtime service account"
exists gcloud iam service-accounts describe "$RUNTIME_SA" ||
  gcloud iam service-accounts create rasoi-api --display-name "Rasoi API (Cloud Run)"
gcloud storage buckets add-iam-policy-binding "gs://${BUCKET}" \
  --member "serviceAccount:${RUNTIME_SA}" --role roles/storage.objectUser >/dev/null

echo "==> Secrets"
exists gcloud secrets describe rasoi-database-password || {
  echo "Missing secret rasoi-database-password; see the header of this script." >&2
  exit 1
}
exists gcloud secrets describe rasoi-sweep-token ||
  openssl rand -hex 32 | tr -d '\n' | gcloud secrets create rasoi-sweep-token --data-file=-
SECRETS=(rasoi-database-password rasoi-sweep-token)
ENV_VARS="FIREBASE_PROJECT_ID=${PROJECT}|DATABASE_URL=${DATABASE_URL}|DATABASE_USER=${DATABASE_USER}|RASOI_VERSION=${VERSION}|RASOI_STORAGE_DIR=/data"
SECRET_VARS="DATABASE_PASSWORD=rasoi-database-password:latest,RASOI_SWEEP_TOKEN=rasoi-sweep-token:latest"
if exists gcloud secrets describe rasoi-llm-api-key; then
  echo "    AI fallback on (${LLM_MODEL})"
  SECRETS+=(rasoi-llm-api-key)
  ENV_VARS="${ENV_VARS}|RASOI_LLM_BASE_URL=${LLM_BASE_URL}|RASOI_LLM_MODEL=${LLM_MODEL}"
  SECRET_VARS="${SECRET_VARS},RASOI_LLM_API_KEY=rasoi-llm-api-key:latest"
else
  echo "    AI fallback off (no rasoi-llm-api-key secret)"
fi
for secret in "${SECRETS[@]}"; do
  gcloud secrets add-iam-policy-binding "$secret" \
    --member "serviceAccount:${RUNTIME_SA}" --role roles/secretmanager.secretAccessor >/dev/null
done

echo "==> Build ${IMAGE}"
gcloud builds submit --tag "$IMAGE" --region "$REGION" .

echo "==> Deploy ${SERVICE}"
# Request-based billing and scale to zero keep it in the free tier; one instance at most, as the server is tested
# single-process. The '^|^' prefix makes '|' the separator, as a JDBC URL may hold commas. gen2 is needed for the Cloud Storage volume; uid/gid match the image's non-root user.
gcloud run deploy "$SERVICE" --image "$IMAGE" --region "$REGION" \
  --service-account "$RUNTIME_SA" --allow-unauthenticated \
  --execution-environment gen2 --cpu 1 --memory 1Gi --cpu-boost \
  --min-instances 0 --max-instances 1 --concurrency 80 --timeout 300 \
  --add-volume "name=documents,type=cloud-storage,bucket=${BUCKET},mount-options=uid=10001;gid=10001" \
  --add-volume-mount "volume=documents,mount-path=/data" \
  --set-env-vars "^|^${ENV_VARS}" \
  --set-secrets "${SECRET_VARS}"
URL="$(gcloud run services describe "$SERVICE" --region "$REGION" --format 'value(status.url)')"

echo "==> Sweep schedule (every 15 minutes)"
TOKEN="$(gcloud secrets versions access latest --secret rasoi-sweep-token)"
SCHEDULE=(--location "$REGION" --schedule "*/15 * * * *" --time-zone "Etc/UTC" --uri "${URL}/internal/sweeps"
  --http-method POST --attempt-deadline 300s)
if exists gcloud scheduler jobs describe rasoi-sweeps --location "$REGION"; then
  gcloud scheduler jobs update http rasoi-sweeps "${SCHEDULE[@]}" --update-headers "X-Rasoi-Sweep-Token=${TOKEN}"
else
  gcloud scheduler jobs create http rasoi-sweeps "${SCHEDULE[@]}" --headers "X-Rasoi-Sweep-Token=${TOKEN}"
fi

echo "==> Health"
curl -fsS "${URL}/health" && echo
echo "Deployed ${VERSION} at ${URL}"
