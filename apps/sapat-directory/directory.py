#!/usr/bin/env python3
"""Shapat directory: codes and names that resolve to a profile's contact address.

Two HTTP listeners:
  - resolver (internal, for smp-server's [NAMES] role):
      GET /health
      GET /v2/resolve/<label>.<tld>   label is a name, a code, or "[<64 hex keccak256>]"
  - API (public, behind TLS):
      POST /api/v1/register  {address}   -> {token, code, name, ...}; the token is shown once
      POST /api/v1/name      {name}      -> change the name
      POST /api/v1/reserve   {}          -> keep the name for good (after 30 days)
      POST /api/v1/address   {address}   -> new contact address
      POST /api/v1/rotate    {}          -> new code, the old one stops resolving
      POST /api/v1/heartbeat {}          -> keeps a temporary name
      POST /api/v1/me        {}          -> current code, name and status
    All but register carry "Authorization: Bearer <token>". The directory keeps only the
    token's SHA-256. The API runs over TLS whose certificate the app pins.

What the directory stores: a hash of the profile's token, its code, its name and its contact
address. No messages, no contacts, no lookup log.
"""

import hashlib
import json
import os
import re
import secrets
import ssl
import sqlite3
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import unquote, urlparse

from Crypto.Hash import keccak

DB_PATH = os.environ.get("SAPAT_DIR_DB", "/data/directory.db")
RESOLVER_BIND = os.environ.get("SAPAT_DIR_RESOLVER_BIND", "0.0.0.0")
RESOLVER_PORT = int(os.environ.get("SAPAT_DIR_RESOLVER_PORT", "8000"))
API_BIND = os.environ.get("SAPAT_DIR_API_BIND", "0.0.0.0")
API_PORT = int(os.environ.get("SAPAT_DIR_API_PORT", "8080"))
TLS_CERT = os.environ.get("SAPAT_DIR_TLS_CERT", "")
TLS_KEY = os.environ.get("SAPAT_DIR_TLS_KEY", "")
TLD = os.environ.get("SAPAT_DIR_TLD", "simplex")
TRUSTED_PROXY = os.environ.get("SAPAT_DIR_TRUSTED_PROXY", "")

RESERVE_AFTER = 30 * 86400
TEMP_NAME_IDLE = 14 * 86400
MAX_BODY = 8192
CODE_DIGITS = 9
NAME_RE = re.compile(r"^[a-z][a-z0-9-]{2,31}$")
ADDRESS_RE = re.compile(r"^(simplex:/|https://)\S{10,2000}$")
RATE = {"register": (5, 3600), "default": (60, 60)}

_lock = threading.Lock()
_rate: dict = {}


def now() -> int:
    return int(time.time())


def label_hash(label: str) -> str:
    h = keccak.new(digest_bits=256)
    h.update(label.lower().encode())
    return h.hexdigest()


def db() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH, timeout=10, isolation_level=None)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    with db() as c:
        c.executescript(
            """
            PRAGMA journal_mode=WAL;
            CREATE TABLE IF NOT EXISTS profiles (
              pubkey TEXT PRIMARY KEY, -- SHA-256 of the profile's token
              code TEXT NOT NULL UNIQUE,
              code_hash TEXT NOT NULL UNIQUE,
              address TEXT NOT NULL,
              created_at INTEGER NOT NULL,
              seen_at INTEGER NOT NULL
            ) STRICT;
            CREATE TABLE IF NOT EXISTS names (
              name TEXT PRIMARY KEY,
              name_hash TEXT NOT NULL UNIQUE,
              pubkey TEXT NOT NULL UNIQUE REFERENCES profiles(pubkey) ON DELETE CASCADE,
              set_at INTEGER NOT NULL,
              reserved INTEGER NOT NULL DEFAULT 0
            ) STRICT;
            """
        )


# --- rules -------------------------------------------------------------------------------------

def new_code(c) -> str:
    while True:
        code = str(secrets.randbelow(9 * 10 ** (CODE_DIGITS - 1)) + 10 ** (CODE_DIGITS - 1))
        if not c.execute("SELECT 1 FROM profiles WHERE code = ?", (code,)).fetchone():
            return code


WORDS = ["tiha", "senka", "vetar", "reka", "sova", "luna", "kamen", "iskra", "magla", "zora", "lisica", "breza"]


def new_name(c) -> str:
    while True:
        name = f"{secrets.choice(WORDS)}-{secrets.randbelow(90000) + 10000}"
        if not name_taken(c, name, None):
            return name


def name_taken(c, name: str, pubkey) -> bool:
    row = c.execute(
        "SELECT n.pubkey, n.reserved, p.seen_at FROM names n JOIN profiles p USING (pubkey) WHERE n.name = ?", (name,)
    ).fetchone()
    if not row or row["pubkey"] == pubkey:
        return False
    if not row["reserved"] and row["seen_at"] < now() - TEMP_NAME_IDLE:
        c.execute("DELETE FROM names WHERE name = ?", (name,))
        return False
    return True


