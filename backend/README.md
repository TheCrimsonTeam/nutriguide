# NutriGuide food-analysis backend

FastAPI service for visual food identification and USDA FoodData Central matching.

## Configure

Set `OPENAI_API_KEY`, `USDA_API_KEY`, and a random `NUTRIGUIDE_CLIENT_TOKEN` (at least 32 characters) in the server environment. The default model is `gpt-6.2-sol`; override it with `OPENAI_VISION_MODEL`.

The Android app reads `FOOD_ANALYSIS_BASE_URL` and `FOOD_ANALYSIS_CLIENT_TOKEN` from uncommitted local.properties or Gradle environment. The shared client token is a private-beta gate, not secure identity: it is packaged into the APK and extractable. Public production service must use verified Android attestation or short-lived user authentication at a trusted gateway and shared ingress rate limits. Provider keys stay server-side.

## Run locally

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements-dev.txt
cp .env.example .env
uvicorn app.main:app --host 0.0.0.0 --port 8080
```

Use a secret manager to load .env values. Do not commit secrets. Use HTTPS outside localhost.

## Deploy with Render

The repository-root `render.yaml` defines a Docker-based Render Blueprint for the `develop` branch. The service builds from `backend/Dockerfile`, uses `/health` for its health check, and runs without a persistent disk.

1. In Render, create a new Blueprint from this repository and select the `develop` branch.
2. During initial Blueprint setup, provide `OPENAI_API_KEY` and `USDA_API_KEY` when prompted. The Blueprint generates `NUTRIGUIDE_CLIENT_TOKEN` on the server. Keep all three values out of Git.
3. Deploy the service. After it is live, open `https://YOUR-SERVICE.onrender.com/health`. The endpoint reports whether the provider and USDA credentials are configured; HTTP 200 alone does not mean food recognition is available.
4. For a closed private-beta build only, set `FOOD_ANALYSIS_BASE_URL` to the service's HTTPS URL (with a trailing slash) and set `FOOD_ANALYSIS_CLIENT_TOKEN` through local.properties or a protected build environment. Never commit either value.

**Security boundary:** the current Android client token is compiled into the APK and can be extracted. It is a private-beta gate, not production authentication. Do not distribute a public build relying on this token. Before public release, place a trusted gateway in front of the API and use verified Android attestation or short-lived user authentication, server-side authorization and shared rate limiting. The app does not yet implement that production identity flow.

Render deployment itself is not performed by this repository change. It requires access to the Render account and real provider credentials.

## Behaviour

Images are decoded, EXIF-oriented, resized and recompressed in memory; they are not written to disk. The vision model returns food names and serving estimates, never nutrient values. The backend queries USDA FoodData Central and returns candidate FDC IDs plus per-100-gram nutrients. Users choose the closest record; NutriGuide scales available nutrients to the reviewed portion. Missing matches and nutrients stay unknown. Candidate scores are text-similarity hints, not calibrated probabilities.

Basic per-process rate limits, retries, timeouts, image hashing and brief duplicate-request coalescing are included. Multi-instance deployments need shared cache/rate limiting.
