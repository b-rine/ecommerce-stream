# KRaft migration, dependency updates and smoke-test CI

Branch `chore/kraft-deps` off `origin/main` `c5af4e0`. One PR; one focused commit per task, with one-line `type: summary` messages and no trailers.

## Why

- Dependabot #6 bumps `confluentinc/cp-kafka` 7.5.0 → 8.4.0. Confluent 8 (Kafka 4) removed ZooKeeper, so merged alone it breaks the stack. The stack moves to a single-node KRaft broker instead, which also makes #5 (the `cp-zookeeper` minor bump) moot.
- #7, #8, #9 and #10 bump fastapi, uvicorn, python-dotenv, pydantic, pydantic-settings, kafka-python (2 → 3), websockets (12 → 17) and faker (20 → 40).
- The repo has no tests or CI, so the bumps get one end-to-end smoke test, which also runs in CI to vet future Dependabot PRs.
- Running with `PYTHONWARNINGS=default` showed deprecations the new versions surface:
  - kafka-python 3 wants `kafka.serializer` classes instead of bare callables (4 warnings);
  - pydantic's class-based `Config` (2 warnings);
  - `datetime.utcnow()` (4 call sites).
  These get fixed here, so the warning gate starts clean.
- #4 bumps `python:3.11-slim` → `3.15-slim`. A Python release can ship before its wheels do. The image moves to 3.14 (matching the other repos), and Dependabot stops proposing minor/major base-image bumps.
- `scripts/run_local.sh` checks port 8000, but it serves on 8001. It also calls the v1 `docker-compose` binary.
- README lists `Python 3.12+`. (Its clone URL, `b-rine/ecommerce-stream`, is canonical and stays as is; `kafka-ecommerce-stream` is an old name that redirects.) Its quick start also never installs `requirements.txt`.
- Two unused imports have been on `main` since before this change (pyflakes): `typing.Any` in `models.py` and `EcommerceEvent` in `app.py`.

The prototype (`/tmp/b4593c6a-ecom-proto`, single throwaway commit) passes the verifier below:
- Python 3.14.10 and cp-kafka 8.4.0;
- the smoke test, including flow and the WebSocket payloads;
- no warnings in the backend log;
- all eight runtime versions;
- the controlled negative failure.

The verifier rejects unchanged `main`, a `kafka-python` revert, and an action-version change.

## Tasks

Each file's diff below is the exact end state (taken from the prototype). Reproduce it byte for byte. Commit order matters for bisect: every commit must leave a stack that builds and passes the smoke test.

1. `docs: add kraft migration and dependency update plan`: this file, at `docs/plans/2026-11-25-kraft-deps.md`.
2. `build: run kafka in kraft mode on cp-kafka 8.4.0`:
   - `docker/docker-compose.yml`: drop the `zookeeper` service; `kafka` drops `depends_on`, `KAFKA_BROKER_ID`, `KAFKA_ZOOKEEPER_CONNECT` and host port 9093, and gains the KRaft settings.
   - `scripts/run_local.sh`: the echo reads `(Kafka)`, and the command becomes `docker compose -f docker/docker-compose.yml up -d kafka` (v2 here, so the launcher works on plugin-only hosts at every commit).
3. `build: update python dependencies`: `requirements.txt`, taking #7 + #8 + #9 + #10.
4. `refactor: use kafka-python serializer classes`: `backend/kafka_producer.py` (the `_JsonSerializer` class keeps `default=str`; key is `DefaultSerializer()`) and `backend/kafka_consumer.py` (`JsonSerializer()` / `DefaultSerializer()`, dropping the now-unused `import json`). This needs kafka-python 3's `kafka.serializer` module, so it must follow Task 3.
5. `refactor: use pydantic ConfigDict instead of class Config`: `backend/config.py` (`SettingsConfigDict`, same three settings) and `backend/models.py` (`ConfigDict(use_enum_values=True)`).
6. `fix: replace deprecated datetime.utcnow`: `backend/models.py`, `backend/events.py` and `backend/kafka_consumer.py` (both the ISO revenue timestamp and the `%Y-%m-%d %H:%M` minute bucket). Use `datetime.now(UTC).replace(tzinfo=None)`, which keeps both existing naive-UTC formats.
7. `refactor: drop unused imports`: `typing.Any` in `backend/models.py`, `EcommerceEvent` in `backend/app.py`.
8. `build: move the backend image to python 3.14`: `docker/Dockerfile.backend`, `FROM` line only.
9. `ci: stop dependabot bumping the python base image`: the `ignore` block under the `docker` ecosystem in `.github/dependabot.yml`.
10. `test: add an end-to-end smoke test`: `scripts/smoke_test.py`.
11. `ci: add a compose smoke-test workflow`:
    - `.github/workflows/ci.yml`, which selects `COMPOSE_FILE=docker/docker-compose.yml:docker/compose.ci.yml`;
    - `docker/compose.ci.yml`, which resets the backend's bind mounts so CI tests the image as built (a broken `COPY` fails);
    - the `github-actions` ecosystem appended to `.github/dependabot.yml`.
12. `fix: check the port run_local.sh serves on`: the port-check block (8000 → 8001), with corrected advice. The script hard-codes 8001, so `SERVER_PORT` doesn't help.
13. `docs: update readme for python 3.14, setup and the smoke test`.

## Smoke test design

