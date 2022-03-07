import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.Application
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.engine.embeddedServer
import io.ktor.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay

const val numRequests = 1000
const val batchSize = 100

fun Application.module() {
    val httpClient = HttpClient(io.ktor.client.engine.cio.CIO)

    install(DefaultHeaders)
    install(Routing) {
        get("/") {
            val r = 1..numRequests
            val total = r.chunked(batchSize).fold(0) { acc, items ->
                val requests = items.map {
                    val echoUrl = url {
                        protocol = URLProtocol.createOrDefault(call.request.origin.scheme)
                        host = call.request.origin.host
                        port = call.request.origin.port
                        path("echo", it.toString())
                    }

                    async {
                        httpClient.get<String>(echoUrl)
                    }
                }

                acc + requests.awaitAll().sumOf(String::toInt)
            }
            call.respondText(total.toString())
        }
        get("/echo/{s}") {
            val s = call.parameters["s"]
            println("s=$s")
            delay(1000) // makes it possible to see the batching
            call.respondText(s ?: "")
        }
    }
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(io.ktor.server.cio.CIO, port, watchPaths = listOf("build"), module = Application::module).start(true)
}
