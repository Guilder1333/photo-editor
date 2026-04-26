package org.photoedit.core.usecase

import org.photoedit.core.ImageBuffer
import org.photoedit.core.Pipeline
import org.photoedit.core.adjustments.Exposure
import org.photoedit.core.model.Photo
import org.photoedit.core.repository.InMemoryEditSessionRepository
import org.photoedit.core.repository.InMemoryPhotoRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class UseCaseTest {

    private lateinit var photoRepo: InMemoryPhotoRepository
    private lateinit var sessionRepo: InMemoryEditSessionRepository

    private lateinit var loadPhotos: LoadPhotosUseCase
    private lateinit var startSession: StartEditSessionUseCase
    private lateinit var applyAdjustment: ApplyAdjustmentUseCase
    private lateinit var undo: UndoAdjustmentUseCase
    private lateinit var redo: RedoAdjustmentUseCase

    private val photo = Photo(
        id = "photo-1",
        uri = "file://photo-1.jpg",
        width = 800,
        height = 600,
        timestampMs = 1_000_000L,
    )
    private val source = ImageBuffer(1, 1, floatArrayOf(0.5f, 0.5f, 0.5f, 1f))

    @BeforeTest
    fun setUp() {
        photoRepo = InMemoryPhotoRepository()
        sessionRepo = InMemoryEditSessionRepository()

        loadPhotos = LoadPhotosUseCase(photoRepo)
        startSession = StartEditSessionUseCase(photoRepo, sessionRepo)
        applyAdjustment = ApplyAdjustmentUseCase(sessionRepo)
        undo = UndoAdjustmentUseCase(sessionRepo)
        redo = RedoAdjustmentUseCase(sessionRepo)
    }

    // ── LoadPhotosUseCase ────────────────────────────────────────────────────

    @Test
    fun `loadPhotos returns empty list when no photos exist`() {
        assertTrue(loadPhotos().isEmpty())
    }

    @Test
    fun `loadPhotos returns all added photos`() {
        photoRepo.add(photo)
        photoRepo.add(photo.copy(id = "photo-2", uri = "file://photo-2.jpg"))
        assertEquals(2, loadPhotos().size)
    }

    // ── StartEditSessionUseCase ──────────────────────────────────────────────

    @Test
    fun `startSession creates a new session for a known photo`() {
        photoRepo.add(photo)
        val session = startSession(photo.id, source)
        assertEquals(photo.id, session.photoId)
        assertNotNull(sessionRepo.load(photo.id))
    }

    @Test
    fun `startSession resumes existing session without overwriting it`() {
        photoRepo.add(photo)
        val first = startSession(photo.id, source)
        val resumed = startSession(photo.id, source)
        assertSame(first, resumed)
    }

    @Test
    fun `startSession throws for unknown photoId`() {
        assertFails { startSession("unknown", source) }
    }

    // ── ApplyAdjustmentUseCase ───────────────────────────────────────────────

    @Test
    fun `applyAdjustment updates the pipeline and pushes to undo stack`() {
        photoRepo.add(photo)
        val session = startSession(photo.id, source)
        val updated = applyAdjustment(session, Exposure(1f))

        assertFalse(updated.history.canRedo)
        assertTrue(updated.history.canUndo)
        // Persisted version should match
        assertEquals(updated, sessionRepo.load(photo.id))
    }

    @Test
    fun `applyAdjustment result renders with the new adjustment`() {
        photoRepo.add(photo)
        val session = startSession(photo.id, source)
        val updated = applyAdjustment(session, Exposure(1f))
        // source pixel[0] = 0.5; +1 EV → 1.0
        assertEquals(1.0f, updated.pipeline.render().pixels[0], 1e-5f)
    }

    @Test
    fun `applying a second adjustment replaces the first (same id) and clears redo`() {
        photoRepo.add(photo)
        var session = startSession(photo.id, source)
        session = applyAdjustment(session, Exposure(1f))
        // Simulate undo then apply a different value (redo is cleared)
        session = undo(session)
        session = applyAdjustment(session, Exposure(2f))

        assertFalse(session.history.canRedo)
        // 0.5 * 2^2 = 2.0
        assertEquals(2.0f, session.pipeline.render().pixels[0], 1e-5f)
    }

    // ── UndoAdjustmentUseCase ────────────────────────────────────────────────

    @Test
    fun `undo with nothing on stack returns session unchanged`() {
        photoRepo.add(photo)
        val session = startSession(photo.id, source)
        val result = undo(session)
        assertSame(session, result)
    }

    @Test
    fun `undo restores the previous pipeline`() {
        photoRepo.add(photo)
        var session = startSession(photo.id, source)
        val pipelineBeforeEdit = session.pipeline

        session = applyAdjustment(session, Exposure(1f))
        session = undo(session)

        // Pipeline reference should be the exact object that was saved as the snapshot.
        assertSame(pipelineBeforeEdit, session.pipeline)
        assertFalse(session.history.canUndo)
        assertTrue(session.history.canRedo)
    }

    @Test
    fun `undo is persisted to the session repository`() {
        photoRepo.add(photo)
        var session = startSession(photo.id, source)
        session = applyAdjustment(session, Exposure(1f))
        val undone = undo(session)
        assertEquals(undone, sessionRepo.load(photo.id))
    }

    // ── RedoAdjustmentUseCase ────────────────────────────────────────────────

    @Test
    fun `redo with nothing on stack returns session unchanged`() {
        photoRepo.add(photo)
        val session = startSession(photo.id, source)
        val result = redo(session)
        assertSame(session, result)
    }

    @Test
    fun `redo reapplies the undone pipeline`() {
        photoRepo.add(photo)
        var session = startSession(photo.id, source)
        session = applyAdjustment(session, Exposure(1f))
        val pipelineAfterEdit = session.pipeline

        session = undo(session)
        session = redo(session)

        assertSame(pipelineAfterEdit, session.pipeline)
        assertTrue(session.history.canUndo)
        assertFalse(session.history.canRedo)
    }

    @Test
    fun `undo then redo is a round-trip`() {
        photoRepo.add(photo)
        var session = startSession(photo.id, source)
        session = applyAdjustment(session, Exposure(1f))
        val afterApply = session.pipeline.render().pixels[0]

        session = undo(session)
        session = redo(session)

        assertEquals(afterApply, session.pipeline.render().pixels[0], 1e-5f)
    }

    @Test
    fun `multiple undo-redo cycles stay consistent`() {
        photoRepo.add(photo)
        var session = startSession(photo.id, source)
        session = applyAdjustment(session, Exposure(1f))  // +1 EV
        session = applyAdjustment(session, Exposure(2f))  // +2 EV (replaces)

        session = undo(session)  // back to no-adjustment
        session = undo(session)  // nothing more — stays put
        assertFalse(session.history.canUndo)

        session = redo(session)  // restores Exposure(1f): 0.5 × 2^1 = 1.0
        assertEquals(1.0f, session.pipeline.render().pixels[0], 1e-5f)
    }
}
