package com.dark.tool_neuron.ultra.agent

import kotlinx.serialization.Serializable

@Serializable
enum class TaskPhase { RECEIVED, PLANNING, AWAITING_PERMISSION, EXECUTING, OBSERVING, RECOVERING, COMPLETED, FAILED, CANCELLED }

@Serializable
data class PlannedStep(
    val id: String,
    val toolName: String,
    val rationale: String,
    val arguments: Map<String, String> = emptyMap(),
    val requiresConfirmation: Boolean = false
)

@Serializable
data class AgentTaskState(
    val taskId: String,
    val request: String,
    val phase: TaskPhase = TaskPhase.RECEIVED,
    val steps: List<PlannedStep> = emptyList(),
    val currentStepIndex: Int = -1,
    val observation: String? = null,
    val error: String? = null,
    val retryCount: Int = 0
)
