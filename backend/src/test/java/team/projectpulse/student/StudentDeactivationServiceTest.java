package team.projectpulse.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import team.projectpulse.section.Section;
import team.projectpulse.section.SectionRepository;
import team.projectpulse.system.UserUtils;
import team.projectpulse.system.exception.ObjectNotFoundException;
import team.projectpulse.team.Team;
import team.projectpulse.user.userinvitation.UserInvitationService;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** UC-STU-deactivate-student trunk: T2, T4, T6, T12, T13. */
@ExtendWith(MockitoExtension.class)
class StudentDeactivationServiceTest {
    @Mock StudentRepository studentRepository;
    @Mock SectionRepository sectionRepository;
    @Mock UserInvitationService invitations;
    @Mock PasswordEncoder encoder;
    @Mock UserUtils userUtils;
    @InjectMocks StudentService service;

    @Test
    void deactivationRetainsIdentityAndMembershipAndRetryDoesNotSave() {
        Student student = new Student("student", "First", "Last", "mail", "hash", true, "student");
        student.setId(3);
        Section section = new Section();
        Team team = new Team();
        section.addStudent(student);
        team.addStudent(student);
        when(userUtils.getUserId()).thenReturn(1);
        when(studentRepository.findDeactivationTarget(3, 1)).thenReturn(Optional.of(student));
        when(studentRepository.save(student)).thenReturn(student);
        assertThat(service.deactivateStudent(3)).isSameAs(student);
        assertThat(service.deactivateStudent(3)).isSameAs(student);
        assertThat(student.isEnabled()).isFalse();
        assertThat(student.getSection()).isSameAs(section);
        assertThat(student.getTeam()).isSameAs(team);
        assertThat(student.getId()).isEqualTo(3);
        assertThat(student.getPassword()).isEqualTo("hash");
        verify(studentRepository, times(1)).save(student);
        verify(studentRepository, never()).findById(any());
    }

    @Test
    void teamlessStudentCanBeDeactivated() {
        Student student = new Student();
        student.setEnabled(true);
        when(userUtils.getUserId()).thenReturn(1);
        when(studentRepository.findDeactivationTarget(3, 1)).thenReturn(Optional.of(student));
        when(studentRepository.save(student)).thenReturn(student);
        assertThat(service.deactivateStudent(3).isEnabled()).isFalse();
        assertThat(student.getTeam()).isNull();
    }

    @Test
    void missingOrOutOfScopeTargetIsNotFoundWithoutWrite() {
        when(userUtils.getUserId()).thenReturn(9);
        when(studentRepository.findDeactivationTarget(3, 9)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deactivateStudent(3)).isInstanceOf(ObjectNotFoundException.class);
        verify(studentRepository, never()).save(any());
        verify(studentRepository, never()).findById(any());
    }

    @Test
    void mixedProfileAndDeactivationRequestChangesNothing() {
        Student student = new Student("original", "First", "Last", "mail", "hash", true, "student");
        when(studentRepository.findById(3)).thenReturn(Optional.of(student));
        Student update = new Student("changed", "Changed", "Changed", "changed", "", false, "student");
        assertThatThrownBy(() -> service.updateStudent(3, update))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("dedicated Deactivate");
        assertThat(student.getUsername()).isEqualTo("original");
        assertThat(student.getEmail()).isEqualTo("mail");
        assertThat(student.isEnabled()).isTrue();
        verify(studentRepository, never()).save(any());
    }

    @Test
    void existingReactivationThroughProfileEditingStillWorks() {
        Student student = new Student("original", "First", "Last", "mail", "hash", false, "student");
        Student update = new Student("updated", "First", "Last", "mail", "", true, "student");
        when(studentRepository.findById(3)).thenReturn(Optional.of(student));
        when(userUtils.hasRole("ROLE_instructor")).thenReturn(true);
        when(studentRepository.save(student)).thenReturn(student);
        assertThat(service.updateStudent(3, update).isEnabled()).isTrue();
        assertThat(student.getUsername()).isEqualTo("updated");
    }

    @Test
    void persistenceFailurePropagatesWithoutSuccessfulResult() {
        Student student = new Student();
        student.setEnabled(true);
        when(userUtils.getUserId()).thenReturn(1);
        when(studentRepository.findDeactivationTarget(3, 1)).thenReturn(Optional.of(student));
        when(studentRepository.save(student)).thenThrow(new IllegalStateException("database failure"));
        assertThatThrownBy(() -> service.deactivateStudent(3)).isInstanceOf(IllegalStateException.class);
    }
}
