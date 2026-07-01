# Android App Spec — Transaction Extraction Client

This document is a complete, self-contained implementation spec for an Android
app that talks to an already-built, already-running personal API gateway. It
is meant to be handed to an implementing agent/developer with no prior context
on this project — everything needed is below. If anything here conflicts with
the live server, the server (this repo, `src/`) is the source of truth; this
gateway's endpoints were all verified working end-to-end before this spec was
written.

## 1. What the app does

1. User provides input: a text message, an image (e.g. a screenshot of a
   payment/transaction), or both.
2. App sends that input to the gateway's `/agent` endpoint, which forwards it
   to a locally-running OpenClaw AI agent and gets back a natural-language /
   JSON reply (typically a structured extraction of transaction details).
3. App parses a JSON object out of that reply.
4. App `POST`s that JSON object to the gateway's `/transactions` endpoint,
   which stores it in a Postgres database on the server.
5. App has a list/detail screen that reads back all saved transactions via
   `GET /transactions` so the user can browse everything that's been saved.

## 2. Network prerequisites (already satisfied, not the app's job to manage)

- The gateway runs on a Mac Mini (`kartiks-mac-mini-7`) on the developer's
  personal Tailscale tailnet.
- **The Android device must have the official Tailscale app installed and
  logged into the same tailnet account** (`kartik.iit96@gmail.com`). This is
  a manual one-time setup step on the phone, done through the Tailscale
  Android app itself — the app being built here does **not** need to
  integrate the Tailscale SDK, manage VPN state, or do anything
  Tailscale-specific. Once the phone is on the tailnet, Tailscale transparently
  routes traffic to the gateway's hostname at the OS/VPN layer; from the app's
  point of view, it's just a normal HTTPS API call.
- Current gateway base URL (tailnet-only, HTTPS, valid public CA cert issued
  automatically by Tailscale — no certificate pinning or custom trust config
  needed):
  ```
  https://kartiks-mac-mini-7.tailafb282.ts.net
  ```
  **Do not hardcode this as a compile-time constant.** Tailscale device names
  / tailnet names can change, and the developer may later switch from
  Tailscale Serve (tailnet-only, current setup) to Funnel (public internet) or
  back. Make this a **user-editable setting** in the app (a simple text field
  in a settings screen, persisted locally), defaulting to the URL above.
- Traffic never leaves the tailnet in the current configuration (Tailscale
  Serve, not Funnel) — but the app should still always use HTTPS and send the
  API key on every request (see §4), as defense in depth and to keep the app
  working unchanged if the server side later switches to Funnel.
- **Troubleshooting note to include in the app or its docs:** if the hostname
  fails to resolve (DNS error) despite the phone showing "connected" in the
  Tailscale app, the fallback is the tailnet IP directly, e.g.
  `https://100.81.125.16/...` — but note that hitting an IP directly means TLS
  hostname verification will fail (the cert is issued for the `.ts.net` name,
  not the IP), so this should only be a manual diagnostic fallback, not
  something the app does automatically. If this comes up, get the current
  IP from someone with terminal access to the tailnet (`tailscale status`).

## 3. Full API reference

All endpoints (except `/healthz`) require this header on every request:
```
Authorization: Bearer <gateway API key>
```
A `401 {"error":"unauthorized"}` means the key is missing or wrong.

### 3.1 `GET /healthz` — unauthenticated

No auth needed. Use this to let the app show a "server reachable" indicator,
e.g. on app start or pull-to-refresh, independent of whether the API key is
configured correctly yet.

Request: none.

Response `200`:
```json
{"status": "ok"}
```

### 3.2 `POST /agent` — send text/image(s), get the agent's reply

Content-Type: `multipart/form-data`. Fields:

| Field   | Type               | Required | Notes |
|---------|--------------------|----------|-------|
| `text`  | string (form field)| No*      | The user's typed message / instruction. |
| `files` | file (repeatable)  | No*      | Zero or more file parts, **field name must be exactly `files`** for every attachment. |

\* At least one of `text` or `files` must be present, or the server returns
`400 {"error": "provide a 'text' field, one or more 'files', or both"}`.

