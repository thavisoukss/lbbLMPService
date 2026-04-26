# LMPS Service — Gemini CLI Instructions

## Project Overview

**lbbLMPService** is a Spring Boot 3.x middleware API for **Cross-bank Fund Transfer (PromptPay/QR)**.
It receives requests from the **LBB Mobile Banking app**, applies business logic, and forwards calls to
the internal **m-smart** service via REST.

- **Group ID / Base package**: `com.lbb.lmps`
- **Artifact**: `lmps`
- **Java**: 21 | **Spring Boot**: 3.4.x | **Build**: Maven
- **Port**: 8084 | **Base URL**: `lmps/api`
- **Security**: JWT (RS256) via `JwtAuthFilter` — all routes authenticated except `/auth/**`
- **Logging**: Log4j2 (`classpath:log4j2.xml`) with Micrometer Brave tracing

### Docs Structure
- /docs/api/       → our controller endpoints (request/response)
- /docs/logic/     → business flow and rules
- /docs/external/  → external APIs we call (m-smart, cbs, etc)
- /docs/db/        → schema / table notes

---

## Profiles & Config

| Profile | File | Purpose |
|---------|------|---------|
| `dev` (default) | `application-dev.yaml` | Dev/staging — m-smart at `172.16.4.18:7001` |
| `local` | `application-local.yaml` | Local dev — same external URLs, DB commented out |

Active profile is set in `application.yaml` via `spring.profiles.active`.

---

## Controller Logging Pattern

**What**: Every controller method must log START, request body, END, final response, and duration_ms — no exceptions.

**Why**: Enables end-to-end traceability and duration monitoring in production without attaching a debugger. Financial transactions require an audit trail at every entry point.

**How**: Follow this exact structure in every controller method:

```java
log.info(">>> START {methodName} >>>");
log.info("> request body: {}", request);
long start = System.currentTimeMillis();
// ... method logic ...
log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
log.info("<<< END {methodName} request <<<");
```

**Example — `logon` endpoint:**
```java
log.info(">>> START logon >>>");
log.info("> request body: {}", request);
long start = System.currentTimeMillis();
// logic
log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
log.info("<<< END logon request <<<");
```

---

## Service Logging Pattern

**What**: Every service method must log key events throughout its execution flow.

**Required log points**:
- Security verification start/pass (`info`)
- WITHDRAW_TXN loaded (`info`)
- Before each external API call (`info`)
- External API success (`info`) / failure (`warn`)
- DB record updated (`info`)
- Method completed (`info`)

**Tag format**: prefix every log line with `[methodName]`.

---

## Coding Standards

### No Temporary Fixes
- Always find and fix the root cause. No workarounds or patches.

### Minimal Code
- Don't add abstractions, helpers, or error handling for scenarios that can't happen.

### Simplicity First
- Make the smallest change that solves the problem.

### Logging
- Use `SLF4J` / `Log4j2` for all logging. Never use `System.out.println`.

### Database Access
- Use Oracle JDBC (`ojdbc8`) with JPA. Apply `@Transactional` at the service layer.

### External API Calls
- Use `RestClient` for all calls with connect-timeout 5s and read-timeout 10s.

---

## Workflow Rules (Gemini CLI)

### 1. Research & Plan Mode
- Use `enter_plan_mode` for any non-trivial task (3+ steps or architectural decisions).
- Systematically map the codebase using `grep_search` and `glob` before acting.

### 2. Self-Improvement Loop
- After any correction from the user, update `.gemini/tasks/lessons.md` with the pattern and a rule to prevent recurrence.
- Review `.gemini/tasks/lessons.md` at the start of each session.

### 3. Validation is Mandatory
- Never mark a task complete without proving it works.
- Use `run_shell_command` to run tests and verify behavior.
- Add new test cases for bug fixes and features.

### 4. Context Efficiency
- Minimize turns by utilizing parallel searching and reading.
- Use `grep_search` to identify points of interest instead of reading files individually.

### 5. Security First
- Never log, print, or commit secrets, API keys, or sensitive credentials.
- Rigorously protect `.env` files and configuration folders.

### 6. Project Skills Usage
- **What**: Use project skills in `.gemini/skills/` for any task matching an available pattern.
- **Why**: Skills encapsulate your team's specific standards (logging, JPA, etc.) and prevent architectural drift.
- **How**: Read the relevant `SKILL.md` before implementation to ensure compliance with local conventions.

### 7. Sub-agent Delegation
- **What**: Delegate complex or domain-specific subtasks to sub-agents.
- **Why**: Keeps the main session context lean and allows for expert focus on specific tech stacks (e.g., Docker, Spring Boot).
- **How**: Use `invoke_agent` with instructions informed by the personas defined in `.gemini/agents/`.

---

## Google Drive — Docs Path

API flow docs live in Google Drive. Read them directly from the local sync folder.

| Device | Path |
|--------|------|
| Mac | `/Users/nohder/Library/CloudStorage/GoogleDrive-noh.sayachack@gmail.com/My Drive/LBB/lbbplus_api_v1/docs/flows` |
| Windows | `C:\Users\YourName\Google Drive\My Drive\LBB\lbbplus_api_v1\docs\flows` |

When the user references a flow doc, read it from the path matching the current OS (`darwin` = Mac, `windows` = Windows).
