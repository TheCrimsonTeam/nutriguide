# NutriGuide

NutriGuide is a native Android app focused on two things: food photo-assisted meal logging and a broad exercise library with workout tracking. The first development slice prioritises local-first logging, reviewable nutrition values, and a modular architecture.

> **Status (26 November 2026):** Initial Android source and Figma screen system are committed on `develop`. The project is not yet a production release. Photo capture, gallery selection, reviewable food drafts, edge-to-edge layout handling and a server-side food-analysis implementation are present. The backend still requires deployment and provider credentials before live scanning is available.

## Product scope

- **Nutrition:** CameraX capture, Android Photo Picker, editable food components, meal types, local diary and meal details. Unknown values remain unknown rather than being guessed.
- **Workouts:** 280+ built-in catalogue entries spanning free weights, machines, calisthenics, home training, cardio, mobility and warm-ups; search and category filters; custom exercises; workout and set logging.
- **Appearance:** Material 3 light, dark and system themes.
- **Offline:** Meal/workout records use Room and are stored on-device. No cloud sync is currently enabled.

The app intentionally has no social feed, calorie targets, deficit recommendations, weight-loss prompts, subscriptions or gamified streaks.

## Repository structure

```text
.
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/thecrimsonteam/nutriguide/
│       │   │   ├── data/catalog/       # Built-in exercise catalogue
│       │   │   ├── data/local/         # Room entities, DAOs and database
│       │   │   ├── data/remote/        # Food analysis API boundary
│       │   │   ├── data/repository/    # Repository interfaces + local adapters
│       │   │   ├── domain/model/       # Reviewable food draft model
│       │   │   └── ui/                 # Compose screens, camera and ViewModel
│       │   └── AndroidManifest.xml
│       └── test/                       # Unit tests
├── docs/
│   ├── architecture.md
│   ├── nutrition-accuracy.md
│   └── data-sources-and-licensing.md
├── tools/benchmark_food_accuracy.py
├── benchmarks/food_accuracy_template.csv
└── .github/workflows/android-ci.yml
```

## Technology

- Kotlin with Jetpack Compose and Material 3
- Android Gradle Plugin 9.5.0; Gradle 9.6.0; JDK 17
- Compose BOM 2026.11.01
- Room 2.8.5 with KSP
- CameraX 1.6.2 and Android Photo Picker
- Retrofit 3 for the replaceable analysis API boundary
- Coroutines and Flow

Versions were selected against the official Android, Kotlin and Gradle release documentation. The AGP 9 line includes built-in Kotlin support; the separate Compose compiler plugin is applied for Compose.

## Build locally

1. Install Android Studio with Android SDK Platform 37.1 and JDK 17.
2. Open this repository in Android Studio and allow Gradle sync. The checked-in Gradle Wrapper downloads the pinned Gradle 9.6.0 distribution on first use.
3. Copy `local.properties.example` to `local.properties`; set `FOOD_ANALYSIS_BASE_URL` and the private-beta `FOOD_ANALYSIS_CLIENT_TOKEN` when the backend is deployed.
4. Build and test:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

After a successful GitHub Actions run, open the run's **Artifacts** section and download `NutriGuide-debug-apk`. Extract the ZIP to get `NutriGuide-debug.apk`, then open the APK on the Android device and approve installation when Android asks. No Android Studio or ADB is needed. The artifact is an APK, not an AAB. Failed runs publish diagnostics instead of pretending an APK exists.

The Gradle Wrapper JAR and launch scripts are pinned from the official Gradle `v9.6.0` source. CI uses the same wrapper so local and automated builds use the same Gradle distribution.

## Food analysis backend configuration

A FastAPI service lives in `backend/`. It uses a vision-capable OpenAI Responses model to identify visible food components and estimate portions; it does not ask the model to supply calories or macros. A separate USDA FoodData Central lookup returns candidate records with FDC IDs and per-100-gram nutrient values. The app lets the user choose a candidate and calculates nutrients from the selected record and reviewed serving weight.

### Configure the service

1. Install Python 3.12 or later.
2. In `backend/`, create a virtual environment, install `requirements-dev.txt`, and copy `.env.example` to `.env`.
3. Set server-side `OPENAI_API_KEY`, `USDA_API_KEY` and a random `NUTRIGUIDE_CLIENT_TOKEN` (at least 32 characters). The vision model defaults to `gpt-6.2-sol`; override using `OPENAI_VISION_MODEL`.
4. Run `uvicorn app.main:app --host 0.0.0.0 --port 8080` from `backend/`.
5. Set `FOOD_ANALYSIS_BASE_URL` and `FOOD_ANALYSIS_CLIENT_TOKEN` in uncommitted Android `local.properties`. Use HTTPS outside localhost.

The shared token is a temporary private-beta request gate, not production authentication: it is embedded in the APK and can be extracted. Before public release, put the endpoint behind verified Android attestation or short-lived authenticated-user tokens at a trusted gateway, with shared rate limits. Never put `OPENAI_API_KEY` or `USDA_API_KEY` in Android configuration or the repository.

Images are size-limited, EXIF-oriented, resized and recompressed in memory; the backend does not store photos. Responses are schema-validated. Candidate values are sourced from USDA and remain unknown when no match or nutrient is available. Similarity scores rank text matches but are not confidence probabilities. USDA FoodData Central is CC0 1.0; the app displays source information.

See `backend/README.md`, [USDA API guide](https://fdc.nal.usda.gov/api-guide/), [OpenAI image input guide](https://developers.openai.com/api/docs/guides/images-vision), [data sources and licensing](docs/data-sources-and-licensing.md), and [nutrition accuracy protocol](docs/nutrition-accuracy.md).

## Git workflow

- Default branch: `main` (kept untouched during initial development).
- Integration branch: `develop`.
- Build and feature work should target `develop` through pull requests.
- Suggested next branches: `feature/food-analysis-backend`, `feature/meal-diary-polish`, `feature/workout-editing`, `feature/benchmark-dataset`.

## Figma

Editable Figma file: [NutriGuide — Android App Design System](https://www.figma.com/design/HbDGl50tZ6xptb1cBtT7Sl)

The file contains eleven native editable screen frames and a semantic Light/Dark variable collection. The Android UI should be kept aligned with these design references as features are finished.

## Testing and release status

GitHub Actions runs unit tests, Android lint, `./gradlew assembleDebug`, APK existence checks and Android debug-signature verification on pushes to `develop` and pull requests. On success, it uploads the installable file as the **`NutriGuide-debug-apk`** artifact (file: `NutriGuide-debug.apk`). It also always uploads `NutriGuide-build-diagnostics` with Gradle logs, test XML results and test reports when available. Both artifacts are retained for 30 days.

Not yet completed: authenticated backend, live AI provider, nutrition database matching UI, IFCT commercial-product permission, image benchmark dataset, cloud sync, full set editing/reordering/deletion, device-level camera verification, release signing, AAB and production deployment. No Play Store publication or paid cloud resource has been initiated.
