# recipe-client-android-quickstart — Repo Card

> Android (Kotlin + Jetpack Compose) voice-agent quickstart. The app owns the Agora RTC + RTM lifecycle and renders live transcripts via the vendored ConversationalAIAPI toolkit. A key-less Python/FastAPI backend mints tokens and runs a managed STT→LLM→TTS cascade.

## Identity

| Field          | Value                                                                              |
| -------------- | ---------------------------------------------------------------------------------- |
| Repo           | `AgoraIO-Conversational-AI/recipe-client-android-quickstart`                       |
| Type           | `frontend-app` (Android client + bundled token service)                            |
| Language       | Kotlin / Android (Jetpack Compose) + Python 3.12 (FastAPI token service)           |
| Deploy Target  | Android device / emulator; `server/` as a reachable FastAPI service                |
| Owner          | Agora Conversational AI DevEx                                                      |
| Last Reviewed  | 2026-06-25                                                                         |
| Recipe Role    | `base`                                                                             |
| Recipe Version | `1.0.0`                                                                            |
| Recipe Status  | `experimental`                                                                     |

## L1 — Summaries

The Audience column helps agents prioritise: **Use** = consuming the recipe's behavior, **Maintain** = modifying internals.

| File                                     | Purpose                                                                               | Audience       |
| ---------------------------------------- | ------------------------------------------------------------------------------------- | -------------- |
| [01_setup](L1/01_setup.md)               | Android Studio / Gradle / SDK toolchain, backend venv, env vars, build + run steps   | Use & Maintain |
| [02_architecture](L1/02_architecture.md) | App ↔ token service ↔ Agora ConvoAI topology and session lifecycle                   | Maintain       |
| [03_code_map](L1/03_code_map.md)         | `android/` and `server/` trees with key file responsibilities                         | Maintain       |
| [04_conventions](L1/04_conventions.md)   | Kotlin/Compose patterns, coroutine/IO threading, Python FastAPI idioms                | Maintain       |
| [05_workflows](L1/05_workflows.md)       | Common changes: add screen, change token endpoint, build/run on device/emulator       | Use            |
| [06_interfaces](L1/06_interfaces.md)     | Token-service REST contract, Agora SDK surface, env vars, response envelope           | Use & Maintain |
| [07_gotchas](L1/07_gotchas.md)           | Emulator host alias, ordering constraints, cleartext traffic, vendored toolkit rules  | Maintain       |
| [08_security](L1/08_security.md)         | Token handling, App Certificate stays server-side, Android permissions                | Maintain       |

## Recipe Profile

This repo declares `Recipe Role: base`. See [RECIPE.md](RECIPE.md) for extension points, invariants, and stable contracts before changing reusable surfaces.
