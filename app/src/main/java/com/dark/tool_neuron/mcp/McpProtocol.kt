package com.dark.tool_neuron.mcp

import com.dark.tool_neuron.plugins.MultiTurnToolResult
import com.dark.tool_neuron.plugins.PluginManager
import com.dark.gguf_lib.toolcalling.ToolDefinitionBuilder
import com.dark.gguf_lib.toolcalling.ToolCall
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small, dependency-free MCP JSON-RPC 2.0 core for the Android process.
 * Transport is intentionally separated so the same contract can be used by
 * the loopback HTTP adapter and future in-process transports.
 */
class McpProtocol(
    private val toolProvider: () -> List<ToolDefinitionBuilder> = {
        PluginManager.getEnabledToolDefinitions()
    },
    private val toolExecutor: suspend (String, JSONObject) -> MultiTurnToolResult = { name, args ->
        PluginManager.executeToolForMultiTurn(ToolCall(name = name, arguments = args))
    }
) {
    companion object {
        const val PROTOCOL_VERSION = "2025-06-18"
        private const val JSONRPC = "2.0"
    }

    suspend fun handle(request: JSONObject): JSONObject? {
        val id = if (request.has("id")) request.opt("id") else null
        val method = request.optString("method", "")
        if (request.optString("jsonrpc") != JSONRPC || method.isBlank()) {
            return error(id, -32600, "Invalid Request")
        }

        return try {
            when (method) {
                "initialize" -> result(id, JSONObject().apply {
                    put("protocolVersion", negotiatedVersion(request.optJSONObject("params")))
                    put("capabilities", JSONObject().put("tools", JSONObject()))
                    put("serverInfo", JSONObject()
                        .put("name", "toolneuron-local")
                        .put("version", "2.0.3-mcp"))
                })
                "notifications/initialized" -> null
                "ping" -> result(id, JSONObject())
                "tools/list" -> result(id, JSONObject().put("tools", listTools()))
                "tools/call" -> callTool(id, request.optJSONObject("params"))
                else -> error(id, -32601, "Method not found: $method")
            }
        } catch (e: Exception) {
            error(id, -32603, e.message ?: "Internal error")
        }
    }

    private fun negotiatedVersion(params: JSONObject?): String {
        val requested = params?.optString("protocolVersion").orEmpty()
        return if (requested == PROTOCOL_VERSION) requested else PROTOCOL_VERSION
    }

    private fun listTools(): JSONArray = JSONArray().also { array ->
        toolProvider().forEach { builder ->
            val openAi = builder.build().toOpenAIFormat()
            val schema = openAi.optJSONObject("parameters") ?: JSONObject()
                .put("type", "object")
            array.put(JSONObject()
                .put("name", openAi.optString("name", builder.name))
                .put("description", openAi.optString("description", ""))
                .put("inputSchema", schema))
        }
    }

    private suspend fun callTool(id: Any?, params: JSONObject?): JSONObject {
        if (params == null) return error(id, -32602, "Missing params")
        val name = params.optString("name")
        if (name.isBlank()) return error(id, -32602, "Missing tool name")
        val args = params.optJSONObject("arguments") ?: JSONObject()
        val result = toolExecutor(name, args)
        val content = JSONArray().put(JSONObject()
            .put("type", "text")
            .put("text", result.resultJson))
        return result(id, JSONObject()
            .put("content", content)
            .put("isError", result.isError))
    }

    private fun result(id: Any?, value: JSONObject): JSONObject = JSONObject()
        .put("jsonrpc", JSONRPC)
        .put("id", id ?: JSONObject.NULL)
        .put("result", value)

    private fun error(id: Any?, code: Int, message: String): JSONObject = JSONObject()
        .put("jsonrpc", JSONRPC)
        .put("id", id ?: JSONObject.NULL)
        .put("error", JSONObject().put("code", code).put("message", message))
}
