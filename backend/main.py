from contextlib import asynccontextmanager
from datetime import datetime, timezone
from typing import Any

from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException
from pydantic import BaseModel, Field

ALLOWED_TOOLS = {"get_device_info", "get_battery_status", "get_current_app", "get_ui_tree", "open_app", "tap_element", "type_text", "create_scene", "write_script", "modify_project_file", "run_project", "stop_project", "get_debug_output"}

class TaskRequest(BaseModel):
    request: str = Field(min_length=1, max_length=8000)
    session_id: str = Field(min_length=1, max_length=128)

class ToolCall(BaseModel):
    name: str
    arguments: dict[str, Any] = Field(default_factory=dict)
    user_confirmed: bool = False

class TaskPlanner:
    def plan(self, request: str) -> list[ToolCall]:
        text = request.lower()
        if "godot" in text:
            return [ToolCall(name="get_current_app"), ToolCall(name="create_scene", arguments={"project_path": "", "scene_path": "res://Main.tscn"})]
        if "battery" in text or "البطارية" in request:
            return [ToolCall(name="get_battery_status")]
        return [ToolCall(name="get_current_app")]

planner = TaskPlanner()

@asynccontextmanager
async def lifespan(app: FastAPI):
    yield

app = FastAPI(title="ULTRA AI PHONE AGENT", version="0.1.0", lifespan=lifespan)

@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok", "service": "ultra-agent-backend"}

@app.post("/v1/tasks/plan")
async def plan_task(payload: TaskRequest) -> dict[str, Any]:
    calls = planner.plan(payload.request)
    return {"session_id": payload.session_id, "created_at": datetime.now(timezone.utc).isoformat(), "steps": [c.model_dump() for c in calls]}

@app.post("/v1/tools/validate")
async def validate_tool(call: ToolCall) -> dict[str, Any]:
    if call.name not in ALLOWED_TOOLS:
        raise HTTPException(status_code=400, detail="Tool is not allowlisted")
    sensitive = {"open_app", "tap_element", "type_text", "create_scene", "write_script", "modify_project_file", "run_project"}
    return {"allowed": call.name not in sensitive or call.user_confirmed, "requires_confirmation": call.name in sensitive}

@app.websocket("/v1/ws/{session_id}")
async def task_stream(websocket: WebSocket, session_id: str):
    await websocket.accept()
    try:
        while True:
            message = await websocket.receive_json()
            request = str(message.get("request", "")).strip()
            if not request:
                await websocket.send_json({"type": "error", "message": "request is required"})
                continue
            await websocket.send_json({"type": "task_started", "session_id": session_id})
            for index, call in enumerate(planner.plan(request)):
                await websocket.send_json({"type": "step", "index": index, "tool": call.name, "status": "awaiting_device_execution", "arguments": call.arguments})
            await websocket.send_json({"type": "task_waiting", "message": "The Android client must approve and execute device tools."})
    except WebSocketDisconnect:
        return
