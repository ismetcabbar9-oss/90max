package com.ninetymax.app.data

import com.ninetymax.app.seed.Seed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
    coerceInputValues = true
}

/**
 * Tek kaynaklı veri deposu. Veriler JSON olarak [dir]/data.json dosyasında tutulur.
 * Yazma işlemi önce geçici dosyaya yapılır ve atomik olarak yer değiştirilir; ayrıca her gün
 * için bir otomatik yedek (son 7 gün) saklanır. Ana dosya bozulursa en yeni sağlam yedek yüklenir.
 */
class Repository(
    private val dir: File,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    private val dataFile = File(dir, "data.json")
    private val backupDir = File(dir, "backups")
    private val writeLock = Mutex()
    @Volatile private var lastWrite: Job? = null

    /** Son yüklemede ana dosya yerine yedekten kurtarma yapıldıysa true. (load()'dan önce tanımlı olmalı.) */
    var recoveredFromBackup = false
        private set

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppData> = _state.asStateFlow()

    val photoDir: File get() = File(dir, "photos").apply { mkdirs() }

    private fun load(): AppData {
        dir.mkdirs()
        val primary = readFile(dataFile)
        val data = primary ?: run {
            val backup = backupFiles().firstNotNullOfOrNull { readFile(it) }
            if (backup != null) recoveredFromBackup = true
            backup ?: AppData()
        }
        return Seed.apply(data, today())
    }

    private fun readFile(f: File): AppData? = try {
        if (f.exists() && f.length() > 0) AppJson.decodeFromString(AppData.serializer(), f.readText()) else null
    } catch (e: Exception) {
        null
    }

    private fun backupFiles(): List<File> =
        (backupDir.listFiles { f -> f.name.startsWith("backup-") && f.name.endsWith(".json") } ?: emptyArray())
            .sortedByDescending { it.name }

    fun update(transform: (AppData) -> AppData) {
        _state.update(transform)
        lastWrite = scope.launch { persist() }
    }

    /** Durumu tamamen değiştirir (içe aktarma). */
    fun replaceAll(data: AppData) = update { Seed.apply(data, today()) }

    /** Sıradaki tüm yazmaların bitmesini bekler ve son durumu diske yazar. */
    suspend fun flush() {
        lastWrite?.join()
        persist()
    }

    private suspend fun persist() = writeLock.withLock {
        // Her yazma en güncel durumu kaydeder; Mutex sırayı korur.
        val latest = _state.value
        val text = AppJson.encodeToString(AppData.serializer(), latest)
        writeAtomic(dataFile, text)
        backupDir.mkdirs()
        val todayBackup = File(backupDir, "backup-${today()}.json")
        writeAtomic(todayBackup, text)
        backupFiles().drop(7).forEach { it.delete() }
    }

    private fun writeAtomic(target: File, text: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
    }

    fun exportJson(): String = AppJson.encodeToString(AppData.serializer(), _state.value)

    fun parseImport(text: String): AppData = AppJson.decodeFromString(AppData.serializer(), text)
}
