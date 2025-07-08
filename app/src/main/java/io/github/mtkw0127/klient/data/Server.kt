package io.github.mtkw0127.klient.data

import io.ktor.http.ContentType
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay

fun main() {
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/for_get") {
                call.respondText("I'm a GET.")
            }
            get("/for_chunked") {
                call.respondTextWriter(contentType = ContentType.Text.Plain) {
                    val messages = listOf(
                        "Chunked",
                        "Response"
                    )
                    for (msg in messages) {
                        write(msg)
                        flush()
                        delay(500)
                    }
                }
            }
            post("/for_post") {
                val body = call.receiveText()
                call.respondText("I'm a Post. Echo: $body")
            }
            put("/for_put") {
                val body = call.receiveText()
                call.respondText("I'm a Put. Echo: $body")
            }
            patch("/for_patch") {
                val body = call.receiveText()
                call.respondText("I'm a Patch. Echo: $body")
            }
            delete("/for_delete") {
                val body = call.receiveText()
                call.respondText("I'm a Delete")
            }
        }
    }.start(wait = true)
}