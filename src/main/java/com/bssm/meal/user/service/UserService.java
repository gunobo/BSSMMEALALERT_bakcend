package com.bssm.meal.user.service;

import com.bssm.meal.comment.repository.CommentRepository;
import com.bssm.meal.like.repository.LikeRepository;
import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.dto.UserResponse;
import com.bssm.meal.user.repository.DeleteRequestRepository;
import com.bssm.meal.user.repository.UserRepository;
import com.bssm.meal.user.domain.DeleteRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final DeleteRequestRepository deleteRequestRepository;

    public UserService(UserRepository userRepository,
                       LikeRepository likeRepository,
                       CommentRepository commentRepository,
                       DeleteRequestRepository deleteRequestRepository) {
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.deleteRequestRepository = deleteRequestRepository;
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User saveOrUpdate(String email, String name, String picture) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    user.setName(name);
                    user.setPicture(picture);
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    return userRepository.save(User.builder()
                            .email(email)
                            .name(name)
                            .picture(picture)
                            .build());
                });
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * ✅ 유저 정보 업데이트 (알레르기, 선호메뉴, 알림 설정)
     */
    @Transactional
    public void updateUserInfo(String email, UserResponse request) {
        log.info("📢 유저 정보 업데이트 시작 - 대상 이메일: {}", email);
        log.info("📢 요청 데이터 -> 알림허용: {}, 선호메뉴: {}, 알레르기: {}",
                request.isAllow_notifications(), request.getFavoriteMenus(), request.getAllergies());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // User 엔티티의 updateInfo 메서드 사용
        user.updateInfo(
                request.getAllergies(),
                request.getFavoriteMenus(),
                request.isAllow_notifications(),
                request.isAllow_allergy_notifications(),
                request.isAllow_favorite_notifications()
        );

        userRepository.save(user);
        log.info("✅ 유저 정보 업데이트 완료 - 현재 DB 저장 값(알림): {}", user.isAllowNotifications());
    }

    /**
     * ✅ 사용자 존재 확인
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * ✅ 계정 및 모든 데이터 삭제 (트랜잭션)
     * ⚠️ Comment는 username 필드를 사용하므로 user.getName() 전달
     */
    @Transactional
    public void deleteUserAndAllData(String email) {
        log.info("🗑️ 계정 삭제 시작 - 사용자: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Long userId = user.getId();
        String username = user.getName(); // ✅ Comment 테이블은 username 컬럼 사용

        try {
            // 1. user_allergies 테이블 데이터 자동 삭제 (orphanRemoval)
            user.getAllergies().clear();

            // 2. user_favorite_menus 테이블 데이터 자동 삭제 (orphanRemoval)
            user.getFavoriteMenus().clear();

            // 3. 좋아요 삭제 (MealLike는 userId 사용)
            int likeCount = likeRepository.deleteByUserId(String.valueOf(userId));
            log.info("  ✓ 좋아요 삭제: {} 개", likeCount);

            // 4. 댓글 삭제 (Comment는 username 사용)
            int commentCount = commentRepository.deleteByUserId(username);
            log.info("  ✓ 댓글 삭제: {} 개", commentCount);

            // 5. FCM 토큰 필드 null 처리
            user.setFcmTokenMobile(null);
            user.setFcmTokenWeb(null);
            log.info("  ✓ FCM 토큰 삭제 완료");

            // 6. 변경사항 flush
            userRepository.flush();

            // 7. 사용자 계정 삭제
            userRepository.delete(user);
            log.info("  ✓ 사용자 계정 삭제 완료");

            log.info("✅ 계정 삭제 완료 - 사용자: {}, 삭제된 데이터: 좋아요 {}, 댓글 {}",
                    email, likeCount, commentCount);

        } catch (Exception e) {
            log.error("❌ 계정 삭제 중 오류 발생 - 사용자: {}", email, e);
            throw new RuntimeException("계정 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * ✅ 계정 삭제 요청 저장
     */
    @Transactional
    public void createDeleteRequest(String email, String reason) {
        DeleteRequest request = DeleteRequest.builder()
                .email(email)
                .reason(reason)
                .status("PENDING")
                .requestedAt(LocalDateTime.now())
                .build();

        deleteRequestRepository.save(request);
        log.info("📧 계정 삭제 요청 저장 완료 - 이메일: {}", email);
    }

    /**
     * ✅ 사용자 정보 조회 (삭제 전 확인용)
     * ⚠️ Like는 userId, Comment는 username 사용
     */
    public Map<String, Object> getUserInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Map<String, Object> info = new HashMap<>();

        // 기본 정보
        info.put("email", user.getEmail());
        info.put("name", user.getName());
        info.put("picture", user.getPicture());

        // 통계 정보
        // ⚠️ 좋아요는 userId(Long을 String으로), 댓글은 username(name)
        int likeCount = likeRepository.countByUserId(String.valueOf(user.getId()));
        int commentCount = commentRepository.countByUserId(user.getName()); // ✅ username 사용

        info.put("likeCount", likeCount);
        info.put("commentCount", commentCount);

        // 사용자 설정
        info.put("favoriteMenus", user.getFavoriteMenus());
        info.put("allergies", user.getAllergies());
        info.put("allowNotifications", user.getAllowNotifications());
        info.put("allowAllergyNotifications", user.getAllowAllergyNotifications());
        info.put("allowFavoriteNotifications", user.getAllowFavoriteNotifications());

        return info;
    }

    /**
     * ✅ 관리자: 계정 삭제 요청 목록 조회
     */
    public List<DeleteRequest> getPendingDeleteRequests() {
        return deleteRequestRepository.findByStatusOrderByRequestedAtDesc("PENDING");
    }

    /**
     * ✅ 관리자: 계정 삭제 요청 처리
     */
    @Transactional
    public void processDeleteRequest(Long requestId, boolean approve) {
        DeleteRequest request = deleteRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("삭제 요청을 찾을 수 없습니다."));

        if (approve) {
            // 사용자 계정 삭제
            if (userRepository.existsByEmail(request.getEmail())) {
                deleteUserAndAllData(request.getEmail());
            }
            request.setStatus("PROCESSED");
        } else {
            request.setStatus("REJECTED");
        }

        request.setProcessedAt(LocalDateTime.now());
        deleteRequestRepository.save(request);

        log.info("✅ 삭제 요청 처리 완료 - ID: {}, 승인: {}", requestId, approve);
    }
}