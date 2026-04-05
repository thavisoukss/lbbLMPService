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
- /docs/api/       → our controller endpoints (request/response)
- /docs/logic/     → business flow and rules
- /docs/external/  → external APIs we call (m-smart, cbs, etc)
- /docs/db/        → schema / table notes

---

## Controller Logging Pattern

Every controller method **must** follow this exact structure (no exceptions):

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

## Coding Standards

- **No temporary fixes** — find root causes; senior-developer standards
- **Minimal code** — don't add abstractions, helpers, or error handling for scenarios that can't happen
- **Simplicity first** — smallest change that solves the problem; impact minimal code
- Use `SLF4J` / `Log4j2` for all logging — never `System.out.println`
- Oracle JDBC (`ojdbc8`) for DB; JPA for data access — use `@Transactional` appropriately
- Use `RestClient` for calls to external API (connect-timeout: 5 s, read-timeout: 10 s)

---

## Profiles & Config

| Profile | File | Purpose |
|---------|------|---------|
| `dev` (default) | `application-dev.yaml` | Dev/staging — m-smart at `172.16.4.18:7001` |
| `local` | `application-local.yaml` | Local dev — same external URLs, DB commented out |

Active profile is set in `application.yaml` via `spring.profiles.active`.

---

## Workflow Rules

### 1. Plan Mode Default
- Enter plan mode for ANY not-trivial task (3+ steps or architectural decisions)
- Use plan mode for verification steps, not just building
- Write detailed specs upfront to reduce ambiguity

### 2. Self-Improvement Loop
- After ANY correction from the user: update `tasks/lessons.md` with the pattern
- Write rules for yourself that prevent the same mistake
- Ruthlessly iterate on these lessons until the mistake rate drops
- Review lessons at session start for a project

### 3. Verification Before Done
- Never mark a task complete without proving it works
- Diff behavior between main and your changes when relevant
- Ask yourself: *"Would a staff engineer approve this?"*
- Run tests, check logs, demonstrate correctness

### 4. After a Correction
Update `tasks/lessons.md` with the pattern that caused the mistake and a rule to prevent it next time.

### 5. Demand Elegance (Balanced)
- For non-trivial changes: pause and ask *"is there a more elegant way?"*
- If a fix feels hacky: "Knowing everything I know now, implement the elegant solution"
- Skip this for simple, obvious fixes. Don't overengineer
- Challenge your own work before presenting it

### 6. Skills usage
- Use skills for any task that requires a capability
- Load skills from `.claude/skills/`
- Invoke skills with natural language
- Each skill is one independent capability

### 7. Subagents usage
- Use subagents liberally to keep the main context window clean
- Load subagents from `.claude/agents/`
- For complex problems, throw more compute at it via subagents
- One task per subagent for focused execution on a given tech stack

## Core Principles
- **Simplicity First**: Make every change as simple as possible. Impact minimal code
- **No Laziness**: Find root causes. No temporary fixes. Senior developer standards
---

## Project General Conventions
- Always use the latest versions of dependencies.
- Always write Java code as the Spring Boot application.
- Always use Maven for dependency management.
- Always create test cases for the generated code both positive and negative.
- Always generate the CircleCI pipeline in the .circleci directory to verify the code.
- Minimize the amount of code generated.
- The Maven artifact name must be the same as the parent directory name.
- Use semantic versioning for the Maven project. Each time you generate a new version, bump the PATCH section of the version number.
- Use `pl.piomin.services` as the group ID for the Maven project and base Java package.
- Generate the Docker Compose file to run all components used by the application.
- Update `README.md` each time you generate a new version.