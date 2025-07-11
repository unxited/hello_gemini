package org.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

class VisaBulletinFetcher {
    private val client = HttpClient(CIO)

    suspend fun fetchHtml(url: String): String {
        val response: HttpResponse = client.get(url)
        return response.bodyAsText()
    }
}
