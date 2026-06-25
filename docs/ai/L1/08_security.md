# 08 · Security

> Trust boundaries, secret handling, Android permissions, and notes for production hardening.

## Trust boundaries

| Hop | Auth |
| --- | ---- |
| Android app → token service | None in local dev (HTTP on the same LAN). |
| Token service → Agora Cloud | Token007, generated from `AGORA_APP_ID` + `AGORA_APP_CERTIFICATE`. |
| Agora Cloud → AI vendors | Agora-managed credentials (keyless). `OPENAI_API_KEY` optional for BYO. |
| App ↔ Agora RTC/RTM | Token007 issued per `get_config` call; expiry 3600s. |

## Secret handling

- **`AGORA_APP_CERTIFICATE` stays on the server.** It lives only in `server/.env.local` (gitignored) and is never returned to the app. The app receives only a short-lived Token007.
- **No secrets in the APK.** `AGENT_BACKEND_URL` is baked in at build time but is a URL, not a credential.
- `server/.env.local` is gitignored; `server/.env.example` (if present) ships placeholders only.
- Tokens (`generate_convo_ai_token`) expire after 3600s; a new token is fetched on each `GET /get_config`.

## Android permissions

| Permission | Declared in manifest | Purpose |
| ---------- | :------------------: | ------- |
| `INTERNET` | Yes | HTTP to token service + Agora RTC/RTM |
| `RECORD_AUDIO` | Yes | Microphone for voice input; requested at runtime in `LandingScreen` via `ActivityResultContracts.RequestPermission` |

The app requests `RECORD_AUDIO` just before the user taps Connect. If denied, `onConnect` is not called.

## Cleartext traffic

`android:usesCleartextTraffic="true"` is set in the manifest for local HTTP development. Before production:
- Scope it to debug builds via a `network_security_config` XML.
- Or use HTTPS for the backend and remove the flag entirely.

## CORS

The backend sets `CORSMiddleware` with `allow_origins=["*"]`. This is open by design for a local/dev recipe. Lock down origins before any production deployment.

## Validation

- `Agent.start()` rejects empty `channel_name` and non-positive `agent_uid`/`user_uid` before issuing tokens or starting a session.
- Route errors are sanitized: `_log_route_error` logs only non-`None` context; SDK exceptions map to 400/500 without leaking internals beyond the message.

## Deployment notes

- The published Docker image is **backend-only** (`:8000`); it does not bundle secrets. Pass `AGORA_APP_ID` and `AGORA_APP_CERTIFICATE` as environment variables at container start.
- Build the Android app in release mode with ProGuard/R8; ensure `AGENT_BACKEND_URL` points at an HTTPS endpoint.

## Related Deep Dives

- None.
