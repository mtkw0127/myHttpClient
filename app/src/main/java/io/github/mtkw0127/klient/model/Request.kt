package io.github.mtkw0127.klient.model

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed interface Request {
    val path: String
    val host: String
    val port: Int

    fun build(): ByteArray

    data class GET(
        override val path: String,
        override val host: String,
        override val port: Int,
    ) : Request {
        override fun build() = buildString {
            append("GET $path HTTP/1.1\r\n")
            append("Host: $host\r\n")
            append("Connection: keep-alive\r\n")
            append("\r\n")
        }.toByteArray()
    }

    data class POST(
        override val path: String,
        override val host: String,
        override val port: Int,
        val content: String,
        val contentType: ContentType,
        val data: String,
    ) : Request {
        enum class ContentType(val value: String) {
            FORM_URLENCODED(
                value = "application/x-www-form-urlencoded"
            ),
            APPLICATION_JSON(
                value = "application/json"
            );
        }

        override fun build(): ByteArray {
            val headerParts = buildString {
                append("POST $path HTTP/1.1\r\n")
                append("Host: $host\r\n")
                append("Connection: keep-alive\r\n")
                append("Content-Type: ${contentType.value}\r\n")
                when (contentType) {
                    ContentType.FORM_URLENCODED -> {
                        val encodedData = URLEncoder.encode(data, StandardCharsets.UTF_8.toString())
                        append("Content-Length: ${encodedData.toByteArray().size}\r\n")
                    }

                    ContentType.APPLICATION_JSON -> {
                        append("Content-Length: ${content.toByteArray().size}\r\n")
                    }
                }
                append("\r\n")
            }.toByteArray()

            val contentBytes = when (contentType) {
                ContentType.FORM_URLENCODED -> URLEncoder.encode(
                    data,
                    StandardCharsets.UTF_8.toString()
                )

                ContentType.APPLICATION_JSON -> content
            }.toByteArray()

            return headerParts + contentBytes
        }
    }
}

fun sampleRequests(): List<Request> {
    return listOf(
        sampleRequest1(),
        sampleRequest2(),
        sampleRequest3(),
        sampleRequest4()
    )
}

// For GET request
fun sampleRequest1(): Request {
    return Request.GET(
        path = "/greet",
        host = "localhost",
        port = 8080,
    )
}

// For POST request with form-urlencoded content type
fun sampleRequest2(): Request {
    return Request.POST(
        path = "/for_post",
        host = "localhost",
        port = 8080,
        content = "Echo: Hello, World!",
        contentType = Request.POST.ContentType.FORM_URLENCODED,
        data = "message=Hello%2C+World%21"
    )
}

// For JSON content type
fun sampleRequest3(): Request {
    return Request.POST(
        path = "/for_post",
        host = "localhost",
        port = 8080,
        content = "{\"message\": \"Hello, World!\"}",
        contentType = Request.POST.ContentType.APPLICATION_JSON,
        data = "{\"message\": \"Hello, World!\"}"
    )
}

// For chunked transfer encoding
fun sampleRequest4(): Request {
    return Request.GET(
        path = "/for_chunked",
        host = "localhost",
        port = 8080,
    )
}