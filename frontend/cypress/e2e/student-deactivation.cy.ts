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

describe('Dedicated student deactivation', () => {
  let enabled: boolean
  beforeEach(() => {
    enabled = true
    cy.intercept('POST', '**/students/search*', (req) =>
      req.reply(
        result({ content: [{ ...student, enabled }], number: 0, size: 50, totalElements: 1 })
      )
    ).as('students')
    cy.intercept('GET', '**/students/4', (req) => req.reply(result({ ...student, enabled }))).as(
      'details'
    )
    cy.intercept('PUT', '**/students/4', { body: result(student) }).as('profile')
    cy.intercept('PUT', '**/students/4/deactivation', (req) => {
      enabled = false
      req.reply({ delay: 500, body: result({ ...student, enabled: false }) })
    }).as('deactivate')
  })

  function openWarning() {
    visitAs('/students')
    cy.wait('@students')
    cy.get('[data-cy=student-edit]').click()
    cy.wait('@details')
    cy.get('[data-cy=student-deactivate]').click()
    cy.get('.student-deactivation-warning').should('be.visible')
  }

  it('warns by persisted name, confirms once, sends no profile edits, and refreshes status (T1,T12)', () => {
    visitAs('/students')
    cy.wait('@students')
    cy.get('[data-cy=student-edit]').click()
    cy.wait('@details')
    cy.get('.el-dialog .el-form-item')
      .contains('First Name:')
      .parent()
      .find('input')
      .clear()
      .type('Unsaved')
    cy.get('[data-cy=student-active-status]').should('contain', 'Enabled')
    cy.get('[data-cy=student-deactivate]').click()
    cy.get('.student-deactivation-warning')
      .should('contain', 'John Smith')
      .and('contain', 'Access, submissions, and reminders will stop')
      .and('contain', 'membership and previously submitted work will remain')
      .and('contain', 'no longer be able to submit new evaluations')
    cy.screenshot('student-deactivation-confirmation')
    cy.get('.student-deactivation-warning').contains('button', 'Confirm').click()
    cy.get('[data-cy=student-deactivate]').should('be.disabled')
    cy.wait('@deactivate')
      .its('request.body')
      .should((body) => {
        expect(body === undefined || body === null || body === '').to.equal(true)
      })
    cy.get('@profile.all').should('have.length', 0)
    cy.get('@deactivate.all').should('have.length', 1)
    cy.contains('John Smith has been deactivated').should('be.visible')
    cy.get('.el-table').should('contain', 'Disabled').and('contain', 'John')
  })

  for (const dismissal of ['Cancel', 'Escape', 'close']) {
    it(`sends nothing and changes no status on ${dismissal} (E1/T3)`, () => {
      openWarning()
      if (dismissal === 'Cancel')
        cy.get('.student-deactivation-warning').contains('button', 'Cancel').click()
      else if (dismissal === 'Escape') cy.get('body').type('{esc}')
      else cy.get('.student-deactivation-warning .el-message-box__headerbtn').click()
      cy.get('.student-deactivation-warning').should('not.exist')
      cy.get('@deactivate.all').should('have.length', 0)
      cy.get('@profile.all').should('have.length', 0)
      cy.get('[data-cy=student-active-status]').should('contain', 'Enabled')
    })
  }

  it('keeps status honest after an error and permits retry (E8/T13)', () => {
    cy.intercept('PUT', '**/students/4/deactivation', {
      statusCode: 500,
      body: { flag: false, code: 500, message: 'Persistence failed' }
    }).as('failed')
    openWarning()
    cy.get('.student-deactivation-warning').contains('button', 'Confirm').click()
    cy.wait('@failed')
    cy.get('[data-cy=student-active-status]').should('contain', 'Enabled')
    cy.get('[data-cy=student-deactivate]').should('be.enabled')
    cy.contains('John Smith has been deactivated').should('not.exist')
  })

  it('reconciles a lost success response by retrying the idempotent action (E2,E8/T13)', () => {
    let first = true
    cy.intercept('PUT', '**/students/4/deactivation', (req) => {
      enabled = false
      if (first) {
        first = false
        // The server committed, but a gateway did not deliver its success response.
        // Destroying the socket lets Chromium automatically retry this idempotent PUT,
        // which bypasses the explicit user-retry path this test needs to exercise.
        req.reply({
          statusCode: 504,
          body: { flag: false, code: 504, message: 'The response timed out after commit' }
        })
      } else req.reply(result({ ...student, enabled: false }))
    }).as('lostResponse')
    openWarning()
    cy.get('.student-deactivation-warning').contains('button', 'Confirm').click()
    cy.wait('@lostResponse')
    cy.contains('John Smith has been deactivated').should('not.exist')
    cy.get('[data-cy=student-deactivate]').should('be.enabled').click()
    cy.get('.student-deactivation-warning').contains('button', 'Confirm').click()
    cy.wait('@lostResponse')
    cy.get('@lostResponse.all').should('have.length', 2)
    cy.get('.el-table').should('contain', 'Disabled')
  })
})
