package org.photoedit.core.repository

import org.photoedit.core.ImageBuffer
import org.photoedit.core.Pipeline
import org.photoedit.core.model.EditSession
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryEditSessionRepositoryTest {

    private lateinit var repo: InMemoryEditSessionRepository

    @BeforeTest
    fun setUp() {
        repo = InMemoryEditSessionRepository()
    }

    private fun session(photoId: String): EditSession {
        val src = ImageBuffer(1, 1, floatArrayOf(0f, 0f, 0f, 1f))
        return EditSession(photoId = photoId, pipeline = Pipeline(src))
    }

    @Test
    fun `load returns null when no session exists`() {
        assertNull(repo.load("photo-1"))
    }

    @Test
    fun `save then load returns the session`() {
        val s = session("photo-1")
        repo.save(s)
        assertEquals(s, repo.load("photo-1"))
    }

    @Test
    fun `save replaces existing session for same photoId`() {
        val first = session("photo-1")
        repo.save(first)

        val src = ImageBuffer(2, 2, FloatArray(16) { 0f })
        val updated = first.copy(pipeline = Pipeline(src))
        repo.save(updated)

        assertEquals(updated, repo.load("photo-1"))
    }

    @Test
    fun `delete removes the session`() {
        repo.save(session("photo-1"))
        repo.delete("photo-1")
        assertNull(repo.load("photo-1"))
    }

    @Test
    fun `delete on unknown id is a no-op`() {
        repo.save(session("photo-1"))
        repo.delete("does-not-exist")
        assertEquals("photo-1", repo.load("photo-1")?.photoId)
    }
}
