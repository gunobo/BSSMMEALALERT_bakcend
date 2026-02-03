package com.bssm.meal.admin.service; // 실제 패키지 구조에 맞게 수정하세요

import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BanSchedulerService {

    private final UserRepository userRepository;
    private final EmailService mailService; // 기존에 만들어두신 메일 서비스 주입

    @Transactional
    @Scheduled(cron = "0 * * * * *")
    public void runAutoUnbanAndNotify() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 차단이 만료된 사용자 리스트를 먼저 가져옵니다.
        List<User> expiredUsers = userRepository.findAllByIsBannedTrueAndBanExpiresAtBefore(now);

        if (!expiredUsers.isEmpty()) {
            for (User user : expiredUsers) {
                // 2. DB 상태 변경
                user.setIsBanned(false);
                user.setBanExpiresAt(null);

                // 3. 메일 발송 (MailService에 구현된 메서드 호출)
                try {
                    mailService.sendUnbanNotification(user.getEmail());
                    log.info("📧 [메일발송] {}님에게 차단 해제 알림을 보냈습니다.", user.getName());
                } catch (Exception e) {
                    log.error("❌ [메일실패] {}님 메일 발송 중 오류: {}", user.getName(), e.getMessage());
                }
            }
            log.info("🔔 총 {}명의 사용자가 자동 해제되었습니다.", expiredUsers.size());
        }
    }
}