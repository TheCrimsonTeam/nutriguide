# NutriGuide food-analysis backend

FastAPI service for visual food identification and USDA FoodData Central matching.

## Configure

Set `OPENAI_API_KEY`, `USDA_API_KEY`, and a random `NUTRIGUIDE_CLIENT_TOKEN` (at least 32 characters) in the server environment. The default model is `gpt-6.2-sol`; override it with `OPENAI_VISION_MODEL`.

The Android app reads `FOOD_ANALYSIS_BASE_URL` and `FOOD_ANALYSIS_CLIENT_TOKEN` from uncommitted local.properties or Gradle environment. The shared client token is a private-beta gate, not secure identity: it is packaged into the APK and extractable. Public production service must use verified Android attestation or short-lived user authentication at a trusted gateway and shared ingress rate limits. Provider keys stay server-side.

## Run

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements-dev.txt
cp .env.example .env
uvicorn app.main:app --host 0.0.0.0 --port 8080
```

Use a secret manager to load .env values. Do not commit secrets. Use HTTPS outside localhost.

## Behaviour

Images are decoded, EXIF-oriented, resized and recompressed in memory; they are not written to disk. The vision model returns food names and serving estimates, never nutrient values. The backend queries USDA FoodData Central and returns candidate FDC IDs plus per-100-gram nutrients. Users choose the closest record; NutriGuide scales available nutrients to the reviewed portion. Missing matches and nutrients stay unknown. Candidate scores are text-similarity hints, not calibrated probabilities.

Basic per-process rate limits, retries, timeouts, image hashing and brief duplicate-request coalescing are included. Multi-instance deployments need shared cache/rate limiting.
