package com.bssm.meal.admin.service;

import com.bssm.meal.admin.dto.NoticeRequest;
import com.bssm.meal.admin.domain.Notification;
import com.bssm.meal.admin.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // ✅ 이미지가 저장될 경로 (프로젝트 루트의 uploads/notices 폴더)
    private final String uploadPath = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "notices" + File.separator;

    public Notification getLatestNotice() {
        return notificationRepository.findFirstByOrderByIdDesc().orElse(null);
    }

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(60L * 1000 * 60);
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        } catch (IOException e) {
            return null;
        }
        emitters.put(userId, emitter);
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));
        return emitter;
    }

    /**
     * ✅ 수정됨: MultipartFile을 인자로 받아 이미지를 저장하고 공지를 전송합니다.
     */
    public void sendGlobalNotice(String title, String content, MultipartFile file) {
        String imageUrl = null;

        // 1. 파일 저장 로직
        if (file != null && !file.isEmpty()) {
            try {
                // 폴더가 없으면 생성
                File folder = new File(uploadPath);
                if (!folder.exists()) folder.mkdirs();

                // 파일명 중복 방지를 위해 UUID 사용
                String originalName = file.getOriginalFilename();
                String fileName = UUID.randomUUID().toString() + "_" + originalName;
                Path path = Paths.get(uploadPath + fileName);

                // 로컬 드라이브에 저장
                Files.write(path, file.getBytes());

                // 브라우저에서 접근할 URL 경로 설정
                imageUrl = "/uploads/notices/" + fileName;
            } catch (IOException e) {
                throw new RuntimeException("이미지 저장 중 오류 발생", e);
            }
        }

        // 2. DB 저장
        Notification notice = Notification.builder()
                .title(title)
                .content(content)
                .imageUrl(imageUrl) // ✅ 이미지 경로 저장
                .build();
        Notification savedNotice = notificationRepository.save(notice);

        // 3. SSE 전송
        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("notice")
                        .data(savedNotice));
            } catch (IOException e) {
                emitters.remove(id);
            }
        });
    }
}