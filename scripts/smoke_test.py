"""End-to-end smoke test: start the stream, confirm events flow through Kafka, and read the WebSocket.

Usage: python scripts/smoke_test.py [base_url]   (default http://localhost:8000)
Needs the stack running (docker compose -f docker/docker-compose.yml up -d --build --wait)
and the `websockets` client library. Exits non-zero with a FAIL: line on any failure.
"""

import asyncio
import json
import re
import signal
import sys
import time
import urllib.error
import urllib.request

import websockets

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8000").rstrip("/")
BUDGET_S = 180
DEADLINE = time.monotonic() + BUDGET_S
NAIVE_ISO = re.compile(r"\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(\.\d+)?")


class SmokeFailure(Exception):
    pass


def remaining(cap: float) -> float:
    left = DEADLINE - time.monotonic()
    if left <= 0:
        raise SmokeFailure("overall 180s budget exhausted")
    return min(cap, left)


def call(method: str, path: str) -> dict:
    req = urllib.request.Request(BASE + path, method=method)
    try:
        with urllib.request.urlopen(req, timeout=remaining(20)) as resp:
            return json.load(resp)
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as e:
        raise SmokeFailure(f"{method} {path}: {e}") from e


def fetch_text(path: str) -> str:
    try:
        with urllib.request.urlopen(BASE + path, timeout=remaining(20)) as resp:
            return resp.read().decode("utf-8")
    except (urllib.error.URLError, TimeoutError, UnicodeDecodeError) as e:
        raise SmokeFailure(f"GET {path}: {e}") from e


def check_dashboard() -> None:
    """The image must ship the frontend: index page and a static asset."""
    index = fetch_text("/")
    if "<html" not in index.lower():
        raise SmokeFailure("GET /: not the dashboard page")
    if "WebSocket" not in fetch_text("/static/js/websocket.js"):
        raise SmokeFailure("GET /static/js/websocket.js: unexpected content")


def wait_for_flow(before: dict) -> dict:
    status = before
    while time.monotonic() < DEADLINE - 5:
        status = call("GET", "/api/stream/status")
        if (status["events_published"] > before["events_published"]
                and status["events_processed"] > before["events_processed"]):
            return status
        time.sleep(2)
    raise SmokeFailure(f"no new events made it through Kafka: before={before} now={status}")


async def read_websocket() -> dict:
    """Collect the first events frame and at least two stats frames (initial + periodic broadcast)."""
    url = BASE.replace("http", "ws", 1) + "/ws"
    events, stats = None, []
    async with websockets.connect(url, open_timeout=remaining(10), close_timeout=2) as ws:
        while events is None or len(stats) < 2:
            msg = json.loads(await asyncio.wait_for(ws.recv(), timeout=remaining(30)))
            if msg.get("type") == "events" and events is None:
                events = msg.get("data")
            elif msg.get("type") == "stats":
                stats.append(msg.get("data"))
    if not isinstance(events, list) or not events:
        raise SmokeFailure(f"websocket 'events' frame carried no events: {events!r}")
    ts = events[0].get("timestamp") if isinstance(events[0], dict) else None
    if not isinstance(ts, str) or not NAIVE_ISO.fullmatch(ts):
        raise SmokeFailure(f"websocket event timestamp not naive ISO: {ts!r}")
    if not all(isinstance(st, dict) and "total_events" in st for st in stats):
        raise SmokeFailure(f"websocket 'stats' frames malformed: {stats!r}")
    return {"events": events, "stats": stats[-1], "stats_frames": len(stats)}


def run() -> None:
    check_dashboard()
    print("dashboard ok")
    before = call("GET", "/api/stream/status")
    started = call("POST", "/api/stream/start")
    if not (started.get("producer") and started.get("consumer")):
        raise SmokeFailure(f"stream did not start: {started}")
    status = wait_for_flow(before)
    print(f"flow ok: published={status['events_published']} processed={status['events_processed']}")
    try:
        seen = asyncio.run(read_websocket())
    except (OSError, asyncio.TimeoutError, websockets.WebSocketException, ValueError) as e:
        raise SmokeFailure(f"websocket: {e!r}") from e
    print(f"websocket ok: {len(seen['events'])} events in first frame, {seen['stats_frames']} stats frames")
    stats = call("GET", "/api/stats")
    if not stats.get("total_events", 0) > 0:
        raise SmokeFailure(f"/api/stats total_events not positive: {stats.get('total_events')!r}")
    print(f"stats ok: total_events={stats['total_events']}")


STOP_BUDGET_S = 15


def _timeout_after(seconds: int):
    def handler(signum, frame):
        raise SmokeFailure(f"hard timeout after {seconds}s")
    return handler


def stop_stream() -> None:
    signal.signal(signal.SIGALRM, _timeout_after(STOP_BUDGET_S))
    try:
        signal.alarm(STOP_BUDGET_S)
        req = urllib.request.Request(BASE + "/api/stream/stop", method="POST")
        urllib.request.urlopen(req, timeout=10).close()
    finally:
        signal.alarm(0)


def main() -> int:
    failure = None
    unwinding = True  # stays True only if run() raised something not handled below
    try:
        signal.signal(signal.SIGALRM, _timeout_after(BUDGET_S + 10))
        try:
            signal.alarm(BUDGET_S + 10)
            run()
        finally:
            signal.alarm(0)
        unwinding = False
    except SmokeFailure as e:
        failure, unwinding = str(e), False
    except (KeyError, TypeError, AttributeError) as e:
        failure, unwinding = f"unexpected response shape: {e!r}", False
    finally:
        # Always stop the stream, even while another exception propagates.
        try:
            stop_stream()
        except Exception as e:
            if failure is None and not unwinding:
                failure = f"POST /api/stream/stop: {e!r}"
            else:  # keep the primary failure; report the cleanup one alongside it
                print(f"warning: stop also failed: {e!r}")
    if failure is not None:
        print(f"FAIL: {failure}")
        return 1
    print("SMOKE TEST PASSED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
