package io.github.mtkw0127.klient.model

import java.io.ByteArrayOutputStream
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

        companion object {
            // """は生文字列リテラルであり、バックスラッシュでのエスケープ不要
            private val charsetRegex = Regex("""charset=([\w-]+)""")
            private const val KEY_CONTENT_TYPE = "content-type"
            private const val KEY_TRANSFER_ENCODING = "transfer-encoding"
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
        fun from(responseBytes: ByteArray): Response {
            val responseString = responseBytes.toString(Charsets.ISO_8859_1)

            val headerPart = responseString.split("\r\n\r\n", limit = 2)[0]

            val status = headerPart
                .split("\r\n")[0]
                .let { statusLine ->
                    Status(statusLine)
                }
            val headers = headerPart.split("\r\n").drop(1).associate {
                val parts = it.split(":", limit = 2)
                parts[0].lowercase() to parts[1].trim()
            }.let { contentLines ->
                Headers(contentLines)
            }

            // ヘッダーとボディの区切りは "\r\n\r\n" であるため、そこからボディを抽出
            // "\r\n\r\n"のindexを見つけて、改行分の4バイトをスキップしてボディを取得
            val bodyBytes =
                responseBytes
                    .sliceArray(
                        indices = responseString.indexOf("\r\n\r\n") + 4 until responseBytes.size
                    )
                    .let { rawBytes ->
                        if (headers.isTransferEncodingChunked) {
                            parseTransferEncodingChunked(rawBytes, headers)
                        } else {
                            rawBytes
                        }
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