export {}
// UC-STU-deactivate-student T1/T3/T8/T9/T12/T13. HTTP is stubbed; this proves browser behavior,
// not database persistence or HTTP authorization. Integration tests own those oracles.
const student = {
  id: 4,
  username: 'j.smith@abc.edu',
  firstName: 'John',
  lastName: 'Smith',
  email: 'j.smith@abc.edu',
  enabled: true,
  teamId: 1,
  teamName: 'Team1',
  sectionId: 2
}
const result = (data: unknown) => ({ flag: true, code: 200, message: 'Success', data })

function visitAs(path: string, role = 'instructor') {
  cy.visit(path, {
    onBeforeLoad(win) {
      const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }))
      win.localStorage.setItem('token', JSON.stringify({ token: `e30.${payload}.fake` }))
      win.localStorage.setItem(
        'userInfo',
        JSON.stringify({ userInfo: { ...student, roles: role } })
      )
      win.localStorage.setItem(
        'settings',
        JSON.stringify({ defaultSectionId: 2, defaultCourseId: 1 })
      )
    }
  })
}

describe('Evaluation rows after deactivation (T8/T9)', () => {
  const disabled = { ...student, id: 5, firstName: 'Inactive', enabled: false }
  beforeEach(() => {
    cy.intercept('GET', '**/students/teams/1', { body: result([student, disabled]) })
    cy.intercept('GET', '**/sections/2', { body: result({ rubricId: 1 }) })
    cy.intercept('GET', '**/rubrics/1', { body: result({ criteria: [] }) })
  })
  it('excludes disabled teammates from new rows (E6)', () => {
    cy.intercept('GET', '**/evaluations/evaluators/4/week/*', { body: result([]) })
    visitAs('/evaluations/submit-evaluations', 'student')
    cy.get('.el-table').should('contain', 'John Smith').and('not.contain', 'Inactive')
  })
  it('keeps existing evaluations of disabled teammates accessible (E6)', () => {
    cy.intercept('GET', '**/evaluations/evaluators/4/week/*', {
      body: result([
        {
          peerEvaluationId: 1,
          evaluateeId: 5,
          evaluateeName: 'Inactive Smith',
          ratings: [],
          publicComment: 'Saved public comment',
          privateComment: 'Saved private comment'
        }
      ])
    })
    visitAs('/evaluations/submit-evaluations', 'student')
    cy.get('.el-table').should('contain', 'Inactive Smith')
    cy.get('textarea').first().should('have.value', 'Saved public comment')
  })
})
