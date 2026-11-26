# NutriGuide

NutriGuide is a native Android app focused on two things: food photo-assisted meal logging and a broad exercise library with workout tracking. The first development slice prioritises local-first logging, reviewable nutrition values, and a modular architecture.

> **Status (26 November 2026):** Initial Android source and Figma screen system are committed on `develop`. The project is not yet a production release. Photo capture and gallery selection are implemented; remote food analysis is deliberately disabled until a secure backend is configured. A debug APK has not yet been built or verified in this environment.

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

1. Install Android Studio with Android SDK Platform 37 and JDK 17.
2. Open this repository in Android Studio and allow Gradle sync. The checked-in Gradle Wrapper downloads the pinned Gradle 9.6.0 distribution on first use.
3. Copy `local.properties.example` to `local.properties` and optionally set `FOOD_ANALYSIS_BASE_URL`.
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

The Gradle Wrapper JAR and launch scripts are pinned from the official Gradle `v9.6.0` source. CI uses the same wrapper so local and automated builds use the same Gradle distribution.

## Food analysis backend configuration

The Android client calls `POST {FOOD_ANALYSIS_BASE_URL}/food-analysis` as multipart form data with a `photo` field. The response contract is:

```json
{
  "items": [
    {
      "name": "Steamed rice",
      "portionGrams": 150,
      "confidence": 0.82,
      "caloriesKcal": null,
      "proteinG": null,
      "carbsG": null,
      "fatG": null,
      "nutritionSource": null
    }
  ],
  "message": "Review the food and serving estimate.",
  "requiresUserConfirmation": true
}
```

The nullable nutrient fields are intentional. A vision model may identify a food and estimate a visible portion, but it must not invent a nutrition value. A trusted backend should resolve candidate foods against a licensed composition source, return provenance, and leave a value blank when the match is uncertain.

**Never put an AI API key, USDA API key, service-role key or other server secret in Android resources, Gradle BuildConfig, local.properties committed to Git, or the repository.** Store provider credentials in the backend's secret manager. The current app does not implement account authentication or sync, so do not expose a public analysis endpoint to untrusted traffic. Configure user authentication, authorization and per-user quotas before enabling analysis.

See [data sources and licensing](docs/data-sources-and-licensing.md) and [nutrition accuracy protocol](docs/nutrition-accuracy.md).

## Git workflow

- Default branch: `main` (kept untouched during initial development).
- Integration branch: `develop`.
- Build and feature work should target `develop` through pull requests.
- Suggested next branches: `feature/food-analysis-backend`, `feature/meal-diary-polish`, `feature/workout-editing`, `feature/benchmark-dataset`.

## Figma

Editable Figma file: [NutriGuide — Android App Design System](https://www.figma.com/design/HbDGl50tZ6xptb1cBtT7Sl)

The file contains eleven native editable screen frames and a semantic Light/Dark variable collection. The Android UI should be kept aligned with these design references as features are finished.

## Testing and release status

GitHub Actions runs unit tests, Android lint and a debug build on pushes to `develop` and pull requests. CI will upload the debug APK when the build succeeds.

Not yet completed: authenticated backend, live AI provider, nutrition database matching UI, IFCT commercial-product permission, image benchmark dataset, cloud sync, full set editing/reordering/deletion, device-level camera verification, release signing, AAB and production deployment. No Play Store publication or paid cloud resource has been initiated.
