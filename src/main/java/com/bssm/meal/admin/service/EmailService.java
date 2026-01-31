package com.bssm.meal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service // ✅ 이 어노테이션이 있어야 컨트롤러에서 'EmailService'를 불러올 수 있습니다.
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender; // pom.xml에 의존성 추가 시 자동 주입됨

    @Async // ✅ 메일 보내느라 관리자 화면이 멈추지 않게 비동기로 처리
    public void sendReportResult(String toEmail, String status, String adminMsg) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("[BSSM 급식알리미] 신고 처리 결과 안내드립니다.");

            // 상태 한글화
            String statusTitle = "RESOLVED".equalsIgnoreCase(status) ? "✅ 해결(승인)" : "❌ 거부(반려)";

            String content = String.format(
                    "안녕하세요. BSSM 급식알리미 운영팀입니다.\n\n" +
                            "귀하께서 접수하신 신고 건의 처리 결과입니다.\n\n" +
                            "------------------------------------------\n" +
                            "📌 처리 상태: %s\n" +
                            "💬 관리자 답변: %s\n" +
                            "------------------------------------------\n\n" +
                            "이용해 주셔서 감사합니다.",
                    statusTitle, adminMsg
            );

            message.setText(content);
            mailSender.send(message);

            log.info("📧 메일 전송 성공: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ 메일 전송 중 에러 발생: {}", e.getMessage());
        }
    }
}