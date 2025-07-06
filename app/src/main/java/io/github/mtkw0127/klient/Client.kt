package io.github.mtkw0127.klient

import io.github.mtkw0127.klient.model.Response
import java.net.Socket
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class Method(val value: String) {
    GET(
        value = "GET"
    ),
    POST(
        value = "POST"
    )
}

enum class ContentType(val value: String) {
    FORM_URLENCODED(
        value = "application/x-www-form-urlencoded"
    ),
    APPLICATION_JSON(
        value = "application/json"
    );
}

fun main() {
    val host = "localhost"
    val path = "/chunked"
    val port = 8080
    val method = Method.GET
    // For Post
    val content = "name=mtkw&age=31&country=Japan"
    val contentType = ContentType.FORM_URLENCODED
    Socket(host, port).use { socket ->
        val out = socket.outputStream

        // Base requests
        val requests = mutableListOf(
            "${method.value} $path HTTP/1.1",
            "Host: $host",
        )

        val urlEncodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8.toString())

        if (method == Method.POST) {
            requests.add("Content-Type: ${contentType.value}")
            if (contentType == ContentType.FORM_URLENCODED) {
                requests.add("Content-Length: ${urlEncodedContent.toByteArray().size}")
            } else {
                requests.add("Content-Length: ${content.toByteArray().size}")
            }
        }

        // End
        requests.add("Connection: keep-alive")
        requests.add("\r\n")

        val requestBytes = requests.joinToString("\r\n").toByteArray()

        out.write(requestBytes)
        if (method == Method.POST) {
            val byteContent = if (contentType == ContentType.FORM_URLENCODED) {
                urlEncodedContent
            } else {
                content
            }.toByteArray()
            out.write(byteContent)
        }
        out.flush()

        println("==Sending request to server==")
        requests.forEach(::println)
        if (method == Method.POST) {
            val content = if (contentType == ContentType.FORM_URLENCODED) {
                urlEncodedContent
            } else {
                content
            }
            println(content)
        }

        val response = Response.from(input = socket.inputStream)

        println("==Parsed response==")
        println(response)
    }
}