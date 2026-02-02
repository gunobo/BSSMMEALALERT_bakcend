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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NoticeController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * 1. [홈피 접속용] 최신 공지사항 단건 조회
     */
    @GetMapping("/latest")
    public ResponseEntity<Notification> getLatestNotice() {
        return ResponseEntity.ok(notificationService.getLatestNotice());
    }

    /**
     * 2. [게시판 목록용] 전체 공지사항 조회 (타입이 'NOTICE'인 것만 서비스에서 필터링됨)
     */
    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAllNotices() {
        return ResponseEntity.ok(notificationService.getAllNotices());
    }

    /**
     * 3. [상세 페이지용] 공지사항 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotice(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNoticeById(id));
    }

    /**
     * 4. [게시판용] 공지사항 등록
     * 게시판에 글을 쓸 때는 실시간 알람(SSE)이 가지 않도록 설정 (false)
     * 타입은 "NOTICE"로 지정하여 저장합니다.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Notification> createNotice(
            @RequestPart("title") String title,
            @RequestPart("content") String content,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        // ✅ 수정: 인자 5개를 전달합니다 (제목, 내용, 파일, 실시간알림여부, 타입)
        return ResponseEntity.ok(notificationService.saveNoticeWithFile(title, content, file, false, "NOTICE"));
    }

    /**
     * 5. SSE 실시간 구독
     */
    @GetMapping(value = "/subscribe/{id:.+}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable("id") String id) {
        log.info("🔔 SSE 구독 요청 - 식별자: {}", id);

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

        return notificationService.subscribe(user.getId());
    }
}