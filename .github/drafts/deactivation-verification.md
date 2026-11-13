# UC-STU-deactivate-student build verification

Implementation follows merged design PR #2. This is agent verification, not the developer's personal review or a fresh-context review.

## Results

- Targeted trunk run: 47 tests passed before commit `6e9cbcb`.
- Full backend rerun from the developer's terminal: **480 tests passed, zero failures/errors/skips**, completed 2026-11-13 00:17:30 America/Chicago. The local Surefire XML reports independently total 480/0/0/0, including eight passing `StudentDeactivationIntegrationTest` methods. The earlier sandbox run had 26 Docker initialization errors; those were environmental and are superseded by this successful rerun.
- Final targeted run after the mutation experiment and scoped details-read adjustment: **60 tests passed, zero failures/errors/skips**. Java 23 and Mockito 5.20.0's explicit Java agent were used because Java 26's automatic attachment did not complete reliably. No Java version or test dependency was changed in the project.
- After the developer opened Docker Desktop, the Linux-engine pipe became present but this session received `permission denied` when connecting. An empty Docker Desktop container list is not an error; Testcontainers creates containers during the test run. The developer subsequently ran the full backend suite successfully from a terminal that could access the engine.
- `npm run build-only`: production bundle built. `npm run build` failed because `vue-tsc` reported errors in unchanged SearchPeriod, SearchWeek, TopNavigationBar, activity/evaluation admin charts, RamDocumentEditor, Teams, and UserRegistration files. No diagnostic names a changed deactivation file. Those unrelated files were not edited.
- ESLint passed for changed frontend source and both new Cypress specs.
- Cypress rerun from the developer's terminal: **all 8 tests passed**, zero failing/pending/skipped (six dedicated-action tests and two evaluation-row tests). The earlier socket-destruction fixture permitted Chromium to retry the PUT automatically; modeling a gateway 504 after the mocked server commits makes the manual-retry oracle deterministic. Production code was unchanged. The successful main-flow test produced [the confirmation screenshot](student-deactivation-confirmation.png), which was visually inspected. Browser HTTP is stubbed; live backend + browser verification is not claimed.
- Direct Git push failed DNS resolution. The connected GitHub source-write API returned `403 Resource not accessible by integration`. Local `.git` is read-only to this session. Source commits and push therefore require the prepared local script; no build PR exists yet.

## Design test list

The full 480-test backend suite and all eight Cypress tests passed. Partial verification identifies a remaining untested oracle or live interaction; a passing suite does not prove assertions that were never written. Browser tests use stubbed HTTP.

| Row | Passing evidence | Remaining verification |
|---|---|---|
| T1 main flow | MVC DTO contract and integration `dedicatedActionPreservesMembershipAndWorkAndRetrySucceeds` | Stubbed browser main-flow test and inspected screenshot also pass; live browser/backend run not performed |
| T2 retained records/teamless | Service preservation/teamless tests; integration work snapshot verifies IDs, membership, activities, evaluations, ratings and comments; teamless HTTP success | None for those persisted assertions |
| T3 E1 | Cypress Cancel, Escape, close and zero-request assertions passed | None for the specified dismissal paths |
| T4 E2 | Unit retry verifies one save; integration repeats HTTP action successfully with records unchanged | Concurrent callers not exercised |
| T5 E3 | Guard unit tests; integration owner/assigned-instructor success without default assignment, outsider/student/unrelated instructor 403 and anonymous 401 | Non-null different default course section not separately exercised |
| T6 E4 | Bound-id and scoped-finder units, MVC post-gate 404; integration real JPQL and nonexistent/malformed route refusals | Actual concurrent deletion/scope-change race not exercised |
| T7 E5 | Validator and RSA decoder tests; integration pre-issued token rejected on protected read/write after disable, fresh login refused, active token works, disabled instructor/invalid identity/expiry refused | Separate persistence-context or concurrent commit timing not exercised |
| T8 E6 new evaluation | Active/inactive unit cases; integration rejects new evaluation of persisted disabled student for pre-deactivation week | Browser new-row filtering passed; inactive-target POST and stale-page rejection not exercised end to end |
| T9 E6 existing evaluation | Open/closed-window units; integration edits saved evaluation of disabled student within window | Existing browser-row accessibility passed with stubbed HTTP |
| T10 E7 history | Unit week-specific selection, scores/comments; integration saved teamless history, authored contributions, private-summary withholding and empty-week exclusion | No additional browser verification |
| T11 E7 obligations | Submission-status units and integration exclude disabled student from both obligations while preserving history | Live delivery of a deactivation-specific reminder scenario not tested |
| T12 dedicated action | Mixed-profile unit and HTTP rejection; unchanged-status updates and retained reactivation unit pass | Dedicated browser control and unsaved-profile isolation passed with stubbed HTTP |
| T13 E8 | Save-failure propagation unit, browser failed-request honesty and explicit retry after a mocked committed-but-timed-out response pass | Transaction rollback and reload reconciliation are not verified |

