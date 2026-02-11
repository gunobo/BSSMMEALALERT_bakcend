package com.bssm.meal.admin.controller;

import com.bssm.meal.favorite.repository.AdminNotificationRepository;
import com.bssm.meal.favorite.service.MealNotificationService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/notification")
public class AdminNotificationController {

    private final AdminNotificationRepository adminNotificationRepository;
    private final MealNotificationService mealNotificationService;

    // ✅ 생성자 주입 (Lombok 없이 직접 작성하여 의존성 주입 보장)
    public AdminNotificationController(AdminNotificationRepository adminNotificationRepository,
                                       MealNotificationService mealNotificationService) {
        this.adminNotificationRepository = adminNotificationRepository;
        this.mealNotificationService = mealNotificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> createNotification(@RequestBody AdminPushRequest request) {
        com.bssm.meal.favorite.dto.NotificationRequest serviceDto = new com.bssm.meal.favorite.dto.NotificationRequest();
        serviceDto.setTitle(request.getTitle());
        serviceDto.setBody(request.getBody());
        serviceDto.setTargetType(request.getTargetType());
        serviceDto.setTargetEmails(request.getTargetEmails());
        serviceDto.setTargetDate(request.getTargetDate());

        // ✅ 예약 시간이 없으면 현재 시간으로 설정 (DB 포맷에 맞춤)
        if (request.getScheduledTime() == null) {
            serviceDto.setScheduledTime(LocalDateTime.now());
        } else {
            serviceDto.setScheduledTime(request.getScheduledTime());
        }

        mealNotificationService.processAdminNotification(serviceDto);
        return ResponseEntity.ok("알림 요청이 정상적으로 처리되었습니다.");
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(mealNotificationService.getNotificationStats());
    }

    @Getter
    @NoArgsConstructor
    public static class AdminPushRequest {
        private String title;
        private String body;
        private String targetType;
        private List<String> targetEmails;
        private String targetDate;

        @com.fasterxml.jackson.annotation.JsonFormat(
                shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm"
        )
        private java.time.LocalDateTime scheduledTime;
    }
}