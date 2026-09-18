package com.campusgig.user.presentation;

import com.campusgig.user.domain.Availability;
import com.campusgig.user.domain.EmailAddress;
import com.campusgig.user.domain.Student;
import com.campusgig.user.service.NotFoundException;
import com.campusgig.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private Student alex() {
        return new Student("Alex Nguyen", new EmailAddress("alex@uowmail.edu.au"),
                Set.of("barista"), Availability.WEEKENDS);
    }

    @Test
    void returnsAStudent() throws Exception {
        when(userService.getStudent(1L)).thenReturn(alex());

        mockMvc.perform(get("/api/students/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alex Nguyen"))
                .andExpect(jsonPath("$.email").value("alex@uowmail.edu.au"));
    }

    @Test
    void unknownStudentReturns404() throws Exception {
        when(userService.getStudent(99L)).thenThrow(new NotFoundException("Student 99 not found"));

        mockMvc.perform(get("/api/students/99")).andExpect(status().isNotFound());
    }

    @Test
    void registeringAStudentReturns201() throws Exception {
        when(userService.registerStudent(anyString(), anyString(), any(), any())).thenReturn(alex());

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alex Nguyen","email":"alex@uowmail.edu.au",
                                 "skills":["barista"],"availability":"WEEKENDS"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        when(userService.registerStudent(anyString(), eq("taken@uowmail.edu.au"), any(), any()))
                .thenThrow(new IllegalStateException("A student with this email already exists"));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alex","email":"taken@uowmail.edu.au","skills":[],"availability":"FLEXIBLE"}
                                """))
                .andExpect(status().isConflict());
    }
}
