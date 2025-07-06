package io.github.mtkw0127.klient.data.model

import java.net.Socket


class HttpClient {
    private val connectionPool = mutableMapOf<String, Socket>()

    fun send(request: Request): Response {
        val socket = getOrCreateSocket(request.host, request.port)
        socket.outputStream.write(request.build())
        socket.outputStream.flush()
        val response = Response.from(socket.inputStream)
        when {
            response.headers.isClose -> remove(request.host, request.port)
            response.headers.isKeepAlive.not() -> remove(request.host, request.port)
        }
        return response
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

    private fun remove(host: String, port: Int) {
        val key = "$host:$port"
        connectionPool[key]?.close()
        connectionPool.remove(key)
    }
}