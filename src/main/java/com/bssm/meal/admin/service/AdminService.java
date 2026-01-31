package com.bssm.meal.admin.service;

import com.bssm.meal.admin.dto.AdminStatsResponse;
import com.bssm.meal.like.repository.LikeRepository;
import com.bssm.meal.user.repository.UserRepository;
import com.bssm.meal.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final ReportRepository reviewRepository;

    /**
     * 리액트 관리자 페이지(AdminPage)에서 필요한 모든 통계 데이터를 한 번에 조회
     */
    public AdminStatsResponse getOverallStats() {
        // 1. 전체 사용자 수 조회
        long totalUsers = userRepository.count();

        // 2. 오늘의 좋아요 수 조회 (오늘 00:00:00 이후 생성된 데이터 기준)
        long todayLikes = likeRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay());

        // 3. 신고된 리뷰 목록 가공 (userEmail 필드 추가 매핑)
        var reportedReviews = reviewRepository.findByIsReportedTrue().stream()
                .map(r -> AdminStatsResponse.ReportDto.builder()
                        .id(r.getId())
                        .userName(r.getUser().getName())
                        .userEmail(r.getUser().getEmail()) // ✅ 이메일 정보 추가
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

        // 최종 DTO 조립
        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .todayLikes(todayLikes)
                .reportedReviews(reportedReviews)
                .popularMenus(popularMenus)
                .build();
    }
    @Transactional
    public void processReport(Long reportId) {
        // 1. 엔티티 조회 (reviewRepository가 관리하는 엔티티에 따라 수정)
        var review = reviewRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("해당 신고 내역이 없습니다."));

        // 2. 상태 변경 (Setter가 있으면 setIsReported, 없으면 만든 메서드 호출)
        review.setIsReported(false);
        // 또는 review.updateReportStatus(false);

        // ✅ @Transactional이 붙어있으므로 별도의 save() 호출 없이도 DB에 반영됩니다(Dirty Checking).
    }
}