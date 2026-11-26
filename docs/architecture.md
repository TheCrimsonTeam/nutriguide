# Architecture decisions

## App layers

```text
Compose UI
  └── AppViewModel (lifecycle-scoped state + actions)
       ├── NutritionRepository ── Room MealDao / FoodItemEntity
       ├── TrainingRepository ─── Room WorkoutDao / WorkoutEntry / WorkoutSet
       ├── ExerciseRepository ─── Room CustomExerciseDao
       └── FoodAnalysisApi ─────── secure remote analysis endpoint
```

- UI owns navigation and input state; it does not write directly to Room.
- ViewModel actions invoke repository abstractions and expose observable state.
- Room is the source of truth for saved meals, food components, workouts, exercise entries, sets and user-created exercises.
- Built-in exercises are versioned Kotlin catalogue data; user-created exercises are persisted in Room.
- The remote analysis client is optional and created only when a backend URL is configured.

## Backend choice

**Recommended next step: Supabase**, subject to project setup and security review. It provides Postgres, Auth, Storage and Edge Functions in one service, while Room remains the offline source of truth. Use Auth identities, row-level security, short-lived signed upload URLs, and Edge Functions to proxy image inference and nutrition-data access. Avoid storing raw meal photographs by default; process in memory or temporary storage and delete them immediately after analysis.

Firebase is also viable and offers mature Android Auth/Firestore/Storage SDKs, but it would add a second data model beside Room and stronger Google-specific coupling. A custom backend offers flexibility but creates unnecessary operational work for this narrowly scoped first release.

No Supabase/Firebase project has been created in this task: project credentials, data region, privacy settings and billing authorization were not supplied. No cloud service has been deployed.

## Food analysis boundaries

The vision model should return only:
- Perceived food/component labels
- Portion estimates with units
- Recognition confidence / uncertainty
- Preparation questions where visible evidence is insufficient

A separate nutrition resolver should:
1. Search approved nutrition datasets.
2. Present multiple plausible matches rather than selecting a weak match silently.
3. Compute item macros from reference values and the reviewed portion.
4. Preserve source name, dataset version, food identifier and transformation method.
5. Keep unresolved nutrients null.

The app must not treat model-generated macro values as measured facts.

## Sync and conflict strategy

Not implemented yet. When cloud sync is added, use immutable record IDs, server-side ownership, updated-at/version fields and idempotent upserts. Keep writes local-first and queue operations for later sync. Explicitly handle deleted records to prevent them being resurrected from another device.

## Privacy controls

- Use HTTPS only.
- Do not log image bytes, signed URLs, tokens or nutrition diary contents.
- Keep provider API keys server-side.
- Require authenticated users and per-user data policies before enabling the endpoint.
- Provide user initiated deletion for cloud and local data.
- Default to no permanent raw-image retention.
