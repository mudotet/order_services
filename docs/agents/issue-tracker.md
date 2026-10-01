# Issue tracker: GitHub

Issues and specs live in GitHub Issues for `mudotet/order_services`.
Use `gh` with `--repo mudotet/order_services` for issue operations.

## Operations

- Publish: `gh issue create --repo mudotet/order_services --title "..." --body "..."`
- Fetch: `gh issue view <number> --repo mudotet/order_services --comments`
- List: `gh issue list --repo mudotet/order_services --state open --json number,title,body,labels,comments`
- Comment: `gh issue comment <number> --repo mudotet/order_services --body "..."`
- Label: `gh issue edit <number> --repo mudotet/order_services --add-label "..."` or `--remove-label "..."`
- Close: `gh issue close <number> --repo mudotet/order_services --comment "..."`

Existing `.scratch/` specs and `docs/superpowers/` plans remain reference material.
Compare them with current code; do not migrate or republish them unless requested.

## Pull requests as a triage surface

**PRs as a request surface: no.**

GitHub issues and PRs share numbers. When necessary, resolve a reference
with `gh pr view <number>` and fall back to `gh issue view <number>`.

## Wayfinding

- Map: one issue labelled `wayfinder:map`.
- Children: sub-issues labelled `wayfinder:<type>`; otherwise use a map task list
  and `Part of #<map>` in each child.
- Blocking: native issue dependencies, using the blocker's database ID,
  not its issue number; otherwise record `Blocked by: #<number>`.
- Frontier: first open child in map order without an open blocker or assignee.
- Claim: assign the ticket with `gh issue edit <number> --add-assignee @me`.
- Resolve: comment with the answer, close the child, and update the map's
  Decisions-so-far with a summary and link.

Use the explicit repository for all commands above. Configure workflows
without creating remote issues, labels, or assignments during setup.
