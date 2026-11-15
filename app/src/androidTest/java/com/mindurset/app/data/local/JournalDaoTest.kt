package com.mindurset.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JournalDaoTest {
    private lateinit var database: MindUrSetDatabase
    private lateinit var dao: JournalDao

    @Before
    fun createDb() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                MindUrSetDatabase::class.java,
            ).allowMainThreadQueries().build()
        dao = database.journalDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    private fun journal(
        id: String,
        userId: String = "user-1",
        challenge: String = "challenge",
        reframe: String = "reframe",
        gratitude: String = "gratitude",
        createdAt: Long = 1000L,
        updatedAt: Long = 1000L,
    ) = JournalEntryEntity(
        id = id,
        userId = userId,
        challenge = challenge,
        reframe = reframe,
        gratitude = gratitude,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    @Test
    fun upsert_twice_on_one_id_keeps_one_row_with_the_newer_body() = runTest {
        dao.upsert(journal(id = "j1", challenge = "initial challenge", updatedAt = 1000L))
        dao.upsert(journal(id = "j1", challenge = "updated challenge", updatedAt = 2000L))

        assertEquals(1, dao.count("user-1").first())
        val entry = dao.observeById("j1").first()
        assertEquals("updated challenge", entry?.challenge)
        assertEquals(2000L, entry?.updatedAt)
    }

    @Test
    fun delete_removes_only_that_row() = runTest {
        dao.upsert(journal(id = "j1"))
        dao.upsert(journal(id = "j2"))

        dao.delete("j1")

        assertEquals(1, dao.count("user-1").first())
        assertNull(dao.observeById("j1").first())
        assertEquals("j2", dao.observeById("j2").first()?.id)
    }
}
