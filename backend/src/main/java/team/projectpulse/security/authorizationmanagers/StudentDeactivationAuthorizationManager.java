package team.projectpulse.security.authorizationmanagers;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import team.projectpulse.student.StudentSecurityService;
import java.util.function.Supplier;

@Component
public class StudentDeactivationAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    private final StudentSecurityService studentSecurityService;

    public StudentDeactivationAuthorizationManager(StudentSecurityService studentSecurityService) {
        this.studentSecurityService = studentSecurityService;
    }

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
                                         RequestAuthorizationContext context) {
        Authentication caller = authentication.get();
        Integer studentId = PathVariables.readId(context, "studentId");
        return new AuthorizationDecision(caller != null && caller.isAuthenticated() && studentId != null
                && this.studentSecurityService.canDeactivateStudent(studentId));
    }
}
