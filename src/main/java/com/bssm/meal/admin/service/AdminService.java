package com.bssm.meal.admin.service;

import com.bssm.meal.admin.dto.AdminStatsResponse;
import com.bssm.meal.admin.dto.UserDetailResponse; // ✅ DTO 추가
import com.bssm.meal.like.repository.LikeRepository;
import com.bssm.meal.user.repository.UserRepository;
import com.bssm.meal.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final ReportRepository reviewRepository; // (변수명은 유지하되 타입은 ReportRepository)

    /**
     * ✅ [추가] 관리자 페이지용 전체 사용자 상세 목록 조회
     */
    public List<UserDetailResponse> getAllUsersForAdmin() {
        return userRepository.findAll().stream()
                .map(user -> UserDetailResponse.builder()
                        .id(user.getUserId()) // 또는 user.getId() (엔티티 필드명에 맞춰 수정)
                        .email(user.getEmail())
                        // 리스트가 null일 경우 프론트 map 에러 방지를 위해 빈 리스트 처리
                        .allergies(Optional.ofNullable(user.getAllergies()).orElse(Collections.emptyList()))
                        .favoriteMenus(Optional.ofNullable(user.getFavoriteMenus()).orElse(Collections.emptyList()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 리액트 관리자 페이지(AdminPage)에서 필요한 모든 통계 데이터를 한 번에 조회
     */
    public AdminStatsResponse getOverallStats() {
        // 1. 전체 사용자 수 조회
        long totalUsers = userRepository.count();

        // 2. 오늘의 좋아요 수 조회
        long todayLikes = likeRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay());

        // 3. 신고된 리뷰 목록 가공
        var reportedReviews = reviewRepository.findByIsReportedTrue().stream()
                .map(r -> AdminStatsResponse.ReportDto.builder()
                        .id(r.getId())
                        .userName(r.getUser().getName())
                        .userEmail(r.getUser().getEmail())
                        .content(r.getContent())
                        .reason(r.getReportReason())
                        .build())
                .collect(Collectors.toList());

        // 4. 인기 메뉴 TOP 5 가공
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

    /**
     * 신고 처리 (상태 변경)
     */
    @Transactional
    public void processReport(Long reportId) {
        var review = reviewRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("해당 신고 내역이 없습니다."));
        review.setIsReported(false);
    }

    /**
     * ✅ [추가] 신고 게시글 삭제
     */
    @Transactional
    public void deleteReport(Long reportId) {
        if (!reviewRepository.existsById(reportId)) {
            throw new IllegalArgumentException("삭제할 신고 내역이 존재하지 않습니다.");
        }
        reviewRepository.deleteById(reportId);
    }
}