package com.bssm.meal.favorite.service;

import com.bssm.meal.favorite.dto.NotificationRequest;
import com.bssm.meal.favorite.entity.AdminNotification;
import com.bssm.meal.favorite.entity.FcmToken;
import com.bssm.meal.favorite.repository.AdminNotificationRepository;
import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.repository.UserRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealNotificationService {

    private final UserRepository userRepository;
    private final AdminNotificationRepository adminNotificationRepository;
    private final FcmService fcmService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${neis.api.key}")
    private String apiKey;
    @Value("${neis.api.atpt-code}")
    private String atptCode;
    @Value("${neis.api.schul-code}")
    private String schulCode;

    // ==========================================
    // [1] 관리자 푸시 및 예약 관련 메서드
    // ==========================================

    @Transactional
    public void processAdminNotification(NotificationRequest request) {
        boolean isReserved = request.getScheduledTime() != null && request.getScheduledTime().isAfter(LocalDateTime.now());

        // 현재 로그인한 관리자의 정보를 SecurityContext에서 가져옴
        String adminName = SecurityContextHolder.getContext().getAuthentication().getName();

        String senderDisplayName = userRepository.findByEmail(adminName)
                .map(User::getName)
                .orElse("관리자");

        AdminNotification noti = AdminNotification.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .targetDate(request.getTargetDate())
                .targetType(request.getTargetType())
                .targetEmails(request.getTargetEmails() != null ? String.join(",", request.getTargetEmails()) : null)
                .scheduledTime(request.getScheduledTime())
                .createdBy(senderDisplayName)
                .sent(false)
                .build();

        if (!isReserved) {
            // ✅ fcmService 내부에서 MOBILE/WEB 토큰을 분류해서 발송함
            fcmService.sendAdminPush(
                    request.getTargetType(),
                    request.getTargetEmails(),
                    request.getTitle(),
                    request.getBody(),
                    request.getTargetDate(),
                    senderDisplayName
            );
            noti.setSent(true);
        }

        adminNotificationRepository.save(noti);
    }

    public Map<String, Object> getNotificationStats() {
        List<AdminNotification> logs = adminNotificationRepository.findTop10ByOrderByCreatedAtDesc();
        long totalSentCount = adminNotificationRepository.count();

        List<Map<String, Object>> mappedLogs = logs.stream().map(logEntry -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", logEntry.getId());
            map.put("sentAt", logEntry.getCreatedAt());
            map.put("senderEmail", logEntry.getCreatedBy());
            map.put("title", logEntry.getTitle());
            map.put("targetType", logEntry.getTargetType());
            map.put("totalCount", logEntry.getTotalCount());
            map.put("successCount", logEntry.getSuccessCount());
            map.put("failureCount", logEntry.getFailureCount());
            return map;
        }).collect(Collectors.toList());

        return Map.of(
                "recentLogs", mappedLogs,
                "totalSentCount", totalSentCount
        );
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void checkAdminReservedNotifications() {
        List<AdminNotification> reserved = adminNotificationRepository.findBySentFalseAndScheduledTimeBefore(LocalDateTime.now());
        for (AdminNotification noti : reserved) {
            List<String> emailList = (noti.getTargetEmails() != null && !noti.getTargetEmails().isEmpty())
                    ? Arrays.asList(noti.getTargetEmails().split(",")) : null;

            fcmService.sendAdminPush(noti.getTargetType(), emailList, noti.getTitle(), noti.getBody(), noti.getTargetDate(), "SYSTEM_RESERVED");

            noti.setSent(true);
            adminNotificationRepository.save(noti);
        }
    }

    // ==========================================
    // [2] 자동 급식 알림 메서드
    // ==========================================

    @Scheduled(cron = "0 35 7 * * *")
    public void notifyMorning() { processMealSpecificNotification("조식"); }

    @Scheduled(cron = "0 30 11 * * *")
    public void notifyLunch() { processMealSpecificNotification("중식"); }

    @Scheduled(cron = "0 20 17 * * *")
    public void notifyDinner() { processMealSpecificNotification("석식"); }

    @Transactional(readOnly = true)
    public void processMealSpecificNotification(String mealType) {
        List<MealDetail> meals = fetchDetailedMealsFromNeis(LocalDate.now());
        String todayStr = LocalDate.now().toString();

        meals.stream().filter(m -> m.getMealType().equals(mealType)).findFirst().ifPresent(meal -> {
            String menu = meal.getMenuContent();

            userRepository.findByAllowNotificationsTrue().forEach(user -> {
                // ✅ 유저의 모든 기기 토큰(폰, PC 등)을 가져옴
                List<String> tokens = user.getFcmTokens();
                if (tokens.isEmpty()) return;

                List<String> favs = user.getFavoriteMenus().stream().filter(menu::contains).collect(Collectors.toList());
                List<String> allergies = (user.getAllergies() != null) ?
                        user.getAllergies().stream().filter(menu::contains).collect(Collectors.toList()) : new ArrayList<>();

                if (!favs.isEmpty() || !allergies.isEmpty()) {
                    StringBuilder sb = new StringBuilder("[" + mealType + "] ");
                    if (!favs.isEmpty()) sb.append("⭐즐겨찾기: ").append(String.join(", ", favs));
                    if (!allergies.isEmpty()) sb.append(sb.length() > 15 ? "\n" : " ").append("⚠️알레르기 주의: ").append(String.join(", ", allergies));

                    // ✅ fcmService.sendPushToTokens가 기기별(MOBILE/WEB)로 나눠서 적절한 설정을 입혀 발송함
                    fcmService.sendPushToTokens(tokens, "🍱 " + mealType + " 급식 체크", sb.toString(), todayStr, "MEAL_ALERT", "SYSTEM");
                }
            });
        });
    }

    // ==========================================
    // [3] 공통 유틸리티
    // ==========================================

    private List<MealDetail> fetchDetailedMealsFromNeis(LocalDate date) {
        List<MealDetail> details = new ArrayList<>();
        String url = String.format("https://open.neis.go.kr/hub/mealServiceDietInfo?KEY=%s&Type=json&ATPT_OFCDC_SC_CODE=%s&SD_SCHUL_CODE=%s&MLSV_YMD=%s",
                apiKey, atptCode, schulCode, date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        try {
            String res = restTemplate.getForObject(url, String.class);
            JSONObject json = new JSONObject(res);
            if (json.has("mealServiceDietInfo")) {
                JSONArray rows = json.getJSONArray("mealServiceDietInfo").getJSONObject(1).getJSONArray("row");
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject r = rows.getJSONObject(i);
                    details.add(new MealDetail(r.getString("MMEAL_SC_NM"), cleanMenuText(r.getString("DDISH_NM"))));
                }
            }
        } catch (Exception e) { log.error("API 에러: {}", e.getMessage()); }
        return details;
    }

    private String cleanMenuText(String raw) {
        return raw.replaceAll("<br/>", " ").replaceAll("\\([0-9.]+\\)", "").replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9 ]", "").trim();
    }

    @Getter @AllArgsConstructor
    public static class MealDetail { private String mealType; private String menuContent; }
}