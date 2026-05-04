# LMPS Service — Claude Code Instructions

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
- /docs/api/       → our controller endpoints (request/response); includes p2p and LMP controllers, error codes
- /docs/logic/     → business flow and rules per feature (inquiry, transfer-out, p2p, QR, bio-verify)
- /docs/external/  → external APIs we call (m-smart, cbs, minio)
- /docs/db/        → schema / table notes; /docs/db/tables/ has per-table markdown files
- /docs/test/      → manual curl/shell scripts for smoke-testing endpoints

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

**What**: Every service method must log key events throughout its execution flow to support operations team debugging and monitoring.

**Why**: Controllers capture entry/exit. Services hold the business logic where failures actually occur — validation, external calls, DB writes. Without service-level key-event logs, ops cannot pinpoint which step failed or how long each step took.

**Required log points** (log every applicable one, in order):

| Event | Level | What to include |
|-------|-------|-----------------|
| Validation/security start | `info` | key identity field (`customerId`, `txnId`) |
| Validation/security pass | `info` | same field |
| Key state loaded from DB | `info` | entity `id` + `txnId` |
| Before each external API call | `info` | key request fields + `elapsed_ms` so far |
| External API success | `info` | key response fields (e.g. `cbsRefNo`, `txnId`) |
| External API failure | `warn` | `code`, `msg` |
| Unhandled exception | `error` | exception message + `txnId` |
| DB write completed | `info` | `id`, new status, key reference fields |
| Method completed | `info` | `txnId`, key amounts, `duration_ms` |

**Example — transfer service method:**
```java
log.info("[transferOutAccount] verifying security questions customerId={}", customerId);
// ... verify ...
log.info("[transferOutAccount] security questions verified ok customerId={}", customerId);
log.info("[transferOutAccount] loaded withdraw txn id={} txnId={}", withdrawTxn.getId(), withdrawTxn.getTransactionId());
log.info("[transferOutAccount] calling m-smart transfer-out txnId={} amount={} fee={} ccy={} elapsed_ms={}", txnId, amount, fee, ccy, System.currentTimeMillis() - start);
// ... call ...
log.info("[transferOutAccount] m-smart transfer-out success cbsRefNo={} txnId={}", cbsRefNo, txnId);
log.info("[transferOutAccount] withdraw txn updated id={} status=COMPLETED cbsRefNo={} txnId={}", id, cbsRefNo, txnId);
log.info("[transferOutAccount] completed txnId={} totalAmount={} feeAmt={} cbsRefNo={} duration_ms={}", txnId, totalAmount, feeAmt, cbsRefNo, System.currentTimeMillis() - start);
```

**Tag format**: prefix every log line with `[methodName]` so grep can isolate a single flow across interleaved threads.

---

## Coding Standards

### No Temporary Fixes
**What**: Always find and fix the root cause. No workarounds or patches.
**Why**: Temporary fixes accumulate as hidden debt in a financial middleware — they surface as incidents in production.
**How**: Before writing code, identify the actual failure point. If a fix feels like a patch, it is — find the real cause instead.

### Minimal Code
**What**: Don't add abstractions, helpers, or error handling for scenarios that can't happen.
**Why**: Unnecessary code increases cognitive load and surface area for bugs.
**How**: Ask "can this scenario actually occur?" before adding a code path. If not, omit it.

### Simplicity First
**What**: Make the smallest change that solves the problem.
**Why**: Overengineering introduces risk in a system with hard uptime requirements.
**How**: Write the change, then ask "can this be smaller?" before submitting.

### Logging
**What**: Use `SLF4J` / `Log4j2` for all logging.
**Why**: Consistent log format enables log aggregation and alerting. `System.out.println` bypasses the logging pipeline entirely.
**How**: Inject `Logger log = LoggerFactory.getLogger(...)` — never use `System.out.println` or `System.err`.

### Database Access
**What**: Use Oracle JDBC (`ojdbc8`) with JPA. Apply `@Transactional` at the service layer — read-only queries use `@Transactional(readOnly = true)`.
**Why**: Consistent transaction boundaries prevent partial writes. Read-only hints allow Oracle to skip undo log generation, improving query performance.
**How**: Annotate service methods, not repositories. Write operations get `@Transactional`; read-only queries get `@Transactional(readOnly = true)`.

### External API Calls
**What**: Use `RestClient` for all calls to external APIs with connect-timeout 5 s and read-timeout 10 s.
**Why**: m-smart and CBS can hang under load — unbounded timeouts will exhaust the thread pool.
**How**: Configure timeouts in the `RestClient` bean. Never use `RestTemplate` or raw `HttpURLConnection`.