**Images** (mime types `image/jpeg`, `image/png`, `image/gif`, `image/webp`,
`image/heic`, `image/heif`) are given genuine vision analysis by the backing
AI model — the model actually "sees" the image, this isn't OCR bolted on
separately. Other file types (text, code, PDF, etc.) are handled differently
server-side (the agent reads them via its own tools) but the client-side API
contract is identical either way — just attach the file under `files`.

**Limits** (server returns `400` with a descriptive `error` field if
exceeded — check for this and show the user a clear message, don't just
treat any non-200 as a generic failure):
- Up to **10 files** total per request, **25MB per file** (any file type).
- Of those, up to **8 images** per request specifically.
- Each image up to **10MB**; total image bytes across the request up to
  **20MB**.
- If the user's photo is likely to exceed 10MB (common with modern phone
  cameras), **compress/resize client-side before upload** (e.g. downscale to
  a max dimension like 2000px and re-encode as JPEG ~85% quality) rather than
  relying on the server to reject it.

**Timeouts — this is important, do not use default HTTP client timeouts:**
The agent call can legitimately take several minutes (it's running a local
LLM, not calling a fast cloud API). The gateway itself will wait up to **15
minutes** (`900000`ms) for OpenClaw's response before giving up. Configure
your HTTP client (e.g. OkHttp) with:
- `connectTimeout`: normal, e.g. 15–30s (this is just for establishing the
  TLS connection over the tailnet, should be fast).
- `readTimeout` / `callTimeout`: **at least 15 minutes**, ideally slightly
  above the server's own limit (e.g. 16 minutes) so the server's own timeout
  error (a graceful `200` response with an error message in `reply`, see
  below) is what the user sees, not a client-side timeout exception.
- Do this work off the main thread (coroutine + `Dispatchers.IO`, or
  WorkManager if you want the request to survive the user backgrounding the
  app — recommended given how long this can take). Show a clear
  "thinking..." / progress state in the UI; do not block or freeze the UI
  thread.

Example request (conceptually — see §6 for a full curl example):
```
POST /agent
Authorization: Bearer <key>
Content-Type: multipart/form-data; boundary=...

--boundary
Content-Disposition: form-data; name="text"

Extract the transaction details from the attached screenshot and return them as structured JSON.
--boundary
Content-Disposition: form-data; name="files"; filename="screenshot.jpg"
Content-Type: image/jpeg

<binary image bytes>
--boundary--
```

