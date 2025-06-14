package io.github.mtkw0127.klient.model

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

        companion object {
            // """は生文字列リテラルであり、バックスラッシュでのエスケープ不要
            private val charsetRegex = Regex("""charset=([\w-]+)""")
            private const val KEY_CONTENT_TYPE = "Content-Type"
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
}