---

## Workflow Rules

### 1. Plan Mode Default
**What**: Enter plan mode for any non-trivial task — 3+ steps or any architectural decision.
**Why**: Implementing before aligning on approach wastes effort and introduces risk of rework.
**How**: Use plan mode for both building and verification steps. Write detailed specs upfront before touching code.

### 2. Self-Improvement Loop
**What**: After any correction from the user, update `tasks/lessons.md` with the pattern and a rule to prevent recurrence.
**Why**: Repeating the same mistake signals a missing rule, not a one-off error. Persistent rules eliminate classes of mistakes.
**How**: Record the pattern that caused the mistake, the rule that prevents it, and review `tasks/lessons.md` at the start of each session.

### 3. Verification Before Done
**What**: Never claim a change is complete without running verification and showing evidence.
**Why**: "I edited the file" is not done. "I edited the file and here's the output" is done. "Should work now" is never acceptable — assertions require evidence.
**How**: Run tests, check logs, and diff behavior between main and your changes. Show the output. Ask: *"Would a staff engineer approve this?"*

### 4. Demand Elegance (Balanced)
**What**: For non-trivial changes, pause and evaluate whether a more elegant solution exists before finalizing.
**Why**: Hacky solutions compound — in a financial API, a clever shortcut today becomes an incident tomorrow.
**How**: After writing a fix, ask *"Knowing everything I know now, is this the elegant solution?"* Skip this step for simple, obvious fixes. Don't overengineer.

### 5. Skills Usage
**What**: Use skills for any task that matches an available capability.
**Why**: Skills encapsulate specialized patterns and prevent reinventing solved problems.
**How**: Load skills from `.claude/skills/`. Invoke with natural language. Treat each skill as one independent capability.

### 6. Subagents Usage
**What**: Delegate complex or isolated subtasks to subagents.
**Why**: Subagents keep the main context window clean and allow focused execution per tech area.
**How**: Load subagents from `.claude/agents/`. Assign one task per subagent. For complex problems, use multiple subagents in parallel.

### 7. No Magic
**What**: All assumptions must be stated explicitly. Never invent infrastructure, services, or behavior that wasn't specified.
**Why**: Hidden assumptions in a financial middleware cause silent failures — wrong routing, phantom services, misread contracts.
**How**: If context is missing, state the assumption before proceeding: *"I'm assuming X — correct me if wrong."* Never hallucinate config, endpoints, or DB schema.

### 8. Dissent
**What**: Before any major change, surface concerns out loud before touching code.
**Why**: Momentum kills judgment. The questions nobody asks are the ones that cause incidents.
**How**: For any significant change, answer these before starting:
- What's the blast radius if this goes wrong?
- What assumptions are we making?
- What's the reversibility path?
- What are we NOT seeing because of momentum?

### 9. Scope Drift Detection
**What**: Track stated goals vs actual execution. Flag scope creep immediately.
**Why**: "Just one more thing" accumulates silently — a bug fix becomes a module rewrite without anyone deciding that.
**How**: State the original goal at the start. If the work grows beyond it, stop and flag: *"The ask was X but we're now doing Y — is that intentional?"* Treat nice-to-haves as out-of-scope unless explicitly promoted.

### 10. Reversibility (R0 / R1 / R2)
**What**: Classify every action by reversibility before executing it.
**Why**: Not all actions carry equal risk. The cost of pausing on an R0 is low; the cost of proceeding is potentially catastrophic.
**How**:
- **R0 (irreversible)** — STOP. Describe the action and ask before proceeding. Examples: dropping DB tables, force-pushing main, deleting prod data.
- **R1 (costly to reverse)** — Proceed, but explain the reasoning. Examples: schema migrations, dependency upgrades, config changes in shared services.
- **R2 (easily reversed)** — Just do it. Examples: editing local source files, adding logs, writing tests.

---

## Google Drive — Docs Path

API flow docs live in Google Drive. Read them directly from the local sync folder.

| Device | Path |
|--------|------|
| Mac | `/Users/nohder/Library/CloudStorage/GoogleDrive-noh.sayachack@gmail.com/My Drive/LBB/lbbplus_api_v1/docs/flows` |
| Windows | `C:\Users\YourName\Google Drive\My Drive\LBB\lbbplus_api_v1\docs\flows` |

When the user references a flow doc, read it from the path matching the current OS (`darwin` = Mac, `windows` = Windows).