def status(c, pubkey: str) -> dict:
    p = c.execute("SELECT * FROM profiles WHERE pubkey = ?", (pubkey,)).fetchone()
    n = c.execute("SELECT * FROM names WHERE pubkey = ?", (pubkey,)).fetchone()
    reservable_at = p["created_at"] + RESERVE_AFTER
    return {
        "code": p["code"],
        "name": n["name"] if n else None,
        "reserved": bool(n["reserved"]) if n else False,
        "reservableAt": reservable_at,
        "canReserve": bool(n) and not n["reserved"] and now() >= reservable_at,
    }


class ApiError(Exception):
    def __init__(self, status: int, error: str):
        super().__init__(error)
        self.status = status
        self.error = error


def token_hash(token) -> str:
    if not isinstance(token, str) or len(token) < 32:
        raise ApiError(401, "noToken")
    return hashlib.sha256(token.encode()).hexdigest()


def require_profile(c, pubkey: str):
    if not c.execute("SELECT 1 FROM profiles WHERE pubkey = ?", (pubkey,)).fetchone():
        raise ApiError(404, "noProfile")
    c.execute("UPDATE profiles SET seen_at = ? WHERE pubkey = ?", (now(), pubkey))


def api(op: str, body: dict, token=None) -> dict:
    with _lock, db() as c:
        c.execute("BEGIN IMMEDIATE")
        try:
            result = _api(c, op, body, token)
            c.execute("COMMIT")
            return result
        except Exception:
            c.execute("ROLLBACK")
            raise


def _api(c, op: str, body: dict, token) -> dict:
    if op == "register":
        address = body.get("address")
        if not isinstance(address, str) or not ADDRESS_RE.match(address):
            raise ApiError(400, "badAddress")
        token = secrets.token_urlsafe(32)
        pubkey = token_hash(token)
        if True:
            code = new_code(c)
            c.execute(
                "INSERT INTO profiles (pubkey, code, code_hash, address, created_at, seen_at) VALUES (?, ?, ?, ?, ?, ?)",
                (pubkey, code, label_hash(code), address, now(), now()),
            )
            name = new_name(c)
            c.execute(
                "INSERT INTO names (name, name_hash, pubkey, set_at) VALUES (?, ?, ?, ?)",
                (name, label_hash(name), pubkey, now()),
            )
        return {"token": token, **status(c, pubkey)}
    if op == "name":
        name = body.get("name")
        name = name.lower() if isinstance(name, str) else ""
        if not NAME_RE.match(name):
            raise ApiError(400, "badName")
        pubkey = token_hash(token)
        require_profile(c, pubkey)
        current = c.execute("SELECT * FROM names WHERE pubkey = ?", (pubkey,)).fetchone()
        if current and current["name"] == name:
            return status(c, pubkey)
        if current and current["reserved"]:
            raise ApiError(409, "nameReserved")
        if name_taken(c, name, pubkey):
            raise ApiError(409, "nameTaken")
        c.execute("DELETE FROM names WHERE pubkey = ?", (pubkey,))
        c.execute(
            "INSERT INTO names (name, name_hash, pubkey, set_at) VALUES (?, ?, ?, ?)", (name, label_hash(name), pubkey, now())
        )
        return status(c, pubkey)
    if op == "address":
        address = body.get("address")
        if not isinstance(address, str) or not ADDRESS_RE.match(address):
            raise ApiError(400, "badAddress")
        pubkey = token_hash(token)
        require_profile(c, pubkey)
        c.execute("UPDATE profiles SET address = ? WHERE pubkey = ?", (address, pubkey))
        return status(c, pubkey)
    if op in ("reserve", "rotate", "heartbeat", "me"):
        pubkey = token_hash(token)
        require_profile(c, pubkey)
        if op == "reserve":
            st = status(c, pubkey)
            if not st["canReserve"]:
                raise ApiError(409, "cannotReserveYet" if st["name"] else "noName")
            c.execute("UPDATE names SET reserved = 1 WHERE pubkey = ?", (pubkey,))
        elif op == "rotate":
            code = new_code(c)
            c.execute("UPDATE profiles SET code = ?, code_hash = ? WHERE pubkey = ?", (code, label_hash(code), pubkey))
        return status(c, pubkey)
    raise ApiError(404, "noSuchOperation")


# --- resolver ----------------------------------------------------------------------------------

