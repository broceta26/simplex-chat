# Shapat directory

Codes and names that resolve to a profile's contact address (spec: replica/spec-v2.md, R3).

- `directory.py` — two listeners: the resolver (port 8000, internal, queried by smp-server's `[NAMES]` role) and the API (port 8080, TLS with a certificate the app pins) where the app registers and gets a token; later requests carry the token, of which the directory keeps only the SHA-256.
- Stores per profile: public key, code, name, contact address, created/seen times. No lookup log.
- Rules: 9-digit code, permanent but rotatable; name `[a-z][a-z0-9-]{2,31}`, unique; a temporary name frees up after 14 days without the owner's heartbeat; it can be reserved for good 30 days after registration.

Run tests: `pip install pycryptodome && python3 -m unittest test_directory -v`
