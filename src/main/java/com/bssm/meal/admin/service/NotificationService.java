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
     * 1. [홈페이지용] 가장 최근 'NOTICE' 타입 공지사항 1건 조회
     */
    @Transactional(readOnly = true)
    public Notification getLatestNotice() {
        return notificationRepository.findFirstByTypeOrderByCreatedAtDesc("ALARM").orElse(null);
    }

    /**
     * 2. [게시판용] 'NOTICE' 타입의 모든 공지사항 리스트 조회 (최신순)
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
     * @param sendAlert true일 때만 SSE 실시간 알림을 보냄
     */
    @Transactional
    public Notification saveNoticeWithFile(String title, String content, MultipartFile file, boolean sendAlert, String type) {
        String imageUrl = null;

        if (file != null && !file.isEmpty()) {
            try {
                File folder = new File(uploadPath);
                if (!folder.exists()) folder.mkdirs();

                String originalName = file.getOriginalFilename();
                String fileName = UUID.randomUUID().toString() + "_" + originalName;
                Path path = Paths.get(uploadPath + fileName);

                Files.write(path, file.getBytes());
                imageUrl = "/uploads/notices/" + fileName;
            } catch (IOException e) {
                log.error("이미지 저장 실패: {}", e.getMessage());
                throw new RuntimeException("이미지 저장 중 오류 발생", e);
            }
        }

        Notification notice = Notification.builder()
                .title(title)
                .content(content)
                .imageUrl(imageUrl)
                .type(type) // "NOTICE" 또는 "ALARM"
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotice = notificationRepository.save(notice);

        // ✅ [중요] sendAlert가 true인 경우에만 알림 로직 실행
        if (sendAlert) {
            log.info("🔔 실시간 알림(SSE) 발송 시작: {}", savedNotice.getTitle());
            sendSseNotification(savedNotice);
        } else {
            log.info("📝 일반 게시글 저장 완료 (알림 미발송): {}", savedNotice.getTitle());
        }

        return savedNotice;
    }

    /**
     * 5. SSE 구독 설정
     */
    public SseEmitter subscribe(Long userId) {
        // 기존 연결이 있다면 명시적으로 종료 후 새로 생성 (중복 알림 방지)
        if (emitters.containsKey(userId)) {
            emitters.get(userId).complete();
            emitters.remove(userId);
        }

        SseEmitter emitter = new SseEmitter(30L * 60 * 1000); // 30분

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
        if (emitters.isEmpty()) {
            log.info("발송할 대상(SSE 구독자)이 없습니다.");
            return;
        }

        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("notice")
                        .data(notice));
            } catch (Exception e) {
                log.warn("SSE 전송 실패, 유저 {}의 연결을 제거합니다.", id);
                emitters.remove(id);
            }
        });
    }
}