package com.bssm.meal.favorite.service;

import com.bssm.meal.favorite.dto.PushRequestDto;
import com.bssm.meal.favorite.entity.NotificationHistory;
import com.bssm.meal.favorite.repository.NotificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationHistoryRepository historyRepository;
    private final StringRedisTemplate redisTemplate;
    private final FcmService fcmService;

    public void sendNotification(PushRequestDto request) {
        // 1. 유효성 검사 (테스트 메시지이거나 내용이 있을 때만 통과)
        boolean isTest = StringUtils.hasText(request.getTitle()) && request.getTitle().contains("테스트");
        if (!isTest && isMenuEmpty(request.getBody())) {
            log.info("ℹ️ 급식 메뉴 정보가 없어 발송을 중단합니다.");
            return;
        }

        // 2. 관리자 정보 (발송자)
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // 3. Redis 분산 락 (중복 발송 방지)
        // 제목 + 타겟타입을 조합하여 유니크한 키 생성 (내용 hash는 너무 민감할 수 있으니 선택적 사용)
        String lockKey = "notif_lock:" + request.getTargetType() + ":" + request.getTitle().replaceAll("\\s", "");

        // 30초 동안 락을 걸어 연타로 클릭하거나 중복 API 호출이 발생하는 것을 원천 차단
        Boolean isFirst = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(30));

        if (Boolean.FALSE.equals(isFirst)) {
            log.warn("⚠️ 동일한 알림이 이미 처리 중이거나 방금 발송되었습니다 (중복 차단): {}", request.getTitle());
            return;
        }

        try {
            log.info("🚀 FCM 발송 프로세스 진입 - 발송자: {}, 제목: {}, 타겟: {}",
                    adminEmail, request.getTitle(), request.getTargetType());

            // 4. ⭐️ 실제 FCM 발송 로직 호출 (FcmService에서 만든 메서드 활용)
            fcmService.sendAdminPush(
                    request.getTargetType(),   // ALL 또는 TARGET
                    request.getTargetEmails(), // 대상 이메일 리스트
                    request.getTitle(),
                    request.getBody(),
                    request.getTargetDate(),   // 클릭 시 이동할 날짜
                    adminEmail
            );

            // 5. 발송 기록 저장 (FCM 발송이 성공한 후에만 실행됨)
            NotificationHistory history = NotificationHistory.builder()
                    .title(request.getTitle())
                    .body(request.getBody())
                    .senderEmail(adminEmail)
                    .targetType(request.getTargetType())
                    .build();

            historyRepository.save(history);
            log.info("✅ 알림 발송 기록 저장 완료");

        } catch (Exception e) {
            log.error("❌ 알림 처리 중 예외 발생: {}", e.getMessage());
            // 예외 발생 시에만 락을 즉시 해제하여 재시도가 가능하도록 함
            redisTemplate.delete(lockKey);
            throw e;
        }
        // 성공 시에는 30초 락이 유지되므로 중복 클릭 방지됨
    }

    private boolean isMenuEmpty(String body) {
        if (!StringUtils.hasText(body)) return true;
        String clean = body.replaceAll("[\\s\\p{Punct}]", "");
        return clean.isEmpty() || clean.contains("급식이없습니다") || clean.contains("정보가없습니다");
    }
}