package com.campusgig.matching.presentation;

import com.campusgig.matching.presentation.dto.NotificationResponse;
import com.campusgig.matching.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/students/{studentId}")
    public List<NotificationResponse> forStudent(@PathVariable("studentId") Long studentId) {
        return notificationService.forStudent(studentId).stream().map(NotificationResponse::from).toList();
    }
}
