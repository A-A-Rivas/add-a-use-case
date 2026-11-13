# Student deactivation: documentation checks

Scope: all five core requirements documents, traceability, and architecture for mechanical/reference checks; changed STU/EVA policies and their direct references for prose/policy review. This is a documentation pass, not executable feature verification.

- **project-glossary.md formatting:** numbered headings=0; explicit anchors=0; source TOCs=0; existing smart quote characters=0. No unrelated typography rewritten.
- **vision-and-scope.md formatting:** numbered headings=0; explicit anchors=0; source TOCs=0; existing smart quote characters=0. No unrelated typography rewritten.
- **use-cases.md formatting:** numbered headings=0; explicit anchors=0; source TOCs=0; existing smart quote characters=0. No unrelated typography rewritten.
- **business-rules.md formatting:** numbered headings=0; explicit anchors=0; source TOCs=0; existing smart quote characters=0. No unrelated typography rewritten.
- **software-requirements-specification.md formatting:** numbered headings=0; explicit anchors=0; source TOCs=0; existing smart quote characters=0. No unrelated typography rewritten.

## Deterministic checks

- **ID resolution:** 305 unique requirement/design IDs inventoried; no unresolved concrete IDs after excluding the `DE-like` naming example. Repeated KD IDs in architecture constraint/component tables are citations, not duplicate definitions; no core requirement definition collision found.
- **UC trace coverage:** 98 use cases and 98 rows; missing=[]; orphan=[]; duplicate rows=[].
- **UC completeness:** 25 active use cases still have empty extensions (the remaining OI-40 list); 2 deferred/tabled use cases excluded. The edited deactivation use case has E1–E8 and all required fields.
- **Links and anchors:** broken=[]; new STU-design links broken=[]. Directory links and fenced skeleton examples were accounted for.
- **Duplicate headings:** ['traceability.md: How to use it']; these are inherited, outside the changed policy scope.
- **Active FR coverage:** missing matrix rows=[]; orphan rows=[]; deferred FRs excluded=['FR-HIS-checkpoint', 'FR-HIS-restore', 'FR-HIS-retention', 'FR-TPL-customize'].
- **Feature binding:** use-case areas without feature=[]; active FRs without feature=[]; active FRs with multiple homes=[].
- **BO coverage:** non-emergent objectives without feature=[]; explicit platform-level exceptions=['BO-RAM-research'].
- **Measurement coverage:** metrics without a matrix row=[]. Business-objective/metric associations were inventoried; evaluating the metrics is outside this work.
- **Business-rule index:** regenerated all 39 rule rows from forward UC Business Rules fields, SRS contexts, and QS matrix citations. All three new rules have forward citations.
- **Quality and deferred-scope checks:** no quality thresholds or deferred scope changed. Qualitative requirements and the full QS-to-attribute semantic binding were not exhaustively reviewed; numeric heuristics alone cannot establish their correctness.

## Scoped judgment and limits

- Reviewed the changed deactivation use case E1–E8, three new business rules, direct EVA/reactivation citations, and STU design. Policies match the developer decisions; business rules are cited from the use case rather than copied. Reactivation is constrained in documentation only.
- Fresh-context questions test found two issues: stale architecture authentication prose and an unconfirmed report URL. Both were corrected and the independent agent rechecked both fixes; confirmed existing report route is `/evaluations/sections/{sectionId}/week/{week}`.
- New design links resolve. No schema change or production implementation is claimed. T1–T13 are planned tests, not passing tests.
- Not verified: executable backend/frontend tests, running-app behavior, mutation testing, personal student review, remote GitHub artifacts, the design merge gate, and a full semantic interpretation of all pre-existing requirements. Numeric quality heuristics, inherited generic ID examples, and pre-existing structural gaps are findings for review rather than changes in this assignment.
