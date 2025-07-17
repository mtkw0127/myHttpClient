package io.github.mtkw0127.klient.cache

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI
import java.security.MessageDigest

class DiskCacheImpl(
    private val cacheDir: File,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Cache {

    override suspend fun saveBody(
        uri: URI,
        byteArray: ByteArray
    ) = withContext(dispatcher) {
        val file = getFile(uri)
        if (file.exists()) {
            file.delete()
            file.createNewFile()
        }
        FileOutputStream(file).use {
            it.write(byteArray)
            it.flush()
        }
    }

    override suspend fun getBody(uri: URI): ByteArray? = withContext(dispatcher) {
        val file = getFile(uri)
        if (file.exists().not()) return@withContext null
        FileInputStream(getFile(uri)).use { inputStream ->
            return@withContext inputStream.readBytes()
        }
    }

    override suspend fun clear(uri: URI): Unit = withContext(dispatcher) {
        getFile(uri).delete()
    }

    override suspend fun clearAll(): Unit = withContext(dispatcher) {
        cacheDir.deleteRecursively()
    }

    private fun getFile(uri: URI): File {
        return File(cacheDir, "${sanitize(uri)}.body")
    }

    private fun sanitize(uri: URI): String {
        // TODO: sanitize the URI to create a valid file name
        val bytes = uri.toString().toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}