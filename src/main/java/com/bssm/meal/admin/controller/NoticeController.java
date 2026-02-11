package com.bssm.meal.admin.controller;

import com.bssm.meal.admin.domain.Notification;
import com.bssm.meal.admin.service.AdminNotificationService;
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

    private final AdminNotificationService adminNotificationService;
    private final UserRepository userRepository;

    /**
     * 1. [홈피 접속용] 최신 공지사항 단건 조회
     */
    @GetMapping("/latest")
    public ResponseEntity<Notification> getLatestNotice() {
        return ResponseEntity.ok(adminNotificationService.getLatestNotice());
    }

    /**
     * 2. [게시판 목록용] 전체 공지사항 조회
     */
    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAllNotices() {
        return ResponseEntity.ok(adminNotificationService.getAllNotices());
    }

    /**
     * 3. [상세 페이지용] 공지사항 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotice(@PathVariable Long id) {
        return ResponseEntity.ok(adminNotificationService.getNoticeById(id));
    }

    /**
     * 4. [게시판용] 공지사항 등록
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Notification> createNotice(
            @RequestPart("title") String title,
            @RequestPart("content") String content,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        return ResponseEntity.ok(adminNotificationService.saveNoticeWithFile(title, content, file, false, "NOTICE"));
    }

    /**
     * ✅ 5. [게시판용] 공지사항 수정 (추가)
     * 프론트엔드의 axios.put 요청을 처리합니다.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Notification> updateNotice(
            @PathVariable Long id,
            @RequestPart("title") String title,
            @RequestPart("content") String content,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        log.info("📝 공지사항 수정 요청 - ID: {}", id);
        // NotificationService에 updateNoticeWithFile 메서드를 구현해야 합니다.
        return ResponseEntity.ok(adminNotificationService.updateNoticeWithFile(id, title, content, file));
    }

    /**
     * ✅ 6. [게시판용] 공지사항 삭제 (추가)
     * 프론트엔드의 axios.delete 요청을 처리합니다.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long id) {
        log.info("🗑️ 공지사항 삭제 요청 - ID: {}", id);
        adminNotificationService.deleteNotice(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 7. SSE 실시간 구독
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

        return adminNotificationService.subscribe(user.getId());
    }
}