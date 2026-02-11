package com.bssm.meal.favorite.service;

import com.bssm.meal.favorite.entity.NotificationHistory;
import com.bssm.meal.favorite.repository.NotificationHistoryRepository;
import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.repository.UserRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final UserRepository userRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final FirebaseMessaging firebaseMessaging;

    private static final String ANDROID_CHANNEL_ID = "default_channel_id";

    /**
     * ✅ 토큰 저장 (User 엔티티 필드에 직접 저장)
     */
    @Transactional
    public void saveToken(Long userId, String token, String deviceType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 없습니다."));

        user.updateFcmToken(token, deviceType);

        // ✅ 알림 허용 자동 활성화
        if (user.getAllowNotifications() == null || !user.getAllowNotifications()) {
            user.setAllowNotifications(true);
        }

        userRepository.save(user);

        // ✅ 디버깅 로그
        log.info("✅ FCM 토큰 저장 완료: User={}, Type={}, Token={}",
                user.getEmail(), deviceType, token);
        log.info("   현재 저장된 토큰들: Mobile={}, Web={}",
                user.getFcmTokenMobile(), user.getFcmTokenWeb());
        log.info("   알림 허용 상태: {}", user.isAllowNotifications());
    }

    /**
     * ✅ 특정 유저에게 푸시 발송
     */
    @Transactional
    public void sendPushToUser(Long userId, String title, String body) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            log.warn("🚫 존재하지 않는 유저: {}", userId);
            return;
        }

        if (!user.isAllowNotifications()) {
            log.warn("🚫 알림 거부 유저: {} (allow_notifications={})",
                    user.getEmail(), user.getAllowNotifications());
            return;
        }

        // 유저가 가진 모든 토큰(Mobile, Web)을 가져옴
        List<String> userTokens = user.getFcmTokens();

        // ✅ 디버깅 로그
        log.info("🔍 유저 토큰 조회: email={}, tokenCount={}, tokens={}",
                user.getEmail(), userTokens.size(), userTokens);

        if (userTokens.isEmpty()) {
            log.warn("⚠️ 유저에게 등록된 토큰이 없습니다: {} (Mobile={}, Web={})",
                    user.getEmail(), user.getFcmTokenMobile(), user.getFcmTokenWeb());
            return;
        }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 공통 발송 메서드 호출
        sendPushToTokens(userTokens, title, body, today, "TARGET", "SYSTEM");
    }

    /**
     * ✅ 관리자 푸시 (ALL 또는 특정 타겟)
     */
    @Transactional
    public void sendAdminPush(String targetType, List<String> targetEmails,
                              String title, String body, String targetDate, String senderEmail) {
        List<String> tokenStrings = new ArrayList<>();

        if ("ALL".equalsIgnoreCase(targetType)) {
            List<User> allUsers = userRepository.findAll();
            log.info("🔍 전체 유저 수: {}", allUsers.size());

            int allowedCount = 0;
            int deniedCount = 0;
            int noTokenCount = 0;

            for (User u : allUsers) {
                if (!u.isAllowNotifications()) {
                    deniedCount++;
                    log.debug("  ⊘ 알림 비활성화: {} (allow_notifications={})",
                            u.getEmail(), u.getAllowNotifications());
                    continue;
                }

                List<String> tokens = u.getFcmTokens();
                if (tokens.isEmpty()) {
                    noTokenCount++;
                    log.debug("  ⊘ 토큰 없음: {} (Mobile={}, Web={})",
                            u.getEmail(), u.getFcmTokenMobile(), u.getFcmTokenWeb());
                    continue;
                }

                allowedCount++;
                tokenStrings.addAll(tokens);
                log.debug("  ✓ {}: {}개 토큰 추가", u.getEmail(), tokens.size());
            }

            log.info("📊 유저 분석: 전체={}, 알림허용={}, 알림거부={}, 토큰없음={}",
                    allUsers.size(), allowedCount, deniedCount, noTokenCount);

        } else if (targetEmails != null && !targetEmails.isEmpty()) {
            log.info("🔍 타겟 이메일 수: {}", targetEmails.size());
            log.debug("   타겟 이메일: {}", targetEmails);

            List<User> targetUsers = userRepository.findByEmailIn(targetEmails);
            log.info("   DB에서 찾은 유저 수: {}", targetUsers.size());

            for (User u : targetUsers) {
                if (!u.isAllowNotifications()) {
                    log.debug("  ⊘ 알림 비활성화: {}", u.getEmail());
                    continue;
                }

                List<String> tokens = u.getFcmTokens();
                if (tokens.isEmpty()) {
                    log.debug("  ⊘ 토큰 없음: {}", u.getEmail());
                    continue;
                }

                tokenStrings.addAll(tokens);
                log.debug("  ✓ {}: {}개 토큰 추가", u.getEmail(), tokens.size());
            }
        }

        log.info("📢 푸시 발송 대상 토큰 수: {}건", tokenStrings.size());

        if (tokenStrings.isEmpty()) {
            log.warn("⚠️ 발송 가능한 토큰이 없어 푸시를 전송하지 않습니다.");

            // 빈 히스토리 저장
            notificationHistoryRepository.save(NotificationHistory.builder()
                    .title(title)
                    .body(body)
                    .senderEmail(senderEmail)
                    .targetType(targetType)
                    .totalCount(0)
                    .successCount(0)
                    .failureCount(0)
                    .build());
            return;
        }

        String date = (targetDate == null || targetDate.isEmpty())
                ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : targetDate;

        sendPushToTokens(tokenStrings, title, body, date, targetType, senderEmail);
    }

    /**
     * ✅ 실제 발송 처리 (List<String> 기반)
     */
    @Transactional
    public void sendPushToTokens(List<String> tokenStrings, String title, String body,
                                 String targetDate, String type, String senderEmail) {
        if (tokenStrings == null || tokenStrings.isEmpty()) {
            log.warn("⚠️ 발송할 대상 토큰이 없습니다.");
            return;
        }

        List<String> distinctTokens = tokenStrings.stream()
                .filter(t -> t != null && !t.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());

        log.info("🚀 FCM 발송 시작: 원본 토큰 {}개 → 중복제거 후 {}개",
                tokenStrings.size(), distinctTokens.size());

        BatchResponse response = sendBatch(distinctTokens, title, body, targetDate);

        int success = (response != null) ? response.getSuccessCount() : 0;
        int failure = (response != null) ? response.getFailureCount() : 0;

        // ✅ 실패한 토큰 상세 로그
        if (response != null && failure > 0) {
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse sr = responses.get(i);
                if (!sr.isSuccessful() && sr.getException() != null) {
                    log.warn("❌ 토큰 발송 실패 [{}]: {}",
                            i, sr.getException().getMessage());
                }
            }
        }

        notificationHistoryRepository.save(NotificationHistory.builder()
                .title(title)
                .body(body)
                .senderEmail(senderEmail)
                .targetType(type)
                .totalCount(distinctTokens.size())
                .successCount(success)
                .failureCount(failure)
                .build());

        log.info("📊 발송 완료 - 전체: {}, 성공: {}, 실패: {}",
                distinctTokens.size(), success, failure);
    }

    /**
     * ✅ FCM 배치 전송
     */
    private BatchResponse sendBatch(List<String> tokenStrings, String title, String body, String targetDate) {
        if (tokenStrings == null || tokenStrings.isEmpty()) {
            log.warn("⚠️ sendBatch: 전송할 토큰이 없습니다.");
            return null;
        }

        MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                .addAllTokens(tokenStrings)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("title", title)
                .putData("body", body)
                .putData("targetDate", targetDate)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setChannelId(ANDROID_CHANNEL_ID)
                                .setClickAction("OPEN_ACTIVITY")
                                .setSound("default")
                                .build())
                        .build())
                .setWebpushConfig(WebpushConfig.builder()
                        .setNotification(WebpushNotification.builder()
                                .setIcon("https://bssm.imjemin.co.kr/logo192.png")
                                .putCustomData("requireInteraction", "true")
                                .build())
                        .build());

        try {
            log.debug("🔧 FCM 메시지 빌드 완료, 전송 시작...");
            BatchResponse response = firebaseMessaging.sendEachForMulticast(messageBuilder.build());
            log.debug("✅ FCM 전송 요청 완료");
            return response;
        } catch (Exception e) {
            log.error("❌ FCM 전송 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * ✅ 토큰 삭제 (필드 비우기 방식)
     */
    @Transactional
    public void deleteToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("⚠️ 삭제할 토큰이 비어있습니다.");
            return;
        }

        log.info("🗑️ 토큰 삭제 시작: {}", token);

        int deletedCount = 0;
        List<User> allUsers = userRepository.findAll();

        for (User u : allUsers) {
            boolean changed = false;

            if (token.equals(u.getFcmTokenMobile())) {
                u.setFcmTokenMobile(null);
                changed = true;
                log.info("  ✓ Mobile 토큰 삭제: {}", u.getEmail());
            }

            if (token.equals(u.getFcmTokenWeb())) {
                u.setFcmTokenWeb(null);
                changed = true;
                log.info("  ✓ Web 토큰 삭제: {}", u.getEmail());
            }

            if (changed) {
                userRepository.save(u);
                deletedCount++;
            }
        }

        log.info("🗑️ 토큰 삭제 완료: {}명의 유저에서 삭제됨", deletedCount);
    }
}