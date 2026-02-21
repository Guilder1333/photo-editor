# Remote Photo Editing

A lightweight Android photo editing app with AI-powered inpainting capabilities via AUTOMATIC1111 Stable Diffusion WebUI.

## Project Vision

A streamlined alternative to Adobe Lightroom, focused on essential photo editing features with remote AI processing for advanced operations like inpainting.

## Roadmap

### Iteration 1 (Current Target)

#### Core Features
- **Image I/O**
  - Open images from gallery/camera
  - Save edited images to device storage
  - Support common formats (JPEG, PNG)

- **Mask Tools**
  - Brush tool for marking regions
  - Adjustable brush size
  - Clear/undo mask operations

- **Adjustments & Color Correction**
  - Brightness / Contrast
  - Saturation / Vibrance
  - Exposure
  - Highlights / Shadows
  - Temperature / Tint
  - Apply to whole image or masked region only

- **AI Inpainting**
  - Connect to AUTOMATIC1111 API
  - Send masked region for inpainting
  - Configurable server URL
  - Basic prompt input for inpainting

### Future Iterations (Planned)

- Layers support
- History/undo stack
- Preset filters
- Curves adjustment
- HSL panel
- Batch processing
- Local AI processing option
- Cloud sync

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3, edge-to-edge)
- **Architecture**: MVVM + Repository
- **Min SDK**: 35 (Android 15) / Target SDK 36
- **Image Viewing**: [Subsampling Scale Image View](https://github.com/davemorrissey/subsampling-scale-image-view) — tiled decoding for large images with pinch-zoom
- **Image Loading**: Coil (`coil-compose`) with crossfade for gallery thumbnails
- **Image Processing**: Android `ColorMatrix` / `ColorMatrixColorFilter` on hardware-accelerated layer paint
- **Icons**: Material Icons Extended
- **Networking**: Retrofit/OkHttp (planned, for A1111 API)
- **DI**: Hilt (planned)

## Architecture

### Overview

Single-activity app, fully Compose-based. No navigation library — `MainActivity` conditionally renders `GalleryScreen` or `EditScreen` based on ViewModel state. No Fragments.

### Module structure

All source lives under `app/src/main/java/org/photoedit/remote/`:

```
├── MainActivity.kt              ← single Activity, screen routing
├── model/
│   └── GalleryImage.kt          ← data class (id, uri)
├── repository/
│   └── GalleryRepository.kt     ← SharedPreferences persistence
├── viewmodel/
│   └── GalleryViewModel.kt      ← gallery state, selection, edit routing
└── ui/
    ├── GalleryScreen.kt          ← image grid, FAB picker, selection mode
    ├── MenuPanel.kt              ← hamburger menu (sidebar or top bar)
    ├── SelectionPanel.kt         ← multi-select bottom bar
    ├── EditScreen.kt             ← image editor, zoom, color matrix pipeline
    ├── EditMenuPanel.kt          ← tool menu (sidebar or bottom bar)
    ├── theme/                    ← Material 3 theme, colors, typography
    └── tools/                    ← editor tool plugins
        ├── EditTool.kt           ← interface: icon, label, hint, Content()
        ├── adjustments/
        │   └── AdjustmentsTool.kt
        └── colorcorrection/
            └── ColorCorrectionTool.kt
```

### Data flow

```
User action → Composable → ViewModel → Repository (SharedPreferences)
                                ↓
                     StateFlow update → recomposition
```

- **Gallery state** (`images`, `selection`, `currentEditImage`) is managed by `GalleryViewModel` using `StateFlow`, collected via `collectAsState()` in Compose.
- **Persistence**: URIs stored in SharedPreferences as newline-separated `id|uri` strings. `takePersistableUriPermission()` keeps content URIs accessible across restarts.
- **Editor state** (temperature, tint, vibrance) lives as local `remember` state in `EditScreen` — not yet in a ViewModel.

### Orientation handling

All screens check `LocalConfiguration.current.orientation` and switch between `Row` (landscape) and `Column` (portrait) layouts. Menu panels become sidebars in landscape, bars in portrait. No resource qualifiers or separate configs.

### Editor tool plugin system

Each tool implements the `EditTool` interface and lives in its own package under `ui/tools/`. Tools are registered in the `defaultEditTools` list in `EditMenuPanel.kt`. The menu renders an icon strip; tapping a tool expands its `Content()` composable in a sub-panel.

Image effects are applied in real-time by composing `ColorMatrix` transforms (temperature, tint, vibrance) and setting them as a `ColorMatrixColorFilter` on the `SubsamplingScaleImageView`'s hardware layer paint — no bitmap copies needed for preview.

## AUTOMATIC1111 API Integration

The app connects to a running instance of [AUTOMATIC1111 Stable Diffusion WebUI](https://github.com/AUTOMATIC1111/stable-diffusion-webui) for inpainting operations.

### Required A1111 Setup
1. Run WebUI with `--api` flag
2. Configure server URL in app settings
3. Ensure network accessibility from Android device

## Building

```bash
./gradlew assembleDebug
```

## License

TBD
