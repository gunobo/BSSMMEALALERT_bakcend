package com.bssm.meal.admin.controller;

import com.bssm.meal.admin.domain.Notification;
import com.bssm.meal.admin.service.NotificationService;
import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NoticeController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * 1. 최신 공지사항 단건 조회
     */
    @GetMapping("/latest")
    public ResponseEntity<Notification> getLatestNotice() {
        return ResponseEntity.ok(notificationService.getLatestNotice());
    }

    /**
     * 2. SSE 실시간 구독
     * {id}를 사용하여 유저의 고유 식별자(PK 또는 이메일)를 받습니다.
     * 정규표현식 {id:.+} 을 사용하여 이메일에 포함된 마침표(.)가 잘리지 않도록 합니다.
     */
    @GetMapping(value = "/subscribe/{id:.+}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable("id") String id) {
        log.info("🔔 SSE 구독 요청 - 식별자: {}", id);

        // 1. 입력받은 값이 이메일 형식인 경우와 ID(숫자) 형식인 경우를 모두 대응합니다.
        User user;
        if (id.contains("@")) {
            user = userRepository.findByEmail(id)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저(이메일)입니다: " + id));
        } else {
            try {
                Long userId = Long.parseLong(id);
                user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저(ID)입니다: " + id));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("유효하지 않은 유저 식별자 형식입니다: " + id);
            }
        }

        // 2. 서비스 레이어에 유저 PK(Long) 전달
        return notificationService.subscribe(user.getId());
    }
}