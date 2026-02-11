package com.bssm.meal.admin.service;

import com.bssm.meal.admin.dto.AdminStatsResponse;
import com.bssm.meal.admin.dto.UserDetailResponse;
import com.bssm.meal.comment.repository.CommentRepository;
import com.bssm.meal.like.repository.LikeRepository;
import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.repository.UserRepository;
import com.bssm.meal.report.repository.ReportRepository;
import com.bssm.meal.favorite.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private final CommentRepository commentRepository;
    private final EmailService emailService;
    private final FcmTokenRepository fcmTokenRepository;

    /**
     * 관리자 페이지용 전체 사용자 상세 목록 조회
     */
    public List<UserDetailResponse> getAllUsersForAdmin() {
        return userRepository.findAll().stream()
                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());
    }

    /**
     * 유저 검색 및 필터링 조회 (추가된 메서드)
     */
    public List<UserDetailResponse> getUsersByFilter(String type, String keyword) {
        // 검색어가 없으면 전체 조회
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllUsersForAdmin();
        }

        // 검색 타입에 따라 이름 혹은 이메일 변수 설정
        String name = "username".equals(type) ? keyword : null;
        String email = "email".equals(type) ? keyword : null;

        // UserRepository에 추가한 findUsersByFilter 호출
        return userRepository.findUsersByFilter(name, email).stream()
                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());
    }

    /**
     * User 엔티티를 UserDetailResponse DTO로 변환하는 공통 로직
     */
    private UserDetailResponse convertToDetailResponse(User user) {
        return UserDetailResponse.builder()
                .id(String.valueOf(user.getId()))
                .email(user.getEmail())
                .userName(user.getName())
                .googleId(user.getGoogleId())
                .picture(user.getPicture())
                .banned(user.isBanned())
                .banReason(user.getBanReason())
                .banExpiresAt(user.getBanExpiresAt())
                .allergies(Optional.ofNullable(user.getAllergies()).orElse(Collections.emptyList()))
                .favoriteMenus(Optional.ofNullable(user.getFavoriteMenus()).orElse(Collections.emptyList()))
                .build();
    }

    /**
     * 리액트 관리자 페이지용 종합 통계 데이터 조회
     */
    public AdminStatsResponse getOverallStats() {
        // 기본 카운트 통계
        long totalUsers = userRepository.count();
        long todayLikes = likeRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay());
        long totalComments = commentRepository.count();

        // 1. 신고 내역 조회
        var reportedReviews = reviewRepository.findByIsReportedTrue().stream()
                .filter(r -> r.getUser() != null)
                .map(r -> AdminStatsResponse.ReportDto.builder()
                        .id(r.getId())
                        .userName(r.getUser().getName())
                        .userEmail(r.getUser().getEmail())
                        .content(r.getContent())
                        .reason(r.getReportReason())
                        .build())
                .collect(Collectors.toList());

        // 2. 인기 메뉴 TOP 5
        var popularMenus = likeRepository.findTop5PopularMenus(PageRequest.of(0, 5))
                .stream()
                .map(r -> AdminStatsResponse.MenuStatsDto.builder()
                        .name(r.getMealKey())
                        .date(r.getMealDate())
                        .type(r.getMealType())
                        .votes(r.getLikeCount())
                        .build())
                .collect(Collectors.toList());

        // 3. 댓글 활성 메뉴 TOP 5
        var topCommentedMenus = commentRepository.findTopCommentedMenus(PageRequest.of(0, 5))
                .stream()
                .map(r -> AdminStatsResponse.CommentRankingDto.builder()
                        .mealKey(r.getMealKey())
                        .count(r.getCount())
                        .build())
                .collect(Collectors.toList());

        // 4. 주간 피드백 추이 데이터 생성
        List<AdminStatsResponse.DailyStatsDto> dailyStats = getWeeklyTrendData();

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .todayLikes(todayLikes)
                .totalComments(totalComments)
                .reportedReviews(reportedReviews)
                .popularMenus(popularMenus)
                .topCommentedMenus(topCommentedMenus)
                .dailyStats(dailyStats)
                .build();
    }

    /**
     * 최근 7일간의 요일별 데이터 추이 계산
     */
    private List<AdminStatsResponse.DailyStatsDto> getWeeklyTrendData() {
        List<AdminStatsResponse.DailyStatsDto> stats = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String[] dayNames = {"", "월", "화", "수", "목", "금", "토", "일"};

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateStr = date.format(formatter);
            String dayLabel = dayNames[date.getDayOfWeek().getValue()];

            long commentCount = 0;
            long likeCount = 0;

            try {
                commentCount = commentRepository.countByMealDate(dateStr);
                likeCount = likeRepository.countByMealDate(dateStr);
            } catch (Exception e) {
                log.warn("데이터 집계 실패 - 날짜: {} , 사유: {}", dateStr, e.getMessage());
            }

            stats.add(AdminStatsResponse.DailyStatsDto.builder()
                    .dayOfWeek(dayLabel + "(" + date.getDayOfMonth() + "일)")
                    .commentCount(commentCount)
                    .likeCount(likeCount)
                    .build());
        }
        return stats;
    }

    /**
     * 신고 상태 해제 (반려 처리)
     */
    @Transactional
    public void processReport(Long reportId) {
        reviewRepository.findById(reportId)
                .ifPresentOrElse(
                        r -> r.setIsReported(false),
                        () -> { throw new IllegalArgumentException("해당 신고 내역이 없습니다."); }
                );
    }

    /**
     * 신고 내역 및 관련 데이터 삭제
     */
    @Transactional
    public void deleteReport(Long reportId) {
        if (!reviewRepository.existsById(reportId)) {
            throw new IllegalArgumentException("삭제할 신고 내역이 존재하지 않습니다.");
        }
        reviewRepository.deleteById(reportId);
    }

    /**
     * 사용자 차단 관리 및 이메일 알림 자동화
     */
    @Transactional
    public void updateUserBannedStatus(String email, boolean status, String reason, Integer min) {
        log.info("차단 업데이트 요청 - 유저: {}, 상태: {}, 기간: {}분", email, status, min);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        if (status) {
            LocalDateTime expiresAt = (min != null && min > 0) ? LocalDateTime.now().plusMinutes(min) : null;
            user.updateBannedStatus(true, reason, expiresAt);

            try {
                emailService.sendBanNotification(email, reason, expiresAt);
            } catch (Exception e) {
                log.error("차단 안내 메일 발송 실패: {}", e.getMessage());
            }
        } else {
            user.updateBannedStatus(false, null, null);

            try {
                emailService.sendUnbanNotification(email);
            } catch (Exception e) {
                log.error("차단 해제 메일 발송 실패: {}", e.getMessage());
            }
        }
        userRepository.save(user);
    }

    @Transactional
    public void forceLogoutUser(Long userId) {
        // 1. 유저 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 2. FCM 토큰 삭제 (알림 차단)
        fcmTokenRepository.deleteByUserId(userId);

        // 3. (중요) Redis를 사용 중이라면 유저의 RefreshToken 삭제
        // redisTemplate.delete("RT:" + user.getEmail());

        log.info("🚫 유저 {} 강제 로그아웃 처리 완료", user.getEmail());
    }
}