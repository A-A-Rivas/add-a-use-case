package team.projectpulse.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.*;
import team.projectpulse.student.Student;
import team.projectpulse.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** T7/E5: exercise the production RSA encoder/decoder wiring, not only the validator in isolation. */
class ActiveAccountJwtDecoderTest {
    UserRepository users;
    Student account;
    JwtEncoder encoder;
    JwtDecoder decoder;

    @BeforeEach
    void setup() throws Exception {
        // Route-manager dependencies are unused by these two bean factory methods.
        var constructor = SecurityConfiguration.class.getConstructors()[0];
        var configuration = (SecurityConfiguration) constructor.newInstance(new Object[constructor.getParameterCount()]);
        users = mock(UserRepository.class);
        account = new Student();
        account.setEnabled(true);
        when(users.findById(3)).thenReturn(Optional.of(account));
        encoder = configuration.jwtEncoder();
        decoder = configuration.jwtDecoder(new ActiveAccountJwtValidator(users));
    }

    String token(Instant expiration) {
        return encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder().subject("student")
                .issuedAt(Instant.now().minusSeconds(600)).expiresAt(expiration).claim("userId", 3).build())).getTokenValue();
    }

    @Test
    void sameSignedTokenIsRejectedAfterAccountIsDisabled() {
        String token = token(Instant.now().plusSeconds(600));
        assertThat(decoder.decode(token).getSubject()).isEqualTo("student");
        account.setEnabled(false);
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void defaultExpirationValidationStillRejectsSignedExpiredToken() {
        assertThatThrownBy(() -> decoder.decode(token(Instant.now().minusSeconds(120))))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void modifiedSignatureIsRejected() {
        String token = token(Instant.now().plusSeconds(600));
        int signatureStart = token.lastIndexOf('.') + 1;
        String tampered = token.substring(0, signatureStart) +
                (token.charAt(signatureStart) == 'A' ? 'B' : 'A') + token.substring(signatureStart + 1);
        assertThatThrownBy(() -> decoder.decode(tampered)).isInstanceOf(JwtException.class);
    }
}
