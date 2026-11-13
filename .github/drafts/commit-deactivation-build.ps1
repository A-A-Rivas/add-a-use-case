$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Set-Location -LiteralPath $repoRoot

function Invoke-Git {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]] $Arguments)
    & git @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Git failed: $($Arguments -join ' ')" }
}

$branch = (& git branch --show-current).Trim()
if ($branch -ne 'feat/deactivate-student') { throw "Expected feat/deactivate-student; found $branch" }
$origin = (& git remote get-url origin).Trim()
if ($origin -notmatch '^https://github\.com/A-A-Rivas/add-a-use-case(?:\.git)?/?$') {
    throw "Refusing to push to an unexpected origin: $origin"
}
& git diff --cached --quiet
if ($LASTEXITCODE -ne 0) { throw 'There are staged changes. Review and unstage those before using this script.' }
Invoke-Git merge-base --is-ancestor 6e9cbcb HEAD
Invoke-Git diff --check

function Commit-Files {
    param([string] $Message, [string[]] $Paths)
    Invoke-Git add -- @Paths
    & git diff --cached --quiet
    if ($LASTEXITCODE -eq 0) { Write-Host "Already committed: $Message"; return }
    if ($LASTEXITCODE -ne 1) { throw 'Unable to inspect staged changes' }
    Invoke-Git commit -m $Message -m 'Refs #1'
}

Commit-Files 'feat(security): authorize scoped student deactivation' @(
    'backend/src/main/java/team/projectpulse/security/SecurityConfiguration.java',
    'backend/src/main/java/team/projectpulse/student/StudentSecurityService.java',
    'backend/src/main/java/team/projectpulse/student/StudentService.java',
    'backend/src/test/java/team/projectpulse/student/StudentServiceTest.java',
    'backend/src/test/java/team/projectpulse/student/StudentDeactivationServiceTest.java',
    'backend/src/main/java/team/projectpulse/security/authorizationmanagers/StudentDeactivationAuthorizationManager.java',
    'backend/src/test/java/team/projectpulse/security/authorizationmanagers/StudentDeactivationAuthorizationManagerTest.java',
    'backend/src/test/java/team/projectpulse/student/StudentDeactivationSecurityTest.java',
    'backend/src/test/java/team/projectpulse/security/ActiveAccountJwtDecoderTest.java'
)
Commit-Files 'feat(stu): expose dedicated deactivation endpoint' @(
    'backend/src/main/java/team/projectpulse/student/StudentController.java',
    'backend/src/test/java/team/projectpulse/student/StudentDeactivationControllerTest.java',
    'backend/src/test/java/team/projectpulse/student/StudentDeactivationIntegrationTest.java',
    'backend/src/test/java/team/projectpulse/evaluation/EvaluationServiceTest.java'
)
Commit-Files 'feat(stu): confirm deactivation without saving profile edits' @(
    'frontend/src/apis/student/index.ts',
    'frontend/src/pages/students/EditStudentForm.vue',
    'frontend/src/pages/students/Students.vue',
    'frontend/cypress/e2e/student-deactivation.cy.ts'
)
Commit-Files 'fix(evaluation): exclude inactive students from new browser rows' @(
    'frontend/src/pages/evaluations/SubmitTeamsEvaluations.vue',
    'frontend/cypress/e2e/student-deactivation-evaluations.cy.ts'
)
Commit-Files 'docs(stu): trace implementation and verification limits' @(
    'docs/traceability.md',
    'docs/design/stu.md',
    'docs/design/architectural-design.md',
    '.github/drafts/deactivation-verification.md',
    '.github/drafts/student-deactivation-confirmation.png',
    '.github/drafts/deactivate-student-build-pr.md',
    '.github/drafts/commit-deactivation-build.ps1'
)

Invoke-Git push -u origin feat/deactivate-student
Write-Host 'Pushed only to A-A-Rivas/add-a-use-case. The build PR must remain open and unmerged.'
