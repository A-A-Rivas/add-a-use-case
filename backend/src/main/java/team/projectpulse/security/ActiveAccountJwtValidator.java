package team.projectpulse.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;

/** Live account status, checked before the authenticated caller exists. */
@Component
public class ActiveAccountJwtValidator implements OAuth2TokenValidator<Jwt> {
    private final UserRepository userRepository;

    public ActiveAccountJwtValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Object identity = jwt.getClaims().get("userId");
        Integer userId;
        try {
            if (!(identity instanceof Integer || identity instanceof Long || identity instanceof String)) {
                return denied();
            }
            userId = Integer.valueOf(identity.toString());
            if (userId <= 0) {
                return denied();
            }
        } catch (NumberFormatException exception) {
            return denied();
        }
        // Database failures deliberately propagate: an unavailable account store must not grant access.
        return this.userRepository.findById(userId).filter(PeerEvaluationUser::isEnabled).isPresent()
                ? OAuth2TokenValidatorResult.success() : denied();
    }

    private OAuth2TokenValidatorResult denied() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Account is not active.", null));
    }
}
