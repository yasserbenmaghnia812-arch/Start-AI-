package com.dark.tool_neuron.ultra.agent

import java.util.UUID
import com.dark.tool_neuron.ultra.mcp.UltraToolAllowlist

class InitialPlanner {
    fun createTask(request: String): AgentTaskState {
        require(request.isNotBlank()) { "Request must not be blank" }
        val normalized = request.trim()
        val steps = when {
            normalized.contains("battery", ignoreCase = true) || normalized.contains("البطارية") -> listOf(
                PlannedStep("battery", "get_battery_status", "Read battery status")
            )
            normalized.contains("current app", ignoreCase = true) || normalized.contains("التطبيق الحالي") -> listOf(
                PlannedStep("foreground", "get_current_app", "Read the foreground package")
            )
            else -> emptyList()
        }
        steps.forEach { UltraToolAllowlist.requireAllowed(it.toolName) }
        return AgentTaskState(
            taskId = UUID.randomUUID().toString(),
            request = normalized,
            phase = if (steps.isEmpty()) TaskPhase.AWAITING_PERMISSION else TaskPhase.PLANNING,
            steps = steps
        )
    }
}