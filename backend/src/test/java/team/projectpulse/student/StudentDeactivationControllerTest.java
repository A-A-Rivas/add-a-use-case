package team.projectpulse.student;

import org.junit.jupiter.api.Test;
import team.projectpulse.section.Section;
import team.projectpulse.student.converter.StudentToStudentDtoConverter;
import team.projectpulse.system.exception.ExceptionHandlerAdvice;
import team.projectpulse.system.exception.ObjectNotFoundException;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** T1/T6: MVC contract only; full security and persistence belong to StudentDeactivationIntegrationTest. */
class StudentDeactivationControllerTest {
    @Test
    void actionReturnsDisabledDtoWithoutExposingPassword() throws Exception {
        var service = mock(StudentService.class);
        var controller = new StudentController(service, null, new StudentToStudentDtoConverter(), null);
        var student = new Student("student", "John", "Smith", "mail", "secret", false, "student");
        student.setId(3);
        var section = new Section();
        section.setSectionId(2);
        student.setSection(section);
        when(service.deactivateStudent(3)).thenReturn(student);
        standaloneSetup(controller).addPlaceholderValue("api.endpoint.base-url", "/api/v1").build()
                .perform(put("/api/v1/students/3/deactivation"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.enabled").value(false)).andExpect(jsonPath("$.data.password").doesNotExist());
        verify(service).deactivateStudent(3);
    }

    @Test
    void postGateMissingTargetMapsTo404Envelope() throws Exception {
        var service = mock(StudentService.class);
        when(service.deactivateStudent(3)).thenThrow(new ObjectNotFoundException("student", 3));
        var controller = new StudentController(service, null, null, null);
        standaloneSetup(controller).addPlaceholderValue("api.endpoint.base-url", "/api/v1")
                .setControllerAdvice(new ExceptionHandlerAdvice()).build()
                .perform(put("/api/v1/students/3/deactivation"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.flag").value(false));
    }
}
