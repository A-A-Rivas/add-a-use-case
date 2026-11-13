package team.projectpulse.student;

import jakarta.persistence.EntityManager;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.user.UserRepository;
import team.projectpulse.evaluation.EvaluationService;
import team.projectpulse.evaluation.PeerEvaluation;
import team.projectpulse.evaluation.PeerEvaluationRepository;
import team.projectpulse.notification.SubmissionStatusService;
import team.projectpulse.notification.SubmittableItem;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** UC-STU-deactivate-student: real signed tokens, real scoped query and persisted records (T1-T8,T12). */
class StudentDeactivationIntegrationTest extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired StudentRepository students;
    @Autowired EntityManager em;
    @Autowired JwtEncoder encoder;
    @Autowired EvaluationService evaluations;
    @Autowired PeerEvaluationRepository evaluationRepository;
    @Autowired SubmissionStatusService submissionStatus;
    Student john;
    String admin;

    String login(String email) throws Exception {
        String body = mvc.perform(post("/api/v1/users/login").with(httpBasic(email, "123456")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + new JSONObject(body).getJSONObject("data").getString("token");
    }

    @BeforeEach
    void prepare() throws Exception {
        john = (Student) users.findByEmail("j.smith@abc.edu").orElseThrow();
        admin = login("b.wei@abc.edu");
    }

    String action(Integer id) { return "/api/v1/students/" + id + "/deactivation"; }

    List<?> workSnapshot() {
        return List.of(
                em.createQuery("select a.activityId, a.student.id, a.team.teamId, a.week, a.activity, a.description, a.status, a.comments from Activity a order by a.activityId").getResultList(),
                em.createQuery("select e.peerEvaluationId, e.evaluator.id, e.evaluatee.id, e.week, e.totalScore, e.publicComment, e.privateComment from PeerEvaluation e order by e.peerEvaluationId").getResultList(),
                em.createQuery("select r.ratingId, r.peerEvaluation.peerEvaluationId, r.criterion.criterionId, r.actualScore from Rating r order by r.ratingId").getResultList());
    }

    @Test
    void dedicatedActionPreservesMembershipAndWorkAndRetrySucceeds() throws Exception {
        var before = workSnapshot();
        Integer id = john.getId(), team = john.getTeam().getTeamId(), section = john.getSection().getSectionId();
        for (int retry = 0; retry < 2; retry++) {
            mvc.perform(put(action(id)).header("Authorization", admin))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.flag").value(true))
                    .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.enabled").value(false));
        }
        em.flush();
        em.clear();
        var stored = students.findById(id).orElseThrow();
        assertThat(stored.isEnabled()).isFalse();
        assertThat(stored.getTeam().getTeamId()).isEqualTo(team);
        assertThat(stored.getSection().getSectionId()).isEqualTo(section);
        assertThat(workSnapshot()).usingRecursiveComparison().isEqualTo(before);
    }

    @Test
    void ownerWithoutSectionAssignmentCanReadAndDeactivateDespiteDifferentDefault() throws Exception {
        Instructor owner = (Instructor) users.findByEmail("b.wei@abc.edu").orElseThrow();
        john.getSection().getInstructors().remove(owner);
        owner.setDefaultSection(null);
        em.flush();
        assertThat(students.findDeactivationTarget(john.getId(), owner.getId())).isPresent();
        mvc.perform(get("/api/v1/students/" + john.getId()).header("Authorization", admin))
                .andExpect(status().isOk());
        mvc.perform(put(action(john.getId())).header("Authorization", admin)).andExpect(status().isOk());
    }

    @Test
    void assignedInstructorSucceedsWithoutDefaultSectionAndTeamlessStudentSucceeds() throws Exception {
        Instructor instructor = (Instructor) users.findByEmail("b.gates@abc.edu").orElseThrow();
        john.getSection().addInstructor(instructor);
        instructor.setDefaultSection(null);
        john.setTeam(null);
        em.flush();
        String token = login(instructor.getEmail());
        mvc.perform(put(action(john.getId())).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.teamId").isEmpty());
    }

    @Test
    void unauthorizedAndInvalidTargetsAreDeniedWithoutWrites() throws Exception {
        String student = login("j.smith@abc.edu");
        String outsider = login("t.cook@abc.edu");
        Instructor unrelated = (Instructor) users.findByEmail("b.gates@abc.edu").orElseThrow();
        john.getSection().getInstructors().remove(unrelated);
        em.flush();
        String instructor = login(unrelated.getEmail());
        for (String token : List.of(student, outsider, instructor)) {
            mvc.perform(put(action(john.getId())).header("Authorization", token))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.flag").value(false));
        }
        mvc.perform(put(action(john.getId()))).andExpect(status().isUnauthorized());
        for (String id : List.of("999999", "bad")) {
            mvc.perform(put("/api/v1/students/" + id + "/deactivation").header("Authorization", admin))
                    .andExpect(status().isForbidden());
        }
        assertThat(john.isEnabled()).isTrue();
    }

    @Test
    void tokenIssuedBeforeDeactivationFailsProtectedReadWriteAndFreshLogin() throws Exception {
        String token = login("j.smith@abc.edu");
        mvc.perform(get("/api/v1/students/" + john.getId()).header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(put(action(john.getId())).header("Authorization", admin)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/students/" + john.getId()).header("Authorization", token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.flag").value(false));
        mvc.perform(post("/api/v1/evaluations").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/users/login").with(httpBasic("j.smith@abc.edu", "123456")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/students/" + john.getId()).header("Authorization", admin)).andExpect(status().isOk());
    }

    String signed(Object id, Instant expires) {
        var claims = JwtClaimsSet.builder().subject("signed-test").issuedAt(Instant.now().minusSeconds(300))
                .expiresAt(expires).claim("authorities", "ROLE_instructor");
        if (id != null) claims.claim("userId", id);
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
    }

    @Test
    void invalidIdentityDisabledInstructorAndExpiredTokenAreRejected() throws Exception {
        for (Object identity : new Object[]{null, "bad", 999999, 2147483648L}) {
            mvc.perform(get("/api/v1/students/" + john.getId()).header("Authorization", signed(identity, Instant.now().plusSeconds(600))))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/v1/students/" + john.getId()).header("Authorization", signed(john.getId(), Instant.now().minusSeconds(120))))
                .andExpect(status().isUnauthorized());
        Instructor owner = (Instructor) users.findByEmail("b.wei@abc.edu").orElseThrow();
        owner.setEnabled(false);
        em.flush();
        mvc.perform(get("/api/v1/students/" + john.getId()).header("Authorization", admin)).andExpect(status().isUnauthorized());
    }

    @Test
    void mixedProfileUpdateCannotDisableOrChangeProfile() throws Exception {
        String name = john.getFirstName();
        String json = new JSONObject().put("username", john.getUsername()).put("firstName", "Changed")
                .put("lastName", john.getLastName()).put("email", john.getEmail()).put("enabled", false).toString();
        mvc.perform(put("/api/v1/students/" + john.getId()).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Use the dedicated Deactivate action to deactivate a student."));
        assertThat(john.isEnabled()).isTrue();
        assertThat(john.getFirstName()).isEqualTo(name);
    }

    @Test
    void historicalRowsAndAuthoredCommentsSurviveButNewEvaluationsAndObligationsStop() throws Exception {
        Student teammate = john.getTeam().getStudents().stream().filter(s -> !s.getId().equals(john.getId()))
                .findFirst().orElseThrow();
        String week = "2023-W32"; // Previous week under the dev clock defined by the architecture.
        var received = evaluationRepository.save(new PeerEvaluation(week, teammate, john, List.of(), "history-public", "history-private"));
        evaluationRepository.save(new PeerEvaluation(week, john, teammate, List.of(), "authored-public", "authored-private"));
        mvc.perform(put(action(john.getId())).header("Authorization", admin)).andExpect(status().isOk());
        assertThatThrownBy(() -> evaluations.addPeerEvaluation(new PeerEvaluation(week, teammate, john, List.of(), "new", "new")))
                .isInstanceOf(team.projectpulse.system.exception.PeerEvaluationIllegalArgumentException.class).hasMessageContaining("inactive student");
        var updated = evaluations.updatePeerEvaluation(received.getPeerEvaluationId(),
                new PeerEvaluation(week, teammate, john, List.of(), "edited-history", "history-private"));
        assertThat(updated.getPublicComment()).isEqualTo("edited-history");
        // Membership can change independently after deactivation without erasing the recorded row.
        john.setTeam(null);
        em.flush();
        var history = evaluations.generateWeeklyPeerEvaluationReportForSection(john.getSection().getSectionId(), week)
                .getPeerEvaluationAverages().stream().filter(a -> a.getStudentId().equals(john.getId())).findFirst().orElseThrow();
        assertThat(history.getPublicComments()).contains("edited-history");
        assertThat(history.getPrivateComments()).contains("history-private");
        assertThat(evaluations.getPeerEvaluationAverage(week, teammate).getPublicComments()).contains("authored-public");
        assertThat(evaluations.generateWeeklyPeerEvaluationSummaryForStudent(john.getId(), week).getPrivateComments()).isNull();
        for (SubmittableItem item : SubmittableItem.values()) {
            assertThat(submissionStatus.findOwing(john.getSection().getSectionId(), item, week))
                    .extracting(Student::getId).doesNotContain(john.getId());
        }
        assertThat(evaluations.generateWeeklyPeerEvaluationReportForSection(john.getSection().getSectionId(), "2099-W01")
                .getPeerEvaluationAverages()).extracting(a -> a.getStudentId()).doesNotContain(john.getId());
    }
}
