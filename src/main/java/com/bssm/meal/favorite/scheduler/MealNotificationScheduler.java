package com.bssm.meal.favorite.scheduler;

import com.bssm.meal.favorite.service.NotificationFilterService;
import com.bssm.meal.like.service.MealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MealNotificationScheduler {

    private final MealService mealService;
    private final NotificationFilterService filterService;

    /**
     * 아침 알림: 평일 07:30 (조식 정보 기준)
     */
    @Scheduled(cron = "0 30 7 * * MON-FRI")
    public void scheduleMorningMealNotification() {
        sendMealNotification("아침");
    }

    /**
     * 점심 알림: 평일 12:20
     */
    @Scheduled(cron = "0 20 12 * * MON-FRI")
    public void scheduleLunchMealNotification() {
        sendMealNotification("점심");
    }

    /**
     * 저녁 알림: 평일 18:00
     */
    @Scheduled(cron = "0 10 18 * * MON-FRI")
    public void scheduleDinnerMealNotification() {
        sendMealNotification("저녁");
    }

    /**
     * 공통 알림 발송 로직
     */
    private void sendMealNotification(String timeLabel) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 나이스 API에서 오늘 전체 메뉴를 가져옴
        List<String> todayMenus = mealService.getMenusByDate(today);

        if (todayMenus != null && !todayMenus.isEmpty()) {
            log.info("🔔 [{}] 맞춤 알림 프로세스 시작 (날짜: {})", timeLabel, today);
            // NotificationFilterService의 List<List<String>> 규격에 맞춰 전송
            filterService.sendFilteredNotifications(today, todayMenus);
        } else {
            log.warn("⚠️ [{}] 알림 발송 실패: 오늘 메뉴 정보가 없습니다.", timeLabel);
        }
    }
}