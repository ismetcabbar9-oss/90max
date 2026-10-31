package com.ninetymax.app

import com.ninetymax.app.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File

class RepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun persistsAndReloads() = runBlocking {
        val dir = tmp.newFolder()
        val repo = Repository(dir)
        repo.update { it.copy(onboarded = true, weights = listOf(WeightEntry("w1", "2026-11-02", 73.4))) }
        repo.flush()
        val reloaded = Repository(dir)
        assertTrue(reloaded.state.value.onboarded)
        assertEquals(73.4, reloaded.state.value.weights.single().kg, 0.0)
        assertTrue(File(dir, "backups").listFiles()!!.isNotEmpty())
    }

    @Test fun recoversFromCorruptFile() = runBlocking {
        val dir = tmp.newFolder()
        val repo = Repository(dir)
        repo.update { it.copy(onboarded = true, profile = it.profile.copy(name = "Test")) }
        repo.flush()
        File(dir, "data.json").writeText("{bozuk json")
        val reloaded = Repository(dir)
        assertTrue(reloaded.recoveredFromBackup)
        assertEquals("Test", reloaded.state.value.profile.name)
    }

    @Test fun zipBackupRoundTrip() = runBlocking {
        val src = Repository(tmp.newFolder())
        src.update { it.copy(onboarded = true, profile = it.profile.copy(name = "Yedek"), photos = listOf(PhotoRecord("p1", "2026-11-02", PhotoAngle.FRONT, "a.jpg"))) }
        File(src.photoDir, "a.jpg").writeBytes(byteArrayOf(1, 2, 3))
        val out = ByteArrayOutputStream()
        Backup.export(src, out)

        val dst = Repository(tmp.newFolder())
        Backup.import(dst, out.toByteArray().inputStream())
        assertEquals("Yedek", dst.state.value.profile.name)
        assertArrayEquals(byteArrayOf(1, 2, 3), File(dst.photoDir, "a.jpg").readBytes())
    }

    @Test fun invalidImportKeepsExistingData() {
        val repo = Repository(tmp.newFolder())
        repo.update { it.copy(profile = it.profile.copy(name = "Koru")) }
        assertThrows(Exception::class.java) { Backup.import(repo, "bu bir yedek değil".byteInputStream()) }
        assertEquals("Koru", repo.state.value.profile.name)
    }

    @Test fun unknownFieldsAreIgnored() {
        val repo = Repository(tmp.newFolder())
        val parsed = repo.parseImport("""{"onboarded":true,"gelecekAlan":123,"profile":{"name":"A"}}""")
        assertTrue(parsed.onboarded)
        assertEquals("A", parsed.profile.name)
    }
}
