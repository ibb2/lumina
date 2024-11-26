package org.example.project.networking

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun runKtorServer(onRedirectReceived: (String) -> Unit) {
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/") {
                val code = call.request.queryParameters["code"]
                if (code != null) {
                    call.respondText("You can close this tab.", ContentType.Text.Html)
                    onRedirectReceived(code)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Missing 'code' parameter")
                }
            }
        }
    }.start(wait = false)
}