package io.github.mtkw0127.klient.data.model

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset

data class Response(
    val status: Status,
    val headers: Headers,
    val bodyBytes: ByteArray,
) {
    data class Status(
        val rawStatus: String
    )

    data class Headers(
        val values: Map<String, String>
    ) {
        val charset: Charset
            get() {
                return charsetRegex
                    .find(checkNotNull(values[KEY_CONTENT_TYPE]))
                    ?.groupValues
                    ?.get(1)?.let {
                        Charset.forName(it)
                    } ?: Charsets.UTF_8
            }

        val isTransferEncodingChunked: Boolean
            get() = values[KEY_TRANSFER_ENCODING] == "chunked"

        val contentLength: Int?
            get() = values[KEY_CONTENT_LENGTH]?.toInt(radix = 10)

        val isKeepAlive: Boolean
            get() = values[KEY_CONNECTION] == "keep-alive"

        val isClose: Boolean
            get() = values[KEY_CONNECTION] == "close"

        companion object {
            // """は生文字列リテラルであり、バックスラッシュでのエスケープ不要
            private val charsetRegex = Regex("""charset=([\w-]+)""")
            private const val KEY_CONTENT_TYPE = "content-type"
            private const val KEY_TRANSFER_ENCODING = "transfer-encoding"
            private const val KEY_CONTENT_LENGTH = "content-length"
            private const val KEY_CONNECTION = "connection"
        }
    }

    val body: String
        get() {
            return bodyBytes.toString(headers.charset)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Response

        if (status != other.status) return false
        if (headers != other.headers) return false
        if (!bodyBytes.contentEquals(other.bodyBytes)) return false
        if (body != other.body) return false

        return true
    }

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + bodyBytes.contentHashCode()
        result = 31 * result + body.hashCode()
        return result
    }

    override fun toString(): String {
        return """
status: $status\n
${headers.values.map { "${it.key}: ${it.value}" }.joinToString("\n")}
body: $body
        """.trim()
    }

    companion object {
        fun from(input: InputStream): Response {
            val window = ArrayDeque<Byte>(4)
            val headerResponse = ByteArrayOutputStream()

            // Header part
            while (true) {
                val byte = input.read()
                if (byte == -1) break
                headerResponse.write(byte)

                if (window.size == 4) window.removeFirst()
                window.addLast(byte.toByte())

                // \r\n\r\n に一致したら終了
                if (window.size == 4 &&
                    window.elementAt(0) == '\r'.code.toByte() &&
                    window.elementAt(1) == '\n'.code.toByte() &&
                    window.elementAt(2) == '\r'.code.toByte() &&
                    window.elementAt(3) == '\n'.code.toByte()
                ) {
                    break
                }
            }

            val headerString = headerResponse.toByteArray().toString(Charsets.ISO_8859_1)
            val status = headerString
                .split("\r\n")[0]
                .let { statusLine ->
                    Status(statusLine)
                }
            val headers = headerString.split("\r\n")
                .drop(1)
                .mapNotNull {
                    val parts = it.split(":", limit = 2)
                    when {
                        parts.size == 2 -> parts[0].lowercase() to parts[1].trim()
                        else -> null
                    }
                }
                .associate {
                    it
                }.let { contentLines ->
                    Headers(contentLines)
                }

            // Body part
            var totalReadBytes = 0
            val bodyResponse = ByteArrayOutputStream()
            val arrayDeque = ArrayDeque<Byte>(5)
            while (true) {
                val byte = input.read()
                totalReadBytes++
                bodyResponse.write(byte)
                when {
                    byte == -1 -> break
                    headers.isKeepAlive &&
                            headers.isTransferEncodingChunked.not() &&
                            totalReadBytes == checkNotNull(headers.contentLength) -> break

                    headers.isKeepAlive && headers.isTransferEncodingChunked -> {
                        // 0\r\n\r\nが来たら終了
                        if (arrayDeque.size == 5) arrayDeque.removeFirst()
                        arrayDeque.addLast(byte.toByte())
                        // \r\n\r\n に一致したら終了
                        if (arrayDeque.size == 5 &&
                            arrayDeque.elementAt(0) == '0'.code.toByte() &&
                            arrayDeque.elementAt(1) == '\r'.code.toByte() &&
                            arrayDeque.elementAt(2) == '\n'.code.toByte() &&
                            arrayDeque.elementAt(3) == '\r'.code.toByte() &&
                            arrayDeque.elementAt(4) == '\n'.code.toByte()
                        ) {
                            break
                        }
                    }
                }
            }

            val bodyBytes = if (headers.isTransferEncodingChunked) {
                parseTransferEncodingChunked(
                    bodyBytes = bodyResponse.toByteArray(),
                    headers = headers,
                )
            } else {
                bodyResponse.toByteArray()
            }

            return Response(
                status = status,
                headers = headers,
                bodyBytes = bodyBytes,
            )
        }


        private fun ByteArray.findCrlfIndex(startIndex: Int): Int {
            val targetBytes = "\r\n".toByteArray()
            val slicedBytes = sliceArray(startIndex until size)
            for (i in slicedBytes.indices) {
                if (slicedBytes.sliceArray(i until i + targetBytes.size)
                        .contentEquals(targetBytes)
                ) {
                    return startIndex + i
                }
            }
            return -1
        }

        private fun parseTransferEncodingChunked(
            bodyBytes: ByteArray,
            headers: Headers,
        ): ByteArray {
            var index = 0
            val output = ByteArrayOutputStream()
            while (true) {
                val chunkedSizeEndIndex = bodyBytes.findCrlfIndex(index)
                val chunkedSizeBytes = bodyBytes.sliceArray(
                    index until chunkedSizeEndIndex
                )
                // chunkedSizeBytesは16進数で表現されているため、文字列に変換してから整数に変換
                val chunkedSize = chunkedSizeBytes.toString(headers.charset).toInt(radix = 16)
                if (chunkedSize == 0) break
                val dataStartIndex =
                    index + chunkedSizeBytes.size + 2 // +2 for CRLF after the size
                val dataEndIndex = dataStartIndex + chunkedSize
                val chunkedData =
                    bodyBytes.sliceArray(dataStartIndex until dataEndIndex)
                output.write(chunkedData)
                index = dataEndIndex + 2 // +2 for CRLF after the chunk data
            }
            return output.toByteArray()
        }
    }

}