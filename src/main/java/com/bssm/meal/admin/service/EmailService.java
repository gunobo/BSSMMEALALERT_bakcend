package com.bssm.meal.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value; // ✅ 추가
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // ✅ application.properties(혹은 secret)에 설정된 이메일 주소를 가져옵니다.
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * ✅ 1. 계정 이용 제한(차단) 안내 메일 전송
     */
    @Async
    public void sendBanNotification(String toEmail, String reason, LocalDateTime expiresAt) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail); // ✅ 발신자 주소 명시 (에러 해결 핵심!)
            message.setTo(toEmail);
            message.setSubject("[BSSM 급식알리미] 귀하의 계정 이용이 일시적으로 제한되었습니다.");

            String expireStr = (expiresAt != null)
                    ? expiresAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " 까지"
                    : "영구 제한";

            String content = String.format(
                    "안녕하세요. BSSM 급식알리미 운영팀입니다.\n\n" +
                            "귀하의 계정 활동 중 서비스 운영 정책 위반이 확인되어\n" +
                            "아래와 같이 이용 제한 조치가 취해졌음을 알려드립니다.\n\n" +
                            "------------------------------------------\n" +
                            "🚫 제한 사유: %s\n" +
                            "⏳ 제한 기간: %s\n" +
                            "------------------------------------------\n\n" +
                            "제한 기간이 종료되면 자동으로 서비스 이용이 가능해집니다.\n" +
                            "본 조치에 대해 문의사항이 있으시면 관리자 메일로 연락 부탁드립니다.\n\n" +
                            "감사합니다.",
                    (reason != null && !reason.isEmpty()) ? reason : "운영 정책 위반",
                    expireStr
            );

            message.setText(content);
            mailSender.send(message);

            log.info("📧 차단 안내 메일 전송 성공: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ 차단 메일 전송 에러: {}", e.getMessage());
        }
    }

    /**
     * ✅ 2. 계정 이용 제한 해제 안내 메일 전송
     */
    @Async
    public void sendUnbanNotification(String toEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail); // ✅ 발신자 주소 명시
            message.setTo(toEmail);
            message.setSubject("[BSSM 급식알리미] 계정 이용 제한이 해제되었습니다.");

            String content =
                    "안녕하세요. BSSM 급식알리미 운영팀입니다.\n\n" +
                            "귀하의 계정에 적용되었던 이용 제한 조치가 해제되었습니다.\n" +
                            "이제 정상적으로 모든 서비스를 이용하실 수 있습니다.\n\n" +
                            "기다려주셔서 감사합니다. 앞으로 더욱 쾌적한 서비스를 위해 노력하겠습니다.\n\n" +
                            "감사합니다.";

            message.setText(content);
            mailSender.send(message);

            log.info("📧 차단 해제 메일 전송 성공: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ 차단 해제 메일 전송 에러: {}", e.getMessage());
        }
    }

    /**
     * 3. 신고 처리 결과 안내 메일 전송
     */
    @Async
    public void sendReportResult(String toEmail, String status, String adminMsg) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail); // ✅ 발신자 주소 명시
            message.setTo(toEmail);
            message.setSubject("[BSSM 급식알리미] 신고 처리 결과 안내드립니다.");

            String statusTitle = "RESOLVED".equalsIgnoreCase(status) ? "✅ 해결(승인)" : "❌ 거부(반려)";

            String content = String.format(
                    "안녕하세요. BSSM 급식알리미 운영팀입니다.\n\n" +
                            "귀하께서 접수하신 신고 건의 처리 결과입니다.\n\n" +
                            "------------------------------------------\n" +
                            "📌 처리 상태: %s\n" +
                            "💬 관리자 답변: %s\n" +
                            "------------------------------------------\n\n" +
                            "항상 서비스를 이용해 주셔서 감사합니다.",
                    statusTitle, adminMsg
            );

            message.setText(content);
            mailSender.send(message);

            log.info("📧 신고 결과 메일 전송 성공: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ 신고 결과 메일 전송 에러: {}", e.getMessage());
        }
    }

    /**
     * ✅ 4. 신규 가입 환영 메일 전송
     */
    @Async
    public void sendWelcomeEmail(String toEmail, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail); // 아까 추가한 발신자 주소
            message.setTo(toEmail);
            message.setSubject("[BSSM 급식알리미] 회원가입을 진심으로 환영합니다!");

            String content = String.format(
                    "안녕하세요, %s님!\n\n" +
                            "BSSM 급식알리미 서비스에 가입해 주셔서 진심으로 감사합니다.\n" +
                            "이제 매일매일 맛있는 급식 정보를 편하게 받아보실 수 있습니다.\n\n" +
                            "------------------------------------------\n" +
                            "✨ 서비스 주요 기능\n" +
                            "- 등록된 급식 메뉴 확인\n" +
                            "- 선호 메뉴 설정 및 알림\n" +
                            "- 알레르기 설정 및 알림\n" +
                            "------------------------------------------\n\n" +
                            "서비스 이용 중 불편한 점이 있다면 언제든 문의해 주세요.\n" +
                            "감사합니다.",
                    name
            );

            message.setText(content);
            mailSender.send(message);

            log.info("📧 가입 환영 메일 전송 성공: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ 가입 환영 메일 전송 에러: {}", e.getMessage());
        }
    }
}