package com.campusgig.user.presentation;

import com.campusgig.user.presentation.dto.EmployerResponse;
import com.campusgig.user.presentation.dto.RegisterEmployerRequest;
import com.campusgig.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employers")
public class EmployerController {

    private final UserService userService;

    public EmployerController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployerResponse register(@RequestBody RegisterEmployerRequest request) {
        return EmployerResponse.from(userService.registerEmployer(request.companyName(), request.email()));
    }

    @GetMapping
    public List<EmployerResponse> list() {
        return userService.listEmployers().stream().map(EmployerResponse::from).toList();
    }

    @GetMapping("/{id}")
    public EmployerResponse get(@PathVariable("id") Long id) {
        return EmployerResponse.from(userService.getEmployer(id));
    }
}
