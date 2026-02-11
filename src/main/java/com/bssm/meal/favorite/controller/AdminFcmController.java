package com.bssm.meal.favorite.controller;

import com.bssm.meal.favorite.dto.NotificationRequest;
import com.bssm.meal.favorite.dto.PushRequestDto;
import com.bssm.meal.favorite.service.MealNotificationService;
import com.bssm.meal.favorite.service.NotificationService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/fcm")
@RequiredArgsConstructor
public class AdminFcmController {

    private final MealNotificationService mealNotificationService;

    /**
     * 관리자 푸시 알림 발송 (즉시 발송 및 예약 발송 통합)
     */
    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody AdminPushRequest request) {
        // 1. 서비스(MealNotificationService)의 processAdminNotification이
        //    파라미터로 받는 타입인 'NotificationRequest'를 생성해야 합니다.
        NotificationRequest serviceDto = new NotificationRequest();

        serviceDto.setTitle(request.getTitle());
        serviceDto.setBody(request.getBody());
        serviceDto.setTargetType(request.getTargetType());

        // 💡 여기서 에러가 났던 이유:
        // AdminNotification(엔티티)의 setTargetEmails는 String을 받지만,
        // NotificationRequest(DTO)의 setTargetEmails는 List<String>을 받도록 설계되었습니다.
        serviceDto.setTargetEmails(request.getTargetEmails());

        serviceDto.setTargetDate(request.getTargetDate());
        serviceDto.setScheduledTime(request.getScheduledTime());

        // 2. 이제 타입이 일치하므로 에러 없이 호출됩니다.
        mealNotificationService.processAdminNotification(serviceDto);

        return ResponseEntity.ok("알림 요청 성공");
    }

    /**
     * 프론트엔드 JSON 구조와 매핑되는 내부 DTO 클래스
     */
    @Getter
    @NoArgsConstructor
    public static class AdminPushRequest {
        private String title;
        private String body;
        private String targetType;
        private List<String> targetEmails;
        private String targetDate;
        private LocalDateTime scheduledTime;
    }
}