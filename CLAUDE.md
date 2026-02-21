# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android photo gallery + editing app with AI-powered inpainting via AUTOMATIC1111 Stable Diffusion WebUI API. Targets Android 14+ (minSdk 35).

## Workflow

The user runs all builds, tests, and checks themselves. Do not run `./gradlew` or any build/test commands to verify changes.

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

Single-activity, fully Compose-based MVVM with Repository pattern. No navigation library — `MainActivity` conditionally renders `GalleryScreen` or `EditScreen` based on `currentEditImage` state.

```
MainActivity
  ├── GalleryScreen                    ← image grid + picker
  │     ├── MenuPanel                  ← hamburger menu (sidebar / top bar)
  │     ├── GalleryGrid               ← LazyVerticalGrid of thumbnails
  │     └── SelectionPanel             ← bottom bar during multi-select
  │
  └── EditScreen                       ← full-screen image editor
        ├── ZoomableImage              ← SubsamplingScaleImageView via AndroidView
        └── EditMenuPanel              ← tool sidebar / bottom bar
              └── EditTool.Content()   ← active tool's UI panel
```

### Data flow

```
User action → Composable callback → ViewModel method → Repository
                                          ↓
                              StateFlow update → recomposition
```

- `GalleryViewModel` (AndroidViewModel) owns all gallery state: `images`, `selection`, `currentEditImage`.
- `GalleryRepository` persists image URIs in SharedPreferences as newline-separated `id|uri` strings.
- `takePersistableUriPermission()` is called when adding images so content URIs survive app restarts.
- Editor state (adjustment values like temperature, tint, vibrance) currently lives as local `remember` state in `EditScreen`, not in a ViewModel.
- Image effects are applied via `ColorMatrix` + `ColorMatrixColorFilter` on the `SubsamplingScaleImageView`'s hardware layer paint — real-time preview, no bitmap copies.

### Orientation handling

Every screen checks `LocalConfiguration.current.orientation` and switches layout:
- **Landscape**: `Row` — menu panels become sidebars (MenuPanel left, EditMenuPanel right)
- **Portrait**: `Column` — menu panels become top/bottom bars

No separate Activities, Fragments, or resource qualifiers.

### Editor tool plugin system

Tools implement the `EditTool` interface (`ui/tools/EditTool.kt`): `icon`, `label`, `hint`, and `@Composable Content()`. `EditMenuPanel` renders all registered tools via `defaultEditTools` list; selecting one opens its `Content()` in a sub-panel. Each tool lives in its own package under `ui/tools/`.

Current tools:
- `ui/tools/adjustments/` — placeholder (brightness, contrast planned)
- `ui/tools/colorcorrection/` — temperature, tint, vibrance, saturation sliders

To add a new tool: implement `EditTool` as an `object`, add it to `defaultEditTools` in `EditMenuPanel.kt`.

## Key Files

| File | Purpose |
|------|---------|
| `ui/GalleryScreen.kt` | Main screen: adaptive grid, FAB for image picker, long-press selection mode |
| `ui/MenuPanel.kt` | Hamburger menu — always shows icons; expand reveals labels. Landscape = left sidebar, portrait = top bar |
| `ui/SelectionPanel.kt` | Bottom bar shown during multi-select (count + delete) |
| `ui/EditScreen.kt` | Full-screen image editor with pinch-zoom (focal-point aware), pan, and tool registration |
| `ui/EditMenuPanel.kt` | Editor tool menu — opposite side from back button. Landscape = right sidebar, portrait = bottom bar |
| `ui/tools/EditTool.kt` | Interface contract for editor tools (icon, label, hint, Content composable) |
| `ui/tools/adjustments/` | Adjustments tool (brightness, contrast, etc.) |
| `ui/tools/colorcorrection/` | Color correction tool (tint, vibrance, etc.) |
| `viewmodel/GalleryViewModel.kt` | Gallery state (`images`, `selection`, `currentEditImage`), URI permission handling |
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

- Image editing: masks, save/export (adjustments and color correction tools exist but may not yet apply changes to the bitmap)
- All `MenuPanel` item `onClick` handlers are empty `{}`
- A1111 network layer
- Hilt DI setup
- Storage/camera permissions in manifest
