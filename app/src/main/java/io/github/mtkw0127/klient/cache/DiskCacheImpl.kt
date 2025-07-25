package io.github.mtkw0127.klient.cache

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.security.MessageDigest

class DiskCacheImpl(
    private val cacheDir: File,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Cache {

    override suspend fun saveHeader(
        uri: URI,
        byteArray: ByteArray
    ) = withContext(dispatcher) {
        val file = getFile(uri, FileType.HEADER)
        saveFile(file, byteArray)
    }

    override suspend fun saveBody(
        uri: URI,
        byteArray: ByteArray
    ) = withContext(dispatcher) {
        val file = getFile(uri, FileType.BODY)
        saveFile(file, byteArray)
    }

    private fun saveFile(file: File, byteArray: ByteArray) {
        if (file.exists()) {
            file.delete()
            file.createNewFile()
        }
        try {
            FileOutputStream(file).use {
                it.write(byteArray)
                it.flush()
            }
        } catch (e: IOException) {
            throw e
        }
    }

    override suspend fun getBody(uri: URI): ByteArray? = withContext(dispatcher) {
        val file = getFile(uri, FileType.BODY)
        if (file.exists().not()) return@withContext null
        FileInputStream(file).use { inputStream ->
            return@withContext inputStream.readBytes()
        }
    }

    override suspend fun clear(uri: URI): Unit = withContext(dispatcher) {
        FileType.entries.forEach {
            getFile(uri, it).delete()
        }
    }

    override suspend fun clearAll(): Unit = withContext(dispatcher) {
        cacheDir.deleteRecursively()
    }


    private fun getFile(
        uri: URI,
        fileType: FileType,
    ): File {
        return File(cacheDir, "${createHashFrom(uri)}.${fileType.extension}")
    }

    enum class FileType(
        val extension: String
    ) {
        HEADER("header"),
        BODY("body")
    }

    /**
     * URIをMD5でハッシュ化して、16進数文字列に変換する
     * @param uri ハッシュ化するURI
     * @return ハッシュ化されたURIの16進数文字列
     */
    private fun createHashFrom(uri: URI): String {
        // "http://example.com/test"をバイト列として扱いたいため、UTF-8でエンコードする
        val bytes = uri.toString().toByteArray(Charsets.UTF_8)
        // URIを文字コードでエンコードしたbyteArrayをMD5でハッシュ化する
        // MD5は128ビットのハッシュ値を生成するため、256ビットのハッシュ値を得るためには、16進数で表現すると64文字の文字列になる
        // SHA256に比べて高速だが衝突の可能性はある。ファイル名に利用するだけなので問題ないと判断。
        val digest = MessageDigest.getInstance("MD5").digest(bytes)
        // 256bitsのハッシュを16進数文字列で表現する
        // 先頭4bitを16進数で表現すると、64文字の文字列になる
        return digest.joinToString("") { "%02x".format(it) }
    }
}