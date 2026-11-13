Closes #1
Traces to: UC-STU-deactivate-student, docs/design/stu.md
Design pull request: https://github.com/A-A-Rivas/add-a-use-case/pull/2

## Why
A withdrawn student needs confirmed loss of access and future evaluation obligations while the team's membership and submitted record remain intact.

## What changed
Added the dedicated idempotent transition, live-account JWT validation, profile bypass protection, confirmation UI, and week-specific history/eligibility handling.
Kept existing reactivation behavior and membership; no schema change. The details-read route now also admits the course owner so the approved flow is reachable.

## How it was verified
- **Verified:** 8 of 8 Cypress tests; 480 backend tests (zero failures/errors/skips), including the new integration tests; 60 targeted checks, lint, Vite bundle, and an assertion-failing mutation experiment; T1–T13 mapping and exact results are in [.github/drafts/deactivation-verification.md](https://github.com/A-A-Rivas/add-a-use-case/blob/feat/deactivate-student/.github/drafts/deactivation-verification.md).
- **Not verified:** Transaction rollback, concurrent races, reload reconciliation and live browser/backend interaction. Full frontend build fails type checks in unchanged files; the Vite bundle and changed-file lint pass.

[Confirmation screenshot](https://github.com/A-A-Rivas/add-a-use-case/blob/feat/deactivate-student/.github/drafts/student-deactivation-confirmation.png)

## Reviewer focus
Start with trunk `6e9cbcb`, then backend follow-up `0466b26`, which contains the deactivation PUT rule, owner details-read rule, scoped details lookup, controller and tests together.
Review shared JWT decoding, lifecycle/profile safeguards, and evaluation eligibility/history before controller/UI leaves; no refactor or migration commit.
Design change: `stu.md` records owner details-read access and the API row; the architecture now names implemented JWT validation and remaining verification limits.

## The tests I kept
Service/security/controller tests derive from T1/T2/T4/T5/T6/T12/T13; validator/real-RSA tests from FR-SEC-active-account and T7; evaluation/status tests from T8–T11's business rules.
[The verification report](https://github.com/A-A-Rivas/add-a-use-case/blob/feat/deactivate-student/.github/drafts/deactivation-verification.md#tests-and-their-oracles) gives each kept test and its oracle; integration tests passed; all 8 Cypress tests passed with stubbed HTTP.
Rewrote old profile tests to preserve status under the approved dedicated-action contract, and replaced the history test's argument-sensitive mock with fixture-based lookup so mutation reaches its oracle; no tests deleted.

## The code I broke
The agent temporarily restored the old team-only report filter; after correcting a mock setup that initially masked the oracle, the history test failed on missing historical student 3 and unexpected disabled student 4.
Production code was restored and targeted checks reran. Exact red/green results are in the verification report; this does not claim the developer performed the experiment.

## My review
Pending the developer's personal review: triage depth, T1–T13 coverage assessment, and at least one labeled diff comment.
Fresh-context build review has not run; add its findings and each outcome before submission. Agent verification above is separate from that review.

<!-- Base repository must be A-A-Rivas/add-a-use-case; base branch main. Leave this build PR open and unmerged. -->