Response `200`:
```json
{
  "reply": "```json\n{\n  \"amount\": \"₹140\",\n  \"currency\": \"INR\",\n  \"date_time\": \"30 Jun 2026, 2:50 pm\",\n  \"sender\": {\"bank\": \"ICICI Bank\", \"masked_account\": \"******XXXXXX00\"},\n  \"recipient\": \"Bluechip Sec 135 Noida\",\n  \"transaction_id\": \"UPI transaction ID 654795527971\",\n  \"status\": \"Completed\"\n}\n```",
  "filesReceived": ["screenshot.jpg"]
}
```

**Critical detail: `reply` is a plain string, not a JSON object.** The model
usually wraps its JSON answer in a markdown code fence (```` ```json ... ``` ````)
as shown above, because it's a chat model replying conversationally — it is
**not guaranteed** to be pure, directly-parseable JSON with no surrounding
text. See §5 for exactly how to robustly extract the JSON object from this
string. It is also possible the model responds with a plain-text question or
clarification instead of JSON (e.g. if the image was unreadable, or the user's
`text` didn't ask for extraction) — the app must handle that gracefully (show
the raw reply to the user, don't attempt to save it as a transaction).

Error responses:
- `400` — bad request (missing text+files, or image limits exceeded). Body:
  `{"error": "<description>"}`.
- `401` — bad/missing API key.
- `502` — the gateway reached OpenClaw but the call itself failed. Body:
  `{"error": "agent call failed", "detail": "<message>"}`.

### 3.3 `POST /transactions` — save an extracted record

Content-Type: `application/json`. Body: **any JSON object** — send exactly
the object you parsed out of `/agent`'s `reply` field (see §5), unmodified.
The server does its own best-effort mapping of common field names into fixed
database columns server-side; the app does not need to reshape or rename
fields before sending.

Request body example (this is literally what was verified working):
```json
{
  "amount": "₹140",
  "currency": "INR",
  "date_time": "30 Jun 2026, 2:50 pm",
  "sender": {"bank": "ICICI Bank", "masked_account": "******XXXXXX00"},
  "recipient": "Bluechip Sec 135 Noida",
  "transaction_id": "UPI transaction ID 654795527971",
  "status": "Completed"
}
```

Response `201` — the saved row, including the fields the server derived plus
the full original object under `raw_json`:
```json
{
  "id": "1",
  "amount": "140",
  "currency": "INR",
  "transaction_date": "2026-06-30T09:20:00.000Z",
  "sender": "{\"bank\":\"ICICI Bank\",\"masked_account\":\"******XXXXXX00\"}",
  "recipient": "Bluechip Sec 135 Noida",
  "reference_id": "UPI transaction ID 654795527971",
  "status": "Completed",
  "raw_json": { "...": "the exact object you posted, in full" },
  "created_at": "2026-07-01T22:22:48.474Z"
}
```

Field notes for the Android data model:
- `id` — comes back **as a string** (Postgres `bigint`/`bigserial` is
  serialized as a JSON string by the server's DB driver to avoid precision
  loss). Model it as `String`, not `Long`, or parse explicitly if you need a
  numeric id.
- `amount` — numeric-looking string or `null` if the server couldn't parse a
  number out of whatever `amount`/`total`/`value` field you sent.
- `currency` — string or `null`.
- `transaction_date` — ISO-8601 UTC timestamp string or `null` (the server
  attempts to parse whatever date-like field was present; if parsing fails,
  this is `null` but the original string is still inside `raw_json`).
- `sender`, `recipient`, `reference_id`, `status` — plain strings or `null`.
  Note `sender` in the example above: because the original value was a
  nested object (`{bank, masked_account}`), the server stored it as a
  **JSON-encoded string**, not a nested object — always treat these four
  fields as `String?` in your model, never as nested objects, even though the
  original `reply` might have had nested objects for some fields.
- `raw_json` — the full original JSON object you posted, as a nested
  JSON object (not a string) — use this to display full details / anything
  the fixed columns didn't capture, and as a fallback source of truth.
- `created_at` — ISO-8601 UTC timestamp string, set by the server.

Error responses:
- `400 {"error": "request body must be a JSON object"}` — if you send
  anything other than a JSON object (e.g. an array, or non-JSON body).
- `401` — bad/missing API key.
- `500 {"error": "internal error"}` — unexpected server/DB error.

### 3.4 `GET /transactions` — list saved records

Query param: `limit` (optional, integer, default `50`, max `200`).

```
GET /transactions?limit=50
Authorization: Bearer <key>
```

Response `200` — an array, newest first (ordered by `created_at DESC`), each
element shaped exactly like the `/transactions` POST response above:
```json
[
  {
    "id": "1",
    "amount": "140",
    "currency": "INR",
    "transaction_date": "2026-06-30T09:20:00.000Z",
    "sender": "{\"bank\":\"ICICI Bank\",\"masked_account\":\"******XXXXXX00\"}",
    "recipient": "Bluechip Sec 135 Noida",
    "reference_id": "UPI transaction ID 654795527971",
    "status": "Completed",
    "raw_json": { "...": "..." },
    "created_at": "2026-07-01T22:22:48.474Z"
  }
]
```

This is what backs the app's transaction list screen. Call it on screen load
and on pull-to-refresh. An empty array `[]` means no transactions saved yet —
show an empty state, not an error.

### 3.5 `GET /transactions/:id` — single record

```
GET /transactions/1
Authorization: Bearer <key>
```

Response `200`: same shape as one element of the list above.
Response `404 {"error": "not found"}` if the id doesn't exist.
Response `400 {"error": "id must be a number"}` if `:id` isn't numeric.

Use this for a transaction detail screen (e.g. tapping a list item), though
since `GET /transactions` already returns full rows including `raw_json`, you
may not need a separate network call if you're just showing details from data
already in memory from the list.

## 4. Storing the API key on-device

The gateway API key is a long-lived bearer credential with full access to the
agent and the transactions database — treat it like a password:

- **Do not hardcode it in source code or commit it to version control.**
- Store it using Android's `EncryptedSharedPreferences`
  (`androidx.security:security-crypto`) or the Android Keystore, not plain
  `SharedPreferences`.
- Provide a one-time setup screen where the user pastes in their gateway API
  key and the base URL (§2), store both securely, and use them for all
  subsequent requests. Do not bake either into the APK.

## 5. Extracting JSON from the agent's `reply` string

This is the trickiest part of the client logic and needs to be done
carefully. The `reply` field from `/agent` (§3.2) is a natural-language string
that **usually, but not always**, contains a JSON object — often wrapped in a
markdown code fence. Implement extraction in this order, falling through to
the next step if one fails:

1. **Try direct parse first.** Attempt `JSONObject(reply.trim())`. If it
   succeeds, use it as-is. (Handles the case where the model returns pure
   JSON with no markdown wrapper.)
2. **Look for a fenced code block.** Search for a substring matching
   ` ```json ... ``` ` (case-insensitive on the "json" tag; also try a plain
   ` ``` ... ``` ` fence with no language tag) using a regex like:
   ```
   ```(?:json)?\s*\n([\s\S]*?)\n```
   ```
   Extract the captured group and `JSONObject(...)` parse *that*.
3. **Look for the first `{` to last `}` substring** as a last-resort fallback
   (in case the model added prose before/after the JSON without proper code
   fences) and attempt to parse that slice.
4. **If all of the above fail**, treat this as "not a structured extraction" —
   show the raw `reply` text to the user in the UI (e.g. as a chat-style
   response) and do **not** call `/transactions`. Give the user a way to
   retry (e.g. re-word their prompt) rather than silently failing.

Recommended UX: after successfully extracting a JSON object (steps 1–3),
**show the parsed fields to the user in a confirm/edit screen before saving**
(e.g. "We extracted: amount ₹140, ICICI Bank, Completed — Save this
transaction?") rather than silently auto-saving straight to `/transactions`.
Financial data extracted by an AI model can be wrong (misread digits, wrong
field mapped) — a confirmation step lets the user catch and correct mistakes,
or edit a field before it's persisted. This is a recommendation, not a hard
requirement — confirm with the product owner if silent auto-save is
preferred instead.

## 6. Suggested prompt construction

The gateway does **not** add any extraction instruction on its own — whatever
you put in the `text` field is exactly what the agent sees (plus, if
images are attached, an internal note the gateway adds telling the model the
image is already visible to it — you don't need to do anything for that
part, it's automatic server-side).

Recommended strategy: when the user attaches an image and the use case is
"extract transaction details," append a fixed instruction to whatever the
user typed (or use it standalone if the user typed nothing), e.g.:

```
Extract the transaction details from the attached screenshot and return them as structured JSON — include fields like amount, currency, date/time, sender, recipient, transaction/reference ID, and status if visible. If a field isn't visible, omit it rather than guessing.
```

If the user typed their own message (e.g. "how much did I pay Sushil last
week?"), you may want a different mode (a plain Q&A chat, not
extract-and-save) — that's a product decision: does this app only ever do
"extract and save a transaction," or is there also a general chat mode where
the reply is just shown and never posted to `/transactions`? **Confirm this
with the product owner before implementing** — this spec covers the
extraction flow fully; a general chat mode would reuse the same `/agent`
endpoint and response handling, just skip §5's JSON extraction and §3.3
entirely for that mode.

## 7. Suggested architecture (recommendation, not mandatory)

- **Networking:** Retrofit + OkHttp. Define a service interface with:
  - `@Multipart @POST("agent")` taking `@Part("text") RequestBody?` and
    `@Part files: List<MultipartBody.Part>` (empty list if none), returning
    the `{reply, filesReceived}` shape.
  - `@POST("transactions")` taking `@Body` a raw `JsonObject` (or
    `Map<String, Any?>`), returning the saved-row shape from §3.3.
  - `@GET("transactions")` with `@Query("limit")`, returning
    `List<TransactionRow>`.
  - `@GET("transactions/{id}")`.
  - An `OkHttpClient.Builder` with the generous timeouts from §3.2, and an
    interceptor that adds `Authorization: Bearer <key>` (read from encrypted
    storage, §4) to every request.
  - Base URL built from the user-editable setting (§2), not hardcoded.
- **Async/long-running work:** Kotlin coroutines (`Dispatchers.IO`) for the
  `/agent` call given it can take minutes; consider `WorkManager` if you want
  the extraction to survive the app being backgrounded/killed (recommended
  for a good user experience, but a simpler in-app coroutine + progress
  dialog is acceptable for a first version).
- **UI (suggest Jetpack Compose, but not mandatory):**
  1. **Setup/Settings screen** — base URL + API key entry (§2, §4).
  2. **Compose screen** — text input, image picker (gallery) and/or camera
     capture, "Send" button, loading state while waiting for `/agent`.
  3. **Confirm/result screen** — shows extracted fields (§5) with an edit +
     save action that calls `/transactions`, or shows raw text if extraction
     failed.
  4. **Transactions list screen** — calls `GET /transactions`, pull-to-refresh,
     empty state, tap-through to a detail view.
- **Local caching (optional enhancement):** a small Room database mirroring
  `GET /transactions` results for offline viewing is a nice-to-have, not
  required for a first version — the gateway is the source of truth.

## 8. Manual testing checklist (verify against the real server before/while wiring up the app)

These are the exact curl commands already verified working against the live
gateway — use them to sanity-check your understanding of the API, or to
debug the app's HTTP layer against ground truth if something doesn't match.

```bash
# Health check (no auth)
curl https://kartiks-mac-mini-7.tailafb282.ts.net/healthz

