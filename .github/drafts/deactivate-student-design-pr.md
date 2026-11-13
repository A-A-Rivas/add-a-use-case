<!-- Title: Design UC-STU-deactivate-student -->
<!-- Base repository: A-A-Rivas/add-a-use-case; base branch: main. -->
<!-- Replace the pending issue marker with Refs #N after creating the issue. -->

Issue: pending creation; use Refs #N, not Closes #N.
Traces to: UC-STU-deactivate-student, docs/design/stu.md

## Why
A withdrawn student's account needs a confirmed deactivation that stops access and future evaluation obligations while retaining the team's educational record.

## What changed
The developer chose retained membership, blocked new evaluations with preserved history, harmless retries, denial of existing-token access, scoped instructor access, and cancellation without changes; rules and E1–E8 record those decisions.
The STU design specifies the dedicated action, account validator, scoped queries, report selection, profile-edit bypass protection, and T1–T13; reactivation is constrained only in documentation.
The architecture Authentication description now separates live account checks from permanent token revocation; OI-40 records 25 remaining empty-extension use cases, and the BR index is regenerated.

## How it was verified
- **Verified:** full documentation inventories/reference checks with findings in `.github/drafts/deactivate-student-spec-check.md`; changed-policy review; fresh-context questions test below, followed by fixes and recheck.
- **Not verified:** executable tests or UI behavior; no production implementation, mutation experiment, personal student review, remote issue/PR creation, or design merge is claimed.

<details>
<summary>Fresh-context questions test</summary>

An independent agent read only the target use case, cited security FRs/business rules, architecture Crosscutting Concepts, and STU design. It did not read production code or the prior conversation.

1. **Stale authentication statement:** the architecture said tokens remained valid for their full two hours. **Fixed:** architecture now explicitly identifies the designed live-account validator and keeps the permanent per-token revocation limitation separate. The implementation is still pending the merge gate.
2. **Unconfirmed report locator:** the design initially qualified its report endpoint as provisional. **Fixed:** verified the existing controller mapping and recorded `GET /evaluations/sections/{sectionId}/week/{week}`.
3. **Harmless choices retained:** repository method names, JPQL versus specifications, claim-parsing helpers, exact warning wording, and component event names. Different choices break no requirement when they preserve the documented predicate, account-validation failures, warning consequences, and request behavior.

The independent check found no further material policy guess in its limited document scope. These are agent findings, not the student's personal review.

</details>

## Reviewer focus
Check course ownership versus course-section assignment, the distinction between new and existing evaluations, week-specific historical rows, and the active-account validator's concurrency boundary.
Review the approved platform Authentication prose change and the profile-edit bypass policy; T1–T13 cover the main flow and E1–E8 without claiming they already pass.
The developer must review and merge this design pull request before implementation; the later build pull request stays open for grading.
