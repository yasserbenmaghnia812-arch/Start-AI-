package com.dark.tool_neuron.mcp

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class McpProtocolTest {
    private val protocol = McpProtocol(
        toolProvider = { emptyList() },
        toolExecutor = { name, _ ->
            com.dark.tool_neuron.plugins.MultiTurnToolResult(
                toolName = name,
                resultJson = "{\"ok\":true}",
                isError = false,
                pluginName = "test",
                executionTimeMs = 1
            )
        }
    )

    @Test
    fun initializeNegotiatesSupportedVersion() = runBlocking {
        val response = protocol.handle(JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 1)
            .put("method", "initialize")
            .put("params", JSONObject().put("protocolVersion", "2025-06-18")))!!
        assertEquals("2025-06-18", response.getJSONObject("result").getString("protocolVersion"))
        assertEquals("toolneuron-local", response.getJSONObject("result")
            .getJSONObject("serverInfo").getString("name"))
    }

    @Test
    fun notificationHasNoResponse() = runBlocking {
        val response = protocol.handle(JSONObject()
            .put("jsonrpc", "2.0")
            .put("method", "notifications/initialized"))
        assertTrue(response == null)
    }

    @Test
    fun unknownMethodReturnsJsonRpcError() = runBlocking {
        val response = protocol.handle(JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", "x")
            .put("method", "not-a-method"))!!
        assertEquals(-32601, response.getJSONObject("error").getInt("code"))
    }

    @Test
    fun toolCallReturnsMcpContent() = runBlocking {
        val response = protocol.handle(JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 2)
            .put("method", "tools/call")
            .put("params", JSONObject()
                .put("name", "echo")
                .put("arguments", JSONObject().put("value", "x"))))!!
        val result = response.getJSONObject("result")
        assertEquals(false, result.getBoolean("isError"))
        assertEquals("text", result.getJSONArray("content").getJSONObject(0).getString("type"))
    }
}
