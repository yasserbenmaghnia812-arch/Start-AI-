package com.dark.tool_neuron.ultra.agent

import com.dark.tool_neuron.ultra.mcp.UltraToolAllowlist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialPlannerTest {
    @Test
    fun batteryRequestCreatesAllowlistedPlan() {
        val task = InitialPlanner().createTask("ما حالة البطارية؟")
        assertEquals(TaskPhase.PLANNING, task.phase)
        assertEquals("get_battery_status", task.steps.single().toolName)
        assertTrue(task.steps.all { runCatching { UltraToolAllowlist.requireAllowed(it.toolName) }.isSuccess })
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankRequestIsRejected() {
        InitialPlanner().createTask("   ")
    }
}