## Tests and their oracles

- `StudentDeactivationServiceTest.deactivationRetainsIdentityAndMembershipAndRetryDoesNotSave`: POST-2, E2, BR-student-deactivation-membership (T2/T4).
- `teamlessStudentCanBeDeactivated`: BR-student-deactivation-membership (T2).
- `missingOrOutOfScopeTargetIsNotFoundWithoutWrite`: E4 and the scoped service API contract (T6).
- `instructorDetailsReadUsesScopedFinder`, `detailsReadRefusesTargetOutsideCallerScope`: main step 2, E4, and the architecture's scoped persistence convention (T1/T6).
- `mixedProfileAndDeactivationRequestChangesNothing`: Associated Information's dedicated action and unchanged mixed-request fields (T12).
- `existingReactivationThroughProfileEditingStillWorks`: design's explicit retained reactivation behavior (T12).
- `persistenceFailurePropagatesWithoutSuccessfulResult`: E8 (T13); intentionally not a rollback oracle.
- `StudentDeactivationSecurityTest.instructorWithScopedTargetIsAllowed`: E3, BR-section-scoped-access (T5).
- `courseOwnerWithAdminRoleIsAllowed`: BR-student-lifecycle (T5).
- `unrelatedInstructorOrMissingTargetIsDenied`: E3/E4 and fail-closed route contract (T5/T6).
- `studentCannotDeactivateEvenIfTargetWouldBeInScope`, `missingIdentityOrTargetIsDenied`: E3/E4, FR-SEC-authorization (T5/T6).
- `StudentDeactivationAuthorizationManagerTest.delegatesOnlyForAuthenticatedCallerAndValidBoundId`: FR-SEC-deny-unauthorized and the architecture's bound path-variable convention (T5/T6).
- `StudentDeactivationControllerTest.actionReturnsDisabledDtoWithoutExposingPassword`: main step 6, Result/StudentDto contract (T1).
- `postGateMissingTargetMapsTo404Envelope`: E4 and the design's 404 race contract (T6).
- `ActiveAccountJwtValidatorTest.observesCommittedStatusOnEveryValidation`: FR-SEC-active-account (T7); “committed” here is simulated via the repository, not proved as a database commit.
- `rejectsDeletedAccountAndDisabledInstructor`, `rejectsMalformedIdentity`, `rejectsMissingAndFractionalIdentity`: E5 and the validator API contract (T7).
- `accountStoreFailureNeverGrantsAccess`: fail-closed design decision (T7).
- `ActiveAccountJwtDecoderTest.sameSignedTokenIsRejectedAfterAccountIsDisabled`: FR-SEC-active-account (T7).
- `defaultExpirationValidationStillRejectsSignedExpiredToken`, `modifiedSignatureIsRejected`: preserved authentication guarantees (T7).
- `EvaluationServiceTest.testNewEvaluationOfInactiveStudentIsRejected`: E6, BR-evaluation-active-evaluatee (T8).
- `testExistingEvaluationOfInactiveStudentRemainsEditableWithinWindow`, `testExistingEvaluationOfInactiveStudentIsReadOnlyAfterWindowCloses`: BR-evaluation-editable-until-close and BR-evaluation-submission-window (T9).
- `testWeeklyReportSelectsDisabledHistoryOnlyForRequestedWeek`: E7, BR-evaluation-deactivated-history (T10).
- Reused `SubmissionStatusServiceTest` tests derive their expected eligible roster and completeness from BR-submission-owed (T11).
- Existing self/instructor profile tests now submit unchanged `enabled=true`; expecting ordinary editing to disable an account was superseded by the approved dedicated-action contract. Their profile-field assertions remain.
- The history test's exact-argument mock was replaced with a fixture-based answer after mutation testing exposed a Mockito setup error before the requirement assertion. No production expectation was relaxed. No test was deleted.
- All eight `StudentDeactivationIntegrationTest` methods and all eight Cypress tests passed; their T/E mappings are in source and above. Browser fixtures simulate HTTP and do not replace live browser/backend testing.

