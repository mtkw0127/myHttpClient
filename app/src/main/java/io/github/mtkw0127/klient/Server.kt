package io.github.mtkw0127.klient

import io.ktor.http.ContentType
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay

fun main() {
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/") {
                call.respondText("Hello, World!")
            }
            get("/chunked") {
                call.respondTextWriter(contentType = ContentType.Text.Plain) {
                    val messages = listOf(
                        "Hello",
                        "World",
                        "This",
                        "is",
                        "Chunked",
                        "response.",
                        "Good byte! See you later!",
                        "さようなら",
                    )
                    for (msg in messages) {
                        write(msg)
                        flush()
                        delay(500)
                    }
                }
            }
            post("/echo") {
                val body = call.receiveText()
                call.respondText("Echo: $body")
            }
        }
    }.start(wait = true)
}