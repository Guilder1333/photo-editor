# Photo Editing Architecture

Design spec for the photo-editing feature. Kotlin Multiplatform core, CPU-only on client, GPU work deferred to a server later. Built to accept new adjustments without changing the interface, and to be unit-testable in `commonTest`.

## 1. Core decisions

### 1.1 Fixed canonical pipeline (not user-controlled order)

Pixel adjustments are generally **not commutative**: Brightness+Contrast ≠ Contrast+Brightness, Exposure+Shadows ≠ Shadows+Exposure, etc. Letting the UI reorder them would make results nondeterministic.

Instead: the user tweaks parameters in any order, but the engine always runs adjustments in a **fixed canonical order** (Exposure → Contrast → Highlights → Shadows → Whites → Blacks → Curves → HSL → Sharpening). This matches Lightroom, Capture One, Darktable, RawTherapee, Apple Photos.

Benefits:
- Deterministic: same parameters → same result.
- Presets transfer between images and users.
- Undo/redo = swap the parameter set.
- Serialization = save a dict of parameters.
- No combinatorial test explosion.

Escape hatch for cases that genuinely need different ordering: **checkpoints** (see below). A checkpoint bakes current output into a new base image, so further adjustments stack on top of it. AI operations always produce checkpoints because they are expensive and non-commutative.

### 1.2 Cross-platform with Kotlin Multiplatform

- `commonMain`: `ImageBuffer`, `Adjustment`, `Pipeline`, `Checkpoint`, all pixel math. Zero platform dependencies. No `BufferedImage`, no `UIImage`, no `android.graphics.Bitmap`.
- Platform layer: thin adapters for image I/O, color management, display.
- UI: Compose Multiplatform (Android, iOS, desktop; web when stable).
- Tests: `kotlin.test` in `commonTest`, run on every target.

### 1.3 Image representation

- Planar float32 RGBA, stored as a single `FloatArray` in RGBA RGBA… order.
- **Linear light** (not sRGB-encoded). Exposure and most tone work only behave correctly in linear. Platform adapters handle the sRGB ↔ linear conversion at load/display.
- Alpha untouched by tone/color adjustments.
- 8-bit conversion happens only at display time.

## 2. Interfaces

### 2.1 `ImageBuffer`

```kotlin
// commonMain
class ImageBuffer(
    val width: Int,
    val height: Int,
    val pixels: FloatArray  // size = width * height * 4, RGBA RGBA...
) {
    init { require(pixels.size == width * height * 4) }
    fun copy(): ImageBuffer = ImageBuffer(width, height, pixels.copyOf())
}
```

### 2.2 `Adjustment`

```kotlin
// commonMain
interface Adjustment {
    val id: AdjustmentId
    val order: Int                  // canonical pipeline slot
    fun isIdentity(): Boolean       // skip if parameters are neutral
    fun apply(input: ImageBuffer): ImageBuffer   // pure; must not mutate input
}

@JvmInline value class AdjustmentId(val value: String)

object Order {
    const val EXPOSURE   = 100
    const val CONTRAST   = 200
    const val HIGHLIGHTS = 300
    const val SHADOWS    = 310
    const val WHITES     = 320
    const val BLACKS     = 330
    const val CURVES     = 400
    const val HSL        = 500
    const val SHARPEN    = 900
    // Gaps leave room to insert new adjustments without renumbering.
}
```

Contract for every `Adjustment`:
- Pure function of `(input, parameters)`. Same inputs → identical outputs.
- Never mutates `input.pixels`. Always returns a new `ImageBuffer`.
- `isIdentity()` returns true when parameters are at their neutral value, so the pipeline can skip the call.
- `order` determines where it runs in the pipeline, independent of when the user added it.

### 2.3 `Pipeline` and `Checkpoint`

```kotlin
// commonMain
class Pipeline(
    private val source: ImageBuffer,
    private val adjustments: List<Adjustment> = emptyList(),
    private val checkpoints: List<Checkpoint> = emptyList()
) {
    fun render(): ImageBuffer {
        val base = checkpoints.lastOrNull()?.image ?: source
        val active = adjustments
            .sortedBy { it.order }
            .filterNot { it.isIdentity() }
        return active.fold(base) { img, adj -> adj.apply(img) }
    }

    fun withAdjustment(a: Adjustment): Pipeline =
        copy(adjustments = adjustments.filter { it.id != a.id } + a)

    fun addCheckpoint(image: ImageBuffer, producedBy: String): Pipeline =
        copy(
            checkpoints = checkpoints + Checkpoint(image, producedBy, adjustments),
            adjustments = emptyList()  // subsequent adjustments stack on checkpoint
        )

    private fun copy(
        adjustments: List<Adjustment> = this.adjustments,
        checkpoints: List<Checkpoint> = this.checkpoints,
    ) = Pipeline(source, adjustments, checkpoints)
}

data class Checkpoint(
    val image: ImageBuffer,
    val producedBy: String,             // e.g. "ai.sky_replace"
    val bakedAdjustments: List<Adjustment>   // for history/undo display
)
```

Key properties:
- `Pipeline` is immutable. Edits produce a new `Pipeline`. Good for undo/redo.
- `render()` always starts from `source` (or the last checkpoint) and re-runs. No intermediate caching. Caching can be added later without changing the interface.
- Checkpoints freeze pipeline state. AI operations produce them. Adjustments after a checkpoint only affect the checkpointed image.

## 3. Example adjustment: Exposure

