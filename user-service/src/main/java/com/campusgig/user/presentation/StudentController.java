package com.campusgig.user.presentation;

import com.campusgig.user.presentation.dto.RegisterStudentRequest;
import com.campusgig.user.presentation.dto.StudentResponse;
import com.campusgig.user.presentation.dto.UpdateProfileRequest;
import com.campusgig.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final UserService userService;

    public StudentController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse register(@RequestBody RegisterStudentRequest request) {
        return StudentResponse.from(userService.registerStudent(
                request.name(), request.email(), request.skills(), request.availability()));
    }

    @GetMapping("/{id}")
    public StudentResponse get(@PathVariable("id") Long id) {
        return StudentResponse.from(userService.getStudent(id));
    }

    @GetMapping
    public List<StudentResponse> list() {
        return userService.listStudents().stream().map(StudentResponse::from).toList();
    }

    @PutMapping("/{id}/profile")
    public StudentResponse updateProfile(@PathVariable("id") Long id, @RequestBody UpdateProfileRequest request) {
        return StudentResponse.from(userService.updateStudentProfile(id, request.skills(), request.availability()));
    }
}