def resolve(query: str):
    query = query.strip().lower()
    if "." not in query:
        return 400, {"error": "notFullyQualified"}
    label, tld = query.rsplit(".", 1)
    if tld != TLD or "." in label:
        return 400, {"error": "tldNotConfigured"}
    if label.startswith("[") and label.endswith("]") and len(label) == 66:
        h = label[1:-1]
    else:
        h = label_hash(label)
    with db() as c:
        row = c.execute(
            "SELECT p.code AS code, p.address AS address, NULL AS name FROM profiles p WHERE p.code_hash = ? "
            "UNION ALL SELECT p.code, p.address, n.name FROM names n JOIN profiles p USING (pubkey) "
            "WHERE n.name_hash = ? AND (n.reserved = 1 OR p.seen_at >= ?)",
            (h, h, now() - TEMP_NAME_IDLE),
        ).fetchone()
    if not row:
        return 200, {"lastBlockTs": now(), "registration": {"type": "reserved", "reservedReason": "unregistered"}}
    shown = row["name"] or row["code"]
    record = {
        "name": f"{shown}.{TLD}",
        "nickname": row["name"] or "",
        "website": "",
        "location": "",
        "simplexContact": [row["address"]],
        "simplexChannel": [],
        "eth": None,
        "btc": None,
        "xmr": None,
        "dot": None,
        "owner": "",
        "resolver": "",
    }
    expires = now() + 365 * 86400
    return 200, {
        "lastBlockTs": now(),
        "registration": {
            "type": "registered",
            "expires": expires,
            "graceUntil": expires + 30 * 86400,
            "reservedReason_": None,
            "nameRecord": record,
        },
    }


# --- HTTP --------------------------------------------------------------------------------------

def rate_limited(ip: str, op: str) -> bool:
    limit, window = RATE.get(op, RATE["default"])
    key = (ip, op if op in RATE else "default")
    t = now()
    with _lock:
        hits = [x for x in _rate.get(key, []) if x > t - window]
        if len(hits) >= limit:
            _rate[key] = hits
            return True
        hits.append(t)
        _rate[key] = hits
    return False


class Handler(BaseHTTPRequestHandler):
    server_version = "sapat-directory"
    sys_version = ""

    def log_message(self, *args):  # no request log: lookups are not recorded
        pass

    def respond(self, code: int, body: dict):
        data = json.dumps(body).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


class ResolverHandler(Handler):
    def do_GET(self):  # noqa: N802
        parts = [unquote(p) for p in urlparse(self.path).path.split("/") if p]
        if parts == ["health"]:
            self.respond(200, {"ok": True})
        elif len(parts) == 3 and parts[:2] == ["v2", "resolve"]:
            self.respond(*resolve(parts[2]))
        else:
            self.respond(404, {"error": "noSuchRoute"})


class ApiHandler(Handler):
    def client_ip(self) -> str:
        ip = self.client_address[0]
        if TRUSTED_PROXY and ip == TRUSTED_PROXY:
            ip = (self.headers.get("X-Forwarded-For") or ip).split(",")[-1].strip()
        return ip

    def do_GET(self):  # noqa: N802
        if urlparse(self.path).path == "/health":
            self.respond(200, {"ok": True})
        else:
            self.respond(404, {"error": "noSuchRoute"})

    def do_POST(self):  # noqa: N802
        parts = [p for p in urlparse(self.path).path.split("/") if p]
        if len(parts) != 3 or parts[:2] != ["api", "v1"]:
            self.respond(404, {"error": "noSuchRoute"})
            return
        op = parts[2]
        if rate_limited(self.client_ip(), op):
            self.respond(429, {"error": "rateLimited"})
            return
        try:
            length = int(self.headers.get("Content-Length") or 0)
            if length <= 0 or length > MAX_BODY:
                raise ApiError(400, "badRequest")
            body = json.loads(self.rfile.read(length))
            if not isinstance(body, dict):
                raise ApiError(400, "badRequest")
            auth = self.headers.get("Authorization") or ""
            token = auth[7:] if auth.startswith("Bearer ") else None
            self.respond(200, api(op, body, token))
        except ApiError as e:
            self.respond(e.status, {"error": e.error})
        except json.JSONDecodeError:
            self.respond(400, {"error": "badRequest"})


def serve(resolver_port=RESOLVER_PORT, api_port=API_PORT, background=False):
    init_db()
    servers = [
        ThreadingHTTPServer((RESOLVER_BIND, resolver_port), ResolverHandler),
        ThreadingHTTPServer((API_BIND, api_port), ApiHandler),
    ]
    if TLS_CERT:
        ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        ctx.minimum_version = ssl.TLSVersion.TLSv1_2
        ctx.load_cert_chain(TLS_CERT, TLS_KEY)
        servers[1].socket = ctx.wrap_socket(servers[1].socket, server_side=True)
    threads = [threading.Thread(target=s.serve_forever, daemon=True) for s in servers]
    for t in threads:
        t.start()
    if background:
        return servers
    for t in threads:
        t.join()


if __name__ == "__main__":
    serve()