```kotlin
// commonMain/.../adjustments/Exposure.kt
// Linear-light multiplier: output = input * 2^EV. Alpha preserved.
class Exposure(private val ev: Float) : Adjustment {
    override val id = AdjustmentId("exposure")
    override val order = Order.EXPOSURE
    override fun isIdentity() = ev == 0f

    override fun apply(input: ImageBuffer): ImageBuffer {
        val factor = pow2(ev)
        val p = input.pixels
        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            out[i]     = p[i]     * factor  // R
            out[i + 1] = p[i + 1] * factor  // G
            out[i + 2] = p[i + 2] * factor  // B
            out[i + 3] = p[i + 3]           // A
            i += 4
        }
        return ImageBuffer(input.width, input.height, out)
    }

    private fun pow2(x: Float): Float = kotlin.math.exp(x * 0.6931472f)
}
```

## 4. Unit tests

```kotlin
// commonTest/.../adjustments/ExposureTest.kt
import kotlin.test.*

class ExposureTest {
    private fun buf(vararg px: Float) = ImageBuffer(1, px.size / 4, px)

    @Test fun `zero EV is identity`() {
        val input = buf(0.25f, 0.5f, 0.75f, 1f)
        val output = Exposure(0f).apply(input)
        assertContentEquals(input.pixels, output.pixels)
    }

    @Test fun `plus one EV doubles linear rgb`() {
        val input = buf(0.1f, 0.2f, 0.3f, 1f)
        val out = Exposure(1f).apply(input).pixels
        assertEquals(0.2f, out[0], 1e-5f)
        assertEquals(0.4f, out[1], 1e-5f)
        assertEquals(0.6f, out[2], 1e-5f)
        assertEquals(1.0f, out[3], 1e-5f)  // alpha untouched
    }

    @Test fun `isIdentity flags neutral parameters`() {
        assertTrue(Exposure(0f).isIdentity())
        assertFalse(Exposure(0.5f).isIdentity())
    }

    @Test fun `apply does not mutate input`() {
        val input = buf(0.1f, 0.2f, 0.3f, 1f)
        val snapshot = input.pixels.copyOf()
        Exposure(1f).apply(input)
        assertContentEquals(snapshot, input.pixels)
    }
}

class PipelineTest {
    @Test fun `render applies active adjustments`() {
        val src = ImageBuffer(1, 1, floatArrayOf(0.5f, 0.5f, 0.5f, 1f))
        val p = Pipeline(src).withAdjustment(Exposure(1f))
        assertEquals(1.0f, p.render().pixels[0], 1e-5f)
    }

    @Test fun `withAdjustment replaces existing adjustment of same id`() {
        val src = ImageBuffer(1, 1, floatArrayOf(0.25f, 0.25f, 0.25f, 1f))
        val p = Pipeline(src)
            .withAdjustment(Exposure(1f))
            .withAdjustment(Exposure(2f))  // replaces, not stacks
        assertEquals(1.0f, p.render().pixels[0], 1e-5f)  // 0.25 * 4
    }

    @Test fun `identity adjustments are skipped and render equals source`() {
        val src = ImageBuffer(1, 1, floatArrayOf(0.3f, 0.3f, 0.3f, 1f))
        val p = Pipeline(src).withAdjustment(Exposure(0f))
        assertContentEquals(src.pixels, p.render().pixels)
    }

    // Once Contrast exists, add:
    // @Test fun `canonical order is enforced regardless of insertion order`()
    //   — insert Contrast first, then Exposure; assert result equals
    //   applying Exposure before Contrast.
}
```

## 5. Development plan

1. **KMP module scaffolding.** Create the multiplatform module, `commonMain` + `commonTest`. Add `kotlin.test`.
2. **Core interfaces.** `ImageBuffer`, `Adjustment`, `AdjustmentId`, `Order`, `Pipeline`, `Checkpoint`. No implementations yet.
3. **Exposure + tests.** Implement the sample above, get all tests green.
4. **Contrast + ordering test.** Pivot around 0.18 mid-gray in linear light. Add the test proving canonical order holds regardless of insertion order (this is the real validator for the fixed-pipeline design).
5. **Parameter serialization.** Introduce `kotlinx.serialization` for edit state so presets, save/load, and history work.
6. **Stub AI checkpoint.** Add a no-op or trivial `AiOperation` that produces a `Checkpoint`. Test that further adjustments stack on it.
7. **Platform adapter (JVM desktop first).** JPEG → `ImageBuffer` (sRGB → linear), render → display. Easiest target to iterate on.
8. **Compose Multiplatform UI.** Sliders bound to `pipeline.withAdjustment(...)`, debounced re-render.
9. **Real adjustments.** Highlights, Shadows, Whites, Blacks, Curves, HSL, Sharpen — one at a time, each with unit tests.
10. **Other platform adapters.** Android, iOS.

## 6. Things to watch

- **Color space per adjustment.** Exposure belongs in linear RGB. Saturation is often better in Lab. When adding an adjustment whose natural space differs, make the conversion explicit inside `apply`.
- **Precision.** Keep float32 throughout the pipeline. Convert to 8-bit only at display. Avoids banding when stacking adjustments.
- **Masking.** If local adjustments (brush, gradient, AI-segmented) are on the roadmap, add an optional `mask: ImageBuffer?` to the `Adjustment` interface now. Retrofitting later is painful.
- **Ordering decision is sticky.** Changing `Order` constants after users have saved edits will shift their results. Decide the initial order carefully; use the gaps between slots for future insertions.
- **Performance.** `render()` recomputes from source each call. Fine for reasonable image sizes with `isIdentity()` short-circuiting. If it becomes a bottleneck, cache intermediate results keyed by `(adjustments prefix)` — the pure-function shape makes this safe to add without changing the interface.

## 7. Non-goals for v1

- User-controlled adjustment order (use checkpoints if ever needed).
- GPU execution on client (server-side, later).
- Raw file decoding (start with 8-bit JPEG/PNG → linear float).
- Local masking (leave the door open in the interface but don't implement).
