package com.dark.tool_neuron.mcp

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal Streamable HTTP MCP endpoint for local clients.
 * It binds only to 127.0.0.1, validates Origin, and requires a per-run token.
 * JSON responses are used instead of SSE, which is valid for request/response
 * MCP calls and keeps the Android implementation small and offline.
 */
class LocalMcpHttpServer(
    private val protocol: McpProtocol = McpProtocol(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "LocalMcpHttpServer"
        private const val MAX_BODY_BYTES = 1_048_576
        private val ALLOWED_ORIGINS = setOf(
            "http://localhost",
            "http://127.0.0.1",
            "app://toolneuron"
        )
    }

    data class Endpoint(val port: Int, val token: String)

    @Volatile private var endpoint: Endpoint? = null
    private var serverSocket: ServerSocket? = null
    private var job: Job? = null
    private val running = AtomicBoolean(false)

    fun start(preferredPort: Int = 0): Endpoint {
        check(!running.get()) { "MCP server is already running" }
        val socket = ServerSocket(preferredPort, 16, InetAddress.getByName("127.0.0.1"))
        serverSocket = socket
        val value = Endpoint(socket.localPort, UUID.randomUUID().toString())
        endpoint = value
        running.set(true)
        job = scope.launch {
            while (isActive && running.get()) {
                try {
                    handle(socket.accept())
                } catch (e: Exception) {
                    if (running.get()) Log.w(TAG, "MCP connection failed", e)
                }
            }
        }
        return value
    }

    fun stop() {
        running.set(false)
        endpoint = null
        try { serverSocket?.close() } catch (_: Exception) { }
        serverSocket = null
        job?.cancel()
        job = null
    }

    fun endpoint(): Endpoint? = endpoint

    private suspend fun handle(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 10_000
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
            val firstLine = reader.readLine() ?: return
            val requestLine = firstLine.split(' ')
            if (requestLine.size < 2) return respond(client, 400, "Invalid HTTP request")
            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = reader.readLine() ?: return respond(client, 400, "Incomplete HTTP headers")
                if (line.isEmpty()) break
                val index = line.indexOf(':')
                if (index > 0) headers[line.substring(0, index).trim().lowercase()] = line.substring(index + 1).trim()
            }
            if (requestLine[1] != "/mcp") return respond(client, 404, "Not found")
            if (requestLine[0] == "GET") return respond(client, 405, "GET is not enabled; use POST")
            if (requestLine[0] != "POST") return respond(client, 405, "Method not allowed")

            val token = endpoint?.token
            if (token == null || headers["x-toolneuron-token"] != token) {
                return respond(client, 401, "Missing or invalid MCP token")
            }
            val origin = headers["origin"]
            if (origin != null && origin !in ALLOWED_ORIGINS) {
                return respond(client, 403, "Origin not allowed")
            }
            val length = headers["content-length"]?.toIntOrNull()
                ?: return respond(client, 411, "Content-Length required")
            if (length !in 1..MAX_BODY_BYTES) return respond(client, 413, "Request body too large")
            val body = CharArray(length)
            var read = 0
            while (read < length) {
                val count = reader.read(body, read, length - read)
                if (count < 0) return respond(client, 400, "Incomplete request body")
                read += count
            }
            val request = try { JSONObject(String(body)) }
            catch (_: Exception) { return respond(client, 400, "Invalid JSON") }
            val response = protocol.handle(request)
            if (response == null) respond(client, 202, "") else respond(client, 200, response.toString())
        }
    }

    private fun respond(socket: Socket, status: Int, body: String) {
        val reason = when (status) {
            200 -> "OK"; 202 -> "Accepted"; 400 -> "Bad Request"; 401 -> "Unauthorized"
            403 -> "Forbidden"; 404 -> "Not Found"; 405 -> "Method Not Allowed"
            411 -> "Length Required"; 413 -> "Payload Too Large"; else -> "Error"
        }
        val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)
        writer.write("HTTP/1.1 $status $reason\r\n")
        writer.write("Content-Type: application/json; charset=utf-8\r\n")
        writer.write("Content-Length: ${body.toByteArray(Charsets.UTF_8).size}\r\n")
        writer.write("Connection: close\r\n\r\n")
        writer.write(body)
        writer.flush()
    }
}
