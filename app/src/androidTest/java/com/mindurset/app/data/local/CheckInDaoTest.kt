package com.mindurset.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CheckInDaoTest {
    private lateinit var database: MindUrSetDatabase
    private lateinit var dao: CheckInDao

    @Before
    fun createDb() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                MindUrSetDatabase::class.java,
            ).allowMainThreadQueries().build()
        dao = database.checkInDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    private fun checkIn(
        id: String,
        userId: String = "user-1",
        emotion: String = "calm",
        intensity: Int = 3,
        note: String? = null,
        createdAt: Long = 1000L,
    ) = CheckInLogEntity(
        id = id,
        userId = userId,
        emotion = emotion,
        intensity = intensity,
        note = note,
        createdAt = createdAt,
    )

    @Test
    fun inserts_return_newest_first_from_observeAll() = runTest {
        dao.insert(checkIn(id = "c1", createdAt = 1000L))
        dao.insert(checkIn(id = "c2", createdAt = 3000L))
        dao.insert(checkIn(id = "c3", createdAt = 2000L))

        val result = dao.observeAll("user-1").first()

        assertEquals(listOf("c2", "c3", "c1"), result.map { it.id })
        assertEquals(listOf(3000L, 2000L, 1000L), result.map { it.createdAt })
    }

    @Test
    fun count_reports_only_the_requested_users_rows_when_two_users_have_check_ins() = runTest {
        dao.insert(checkIn(id = "u1-1", userId = "user-1"))
        dao.insert(checkIn(id = "u1-2", userId = "user-1"))
        dao.insert(checkIn(id = "u2-1", userId = "user-2"))

        assertEquals(2, dao.count("user-1").first())
        assertEquals(1, dao.count("user-2").first())
        assertEquals(0, dao.count("user-3").first())
    }

    @Test
    fun deleteForUser_removes_one_users_rows_and_leaves_the_others() = runTest {
        dao.insert(checkIn(id = "u1-1", userId = "user-1"))
        dao.insert(checkIn(id = "u1-2", userId = "user-1"))
        dao.insert(checkIn(id = "u2-1", userId = "user-2"))

        dao.deleteForUser("user-1")

        assertEquals(0, dao.count("user-1").first())
        assertTrue(dao.observeAll("user-1").first().isEmpty())

        assertEquals(1, dao.count("user-2").first())
        val remaining = dao.observeAll("user-2").first()
        assertEquals(1, remaining.size)
        assertEquals("u2-1", remaining.first().id)
    }

    @Test
    fun observeRecent_excludes_rows_older_than_the_since_argument() = runTest {
        dao.insert(checkIn(id = "c-old", createdAt = 999L))
        dao.insert(checkIn(id = "c-boundary", createdAt = 1000L))
        dao.insert(checkIn(id = "c-new", createdAt = 1001L))

        val recent = dao.observeRecent(userId = "user-1", since = 1000L).first()

        assertEquals(listOf("c-new", "c-boundary"), recent.map { it.id })
    }

    @Test
    fun insert_with_the_same_id_replaces_the_row_instead_of_duplicating_it() = runTest {
        dao.insert(checkIn(id = "c1", emotion = "anxious", intensity = 8, note = "initial note"))
        dao.insert(checkIn(id = "c1", emotion = "calm", intensity = 2, note = "updated note"))

        assertEquals(1, dao.count("user-1").first())
        val entries = dao.observeAll("user-1").first()
        assertEquals(1, entries.size)
        val single = entries.first()
        assertEquals("c1", single.id)
        assertEquals("calm", single.emotion)
        assertEquals(2, single.intensity)
        assertEquals("updated note", single.note)
    }
}
