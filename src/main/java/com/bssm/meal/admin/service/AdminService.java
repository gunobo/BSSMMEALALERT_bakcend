package com.bssm.meal.admin.service;

import com.bssm.meal.admin.dto.AdminStatsResponse;
import com.bssm.meal.admin.dto.UserDetailResponse;
import com.bssm.meal.like.repository.LikeRepository;
import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.repository.UserRepository;
import com.bssm.meal.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final ReportRepository reviewRepository;
    private final EmailService emailService;

    /**
     * 관리자 페이지용 전체 사용자 상세 목록 조회
     */
    public List<UserDetailResponse> getAllUsersForAdmin() {
        return userRepository.findAll().stream()
                .map(user -> UserDetailResponse.builder()
                        .id(String.valueOf(user.getId()))
                        .email(user.getEmail())
                        // ✅ 이 부분이 누락되어 이름이 안 나왔던 것입니다!
                        // 유저 엔티티의 name(구글 이름)을 DTO의 userName에 담습니다.
                        .userName(user.getName())
                        .googleId(user.getGoogleId()) // ✅ DB의 google_id 매핑
                        .picture(user.getPicture())   // ✅ DB의 picture 매핑
                        .banned(user.isBanned())
                        .banReason(user.getBanReason())
                        .banExpiresAt(user.getBanExpiresAt())
                        .allergies(Optional.ofNullable(user.getAllergies()).orElse(Collections.emptyList()))
                        .favoriteMenus(Optional.ofNullable(user.getFavoriteMenus()).orElse(Collections.emptyList()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 리액트 관리자 페이지용 통계 데이터 조회
     */
    public AdminStatsResponse getOverallStats() {
        long totalUsers = userRepository.count();
        long todayLikes = likeRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay());

        var reportedReviews = reviewRepository.findByIsReportedTrue().stream()
                .map(r -> AdminStatsResponse.ReportDto.builder()
                        .id(r.getId())
                        .userName(r.getUser().getName())
                        .userEmail(r.getUser().getEmail())
                        .content(r.getContent())
                        .reason(r.getReportReason())
                        .build())
                .collect(Collectors.toList());

        var popularMenus = likeRepository.findTop5PopularMenus(PageRequest.of(0, 5))
                .stream()
                .map(r -> AdminStatsResponse.MenuStatsDto.builder()
                        .name(r.getMealKey())
                        .date(r.getMealDate())
                        .type(r.getMealType())
                        .votes(r.getLikeCount())
                        .build())
                .collect(Collectors.toList());

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .todayLikes(todayLikes)
                .reportedReviews(reportedReviews)
                .popularMenus(popularMenus)
                .build();
    }

    @Transactional
    public void processReport(Long reportId) {
        var review = reviewRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("해당 신고 내역이 없습니다."));
        review.setIsReported(false);
    }

    @Transactional
    public void deleteReport(Long reportId) {
        if (!reviewRepository.existsById(reportId)) {
            throw new IllegalArgumentException("삭제할 신고 내역이 존재하지 않습니다.");
        }
        reviewRepository.deleteById(reportId);
    }

    /**
     * ✅ 사용자 차단/해제 처리 및 메일 발송
     */
    @Transactional
    public void updateUserBannedStatus(String email, boolean status, String reason, Integer min) {
        log.info("차단 상태 변경 요청 - 대상: {}, 상태: {}, 사유: {}, 기간(분): {}", email, status, reason, min);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        if (status) {
            // 차단 설정
            LocalDateTime expiresAt = null;
            if (min != null && min > 0 && min < 999999) {
                expiresAt = LocalDateTime.now().plusMinutes(min);
            }

            user.updateBannedStatus(true, reason, expiresAt);

            try {
                emailService.sendBanNotification(email, reason, expiresAt);
            } catch (Exception e) {
                log.error("차단 메일 발송 중 오류 발생: {}", e.getMessage());
            }
        } else {
            // 차단 해제
            user.updateBannedStatus(false, null, null);

            try {
                emailService.sendUnbanNotification(email);
            } catch (Exception e) {
                log.error("해제 메일 발송 중 오류 발생: {}", e.getMessage());
            }
        }

        userRepository.save(user);
    }
}