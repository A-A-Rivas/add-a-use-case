package team.projectpulse.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import team.projectpulse.student.Student;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.user.UserRepository;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** UC-STU-deactivate-student: T7 / E5. */
@ExtendWith(MockitoExtension.class)
class ActiveAccountJwtValidatorTest {
    @Mock UserRepository users;

    private Jwt token(Object identity) {
        var builder = Jwt.withTokenValue("signed-elsewhere").header("alg", "RS256").subject("mutable-name");
        if (identity != null) builder.claim("userId", identity);
        return builder.build();
    }

    @Test
    void observesCommittedStatusOnEveryValidation() {
        Student student = new Student();
        student.setEnabled(true);
        when(users.findById(3)).thenReturn(Optional.of(student));
        var validator = new ActiveAccountJwtValidator(users);
        assertThat(validator.validate(token(3L)).hasErrors()).isFalse();
        student.setEnabled(false);
        assertThat(validator.validate(token(3L)).hasErrors()).isTrue();
        verify(users, times(2)).findById(3);
    }

    @Test
    void rejectsDeletedAccountAndDisabledInstructor() {
        when(users.findById(3)).thenReturn(Optional.empty(), Optional.of(new Instructor()));
        var validator = new ActiveAccountJwtValidator(users);
        assertThat(validator.validate(token("3")).hasErrors()).isTrue();
        assertThat(validator.validate(token(3)).hasErrors()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"bad", "0", "-1", "2147483648", "3.0", ""})
    void rejectsMalformedIdentity(String identity) {
        assertThat(new ActiveAccountJwtValidator(users).validate(token(identity)).hasErrors()).isTrue();
        verifyNoInteractions(users);
    }

    @Test
    void rejectsMissingAndFractionalIdentity() {
        var validator = new ActiveAccountJwtValidator(users);
        assertThat(validator.validate(token(null)).hasErrors()).isTrue();
        assertThat(validator.validate(token(3.5)).hasErrors()).isTrue();
        verifyNoInteractions(users);
    }

    @Test
    void accountStoreFailureNeverGrantsAccess() {
        when(users.findById(3)).thenThrow(new IllegalStateException("unavailable"));
        assertThatThrownBy(() -> new ActiveAccountJwtValidator(users).validate(token(3)))
                .isInstanceOf(IllegalStateException.class);
    }
}
