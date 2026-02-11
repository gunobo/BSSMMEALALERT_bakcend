package com.bssm.meal.admin.service;

import com.bssm.meal.admin.domain.AppDownloadStats;
import com.bssm.meal.admin.domain.Notification;
import com.bssm.meal.admin.repository.AppDownloadRepository;
import com.bssm.meal.admin.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final NotificationRepository notificationRepository;
    private final AppDownloadRepository appDownloadRepository;

    // 유저별 SSE 연결 저장소
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // application.yml의 설정을 읽어오며, 설정이 없으면 실행 경로의 uploads/ 폴더 사용
    @Value("${file.upload-base-dir:uploads/}")
    private String baseUploadPath;

    /**
     * 1. 앱 파일 업로드 전용 기능 (APK, IPA)
     */
    public String uploadAppFile(String type, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("파일이 없습니다.");

        String subPath = type.toLowerCase();
        String fileName = "BSSM_Meal_Latest." + subPath;

        try {
            Path directory = Paths.get(baseUploadPath, subPath).toAbsolutePath().normalize();
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            Path targetPath = directory.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("앱 파일 업로드 성공: {}", targetPath);
            return "/uploads/" + subPath + "/" + fileName;
        } catch (IOException e) {
            log.error("앱 파일 저장 실패", e);
            throw new RuntimeException("앱 파일 저장 중 오류 발생");
        }
    }

    /**
     * 2. 앱 다운로드 시 카운트 증가 및 경로 반환
     */
    @Transactional
    public String getDownloadUrlAndCount(String type) {
        String upperType = type.toUpperCase();

        // DB에서 해당 플랫폼 통계 조회 (없으면 생성)
        AppDownloadStats stats = appDownloadRepository.findById(upperType)
                .orElseGet(() -> new AppDownloadStats(upperType));

        // 카운트 증가 및 시간 업데이트
        stats.increment();
        appDownloadRepository.save(stats);

        // 실제 파일로 리다이렉트할 경로 반환
        return "/uploads/" + type.toLowerCase() + "/BSSM_Meal_Latest." + type.toLowerCase();
    }

    /**
     * 3. 모든 플랫폼 앱 다운로드 통계 조회
     */
    @Transactional(readOnly = true)
    public List<AppDownloadStats> getAllDownloadStats() {
        return appDownloadRepository.findAll();
    }

    /* --- 기존 공지사항 로직 --- */

    @Transactional(readOnly = true)
    public Notification getLatestNotice() {
        return notificationRepository.findFirstByTypeOrderByCreatedAtDesc("ALARM").orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Notification> getAllNotices() {
        return notificationRepository.findAllByTypeOrderByCreatedAtDesc("NOTICE");
    }

    @Transactional(readOnly = true)
    public Notification getNoticeById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다. ID: " + id));
    }

    @Transactional
    public Notification saveNoticeWithFile(String title, String content, MultipartFile file, boolean sendAlert, String type) {
        String imageUrl = saveNoticeFile(file);

        Notification notice = Notification.builder()
                .title(title)
                .content(content)
                .imageUrl(imageUrl)
                .type(type)
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotice = notificationRepository.save(notice);

        if (sendAlert) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendSseNotification(savedNotice);
                }
            });
        }

        return savedNotice;
    }

    @Transactional
    public Notification updateNoticeWithFile(Long id, String title, String content, MultipartFile file) {
        Notification notice = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("수정할 게시글이 존재하지 않습니다. ID: " + id));

        notice.setTitle(title);
        notice.setContent(content);

        if (file != null && !file.isEmpty()) {
            deleteActualFile(notice.getImageUrl());
            String newImageUrl = saveNoticeFile(file);
            notice.setImageUrl(newImageUrl);
        }

        return notice;
    }

    @Transactional
    public void deleteNotice(Long id) {
        Notification notice = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 게시글이 존재하지 않습니다. ID: " + id));

        deleteActualFile(notice.getImageUrl());
        notificationRepository.delete(notice);
    }

    /**
     * 공지사항 이미지 저장
     */
    private String saveNoticeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        try {
            Path directory = Paths.get(baseUploadPath, "notices").toAbsolutePath().normalize();
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            String originalFileName = file.getOriginalFilename();
            String extension = (originalFileName != null && originalFileName.contains("."))
                    ? originalFileName.substring(originalFileName.lastIndexOf(".")) : "";

            String savedFileName = UUID.randomUUID().toString() + extension;
            Path targetPath = directory.resolve(savedFileName);

            Files.copy(file.getInputStream(), targetPath);

            return "/uploads/notices/" + savedFileName;
        } catch (IOException e) {
            log.error("공지사항 이미지 저장 실패", e);
            throw new RuntimeException("이미지 저장 중 오류 발생");
        }
    }

    private void deleteActualFile(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) return;

        try {
            String relativePath = imageUrl.substring(9);
            Path filePath = Paths.get(baseUploadPath).resolve(relativePath).toAbsolutePath().normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("파일 삭제 실패: {}", imageUrl);
        }
    }

    /* --- SSE 관련 로직 --- */

    public SseEmitter subscribe(Long userId) {
        SseEmitter oldEmitter = emitters.remove(userId);
        if (oldEmitter != null) {
            oldEmitter.complete();
        }

        SseEmitter emitter = new SseEmitter(30L * 60 * 1000);

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected!")
                    .comment("heartbeat"));
        } catch (IOException e) {
            log.error("SSE 연결 초기화 실패", e);
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

    private void sendSseNotification(Notification notice) {
        if (emitters.isEmpty()) return;

        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(notice.getId().toString())
                        .name("notice")
                        .data(notice));
            } catch (Exception e) {
                emitter.complete();
                emitters.remove(id);
            }
        });
    }
}