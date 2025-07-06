package io.github.mtkw0127.klient

import io.github.mtkw0127.klient.model.HttpClient
import io.github.mtkw0127.klient.model.sampleRequests

fun main() {
    val client = HttpClient()
    sampleRequests().forEach {
        val response = client.send(it)
        println(response)
    }
}