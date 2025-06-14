package io.github.mtkw0127.klient

import io.github.mtkw0127.klient.model.Response
import java.io.ByteArrayOutputStream
import java.net.Socket

fun main() {
    val host = "localhost"
    val port = 8080
    Socket(host, port).use { socket ->
        val out = socket.outputStream
        val input = socket.inputStream

        val request = buildString {
            append("GET / HTTP/1.1\r\n")
            append("Host: $host\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }.toByteArray()

        out.write(request)
        out.flush()

        val buffer = ByteArray(8192)
        val response = ByteArrayOutputStream()
        var read: Int

        while (input.read(buffer).also { read = it } != -1) {
            response.write(buffer, 0, read)
        }

        val body = Response.from(response.toByteArray())

        println(body)
    }
}