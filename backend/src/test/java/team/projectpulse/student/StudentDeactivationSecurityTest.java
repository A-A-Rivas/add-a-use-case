package team.projectpulse.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.projectpulse.system.UserUtils;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** UC-STU-deactivate-student T5/T6: E3/E4. Scope never depends on the default course section. */
@ExtendWith(MockitoExtension.class)
class StudentDeactivationSecurityTest {
    @Mock StudentRepository repository;
    @Mock UserUtils caller;
    @InjectMocks StudentSecurityService security;

    @Test
    void instructorWithScopedTargetIsAllowed() {
        when(caller.getUserId()).thenReturn(1);
        when(caller.hasRole("ROLE_instructor")).thenReturn(true);
        when(repository.findDeactivationTarget(3, 1)).thenReturn(Optional.of(new Student()));
        assertThat(security.canDeactivateStudent(3)).isTrue();
        verify(caller, never()).getUserSectionId();
    }

    @Test
    void courseOwnerWithAdminRoleIsAllowed() {
        when(caller.getUserId()).thenReturn(1);
        doReturn(false).when(caller).hasRole("ROLE_instructor");
        doReturn(true).when(caller).hasRole("ROLE_admin");
        when(repository.findDeactivationTarget(3, 1)).thenReturn(Optional.of(new Student()));
        assertThat(security.canDeactivateStudent(3)).isTrue();
    }

    @Test
    void unrelatedInstructorOrMissingTargetIsDenied() {
        when(caller.getUserId()).thenReturn(1);
        when(caller.hasRole("ROLE_instructor")).thenReturn(true);
        when(repository.findDeactivationTarget(3, 1)).thenReturn(Optional.empty());
        assertThat(security.canDeactivateStudent(3)).isFalse();
    }

    @Test
    void studentCannotDeactivateEvenIfTargetWouldBeInScope() {
        when(caller.getUserId()).thenReturn(3);
        assertThat(security.canDeactivateStudent(3)).isFalse();
        verifyNoInteractions(repository);
    }

    @Test
    void missingIdentityOrTargetIsDenied() {
        assertThat(security.canDeactivateStudent(null)).isFalse();
        assertThat(security.canDeactivateStudent(3)).isFalse();
        verifyNoInteractions(repository);
    }
}
