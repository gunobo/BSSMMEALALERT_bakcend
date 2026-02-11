package com.bssm.meal.favorite.service;

import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationFilterService {

    private final UserRepository userRepository;
    private final FcmService fcmService;

    @Transactional(readOnly = true)
    public void sendFilteredNotifications(String mealDate, List<String> rawMenus) {

        // 1. 메뉴 정제 (괄호 및 숫자 제거)
        List<String> cleanMenus = rawMenus.stream()
                .map(menu -> menu.replaceAll("\\([^)]*\\)", "").trim())
                .filter(menu -> !menu.isEmpty())
                .collect(Collectors.toList());

        if (cleanMenus.isEmpty()) {
            log.info("{} 날짜에 정제된 메뉴가 없어 알림을 발송하지 않습니다.", mealDate);
            return;
        }

        // 2. 알림 설정이 켜져 있는 유저들 조회
        // (필드명이 allow_notifications이므로 레포지토리 메서드 확인 필요)
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(u -> u.isAllow_notifications() != null && u.isAllow_notifications())
                .collect(Collectors.toList());

        for (User user : activeUsers) {
            // ✅ [수정] FcmToken 객체 리스트가 아닌 String 리스트로 받음
            List<String> userTokens = user.getFcmTokens();
            if (userTokens.isEmpty()) continue;

            StringBuilder messageBody = new StringBuilder();
            boolean shouldSend = false;

            // --- A. 알레르기 필터링 ---
            if (user.isAllow_allergy_notifications()) {
                List<String> matchedAllergies = findMatchedItems(user.getAllergies(), cleanMenus);
                if (!matchedAllergies.isEmpty()) {
                    messageBody.append("⚠️ 못 드시는 [")
                            .append(String.join(", ", matchedAllergies))
                            .append("] 성분이 포함되어 있어요.\n");
                    shouldSend = true;
                }
            }

            // --- B. 선호 메뉴 필터링 ---
            if (user.isAllow_favorite_notifications()) {
                List<String> matchedFavorites = findMatchedItems(user.getFavoriteMenus(), cleanMenus);
                if (!matchedFavorites.isEmpty()) {
                    messageBody.append("⭐ 좋아하는 [")
                            .append(String.join(", ", matchedFavorites))
                            .append("] 메뉴가 오늘 나와요!\n");
                    shouldSend = true;
                }
            }

            // --- C. 발송 실행 ---
            if (shouldSend) {
                // ✅ [수정] fcmService.sendPushToTokens도 이제 List<String>을 인자로 받습니다.
                fcmService.sendPushToTokens(
                        userTokens,
                        "🍱 오늘의 맞춤 급식 알림",
                        messageBody.toString().trim(),
                        mealDate,
                        "FILTERED",
                        "SYSTEM"
                );
            }
        }
    }

    private List<String> findMatchedItems(List<String> userSettings, List<String> cleanMenus) {
        if (userSettings == null || userSettings.isEmpty()) return List.of();

        return userSettings.stream()
                .filter(StringUtils::hasText) // 빈 문자열 방지
                .filter(item -> cleanMenus.stream().anyMatch(menu -> menu.contains(item)))
                .collect(Collectors.toList());
    }
}