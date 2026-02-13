# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android photo gallery + editing app with AI-powered inpainting via AUTOMATIC1111 Stable Diffusion WebUI API. Targets Android 14+ (minSdk 35).

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests (JVM)
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a single unit test class
./gradlew test --tests "org.photoedit.remote.ExampleUnitTest"

# Install on connected device
./gradlew installDebug
```

## Architecture

Single-activity, fully Compose-based MVVM with Repository pattern.

```
MainActivity
  └── GalleryScreen (Compose)
        └── GalleryViewModel (AndroidViewModel)
              └── GalleryRepository (SharedPreferences)
                    └── GalleryImage (model)
```

**State management**: `StateFlow` in ViewModels, collected via `collectAsState()` in Compose.

**Persistence**: `GalleryRepository` stores image URIs in SharedPreferences as newline-separated `id|uri` strings. The ViewModel calls `takePersistableUriPermission()` when adding images so URIs remain accessible across app restarts.

**Orientation handling**: `GalleryScreen` and `MenuPanel` both check `LocalConfiguration.current.orientation` to switch between portrait and landscape layouts. No separate Activities or Fragments are used.

## Key Files

| File | Purpose |
|------|---------|
| `ui/GalleryScreen.kt` | Main screen: adaptive grid, FAB for image picker, long-press selection mode |
| `ui/MenuPanel.kt` | Hamburger menu — always shows icons; expand reveals labels. Landscape = left sidebar, portrait = top bar |
| `ui/SelectionPanel.kt` | Bottom bar shown during multi-select (count + delete) |
| `viewmodel/GalleryViewModel.kt` | Gallery state (`images`, `selection`), URI permission handling |
| `repository/GalleryRepository.kt` | SharedPreferences persistence for gallery URIs |
| `model/GalleryImage.kt` | `data class GalleryImage(id: String, uri: String)` |

## Dependency Notes

- **Icons**: Uses `material-icons-extended` (added to `libs.versions.toml`). Icons like `Icons.Default.Tune` and `Icons.Default.Help` require this — they are NOT in the core icons artifact.
- **Image loading**: Coil (`coil-compose`) with `AsyncImage` and crossfade.
- **Networking**: Retrofit/OkHttp not yet added. Needed for A1111 API integration.
- **DI**: Hilt not yet added (planned).

## A1111 API Integration (Planned)

- Base URL: `http://<server>:7860/sdapi/v1/`
- Inpainting via `POST /img2img`
- Images and masks sent as base64-encoded strings
- Server must be started with `--api` flag

## What Is Not Yet Implemented

- Image editing (adjustments, masks, save)
- All `MenuPanel` item `onClick` handlers are empty `{}`
- A1111 network layer
- Hilt DI setup
- Storage/camera permissions in manifest
