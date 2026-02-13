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
- **UI**: Jetpack Compose
- **Architecture**: MVVM
- **Networking**: Retrofit/OkHttp (for A1111 API)
- **Image Processing**: Android Graphics / RenderScript

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
