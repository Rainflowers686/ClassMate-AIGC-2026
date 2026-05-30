package com.classmate.core.network

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetSocketAddress

class SimpleHttpEngineTest {

    @Test
    fun execute_doesNotFollowRedirects() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        try {
            server.createContext("/redirect") { exchange ->
                exchange.responseHeaders.add("Location", "/final")
                exchange.sendResponseHeaders(302, -1)
                exchange.close()
            }
            server.createContext("/final") { exchange ->
                val body = "followed".toByteArray(Charsets.UTF_8)
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            server.start()

            val port = server.address.port
            val response = SimpleHttpEngine().execute(
                HttpRequest(
                    method = "GET",
                    url = "http://127.0.0.1:$port/redirect"
                )
            )

            assertEquals(302, response.statusCode)
        } finally {
            server.stop(0)
        }
    }
}