## Mutation experiment

The agent temporarily replaced the weekly report eligibility predicate with `.filter(student -> student.getTeam() != null)` in `EvaluationService`.
The first attempt stopped on Mockito's argument-mismatch error, rather than the oracle. The fixture setup was corrected and the experiment repeated.
`EvaluationServiceTest#testWeeklyReportSelectsDisabledHistoryOnlyForRequestedWeek` then failed its expected-student-ID assertion: historical student 3 was missing and empty disabled student 4 appeared. Production code was restored in a `finally` block, then the targeted classes reran.
This was an agent experiment; it is not represented as the developer's personal test review.

## Design adjustments and follow-ups

- The existing student-details GET route allowed only self/assigned instructors. Its rule now composes the existing manager with the dedicated lifecycle manager, letting a course owner reach main step 2 without changing unrelated report/profile route permissions. The service uses the scoped finder for instructors/owners and binds self-service to caller identity; two additional service tests prove the scoped success/refusal paths. `stu.md` records the changed API row and rationale.
- `architectural-design.md` now describes the implemented account validator and passing backend verification, without claiming browser coverage.
- No migration or seed changes. The new action has no request body, uses `UserUtils` for caller scope, and uses the same scoped finder at route and service. Frontend calls use the shared request client. No new calendar/elapsed-time reads were introduced.
- Existing profile-update loads and evaluation DTO conversion still have legacy primary-key lookups. This feature adds no request-body IDs and retains the design's existing team/window checks; broader scoped-finder cleanup is a separate follow-up, not silently included here.
- No new policy/spec requirement was invented. Mechanical UC-row/FR-reference/local-link checks are scoped verification; a full semantic `/spec-build` sweep is not claimed.
- The developer's personal review, labeled diff comment, and fresh-context build review remain to be completed before submission.

## Reproduce

Use Docker Desktop with its Linux engine running. The project's documented backend command is `cd backend; .\mvnw.cmd test` (Java 21-compatible runtime). If Mockito requires an explicit agent under the installed Java runtime, pass `-DargLine=-javaagent:<path-to-mockito-core-5.20.0.jar>` as in this session.
For frontend verification run `npm ci`, `npm run build`, and, against `npm run dev`, `npx cypress run --spec "cypress/e2e/student-deactivation*.cy.ts"`. The new Cypress specs stub HTTP; also exercise the actual running backend for the main flow. The successful main-flow screenshot is retained beside this report; the revised lost-response test passed in the full eight-test browser rerun.

Mechanical checks: 98 use cases and 98 trace rows; no missing, orphan, or duplicate UC rows; all four cited security FR definitions resolve; changed design/report local links resolve.
