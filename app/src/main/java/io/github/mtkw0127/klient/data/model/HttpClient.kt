package io.github.mtkw0127.klient.data.model

import java.net.Socket


class HttpClient {
    private val connectionPool = mutableMapOf<String, Socket>()

    fun send(request: Request): Response {
        val socket = getOrCreateSocket(request.host, request.port)
        socket.outputStream.write(request.build())
        socket.outputStream.flush()
        return Response.from(socket.inputStream)
    }

    private fun getOrCreateSocket(host: String, port: Int): Socket {
        val key = "$host:$port"
        val socket = connectionPool[key]
        return if (socket != null && socket.isClosed.not()) {
            socket
        } else {
            val newSocket = Socket(host, port)
            connectionPool[key] = newSocket
            newSocket
        }
    }
}