# Text-only agent call
curl https://kartiks-mac-mini-7.tailafb282.ts.net/agent \
  -H "Authorization: Bearer <gateway key>" \
  -F "text=say hi in exactly 3 words"

# Text + image agent call (real vision analysis)
curl https://kartiks-mac-mini-7.tailafb282.ts.net/agent \
  -H "Authorization: Bearer <gateway key>" \
  -F "text=Extract the transaction details from the attached screenshot and return them as structured JSON — include fields like amount, currency, date/time, sender, recipient, transaction/reference ID, and status if visible. If a field isn't visible, omit it rather than guessing." \
  -F "files=@/path/to/screenshot.jpg"

# Save an extracted record
curl -X POST https://kartiks-mac-mini-7.tailafb282.ts.net/transactions \
  -H "Authorization: Bearer <gateway key>" \
  -H "Content-Type: application/json" \
  -d '{"amount": "₹140", "currency": "INR", "status": "Completed"}'

# List saved records
curl https://kartiks-mac-mini-7.tailafb282.ts.net/transactions \
  -H "Authorization: Bearer <gateway key>"

# Fetch one record
curl https://kartiks-mac-mini-7.tailafb282.ts.net/transactions/1 \
  -H "Authorization: Bearer <gateway key>"
```

If any of these fail from a phone/terminal *on the tailnet*, that's a network/
Tailscale issue to resolve before debugging app code — confirm these all
work from another device on the tailnet first.

## 9. Open questions to confirm with the product owner before/during implementation

1. **Auto-save vs. confirm-before-save** (§5) — does the extracted JSON get
   saved to `/transactions` automatically, or only after the user reviews and
   confirms it?
2. **Single mode vs. dual mode** (§6) — is this app *only* ever "attach a
   receipt, extract, save," or does it also need a general chat mode against
   the same agent where nothing gets saved?
3. **Editing/deleting saved transactions** — the gateway currently only
   supports create (`POST`) and read (`GET`) for `/transactions`, no
   update/delete endpoints exist yet. If the app needs to let users edit or
   remove a saved transaction, that requires new gateway endpoints (not yet
   built) — flag this back rather than assuming it's possible.
4. **Multiple devices / multiple API keys** — if more than one person's phone
   will use this app, each device should probably get its own gateway API key
   (the gateway supports a comma-separated list in `GATEWAY_API_KEYS`) so
   access can be revoked per-device later. Confirm whether this matters for
   v1.
