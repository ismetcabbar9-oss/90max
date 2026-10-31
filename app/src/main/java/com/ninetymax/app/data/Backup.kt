package com.ninetymax.app.data

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Tüm verileri ve fotoğrafları tek bir ZIP dosyasına yedekler / geri yükler. */
object Backup {
    private const val DATA_ENTRY = "data.json"
    private const val PHOTO_PREFIX = "photos/"

    fun export(repo: Repository, out: OutputStream) {
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(DATA_ENTRY))
            zip.write(repo.exportJson().toByteArray())
            zip.closeEntry()
            repo.photoDir.listFiles()?.forEach { f ->
                zip.putNextEntry(ZipEntry(PHOTO_PREFIX + f.name))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /**
     * ZIP veya düz JSON yedeğini okur ve doğrular. Geçersizse istisna fırlatır ve mevcut veriye dokunmaz.
     * Fotoğraflar yalnızca veri başarıyla çözümlendikten sonra kopyalanır.
     */
    fun import(repo: Repository, input: InputStream): AppData {
        val bytes = input.readBytes()
        val isZip = bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
        if (!isZip) {
            val data = repo.parseImport(bytes.decodeToString())
            repo.replaceAll(data)
            return data
        }
        var data: AppData? = null
        val photos = mutableMapOf<String, ByteArray>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                when {
                    e.name == DATA_ENTRY -> data = repo.parseImport(zip.readBytes().decodeToString())
                    e.name.startsWith(PHOTO_PREFIX) && !e.isDirectory -> {
                        val name = File(e.name).name
                        if (name.isNotBlank() && !name.contains("..")) photos[name] = zip.readBytes()
                    }
                }
                e = zip.nextEntry
            }
        }
        val parsed = data ?: throw IllegalArgumentException("Yedek dosyasında data.json bulunamadı")
        photos.forEach { (name, content) -> File(repo.photoDir, name).writeBytes(content) }
        repo.replaceAll(parsed)
        return parsed
    }
}
