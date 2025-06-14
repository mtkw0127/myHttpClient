package io.github.mtkw0127.klient

import io.ktor.utils.io.charsets.forName
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


        val body = parseResponse(response.toByteArray())

        println("Response Body")
        println(body)
    }
}

private fun parseResponse(responseBytes: ByteArray): String {
    val responseString = responseBytes.toString(Charsets.ISO_8859_1)

    val headerPart = responseString.split("\r\n", limit = 2)[0]

    // """は生文字列リテラルであり、バックスラッシュでのエスケープ不要
    val charset = Regex("""charset=([\w-]+)""")
        .find(headerPart)
        ?.groupValues
        ?.get(1)?.let {
            Charsets.forName(it)
        } ?: Charsets.UTF_8

    val bodyBytes =
        responseBytes.sliceArray(responseString.indexOf("\r\n\r\n") + 4 until responseBytes.size)

    val bodyString = bodyBytes.toString(charset)

    return bodyString
}