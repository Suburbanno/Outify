package cc.tomko.outify.core

import fi.iki.elonen.NanoHTTPD
import java.net.SocketException
import kotlin.concurrent.thread

class AuthCallbackServer(
    port: Int = 5588,
    private val packageName: String = "cc.tomko.outify",
    private val onCodeReceived: (code: String, state: String?) -> Unit
) : NanoHTTPD(port) {

    private val stopDelayMs = 350L

    override fun serve(session: IHTTPSession): Response {
        try {
            if (session.uri == "/login") {
                val code = session.parameters["code"]?.firstOrNull()
                val state = session.parameters["state"]?.firstOrNull()

                if (code != null) {
                    thread {
                        try {
                            // give the server time to finish sending the response
                            Thread.sleep( stopDelayMs )
                            onCodeReceived(code, state)
                        } catch (t: Throwable) { } finally {
                            try {
                                stop()
                            } catch (_: Throwable) {}
                        }
                    }

                    val intentUri =
                        "intent://auth_complete#Intent;scheme=outify;package=$packageName;end"

                    val response = newFixedLengthResponse(
                        Response.Status.REDIRECT,
                        "text/plain",
                        ""
                    )
                    response.addHeader("Location", intentUri)
                    response.addHeader("Connection", "close")
                    return response
                }

                val failIntent = "intent://auth_failed#Intent;scheme=outify;package=$packageName;end"
                val fallback = newFixedLengthResponse(Response.Status.REDIRECT, "text/plain", "")
                fallback.addHeader("Location", failIntent)
                fallback.addHeader("Connection", "close")
                return fallback
            }

            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")

        } catch (e: SocketException) {
            println("AuthCallbackServer: socket closed while responding")
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "")
        } catch (t: Throwable) {
            t.printStackTrace()
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error")
        }
    }
}