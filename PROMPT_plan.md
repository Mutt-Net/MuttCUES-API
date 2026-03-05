# PROMPT_plan.md

Planning mode instructions for Stratum.

---

## Mode: PLANNING

Your goal is to analyze the project and create a prioritized implementation plan. **Do not implement anything** - only plan.

---

## Instructions

### Phase 0: Orientation

**0a.** Study `PROJECT_SPEC.md` to understand the audience, jobs to be done, and technical constraints.

**0b.** Study `specs/*` with parallel subagents to learn the detailed requirements for each topic of concern.

**0c.** Study `IMPLEMENTATION_PLAN.md` (if present) to understand any existing plan.

**0d.** Study `src/lib/*` with parallel subagents to understand shared utilities and components.

**0e.** Study `src/*` with parallel subagents to understand the current application state.

---

### Phase 1: Gap Analysis

Compare `specs/*` against `src/*`:

- What requirements are fully implemented?
- What requirements are partially implemented?
- What requirements are missing entirely?
- What exists but shouldn't (per non-goals)?

**Search for:**
- TODO comments
- Placeholder implementations
- Skipped or flaky tests
- Inconsistent patterns
- Missing error handling

**Do not assume functionality is missing** - confirm with code search first.

---

### Phase 2: Create Implementation Plan

Create or update `IMPLEMENTATION_PLAN.md` as a prioritized bullet list:

```markdown
# Implementation Plan

## Status
- Total tasks: X
- Completed: Y
- Remaining: Z

## Tasks

- [ ] **Task ID**: Task description
  - Spec: `specs/xxx.md`
  - Required tests: [test outcomes from acceptance criteria]
  - Notes: [any context]

- [x] **Task ID**: Completed task
  - Completed: YYYY-MM-DD
```

**Prioritization criteria:**
1. Foundational work first (infrastructure, core utilities)
2. Dependencies before dependents
3. High-value features before edge cases
4. Risk mitigation (hard/uncertain tasks earlier)

---

## Output

**Only** update `IMPLEMENTATION_PLAN.md`. Do not:

- Modify source code
- Create new files (except `specs/` if gaps found)
- Run tests or builds
- Make commits

---

## Important Principles

**Treat `src/lib/` as the standard library** - Prefer consolidated, idiomatic implementations there over ad-hoc copies.

**Plan is disposable** - If wrong or stale, regenerate it.

**Markdown over JSON** - Keep everything human-readable.

**Ultrathink** - Take time to analyze thoroughly before writing the plan.

---

## Ultimate Goal

Create a clear, actionable `IMPLEMENTATION_PLAN.md` that:
- Reflects the true state of the codebase
- Prioritizes work logically
- Includes all missing requirements from specs
- Notes discoveries and potential issues
- Enables efficient building in BUILDING mode
