package io.github.mtkw0127.klient.data.model

import java.io.ByteArrayOutputStream
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

    data class DELETE(
        override val path: String,
        override val host: String,
        override val port: Int,
    ) : Request {
        override fun build() = buildString {
            append("DELETE $path HTTP/1.1\r\n")
            append("Host: $host\r\n")
            append("Connection: keep-alive\r\n")
            append("\r\n")
        }.toByteArray()
    }

    sealed interface WithContent : Request {
        val method: String
        val content: String
        val contentType: ContentType
        val data: String

        val multipartData: List<Part>

        data class Part(
            val name: String,
            val filename: String? = null,
            val contentType: String? = null,
            val content: ByteArray
        )

        companion object {
            const val MULTI_PART_BOUNDARY: String = "----MyHttpClientBoundary"
        }

        enum class ContentType(val value: String) {
            FORM_URLENCODED(
                value = "application/x-www-form-urlencoded"
            ),
            APPLICATION_JSON(
                value = "application/json"
            ),
            MULTIPART_FORM_DATA(
                value = "multipart/form-data; boundary=${MULTI_PART_BOUNDARY}"
            );
        }

        override fun build(): ByteArray {
            val contentBytes = when (contentType) {
                ContentType.FORM_URLENCODED -> URLEncoder.encode(
                    data,
                    StandardCharsets.UTF_8.toString()
                ).toByteArray()

                ContentType.APPLICATION_JSON -> content.toByteArray()

                ContentType.MULTIPART_FORM_DATA -> {
                    val outputStream = ByteArrayOutputStream()
                    val boundary = "--${MULTI_PART_BOUNDARY}\r\n"
                    val parts = multipartData.map { part ->
                        val header = StringBuilder().apply {
                            append(boundary)
                            append("Content-Disposition: form-data; name=\"${part.name}\"")
                            part.filename?.let {
                                append("; filename=\"${part.filename}\"")
                            }
                            append("\r\n")
                            part.contentType?.let {
                                append("Content-Type: $it\r\n")
                            }
                            append("\r\n")
                        }.toString()
                        header.toByteArray() + part.content + "\r\n".toByteArray()
                    }
                    val byteArrayList = parts + "--${MULTI_PART_BOUNDARY}--\r\n".toByteArray()
                    byteArrayList.forEach { outputStream.write(it) }
                    outputStream.toByteArray()
                }
            }

            val headerParts = buildString {
                append("$method $path HTTP/1.1\r\n")
                append("Host: $host\r\n")
                append("Connection: keep-alive\r\n")
                append("Content-Type: ${contentType.value}\r\n")
                append("Content-Length: ${contentBytes.size}\r\n")
                append("\r\n")
            }.toByteArray()

            return headerParts + contentBytes
        }
    }

    data class POST(
        override val method: String = "POST",
        override val path: String,
        override val host: String,
        override val port: Int,
        override val content: String,
        override val contentType: WithContent.ContentType,
        override val data: String,
        override val multipartData: List<WithContent.Part>,
    ) : WithContent

    data class PUT(
        override val method: String = "PUT",
        override val path: String,
        override val host: String,
        override val port: Int,
        override val content: String,
        override val contentType: WithContent.ContentType,
        override val data: String,
        override val multipartData: List<WithContent.Part>,
    ) : WithContent

    data class PATCH(
        override val method: String = "PATCH",
        override val path: String,
        override val host: String,
        override val port: Int,
        override val content: String,
        override val contentType: WithContent.ContentType,
        override val data: String,
        override val multipartData: List<WithContent.Part>,
    ) : WithContent
}

fun sampleRequests(): List<Request> {
    return listOf(
        sampleRequest1(),
        sampleRequest2(),
        sampleRequest3(),
        sampleRequest4(),
        sampleRequest5(),
        sampleRequest6(),
        sampleRequest7(),
        sampleRequest8(),
    )
}

// For GET request
fun sampleRequest1(): Request {
    return Request.GET(
        path = "/for_get",
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
        contentType = Request.WithContent.ContentType.FORM_URLENCODED,
        data = "message=Hello%2C+World%21",
        multipartData = emptyList()
    )
}

// For JSON content type
fun sampleRequest3(): Request {
    return Request.POST(
        path = "/for_post",
        host = "localhost",
        port = 8080,
        content = "{\"message\": \"Hello, World!\"}",
        contentType = Request.WithContent.ContentType.APPLICATION_JSON,
        data = "{\"message\": \"Hello, World!\"}",
        multipartData = emptyList()
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

// For PUT
fun sampleRequest5(): Request {
    return Request.PUT(
        path = "/for_put",
        host = "localhost",
        port = 8080,
        content = "{\"message\": \"Hello, World!\"}",
        contentType = Request.WithContent.ContentType.APPLICATION_JSON,
        data = "{\"message\": \"Hello, World!\"}",
        multipartData = emptyList()
    )
}

// For PUT
fun sampleRequest6(): Request {
    return Request.PATCH(
        path = "/for_patch",
        host = "localhost",
        port = 8080,
        content = "{\"message\": \"Hello, World!\"}",
        contentType = Request.WithContent.ContentType.APPLICATION_JSON,
        data = "{\"message\": \"Hello, World!\"}",
        multipartData = emptyList()
    )
}

// For PUT
fun sampleRequest7(): Request {
    return Request.DELETE(
        path = "/for_delete",
        host = "localhost",
        port = 8080,
    )
}

// For Multipart
fun sampleRequest8(): Request {
    return Request.POST(
        path = "/for_post",
        host = "localhost",
        port = 8080,
        content = "Echo: Hello, World!",
        contentType = Request.WithContent.ContentType.MULTIPART_FORM_DATA,
        data = "message=Hello%2C+World%21",
        multipartData = emptyList(), // TODO: Add multipart data
    )
}