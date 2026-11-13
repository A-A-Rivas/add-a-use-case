package team.projectpulse.security.authorizationmanagers;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import team.projectpulse.student.StudentSecurityService;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentDeactivationAuthorizationManagerTest {
    @Test
    void delegatesOnlyForAuthenticatedCallerAndValidBoundId() {
        var service = mock(StudentSecurityService.class);
        var manager = new StudentDeactivationAuthorizationManager(service);
        var caller = new UsernamePasswordAuthenticationToken("instructor", null,
                List.of(new SimpleGrantedAuthority("ROLE_instructor")));
        when(service.canDeactivateStudent(3)).thenReturn(true);
        var valid = new RequestAuthorizationContext(new MockHttpServletRequest(), Map.of("studentId", "3"));
        assertThat(manager.authorize(() -> caller, valid).isGranted()).isTrue();
        assertThat(manager.authorize(() -> null, valid).isGranted()).isFalse();
        for (String id : List.of("bad", "2147483648", "")) {
            var invalid = new RequestAuthorizationContext(new MockHttpServletRequest(), Map.of("studentId", id));
            assertThat(manager.authorize(() -> caller, invalid).isGranted()).isFalse();
        }
        assertThat(manager.authorize(() -> caller,
                new RequestAuthorizationContext(new MockHttpServletRequest())).isGranted()).isFalse();
        verify(service, times(1)).canDeactivateStudent(3);
    }
}
