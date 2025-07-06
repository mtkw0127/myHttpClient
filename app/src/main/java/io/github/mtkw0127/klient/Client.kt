package io.github.mtkw0127.klient

import io.github.mtkw0127.klient.model.Request
import io.github.mtkw0127.klient.model.Response
import io.github.mtkw0127.klient.model.sampleRequests

fun main() {
    sampleRequests().forEach {
        val response = request(request = it)
        println(response)
    }
}

private fun request(
    request: Request,
): Response {
    // request to server
    val out = request.socket.outputStream
    out.write(request.build())
    out.flush()

    // read response from server
    val input = request.socket.inputStream
    val response = Response.from(input = input)

    return response
}