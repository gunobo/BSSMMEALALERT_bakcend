package com.bssm.meal.favorite.scheduler;

import com.bssm.meal.favorite.entity.AdminNotification;
import com.bssm.meal.favorite.entity.FcmToken;
import com.bssm.meal.favorite.repository.AdminNotificationRepository;
import com.bssm.meal.favorite.repository.FcmTokenRepository;
import com.bssm.meal.favorite.service.FcmService;
import com.bssm.meal.like.service.MealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MealAlarmScheduler {

    private final FcmTokenRepository fcmTokenRepository;
    private final FcmService fcmService;
    private final AdminNotificationRepository adminNotificationRepository;
    private final MealService mealService;

    // 1. 아침 알림
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Seoul")
    @Transactional
    public void sendMorningMealAlarm() {
        if (isMealPresentToday()) {
            sendUniversalPush("[BSSM급식알리미]오늘의 조식 메뉴 🍱", "오늘 아침, 힘차게 시작해봐요!");
        } else {
            log.info("🗓️ 오늘은 조식 정보가 없어 알림을 보내지 않습니다.");
        }
    }

    // 2. 점심 알림
    @Scheduled(cron = "0 20 12 * * *", zone = "Asia/Seoul")
    @Transactional
    public void sendLunchMealAlarm() {
        if (isMealPresentToday()) {
            sendUniversalPush("[BSSM급식알리미]오늘의 중식 메뉴 🍛", "기다리던 점심 시간입니다. 맛있게 드세요!");
        } else {
            log.info("🗓️ 오늘은 중식 정보가 없어 알림을 보내지 않습니다.");
        }
    }

    // 3. 저녁 알림
    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Seoul")
    @Transactional
    public void sendEveningMealAlarm() {
        if (isMealPresentToday()) {
            sendUniversalPush("[BSSM급식알리미]오늘의 석식 메뉴 🍕", "오늘 하루도 고생 많았어요. 즐거운 저녁 되세요!");
        } else {
            log.info("🗓️ 오늘은 석식 정보가 없어 알림을 보내지 않습니다.");
        }
    }

    /**
     * 오늘 날짜에 급식 정보가 존재하는지 체크
     */
    private boolean isMealPresentToday() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        try {
            List<String> todayMenus = mealService.getMenusByDate(today);
            return todayMenus != null && !todayMenus.isEmpty();
        } catch (Exception e) {
            log.error("❌ 급식 정보 조회 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ✅ 공통 푸시 발송 및 DB 저장 로직
     */
    private void sendUniversalPush(String title, String body) {
        log.info("📢 푸시 발송 시작: {}", title);
        List<FcmToken> allTokens = fcmTokenRepository.findAll();

        // 1. 실제 푸시 전송
        for (FcmToken fcmToken : allTokens) {
            try {
                if (fcmToken.getUser() != null) {
                    Long userId = fcmToken.getUser().getId();
                    fcmService.sendPushToUser(userId, title, body);
                }
            } catch (Exception e) {
                log.error("❌ 전송 실패 (토큰 ID: {}): {}", fcmToken.getId(), e.getMessage());
            }
        }

        // 2. ✅ admin_notifications 테이블에 이력 저장
        try {
            AdminNotification history = AdminNotification.builder()
                    .title(title)
                    .body(body)
                    .targetType("ALL") // 전체 발송이므로 ALL
                    .targetDate(LocalDate.now().toString())
                    .scheduledTime(LocalDateTime.now())
                    .sent(true) // 발송 완료 상태
                    .createdBy("SYSTEM_SCHEDULER") // 시스템에 의해 발송됨을 명시
                    .createdAt(LocalDateTime.now())
                    .build();

            adminNotificationRepository.save(history);
            log.info("💾 시스템 푸시 이력이 admin_notifications에 저장되었습니다.");
        } catch (Exception e) {
            log.error("❌ 푸시 이력 저장 실패: {}", e.getMessage());
        }
    }

    /**
     * 1분마다 예약된 관리자 알림 체크 (기존 로직 유지)
     */
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    @Transactional
    public void sendReservedAdminNotifications() {
        LocalDateTime now = LocalDateTime.now();

        List<AdminNotification> reservedNotifications =
                adminNotificationRepository.findBySentFalseAndScheduledTimeBefore(now);

        for (AdminNotification noti : reservedNotifications) {
            try {
                log.info("⏰ 예약된 관리자 푸시 발송 시작: {}", noti.getTitle());

                String sender = (noti.getCreatedBy() != null) ? noti.getCreatedBy() : "SYSTEM_RESERVED";

                fcmService.sendAdminPush(
                        noti.getTargetType(),
                        noti.getTargetEmails() != null ? List.of(noti.getTargetEmails().split(",")) : null,
                        noti.getTitle(),
                        noti.getBody(),
                        noti.getTargetDate(),
                        sender
                );

                noti.setSent(true);
                adminNotificationRepository.save(noti);

            } catch (Exception e) {
                log.error("❌ 예약 푸시 전송 실패 (ID: {}): {}", noti.getId(), e.getMessage());
            }
        }
    }
}