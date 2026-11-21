import base64
import json
import os
import tempfile
import time
import unittest
import urllib.error
import urllib.request

os.environ["SAPAT_DIR_DB"] = os.path.join(tempfile.mkdtemp(), "t.db")
os.environ["SAPAT_DIR_RESOLVER_BIND"] = "127.0.0.1"
os.environ["SAPAT_DIR_API_BIND"] = "127.0.0.1"

import directory as d  # noqa: E402
from Crypto.PublicKey import ECC  # noqa: E402
from Crypto.Signature import eddsa  # noqa: E402

RP, AP = 18700, 18780
ADDR = "simplex:/a#abcdefghijklmnop?h=148.251.84.251"


class Client:
    def __init__(self):
        self.key = ECC.generate(curve="ed25519")
        self.pub = base64.b64encode(self.key.public_key().export_key(format="raw")).decode()

    def call(self, op, value="", **fields):
        ts = int(time.time())
        msg = f"sapat-dir|{op}|{self.pub}|{value}|{ts}".encode()
        sig = base64.b64encode(eddsa.new(self.key, "rfc8032").sign(msg)).decode()
        body = dict(pubkey=self.pub, ts=ts, sig=sig, **fields)
        return post(op, body)


def post(op, body):
    req = urllib.request.Request(
        f"http://127.0.0.1:{AP}/api/v1/{op}", data=json.dumps(body).encode(), headers={"Content-Type": "application/json"}
    )
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, json.loads(r.read())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read())


def resolve(q):
    with urllib.request.urlopen(f"http://127.0.0.1:{RP}/v2/resolve/{q}") as r:
        return json.loads(r.read())


class DirectoryTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        d.RATE["register"] = (100, 3600)
        cls.servers = d.serve(RP, AP, background=True)

    @classmethod
    def tearDownClass(cls):
        for s in cls.servers:
            s.shutdown()

    def test_register_resolve_by_code_name_and_hash(self):
        c = Client()
        st, me = c.call("register", ADDR, address=ADDR)
        self.assertEqual(st, 200)
        self.assertEqual(len(me["code"]), 9)
        self.assertFalse(me["canReserve"])
        for q in (me["code"], me["name"], "[" + d.label_hash(me["name"]) + "]"):
            r = resolve(f"{q}.simplex")["registration"]
            self.assertEqual(r["type"], "registered")
            self.assertEqual(r["nameRecord"]["simplexContact"], [ADDR])
        self.assertEqual(resolve("nobody-here.simplex")["registration"]["type"], "reserved")

    def test_name_change_taken_and_reserve(self):
        a, b = Client(), Client()
        a.call("register", ADDR, address=ADDR)
        b.call("register", ADDR, address=ADDR)
        self.assertEqual(a.call("name", "nikola", name="Nikola")[0], 200)
        self.assertEqual(b.call("name", "nikola", name="nikola"), (409, {"error": "nameTaken"}))
        self.assertEqual(a.call("name", "x", name="x")[1], {"error": "badName"})
        self.assertEqual(a.call("reserve")[1], {"error": "cannotReserveYet"})
        with d.db() as c:
            c.execute("UPDATE profiles SET created_at = created_at - ? WHERE pubkey = ?", (d.RESERVE_AFTER + 1, a.pub))
        st, me = a.call("reserve")
        self.assertTrue(me["reserved"])
        self.assertEqual(a.call("name", "drugo", name="drugo"), (409, {"error": "nameReserved"}))

    def test_idle_temporary_name_is_released(self):
        a, b = Client(), Client()
        a.call("register", ADDR, address=ADDR)
        b.call("register", ADDR, address=ADDR)
        a.call("name", "pera", name="pera")
        with d.db() as c:
            c.execute("UPDATE profiles SET seen_at = seen_at - ? WHERE pubkey = ?", (d.TEMP_NAME_IDLE + 1, a.pub))
        self.assertEqual(resolve("pera.simplex")["registration"]["type"], "reserved")
        self.assertEqual(b.call("name", "pera", name="pera")[0], 200)

    def test_rotate_code(self):
        a = Client()
        old = a.call("register", ADDR, address=ADDR)[1]["code"]
        new = a.call("rotate")[1]["code"]
        self.assertNotEqual(old, new)
        self.assertEqual(resolve(f"{old}.simplex")["registration"]["type"], "reserved")
        self.assertEqual(resolve(f"{new}.simplex")["registration"]["type"], "registered")

    def test_bad_signature_and_old_timestamp(self):
        a, b = Client(), Client()
        ts = int(time.time())
        msg = f"sapat-dir|register|{a.pub}|{ADDR}|{ts}".encode()
        sig = base64.b64encode(eddsa.new(b.key, "rfc8032").sign(msg)).decode()
        self.assertEqual(post("register", dict(pubkey=a.pub, ts=ts, sig=sig, address=ADDR)), (403, {"error": "badSignature"}))
        old = ts - 1000
        msg = f"sapat-dir|register|{a.pub}|{ADDR}|{old}".encode()
        sig = base64.b64encode(eddsa.new(a.key, "rfc8032").sign(msg)).decode()
        self.assertEqual(post("register", dict(pubkey=a.pub, ts=old, sig=sig, address=ADDR)), (400, {"error": "badTimestamp"}))


if __name__ == "__main__":
    unittest.main()
