package com.bssm.meal.admin.service;

import com.bssm.meal.admin.domain.Notification;
import com.bssm.meal.admin.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // 유저별 SSE 연결 저장소
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final String uploadPath = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "notices" + File.separator;

    /**
     * 1. [홈페이지용] 가장 최근 공지사항 조회
     */
    @Transactional(readOnly = true)
    public Notification getLatestNotice() {
        return notificationRepository.findFirstByTypeOrderByCreatedAtDesc("ALARM").orElse(null);
    }

    /**
     * 2. [게시판용] 전체 공지사항 리스트 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public List<Notification> getAllNotices() {
        return notificationRepository.findAllByTypeOrderByCreatedAtDesc("NOTICE");
    }

    /**
     * 3. [상세페이지용] 특정 공지사항 상세 조회
     */
    @Transactional(readOnly = true)
    public Notification getNoticeById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다. ID: " + id));
    }

    /**
     * 4. 공지사항 저장 및 알림 처리
     */
    @Transactional
    public Notification saveNoticeWithFile(String title, String content, MultipartFile file, boolean sendAlert, String type) {
        String imageUrl = saveFile(file);

        Notification notice = Notification.builder()
                .title(title)
                .content(content)
                .imageUrl(imageUrl)
                .type(type)
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotice = notificationRepository.save(notice);

        if (sendAlert) {
            sendSseNotification(savedNotice);
        }

        return savedNotice;
    }

    /**
     * ✅ 5. 공지사항 수정 (추가)
     * 마크다운 본문과 이미지 수정을 처리합니다.
     */
    @Transactional
    public Notification updateNoticeWithFile(Long id, String title, String content, MultipartFile file) {
        Notification notice = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("수정할 게시글이 존재하지 않습니다. ID: " + id));

        // 1. 기본 필드 업데이트 (Dirty Checking)
        notice.setTitle(title);
        notice.setContent(content);

        // 2. 새 이미지 파일이 업로드된 경우
        if (file != null && !file.isEmpty()) {
            // 기존 파일이 있다면 삭제 (서버 용량 관리)
            deleteActualFile(notice.getImageUrl());

            // 새 파일 저장 및 경로 업데이트
            String newImageUrl = saveFile(file);
            notice.setImageUrl(newImageUrl);
        }

        log.info("📝 공지사항 수정 완료: {}", id);
        return notice;
    }

    /**
     * ✅ 6. 공지사항 삭제 (추가)
     * DB 데이터와 실제 서버의 이미지 파일을 함께 삭제합니다.
     */
    @Transactional
    public void deleteNotice(Long id) {
        Notification notice = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 게시글이 존재하지 않습니다. ID: " + id));

        // 실제 이미지 파일 삭제
        deleteActualFile(notice.getImageUrl());

        // DB 레코드 삭제
        notificationRepository.delete(notice);
        log.info("🗑️ 공지사항 삭제 완료: {}", id);
    }

    /**
     * [내부 로직] 파일 저장 처리
     */
    private String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        try {
            File folder = new File(uploadPath);
            if (!folder.exists()) folder.mkdirs();

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadPath + fileName);
            Files.write(path, file.getBytes());

            return "/uploads/notices/" + fileName;
        } catch (IOException e) {
            log.error("이미지 저장 실패: {}", e.getMessage());
            throw new RuntimeException("이미지 저장 중 오류 발생", e);
        }
    }

    /**
     * [내부 로직] 실제 서버 파일 삭제
     */
    private void deleteActualFile(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) return;

        try {
            // URL 경로를 실제 파일 경로로 변환
            String fileName = imageUrl.replace("/uploads/notices/", "");
            Path filePath = Paths.get(uploadPath + fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("파일 삭제 실패 (경로가 유효하지 않음): {}", imageUrl);
        }
    }

    /**
     * 7. SSE 구독 설정
     */
    public SseEmitter subscribe(Long userId) {
        if (emitters.containsKey(userId)) {
            emitters.get(userId).complete();
            emitters.remove(userId);
        }

        SseEmitter emitter = new SseEmitter(30L * 60 * 1000);

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        } catch (IOException e) {
            emitters.remove(userId);
            return null;
        }

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(userId);
        });
        emitter.onError((e) -> emitters.remove(userId));

        emitters.put(userId, emitter);
        return emitter;
    }

    /**
     * [내부 로직] 실시간 알림 전송
     */
    private void sendSseNotification(Notification notice) {
        if (emitters.isEmpty()) return;

        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("notice").data(notice));
            } catch (Exception e) {
                emitters.remove(id);
            }
        });
    }
}