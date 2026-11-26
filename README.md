# NutriGuide

Installable Android preview for the monochrome NutriGuide interface.

## Available in this preview
- Monochrome Jetpack Compose UI.
- Daily dashboard and manually entered food diary.
- Local persistence for manual entries.
- Camera capture and Android Photo Picker with image preview.
- Explicit scanner-unavailable state until a secure AI backend exists.
- Light, dark, and system appearance choices.

This preview does not perform AI food recognition and does not estimate nutrition. Manual values are saved exactly as entered. Workout tracking is not implemented in this starter.

## APK
GitHub Actions builds `app/build/outputs/apk/debug/app-debug.apk` on every push to `main` and uploads it as `NutriGuide-debug-apk`.