`scripts/smoke_test.py`:
- runs on a 180 s overall budget, and every HTTP or WebSocket wait is capped by the remaining budget;
- a `SIGALRM` hard timeout at 190 s bounds anything a socket timeout doesn't (such as trickling responses); the local runner adds `timeout 240` on top;
- requires the published and processed counters to rise above their pre-start values;
- requires a WebSocket `events` frame with at least one event whose `timestamp` is naive ISO (`YYYY-MM-DDTHH:MM:SS[.ffffff]`, the format the frontend parses);
- requires two `stats` frames with `total_events`: the initial frame sent on connect and at least one periodic broadcast;
- requires `/api/stats.total_events` > 0;
- first checks the dashboard (`/` serves the page; `/static/js/websocket.js` serves the script), so an image missing the frontend fails;
- reports failures as `FAIL: <reason>` (with method and path for HTTP), including malformed payloads, and exits 1;
- always calls `/api/stream/stop` under its own 15 s hard deadline, without letting a failed stop mask the primary result.

## Verification (Claude, after implementation)

1. Make a detached checkout of the final SHA under `/tmp`.
2. Run `bash ecom-verify.sh <checkout> <sha>`; it must print `ECOM VERIFICATION PASSED`. The scripts are in the appendix and run from the job tmp dir. The local override maps host ports to 19092/18000 because 9092 and 8000 are in use locally, drops the bind mounts (so the image's own `COPY` is what runs), and sets `PYTHONWARNINGS=default`. It checks:
   - **static:**
     - exact requirements and exactly the 16 planned paths;
     - no ZooKeeper mention and no deprecated API patterns, with pyflakes clean;
     - the compose structure and a valid base64-UUID `CLUSTER_ID`;
     - Dockerfile `FROM` only;
     - the Dependabot ecosystems and the ignore rule;
     - workflow triggers, permissions, `COMPOSE_FILE`, action versions and teardown, and that the merged CI compose config has no backend mounts;
     - `run_local.sh` syntax, Compose v2 and port 8001.
   - **end to end:** Python 3.14 against cp-kafka 8.4.0; the smoke test passes; all eight runtime versions match the pins; no `Traceback`, `Warning` or `PydanticDeprecated` lines in the backend log. This gate covers Python `warnings`; the kafka-python logger's `WARNING` lines about topic auto-creation at startup are expected.
   - Backend-log capture must succeed and be non-empty, so an empty log can't pass the warning check.
   - **negative control:** unreachable Kafka must produce a controlled `FAIL:` line, the log must show the injected `nowhere:29092` broker, and no containers are left behind.
3. **Bisect check:** `bash ecom-bisect.sh <final checkout> <sha>...` must print `BISECT PASSED` for every runtime-changing commit: the commits after Tasks 2 through 8. It runs the final checkout's smoke test (by absolute path) against each commit, since `scripts/smoke_test.py` only lands in Task 10; warnings are allowed at these points.
   - Task 2: KRaft with the old deps on 3.11.
   - Task 3: new deps on 3.11.
   - Task 4: the serializer classes.
   - Task 5: pydantic `ConfigDict`.
   - Task 6: the datetime change.
   - Task 7: the unused imports.
   - Task 8: the 3.14 image.
   The script refuses an empty SHA list. Tasks 9–13 change only CI, the test, `run_local.sh` and docs; the final verifier covers them.
4. Cold Codex review of the implementation.

## Known limits

- The host-side listener (`localhost:9092`) isn't exercised end to end. Locally it's remapped; CI uses the default mapping, and the broker healthcheck uses it from inside the container.
- No data migration from an existing ZooKeeper volume: the README says to recreate the stack.
- The WebSocket check can't distinguish the on-connect `stats` frame from a periodic broadcast; it requires two frames, which the broadcaster alone could supply.

## Publication and PR closure

**Preflight:**
- no remote `chore/kraft-deps` branch and no PR from it;
- `main` has no rulesets.

**Push and open the PR:** push the verified SHA to `refs/heads/chore/kraft-deps`, then open the PR.

**CI gate (the PR's `pull_request` run of `ci.yml`):**
- `Smoke test` concludes `success`;
- its checkout log line `HEAD is now at <merge> Merge <head> into <base>` has `<head>` = the verified full SHA and `<base>` = live `main`;
- `setup-python` reports CPython 3.14.

**Immediately before closing**, re-read the replacement PR and live `main` together. The PR must be open, have base `main`, and have a head equal to the verified full SHA. Live `main` must still equal the base the green run tested. If `main` moved, wait for (or trigger) a fresh run and repeat the gate.

**Then close** #4, #5, #6, #7, #8, #9 and #10, each with a one-line reason pointing at the new PR, only if all of these hold:
- it is still open;
- it is authored by Dependabot;
- its head still matches the full SHA recorded here;
- auto-merge is off.

If any head moved, don't close that PR; report it to the owner instead. #1 and #2 are already closed (their pins were already on `main`).

These PRs close before the replacement merges, which matches the other repos in this pass. If the replacement is abandoned, Dependabot proposes the next versions on its weekly run.

Recorded heads:

| PR  | Full head SHA                              |
| --- | ------------------------------------------ |
| #4  | `2f33a2a2be5736c6ad2c7acfc12f3742e5803e9a` |
| #5  | `c3e0b1bb28e79843a527cfed39f748ac02231c90` |
| #6  | `2b5748ed738a75b1a2b7acd20e14f22a6ba7f887` |
| #7  | `c3befbb72ba8cc0158ee1dc4c25d21ac909b1924` |
| #8  | `9ffa3a4d675ec435515586d76ff4343dbcbe2247` |
| #9  | `0eedfb87cd6fac75c2afc4c63e8fb732b2e659ae` |
| #10 | `b33e05bbe6cdc2aaf972b17b7bd87245a13676f4` |

## Out of scope

- `scripts/setup_kafka.sh`: a host-tool helper, unchanged.
- `run_local.sh` beyond the two fixes: the Ctrl+C message, and macOS lacking `timeout`.

## Spec diffs (prototype vs `c5af4e0`)

### `docker/docker-compose.yml`

````diff
@@ -1,34 +1,21 @@
 services:
-  zookeeper:
-    image: confluentinc/cp-zookeeper:7.5.0
-    hostname: zookeeper
-    container_name: zookeeper
-    ports:
-      - "2181:2181"
-    environment:
-      ZOOKEEPER_CLIENT_PORT: 2181
-      ZOOKEEPER_TICK_TIME: 2000
-    healthcheck:
-      test: ["CMD", "nc", "-z", "localhost", "2181"]
-      interval: 10s
-      timeout: 5s
-      retries: 5
-
   kafka:
-    image: confluentinc/cp-kafka:7.5.0
+    image: confluentinc/cp-kafka:8.4.0
     hostname: kafka
     container_name: kafka
-    depends_on:
-      zookeeper:
-        condition: service_healthy
     ports:
       - "9092:9092"
-      - "9093:9093"
     environment:
-      KAFKA_BROKER_ID: 1
-      KAFKA_ZOOKEEPER_CONNECT: 'zookeeper:2181'
-      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
+      # Single-node KRaft (broker and controller in one process)
+      KAFKA_NODE_ID: 1
+      KAFKA_PROCESS_ROLES: broker,controller
+      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:29093
+      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
+      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
+      KAFKA_LISTENERS: PLAINTEXT://kafka:29092,CONTROLLER://kafka:29093,PLAINTEXT_HOST://0.0.0.0:9092
+      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
       KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
+      CLUSTER_ID: AMaT__e8SZ2X89nJXvuoDA
       KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
       KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
       KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
````

### `scripts/run_local.sh`

````diff
@@ -17,8 +17,8 @@ fi
 cd "$(dirname "$0")/.."
 
 # Start Docker Compose services
-echo "Starting Docker Compose services (Kafka, Zookeeper)..."
-docker-compose -f docker/docker-compose.yml up -d zookeeper kafka
+echo "Starting Docker Compose services (Kafka)..."
+docker compose -f docker/docker-compose.yml up -d kafka
 
 # Wait for Kafka to be ready
 echo "Waiting for Kafka to be ready..."
@@ -39,14 +39,12 @@ docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
 
 echo ""
 
-# Check if port 8000 is already in use
-if lsof -Pi :8000 -sTCP:LISTEN -t >/dev/null 2>&1 || (command -v netstat >/dev/null && netstat -tuln 2>/dev/null | grep -q ':8000 '); then
-    echo "❌ Error: Port 8000 is already in use"
+# Check if port 8001 is already in use
+if lsof -Pi :8001 -sTCP:LISTEN -t >/dev/null 2>&1 || (command -v netstat >/dev/null && netstat -tuln 2>/dev/null | grep -q ':8001 '); then
+    echo "❌ Error: Port 8001 is already in use"
     echo ""
-    echo "To fix this:"
-    echo "  1. Find what's using it: lsof -i :8000  (or: netstat -tuln | grep 8000)"
-    echo "  2. Stop that process, or"
-    echo "  3. Change port in .env: SERVER_PORT=8001"
+    echo "Find what's using it with: lsof -i :8001  (or: netstat -tuln | grep 8001)"
+    echo "Stop that process and try again."
     exit 1
 fi
 
````

### `requirements.txt`

````diff
@@ -1,8 +1,8 @@
-fastapi==0.104.1
-uvicorn[standard]==0.24.0
-websockets==12.0
-kafka-python==2.3.2
-faker==20.1.0
-python-dotenv==1.2.2
-pydantic==2.5.0
-pydantic-settings==2.1.0
+fastapi==0.165.0
+uvicorn[standard]==0.54.0
+websockets==17.4
+kafka-python==3.0.13
+faker==40.54.0
+python-dotenv==1.2.4
+pydantic==2.14.4
+pydantic-settings==2.16.3
````

### `backend/kafka_producer.py`

````diff
@@ -8,6 +8,7 @@ import logging
 from typing import Optional
 from kafka import KafkaProducer
 from kafka.errors import KafkaError
+from kafka.serializer import DefaultSerializer, Serializer
 
 from .config import settings
 from .events import EventGenerator
@@ -17,6 +18,11 @@ from .models import EcommerceEvent
 logger = logging.getLogger(__name__)
 
 
+class _JsonSerializer(Serializer):
+    def serialize(self, topic, headers, data):
+        return json.dumps(data, default=str).encode('utf-8')
+
+
 class KafkaProducerService:
     """Thread-safe Kafka producer service"""
     
@@ -33,8 +39,8 @@ class KafkaProducerService:
         try:
             producer = KafkaProducer(
                 bootstrap_servers=settings.kafka_bootstrap_servers,
-                value_serializer=lambda v: json.dumps(v, default=str).encode('utf-8'),
-                key_serializer=lambda k: k.encode('utf-8') if k else None,
+                value_serializer=_JsonSerializer(),
+                key_serializer=DefaultSerializer(),
                 acks='all',  # Wait for all replicas to acknowledge
                 retries=3,
                 max_in_flight_requests_per_connection=1,
````

### `backend/kafka_consumer.py`

````diff
@@ -1,14 +1,14 @@
 """Kafka consumer service for processing ecommerce events"""
 
-import json
 import threading
 import logging
 import queue
 import time
 from typing import Optional, List, Set
-from datetime import datetime
+from datetime import UTC, datetime
 from kafka import KafkaConsumer
 from kafka.errors import KafkaError
+from kafka.serializer import DefaultSerializer, JsonSerializer
 
 from .config import settings
 from .models import EcommerceEvent, Statistics, EventType
@@ -39,8 +39,8 @@ class KafkaConsumerService:
                 settings.kafka_topic,
                 bootstrap_servers=settings.kafka_bootstrap_servers,
                 group_id=settings.consumer_group_id,
-                value_deserializer=lambda m: json.loads(m.decode('utf-8')),
-                key_deserializer=lambda k: k.decode('utf-8') if k else None,
+                value_deserializer=JsonSerializer(),
+                key_deserializer=DefaultSerializer(),
                 auto_offset_reset='earliest',
                 enable_auto_commit=True,
                 consumer_timeout_ms=1000  # Timeout after 1 second of no messages
@@ -160,7 +160,7 @@ class KafkaConsumerService:
                     self.stats.total_revenue += event.total_amount
                     
                     # Track revenue over time
-                    timestamp = datetime.utcnow().isoformat()
+                    timestamp = datetime.now(UTC).replace(tzinfo=None).isoformat()
                     self.stats.revenue_over_time.append({
                         'timestamp': timestamp,
                         'revenue': event.total_amount
@@ -176,7 +176,7 @@ class KafkaConsumerService:
                             self.stats.category_sales.get(category, 0.0) + event.total_amount
             
             # Track events per minute
-            current_minute = datetime.utcnow().strftime('%Y-%m-%d %H:%M')
+            current_minute = datetime.now(UTC).replace(tzinfo=None).strftime('%Y-%m-%d %H:%M')
             if not self.stats.events_per_minute or \
                self.stats.events_per_minute[-1]['timestamp'] != current_minute:
                 self.stats.events_per_minute.append({
````

### `backend/config.py`

````diff
@@ -1,7 +1,7 @@
 """Configuration management using environment variables"""
 
 from pathlib import Path
-from pydantic_settings import BaseSettings
+from pydantic_settings import BaseSettings, SettingsConfigDict
 
 
 class Settings(BaseSettings):
@@ -23,10 +23,7 @@ class Settings(BaseSettings):
     # Environment
     environment: str = "development"
     
-    class Config:
-        env_file = ".env"
-        env_file_encoding = "utf-8"
-        case_sensitive = False
+    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", case_sensitive=False)
 
 
 # Global settings instance
````

### `backend/models.py`

````diff
@@ -1,9 +1,9 @@
 """Data models for ecommerce events"""
 
 from enum import Enum
-from datetime import datetime
-from typing import Optional, Dict, Any
-from pydantic import BaseModel, Field
+from datetime import UTC, datetime
+from typing import Optional, Dict
+from pydantic import BaseModel, ConfigDict, Field
 
 
 class EventType(str, Enum):
@@ -52,7 +52,7 @@ class EcommerceEvent(BaseModel):
     """Base ecommerce event model"""
     event_id: str
     event_type: EventType
-    timestamp: str = Field(default_factory=lambda: datetime.utcnow().isoformat())
+    timestamp: str = Field(default_factory=lambda: datetime.now(UTC).replace(tzinfo=None).isoformat())
     user: User
     product: Optional[Product] = None
     
@@ -67,8 +67,7 @@ class EcommerceEvent(BaseModel):
     rating: Optional[int] = None
     review_text: Optional[str] = None
     
-    class Config:
-        use_enum_values = True
+    model_config = ConfigDict(use_enum_values=True)
 
 
 class Statistics(BaseModel):
````

### `backend/events.py`

````diff
@@ -3,7 +3,7 @@
 import random
 from typing import List, Optional
 from faker import Faker
-from datetime import datetime
+from datetime import UTC, datetime
 
 from .models import (
     EcommerceEvent, EventType, User, Product, Location, ShippingAddress
@@ -94,7 +94,7 @@ class EventGenerator:
         event_data = {
             'event_id': fake.uuid4(),
             'event_type': event_type,
-            'timestamp': datetime.utcnow().isoformat(),
+            'timestamp': datetime.now(UTC).replace(tzinfo=None).isoformat(),
             'user': user,
             'product': product
         }
````

### `backend/app.py`

````diff
@@ -12,7 +12,7 @@ import uvicorn
 from .config import settings
 from .kafka_producer import KafkaProducerService
 from .kafka_consumer import KafkaConsumerService
-from .models import Statistics, StreamStatus, EcommerceEvent
+from .models import Statistics, StreamStatus
 
 
 # Configure logging
````

### `docker/Dockerfile.backend`

````diff
@@ -1,4 +1,4 @@
-FROM python:3.11-slim
+FROM python:3.14-slim
 
 WORKDIR /app
 
````

### `.github/dependabot.yml`

````diff
@@ -17,6 +17,10 @@ updates:
     schedule:
       interval: "weekly"
     open-pull-requests-limit: 5
+    ignore:
+      # A new Python release can lack wheels; bump the base image by hand.
+      - dependency-name: "python"
+        update-types: ["version-update:semver-major", "version-update:semver-minor"]
     groups:
       minor-and-patch:
         patterns:
@@ -36,3 +40,15 @@ updates:
         update-types:
           - "minor"
           - "patch"
+  - package-ecosystem: "github-actions"
+    directory: "/"
+    schedule:
+      interval: "weekly"
+    open-pull-requests-limit: 5
+    groups:
+      minor-and-patch:
+        patterns:
+          - "*"
+        update-types:
+          - "minor"
+          - "patch"
````

### `scripts/smoke_test.py`

````diff
@@ -0,0 +1,143 @@
+"""End-to-end smoke test: start the stream, confirm events flow through Kafka, and read the WebSocket.
+
+Usage: python scripts/smoke_test.py [base_url]   (default http://localhost:8000)
+Needs the stack running (docker compose -f docker/docker-compose.yml up -d --build --wait)
+and the `websockets` client library. Exits non-zero with a FAIL: line on any failure.
+"""
+
+import asyncio
+import json
+import re
+import signal
+import sys
+import time
+import urllib.error
+import urllib.request
+
+import websockets
+
+BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8000").rstrip("/")
+BUDGET_S = 180
+DEADLINE = time.monotonic() + BUDGET_S
+NAIVE_ISO = re.compile(r"\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(\.\d+)?")
+
+
+class SmokeFailure(Exception):
+    pass
+
+
+def remaining(cap: float) -> float:
+    left = DEADLINE - time.monotonic()
+    if left <= 0:
+        raise SmokeFailure("overall 180s budget exhausted")
+    return min(cap, left)
+
+
+def call(method: str, path: str) -> dict:
+    req = urllib.request.Request(BASE + path, method=method)
+    try:
+        with urllib.request.urlopen(req, timeout=remaining(20)) as resp:
+            return json.load(resp)
+    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as e:
+        raise SmokeFailure(f"{method} {path}: {e}") from e
+
+
+def fetch_text(path: str) -> str:
+    try:
+        with urllib.request.urlopen(BASE + path, timeout=remaining(20)) as resp:
+            return resp.read().decode("utf-8")
+    except (urllib.error.URLError, TimeoutError, UnicodeDecodeError) as e:
+        raise SmokeFailure(f"GET {path}: {e}") from e
+
+
+def check_dashboard() -> None:
+    """The image must ship the frontend: index page and a static asset."""
+    index = fetch_text("/")
+    if "<html" not in index.lower():
+        raise SmokeFailure("GET /: not the dashboard page")
+    if "WebSocket" not in fetch_text("/static/js/websocket.js"):
+        raise SmokeFailure("GET /static/js/websocket.js: unexpected content")
+
+
+def wait_for_flow(before: dict) -> dict:
+    status = before
+    while time.monotonic() < DEADLINE - 5:
+        status = call("GET", "/api/stream/status")
+        if (status["events_published"] > before["events_published"]
+                and status["events_processed"] > before["events_processed"]):
+            return status
+        time.sleep(2)
+    raise SmokeFailure(f"no new events made it through Kafka: before={before} now={status}")
+
+
+async def read_websocket() -> dict:
+    """Collect the first events frame and at least two stats frames (initial + periodic broadcast)."""
+    url = BASE.replace("http", "ws", 1) + "/ws"
+    events, stats = None, []
+    async with websockets.connect(url, open_timeout=remaining(10), close_timeout=2) as ws:
+        while events is None or len(stats) < 2:
+            msg = json.loads(await asyncio.wait_for(ws.recv(), timeout=remaining(30)))
+            if msg.get("type") == "events" and events is None:
+                events = msg.get("data")
+            elif msg.get("type") == "stats":
+                stats.append(msg.get("data"))
+    if not isinstance(events, list) or not events:
+        raise SmokeFailure(f"websocket 'events' frame carried no events: {events!r}")
+    ts = events[0].get("timestamp") if isinstance(events[0], dict) else None
+    if not isinstance(ts, str) or not NAIVE_ISO.fullmatch(ts):
+        raise SmokeFailure(f"websocket event timestamp not naive ISO: {ts!r}")
+    if not all(isinstance(st, dict) and "total_events" in st for st in stats):
+        raise SmokeFailure(f"websocket 'stats' frames malformed: {stats!r}")
+    return {"events": events, "stats": stats[-1], "stats_frames": len(stats)}
+
+
+def run() -> None:
+    check_dashboard()
+    print("dashboard ok")
+    before = call("GET", "/api/stream/status")
+    started = call("POST", "/api/stream/start")
+    if not (started.get("producer") and started.get("consumer")):
+        raise SmokeFailure(f"stream did not start: {started}")
+    status = wait_for_flow(before)
+    print(f"flow ok: published={status['events_published']} processed={status['events_processed']}")
+    try:
+        seen = asyncio.run(read_websocket())
+    except (OSError, asyncio.TimeoutError, websockets.WebSocketException, ValueError) as e:
+        raise SmokeFailure(f"websocket: {e!r}") from e
+    print(f"websocket ok: {len(seen['events'])} events in first frame, {seen['stats_frames']} stats frames")
+    stats = call("GET", "/api/stats")
+    if not stats.get("total_events", 0) > 0:
+        raise SmokeFailure(f"/api/stats total_events not positive: {stats.get('total_events')!r}")
+    print(f"stats ok: total_events={stats['total_events']}")
+
+
+def _hard_timeout(signum, frame):
+    raise SmokeFailure(f"hard timeout after {BUDGET_S + 10}s")
+
+
+def main() -> int:
+    signal.signal(signal.SIGALRM, _hard_timeout)
+    signal.alarm(BUDGET_S + 10)
+    try:
+        run()
+    except SmokeFailure as e:
+        print(f"FAIL: {e}")
+        return 1
+    except (KeyError, TypeError, AttributeError) as e:
+        print(f"FAIL: unexpected response shape: {e!r}")
+        return 1
+    finally:
+        signal.alarm(15)  # cleanup gets its own hard deadline
+        try:
+            req = urllib.request.Request(BASE + "/api/stream/stop", method="POST")
+            urllib.request.urlopen(req, timeout=10).close()
+        except BaseException as e:  # cleanup must not mask the primary result
+            print(f"warning: stop failed: {e!r}")
+        finally:
+            signal.alarm(0)
+    print("SMOKE TEST PASSED")
+    return 0
+
+
+if __name__ == "__main__":
+    sys.exit(main())
````

### `.github/workflows/ci.yml`

````diff
@@ -0,0 +1,34 @@
+name: CI
+
+on:
+  push:
+    branches: [main]
+  pull_request:
+
+permissions:
+  contents: read
+
+jobs:
+  smoke:
+    name: Smoke test
+    runs-on: ubuntu-latest
+    timeout-minutes: 20
+    env:
+      COMPOSE_FILE: docker/docker-compose.yml:docker/compose.ci.yml
+    steps:
+      - uses: actions/checkout@v7
+      - uses: actions/setup-python@v7
+        with:
+          python-version: "3.14"
+      - name: Install smoke-test client
+        run: pip install "$(grep '^websockets==' requirements.txt)"
+      - name: Start stack
+        run: docker compose up -d --build --wait --wait-timeout 300
+      - name: Run smoke test
+        run: python scripts/smoke_test.py
+      - name: Show logs on failure
+        if: failure()
+        run: docker compose logs --no-color --tail 200
+      - name: Stop stack
+        if: always()
+        run: docker compose down -v
````

### `docker/compose.ci.yml`

````diff
@@ -0,0 +1,4 @@
+# CI: run the image as built, so a broken COPY in Dockerfile.backend fails the smoke test.
+services:
+  backend:
+    volumes: !reset []
````

### `README.md`

````diff
@@ -29,28 +29,52 @@ An ecommerce event streaming application built with Python, Apache Kafka, and Fa
 
 ### Prerequisites
 
-- Python 3.12+
-- Docker Desktop (or Docker + Docker Compose)
+- Python 3.14
+- Docker Desktop (or Docker Engine with the Compose v2 plugin, `docker compose`)
 - Git
+- `timeout`, used by `run_local.sh` (GNU coreutils; on macOS `brew install coreutils` and put its `gnubin` directory on `PATH`)
 
 ### Setup & Run
 
 1. **Clone the repository**
     ```bash
     git clone https://github.com/b-rine/ecommerce-stream.git
     cd ecommerce-stream
     ```
 
-2. **Run setup executable**
+2. **Install the Python dependencies**
+    ```bash
+    python3.14 -m venv .venv
+    source .venv/bin/activate
+    pip install -r requirements.txt
+    ```
+
+3. **Upgrading from an older checkout?** Kafka now runs as a single KRaft-mode node, and the old stack's containers and volumes are incompatible. This deletes them, including any Kafka data they hold:
+    ```bash
+    docker compose -f docker/docker-compose.yml down -v --remove-orphans
+    ```
+
+4. **Run setup executable** (starts Kafka with Docker Compose, then the backend locally)
     ```bash
     chmod +x scripts/run_local.sh
     ./scripts/run_local.sh
     ```
 
-3. **Access the application**
+5. **Access the application**
     - Dashboard: http://localhost:8001
     - Use *START STREAM* to begin generating events.
 
+## Smoke Test
+
+CI starts the full stack in Docker (dashboard on http://localhost:8000) and checks that events flow end to end:
+
+```bash
+docker compose -f docker/docker-compose.yml up -d --build --wait
+pip install "$(grep '^websockets==' requirements.txt)"
+python scripts/smoke_test.py   # starts the stream, checks Kafka and the WebSocket, then stops it
+docker compose -f docker/docker-compose.yml down -v
+```
+
 ## License
 
 This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
\ No newline at end of file
````

## Appendix: verification scripts

### `ecom-verify.sh`

````bash
#!/usr/bin/env bash
# usage: ecom-verify.sh <checkout> <sha>  — verify the kraft/deps branch of ecommerce-stream.
# Needs ecom-run.sh and ecom-ports.override.yml (host ports 19092/18000) next to this script.
set -uo pipefail
C=$1 SHA=$2
D=$(cd "$(dirname "$0")" && pwd)
BASE=c5af4e0
fail() { echo "VERIFY FAIL: $*"; exit 1; }
cd "$C" || fail "no checkout"
[ "$(git rev-parse HEAD)" = "$(git rev-parse "$SHA")" ] || fail "checkout is not at $SHA"
[ -z "$(git status --porcelain --untracked-files=all)" ] || fail "checkout is dirty"

echo "== static checks"
expected_req='fastapi==0.165.0
uvicorn[standard]==0.54.0
websockets==17.4
kafka-python==3.0.13
faker==40.54.0
python-dotenv==1.2.4
pydantic==2.14.4
pydantic-settings==2.16.3'
[ "$(cat requirements.txt)" = "$expected_req" ] || fail "requirements.txt differs from #7+#8+#9+#10"
grep -qi zookeeper docker/docker-compose.yml scripts/run_local.sh README.md && fail "zookeeper still referenced"
[ "$(grep -c '^FROM ' docker/Dockerfile.backend)" = 1 ] && grep -qx 'FROM python:3.14-slim' docker/Dockerfile.backend || fail "Dockerfile base image"
[ "$(git diff $BASE -- docker/Dockerfile.backend | grep -cE '^[-+][^-+]')" = 2 ] || fail "Dockerfile changed beyond FROM"
bash -n scripts/run_local.sh || fail "run_local.sh syntax"
expected_paths='.github/dependabot.yml
.github/workflows/ci.yml
README.md
backend/app.py
backend/config.py
backend/events.py
backend/kafka_consumer.py
backend/kafka_producer.py
backend/models.py
docker/Dockerfile.backend
docker/compose.ci.yml
docker/docker-compose.yml
docs/plans/2026-11-25-kraft-deps.md
requirements.txt
scripts/run_local.sh
scripts/smoke_test.py'
[ "$(git diff --name-only $BASE HEAD)" = "$expected_paths" ] || fail "changed paths differ from plan"
grep -nE 'utcnow|class Config|_serializer=lambda|_deserializer=lambda' backend/*.py && fail "deprecated API still used"
uv tool run -q pyflakes backend/ scripts/smoke_test.py || fail "pyflakes"
grep -qE '(^|[^/])docker-compose( |$)' scripts/run_local.sh && fail "run_local.sh still uses docker-compose v1"
grep -q ':8000' scripts/run_local.sh && fail "run_local.sh still checks port 8000"
grep -q 'lsof -Pi :8001' scripts/run_local.sh || fail "run_local.sh does not check port 8001"
docker compose -f docker/docker-compose.yml config -q || fail "compose config invalid"
ci_backend=$(COMPOSE_FILE=docker/docker-compose.yml:docker/compose.ci.yml docker compose config --format json | python3 -I -c 'import json,sys; print(json.load(sys.stdin)["services"]["backend"].get("volumes"))') || fail "ci compose config"
[ "$ci_backend" = None ] || fail "ci override keeps backend mounts: $ci_backend"
python3 -I - <<'EOF' || fail "yaml checks"
import yaml, sys
c = yaml.safe_load(open("docker/docker-compose.yml"))
assert set(c["services"]) == {"kafka", "backend"}, c["services"].keys()
k = c["services"]["kafka"]
assert k["image"] == "confluentinc/cp-kafka:8.4.0", k["image"]
e = k["environment"]
assert e["KAFKA_PROCESS_ROLES"] == "broker,controller"
assert "KAFKA_ZOOKEEPER_CONNECT" not in e and "KAFKA_BROKER_ID" not in e
import base64, uuid
uuid.UUID(bytes=base64.urlsafe_b64decode(e["CLUSTER_ID"] + "=="))
assert len(e["CLUSTER_ID"]) == 22
assert k["ports"] == ["9092:9092"], k["ports"]
assert "depends_on" not in k
d = yaml.safe_load(open(".github/dependabot.yml"))
eco = {u["package-ecosystem"]: u for u in d["updates"]}
assert set(eco) == {"pip", "docker", "docker-compose", "github-actions"}, set(eco)
assert eco["docker"]["ignore"] == [{"dependency-name": "python", "update-types": ["version-update:semver-major", "version-update:semver-minor"]}], eco["docker"].get("ignore")
assert eco["github-actions"]["directory"] == "/"
w = yaml.safe_load(open(".github/workflows/ci.yml"))
on = w.get("on", w.get(True))
assert set(on) == {"push", "pull_request"} and on["push"] == {"branches": ["main"]}, on
assert w["permissions"] == {"contents": "read"}
assert list(w["jobs"]) == ["smoke"]
assert w["jobs"]["smoke"]["env"] == {"COMPOSE_FILE": "docker/docker-compose.yml:docker/compose.ci.yml"}, w["jobs"]["smoke"].get("env")
steps = w["jobs"]["smoke"]["steps"]
uses = [s.get("uses") for s in steps if "uses" in s]
assert uses == ["actions/checkout@v7", "actions/setup-python@v7"], uses
assert steps[1]["with"]["python-version"] == "3.14"
runs = "\n".join(s.get("run", "") for s in steps)
assert "scripts/smoke_test.py" in runs and "down -v" in runs and "--wait" in runs
assert steps[-1].get("if") == "always()" and "down -v" in steps[-1]["run"]
print("static ok")
EOF

echo "== end-to-end on the branch (python 3.14 image, kraft kafka 8.4)"
out=$(bash "$D/ecom-run.sh" "$C" 2>&1); rc=$?
echo "$out" | tail -8
[ $rc = 0 ] || fail "smoke run failed"
grep -qx 'SMOKE TEST PASSED' <<<"$out" || fail "no pass line"
grep -q 'confluentinc/cp-kafka:8.4.0' <<<"$out" || fail "kafka image not 8.4.0"
grep -qx '3\.14\.[0-9]* fastapi==0.165.0 uvicorn==0.54.0 websockets==17.4 kafka-python==3.0.13 faker==40.54.0 python-dotenv==1.2.4 pydantic==2.14.4 pydantic-settings==2.16.3' <<<"$out" || fail "runtime versions"
grep -q 'PYTHONWARNINGS: default' "$D/ecom-ports.override.yml" || fail "warnings not enabled in the run"
[ -s "$D/ecom-backend.log" ] || fail "backend log missing"
grep -nE 'Traceback|Warning|PydanticDeprecated' "$D/ecom-backend.log" && fail "backend log has tracebacks or warnings"

echo "== negative control (unreachable kafka must fail the smoke test)"
cp "$D/ecom-ports.override.yml" "$D/ecom-ports.saved.yml"
printf '      KAFKA_BOOTSTRAP_SERVERS: nowhere:29092\n' >> "$D/ecom-ports.override.yml"
timeout 600 bash "$D/ecom-run.sh" "$C" > "$D/ecom-neg.log" 2>&1; nrc=$?
cp "$D/ecom-ports.saved.yml" "$D/ecom-ports.override.yml"
[ $nrc != 0 ] && grep -qx 'SMOKE FAILED' "$D/ecom-neg.log" && grep -q '^FAIL: ' "$D/ecom-neg.log" && grep -q 'nowhere:29092' "$D/ecom-neg.log" || fail "negative control did not fail as expected (rc=$nrc)"
left=$(docker ps -aq --filter label=com.docker.compose.project=b4593c6a-ecom) || fail "docker ps failed"
[ -z "$left" ] || fail "containers left behind"
echo "negative ok"
echo "ECOM VERIFICATION PASSED"
````

### `ecom-run.sh`

````bash
#!/usr/bin/env bash
# usage: ecom-run.sh <checkout> [smoke_test.py]  — bring the compose stack up (local override), run the smoke test, always tear down.
# The smoke test defaults to the checkout's own; pass an absolute path to test commits that predate it.
set -uo pipefail
C=$1
T=/home/brian/.claude/jobs/b4593c6a/tmp
SMOKE=${2:-$C/scripts/smoke_test.py}
P=b4593c6a-ecom
cd "$C" || exit 1
[ -f "$SMOKE" ] || { echo "no smoke test at $SMOKE"; exit 1; }
for port in 19092 18000; do
  if (exec 3<>/dev/tcp/127.0.0.1/$port) 2>/dev/null; then echo "port $port busy"; exit 1; fi
done
DC=(docker compose -f docker/docker-compose.yml -f "$T/ecom-ports.override.yml" -p "$P")
cleanup() { "${DC[@]}" down -v --remove-orphans > /dev/null 2>&1; }
trap cleanup EXIT
[ -x "$T/ecom-client/bin/python" ] || { uv venv -q --python 3.14 "$T/ecom-client" && VIRTUAL_ENV="$T/ecom-client" uv pip install -q websockets==17.4; } || exit 1
[ "$("$T/ecom-client/bin/python" -c 'import websockets; print(websockets.__version__)')" = 17.4 ] || { echo "client websockets version"; exit 1; }
rm -f "$T/ecom-backend.log"
if ! "${DC[@]}" up -d --build --wait --wait-timeout 300 > "$T/ecom-up.log" 2>&1; then
  tail -20 "$T/ecom-up.log"; "${DC[@]}" ps; "${DC[@]}" logs --tail 40 kafka backend; echo "STACK UP FAILED"; exit 1
fi
"${DC[@]}" ps --format '{{.Service}} {{.Image}} {{.Status}}'
smoke_rc=0; timeout 240 "$T/ecom-client/bin/python" "$SMOKE" http://localhost:18000 || smoke_rc=$?
"${DC[@]}" logs --no-color backend > "$T/ecom-backend.log" 2>&1 || { echo "LOG CAPTURE FAILED"; exit 1; }
[ -s "$T/ecom-backend.log" ] || { echo "LOG CAPTURE EMPTY"; exit 1; }
if [ $smoke_rc != 0 ]; then tail -60 "$T/ecom-backend.log"; echo "SMOKE FAILED"; exit 1; fi
"${DC[@]}" exec -T backend python -c 'import sys
from importlib.metadata import version as v
print(sys.version.split()[0], *(f"{d}=={v(d)}" for d in ("fastapi", "uvicorn", "websockets", "kafka-python", "faker", "python-dotenv", "pydantic", "pydantic-settings")))'
````

### `ecom-bisect.sh`

````bash
#!/usr/bin/env bash
# usage: ecom-bisect.sh <final checkout> <sha>...  — smoke-test intermediate commits with the final smoke test.
set -uo pipefail
F=$1; shift
[ $# -gt 0 ] || { echo "BISECT FAIL: no commits given"; exit 1; }
D=$(cd "$(dirname "$0")" && pwd)
W=/tmp/b4593c6a-ecom-bisect
SMOKE="$F/scripts/smoke_test.py"
for sha in "$@"; do
  git -C "$F" worktree add -q --detach "$W" "$sha" || { echo "BISECT FAIL: checkout $sha"; exit 1; }
  out=$(bash "$D/ecom-run.sh" "$W" "$SMOKE" 2>&1); rc=$?
  git -C "$F" worktree remove --force "$W"
  echo "$out" | grep -E 'cp-kafka|^3\.'; echo "$out" | tail -1
  [ $rc = 0 ] && grep -q 'SMOKE TEST PASSED' <<<"$out" || { echo "BISECT FAIL: $sha"; exit 1; }
  echo "bisect ok: $sha"
done
echo "BISECT PASSED"
````

### `ecom-ports.override.yml`

````yaml
# local-only: remap host ports (9092 and 8000 are taken locally), test the built image without bind mounts, surface warnings
services:
  kafka:
    ports: !override
      - "19092:9092"
  backend:
    ports: !override
      - "18000:8000"
    volumes: !reset []
    environment:
      PYTHONWARNINGS: default
````
