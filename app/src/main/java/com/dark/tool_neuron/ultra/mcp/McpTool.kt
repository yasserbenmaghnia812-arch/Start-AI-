package com.dark.tool_neuron.ultra.mcp

import kotlinx.serialization.Serializable

@Serializable
data class ToolCallContext(
    val sessionId: String,
    val userConfirmed: Boolean = false,
    val requiredPermission: String? = null,
    val targetPackage: String? = null
)

@Serializable
data class ToolDefinition(
    val name: String,
    val domain: String,
    val description: String,
    val requiresConfirmation: Boolean = false,
    val requiresAccessibility: Boolean = false
)

object UltraToolAllowlist {
    val definitions: List<ToolDefinition> = listOf(
        ToolDefinition("open_app", "android", "Open an installed application", requiresConfirmation = true),
        ToolDefinition("get_current_app", "android", "Read the foreground package"),
        ToolDefinition("read_screen", "android", "Read authorized screen content", requiresAccessibility = true),
        ToolDefinition("get_ui_tree", "android", "Read the accessibility tree", requiresAccessibility = true),
        ToolDefinition("tap_element", "android", "Tap a semantic element", requiresAccessibility = true),
        ToolDefinition("type_text", "android", "Enter text into a focused field", requiresConfirmation = true, requiresAccessibility = true),
        ToolDefinition("take_screenshot", "android", "Capture a consented screenshot"),
        ToolDefinition("get_device_info", "system", "Read non-secret device information"),
        ToolDefinition("get_battery_status", "system", "Read battery status"),
        ToolDefinition("get_permission_status", "system", "Read application permission state"),
        ToolDefinition("create_scene", "godot", "Create a scoped Godot scene", requiresConfirmation = true),
        ToolDefinition("write_script", "godot", "Write a scoped Godot script", requiresConfirmation = true),
        ToolDefinition("modify_project_file", "godot", "Modify a scoped Godot file", requiresConfirmation = true),
        ToolDefinition("run_project", "godot", "Run a configured Godot project", requiresConfirmation = true),
        ToolDefinition("stop_project", "godot", "Stop a configured Godot project"),
        ToolDefinition("get_debug_output", "godot", "Read Godot debug output")
    )

    fun requireAllowed(name: String): ToolDefinition = definitions.firstOrNull { it.name == name }
        ?: error("Tool is not allowlisted: $name")
}
