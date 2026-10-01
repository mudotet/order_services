# Agent guide

## Start here

- Read [README.md](README.md) for setup, current routes, limitations, and links to API guides. Verify behavior against the current controllers, services, entities, and `pom.xml` before changing it.
- Inspect `git status` before editing. Preserve existing user changes, including untracked implementation files; keep the diff within the requested task.
- Read the relevant feature spec when implementing a ticket. `.scratch/` specs and `docs/superpowers/` plans record earlier requirements, not guaranteed current contracts. Surface conflicts instead of silently changing code to match an old document.

## Implementation conventions

- Keep the flow `controller -> service/impl -> repository -> database` under `src/main/java/com/example/order_services/`. Put request/response DTOs in their existing packages.
- Follow nearby Lombok, constructor-injection, Bean Validation, JPA, and DTO-mapping patterns. Use existing dependencies; avoid new layers for a single use case.
- JSON APIs use `BaseResponse`; business errors use `ApplicationException` and `EnumCode`, handled by `GlobalExceptionHandler`. CSV downloads return a file directly.
- Respect soft deletion and persisted audit fields. Match the existing MySQL schema; `ddl-auto: validate` validates mappings rather than migrating tables. Review `docs/sql/` before schema work and explicitly identify missing migrations.
- Derive the acting user from authenticated email and resolve their database ID. Preserve role, ownership, shipper-assignment, and delivery-attempt checks; a client-supplied ID is not authorization.
- Keep monetary calculations server-side with `BigDecimal`. Preserve stored order prices and totals when reading tracking or returns.
- Check transaction boundaries and locking before editing checkout or delivery. Checkout currently has a deferred write-transaction issue; do not present it as atomic or silently fix it outside scope.

## Verification

- Java 25 is required. Use the Maven wrapper; there is no configured standalone lint task. `./mvnw -DskipTests compile` checks Java compilation; see README for test and package commands.
- Full tests are not required for every edit. Choose focused checks appropriate to the change or leave runtime verification to the user when requested. Report exactly what ran, what was skipped, and any environment blocker.
- For documentation-only edits, verify local links and compare documented commands/routes with config and controller mappings. Keep historical plans labeled as historical rather than rewriting their original requirements.

## Agent skills

### Issue tracker

Issues and specs live in GitHub Issues for `mudotet/order_services`; read `docs/agents/issue-tracker.md` when fetching or publishing work items.

### Triage labels

The five canonical triage roles use their default label strings; read `docs/agents/triage-labels.md` when assigning readiness.

### Domain docs

Single-context: root `CONTEXT.md` and `docs/adr/`; read `docs/agents/domain.md` before exploring domain concepts or architectural decisions.
