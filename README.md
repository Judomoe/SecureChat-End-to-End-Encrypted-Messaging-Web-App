# SecureChat 🔒

A deployed, full-stack **end-to-end encrypted (E2EE)** 1:1 messaging application.
Two users can register, add contacts, and exchange messages — the server
**never sees plaintext**. Even a full database breach reveals no readable messages.

> **Core guarantee:** All encryption and decryption happens entirely in the browser
> using the native Web Crypto API. No external crypto libraries.

---

## Live Demo
🌐 **[Deployed — link here]**

---

## Cryptographic Design

Six primitives, zero external crypto libraries:

| Purpose | Algorithm | Where |
|---------|-----------|-------|
| Password storage | BCrypt (cost 12) | Server |
| Private key protection | PBKDF2-SHA256 (600K iter) + AES-256-GCM | Browser |
| Key exchange | RSA-OAEP SHA-256 (2048-bit) | Browser |
| Key exchange auth | RSA-PSS SHA-256 | Browser signs, Server verifies |
| Message encryption | AES-256-GCM (256-bit key, 12-byte IV) | Browser |
| Message integrity | HMAC-SHA256 | Browser computes, Server verifies |
| Replay protection | Timestamp ±5 min window | Server |
| Session tokens | JWT HS-256 (24h expiry) | Server |

---

## Three Trust Zones
Browser (Trusted Zone)         Server (Cannot Read)        Database (Encrypted Only)
──────────────────────         ────────────────────        ─────────────────────────
Chat UI                        JWT Auth Filter             users: BCrypt + AES-encrypted
Web Crypto API                 RSA-PSS Verification              private keys
RSA & AES keys in memory only  REST Controllers            messages: ciphertext + HMAC
(never localStorage)                                             + signature
key_materials: RSA-encrypted
AES keys

---

## How It Works

### Registration
1. Browser generates RSA-2048 key pair
2. PBKDF2(password, salt, 600K iterations) → AES-256 key
3. AES-256-GCM encrypts the RSA private key → encrypted blob + IV
4. Server receives: email, BCrypt(password), public key, **encrypted** private key
5. Server never receives the private key in plaintext

### Key Exchange (Alice → Bob)
1. Alice generates a random AES-256 conversation key
2. RSA-OAEP encrypts it with Bob's public key
3. RSA-PSS signs the encrypted key with Alice's private key
4. Server verifies Alice's signature — stores encrypted blob for Bob
5. Bob decrypts with his RSA private key → has the shared AES key

### Sending a Message
Plaintext
→ AES-256-GCM encrypt (conversationKey, randomIV) → ciphertext
→ HMAC-SHA256 (ciphertext + iv + timestamp)       → hmac
→ RSA-PSS sign (hmac)                             → signature
→ POST /messages {ciphertext, iv, hmac, signature, timestamp}

### Server Verification (without decrypting)
1. Timestamp within ±5 min window?
2. Sender is a conversation member?
3. RSA-PSS signature valid?
4. → Store ciphertext only

### Receiving
GET /messages → verify HMAC → AES-256-GCM decrypt → Plaintext

---

## What a Database Breach Exposes

| Data | Can attacker read it? |
|------|-----------------------|
| `messages.ciphertext` | No — needs AES conversation key |
| `users.encrypted_private_key` | No — needs password (PBKDF2 600K iterations) |
| `users.password_hash` | Extremely hard — BCrypt cost 12 |
| Contact metadata (who talks to whom) | Yes — metadata is not encrypted |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19 + TypeScript + Vite |
| Cryptography | Web Crypto API (browser-native) |
| Backend | Spring Boot 3.5 (Java 17) |
| Database | PostgreSQL + Flyway migrations |
| Auth | JWT HS-256 |
| Backend Testing | JUnit 5 + Mockito (73 tests) |
| Frontend Testing | Vitest + Playwright (51 tests) |

---

## Testing

- **73 backend tests** — unit tests (Mockito), MockMvc controller tests,
  crypto round-trip tests with real RSA keys
- **51 frontend tests** — Web Crypto round-trip tests, API layer tests,
  React component tests, hook tests
- **Playwright E2E** — full simulation of two users registering, adding contacts,
  and exchanging encrypted messages with verified round-trip decryption

---

## Known Limitations

| Limitation | Impact |
|-----------|--------|
| No forward secrecy | Compromised conversation key exposes all messages in that conversation |
| Metadata visible to server | Server knows who messages whom and when |
| 1:1 only | No group chat support |
| No key rotation | Conversation keys never change |
| Single device | Private key exists in one browser session only |

Addressing these would require the Signal Double Ratchet protocol.

---

## Run Locally

### Backend
```bash
cd backend
./mvnw spring-boot:run
```
Requires: Java 17, PostgreSQL running locally

### Frontend
```bash
cd frontend
npm install
npm run dev
```

## Future Work
- Forward secrecy via Double Ratchet (Signal protocol)
- Group chat support
- Multi-device key synchronization
- Metadata protection
- Key rotation support
