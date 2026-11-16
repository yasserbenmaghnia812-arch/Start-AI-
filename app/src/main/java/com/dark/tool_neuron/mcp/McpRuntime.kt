package com.dark.tool_neuron.mcp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/** Explicit opt-in runtime entry point. The app does not expose a socket at startup. */
object McpRuntime {
    private val server = LocalMcpHttpServer(scope = CoroutineScope(Dispatchers.IO))

    @Synchronized
    fun start(): LocalMcpHttpServer.Endpoint = server.start()

    @Synchronized
    fun stop() = server.stop()

    fun endpoint(): LocalMcpHttpServer.Endpoint? = server.endpoint()
}
