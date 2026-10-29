# Ghar Firestore Security Specification (Phase 0 & Red Team Audit)

## 1. Data Invariants
1. **Strict Tenant Isolation**: Every document in `/users/{userId}` and its subcollections (`transactions`, `goals`, `maa_chats`) is strictly private to `request.auth.uid == userId`. Cross-user reads, queries, writes, and deletes are unconditionally rejected.
2. **Denormalized Subcollection Ownership & Query Enforcement**: Every subcollection document (`Transaction`, `SavingsGoal`, `MaaChatMessage`) carries `userId == request.auth.uid == userId` so `allow list` validates `resource.data.userId == request.auth.uid` locally without `get()` amplification, forcing clients to filter with `.whereEqualTo("userId", uid)`.
3. **Immutable Provenance**: `userId` and `createdAt` can never be mutated on `update`. `MaaChatMessage` documents are append-only (`allow update: if false;`), allowing only creation and owner deletion (chat reset).
4. **Server-Side Temporal Integrity**: All `createdAt` and `updatedAt` fields must be Firestore `timestamp` values `<= request.time` populated via `FieldValue.serverTimestamp()`.
5. **Bounded Strings, Numerics, and Arrays**: Every string enforces `.size()` bounds, all monetary fields (`*Paise`) are bounded non-negative integers (`<= 100_000_000_000`), and `habitLeaks` is capped at `<= 10` items.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Scrape**: `GET /users/alice_123/transactions` with `auth = null`.
2. **Cross-Tenant Khata Read**: `bob_456` executes `GET /users/alice_123` or `GET /users/alice_123/transactions/tx_1`.
3. **Unfiltered Subcollection List**: `alice_123` queries `/users/alice_123/transactions` where a seeded rogue doc has `userId: "bob_456"` or omits `.where("userId", "==", "alice_123")`.
4. **Identity Spoofing on Create**: `alice_123` writes `/users/alice_123/transactions/tx_spoof` with `{"userId": "bob_456", ...}`.
5. **Shadow Update (Ghost Field Injection)**: `alice_123` updates `/users/alice_123` adding `"isAdmin": true` or `"isVerified": true`.
6. **Value Poisoning on Update**: `alice_123` updates allowlisted key `userName` on `/users/alice_123` with `12345` (int) or a 5,000-char string.
7. **Immutable `createdAt` Tampering**: `alice_123` updates `/users/alice_123/goals/primary_goal` changing `createdAt` to a newer timestamp.
8. **Negative / Overflow Rupee Injection**: `alice_123` creates a transaction with `amountPaise: -5000` or `amountPaise: 999999999999999`.
9. **Enum Bypass**: `alice_123` writes `direction: "FREE_MONEY"` or `guiltSensitivity: "EXTREME"`.
10. **Unbounded Array Exhaustion**: `alice_123` writes `habitLeaks` with 25 entries to `/users/alice_123`.
11. **ID Poisoning**: `alice_123` creates `/users/alice_123/transactions/bad id with spaces!@#` (violates `^[a-zA-Z0-9_\-]+$`).
12. **Chat History Tampering**: `alice_123` attempts `UPDATE` on `/users/alice_123/maa_chats/msg_1` to rewrite Maa's words.

## 3. Red Team Conflict Audit Report
- **Default Deny Catch-All**: Present as first rule (`match /{document=**} { allow read, write: if false; }`).
- **Shadow Update Test**: PASS — `hasOnly` on `create` and `affectedKeys().hasOnly(...)` on `update` block all ghost fields across `users`, `transactions`, `goals`, and `maa_chats`.
- **Value Poisoning Test**: PASS — Every `allow update` begins with `isSignedIn() && isValid[Entity](incoming())` before action branches.
- **PII & Cross-User Isolation Test**: PASS — `/users/{userId}` disallows `list` (`allow list: if false`) and restricts `get` strictly to `request.auth.uid == userId`.
- **Query Trust Test**: PASS — Every `allow list` verifies `request.auth.uid == userId && existing().userId == request.auth.uid` with zero `get()` calls.
