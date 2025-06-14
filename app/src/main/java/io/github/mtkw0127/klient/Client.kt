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

        val body = parseResponse(response.toByteArray())

        println("Response Body")
        println(body)
    }
}

private fun parseResponse(responseBytes: ByteArray): Response {
    val responseString = responseBytes.toString(Charsets.ISO_8859_1)

    val headerPart = responseString.split("\r\n\r\n", limit = 2)[0]

    val statusLine = headerPart.split("\r\n")[0]
    val contentLines = headerPart.split("\r\n").drop(1).associate {
        val parts = it.split(":", limit = 2)
        parts[0] to parts[1]
    }

    // ヘッダーとボディの区切りは "\r\n\r\n" であるため、そこからボディを抽出
    // "\r\n\r\n"のindexを見つけて、改行分の4バイトをスキップしてボディを取得
    val bodyBytes =
        responseBytes.sliceArray(responseString.indexOf("\r\n\r\n") + 4 until responseBytes.size)

    return Response(
        status = Response.Status(statusLine),
        headers = Response.Headers(contentLines),
        bodyBytes = bodyBytes,
    )
}