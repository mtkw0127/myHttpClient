package io.github.mtkw0127.klient.data

import io.github.mtkw0127.klient.data.model.HttpClient
import io.github.mtkw0127.klient.data.model.sampleRequests

fun main() {
    val client = HttpClient()
    sampleRequests().forEach {
        val response = client.send(it)
        println(response)
    }
}