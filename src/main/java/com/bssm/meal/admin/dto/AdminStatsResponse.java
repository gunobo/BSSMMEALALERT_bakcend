package com.bssm.meal.admin.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class AdminStatsResponse {
    private long totalUsers;
    private long todayLikes;
    private long totalComments; // ✅ 추가: 누적 댓글 수
    private List<ReportDto> reportedReviews; // 신고 내역 DTO
    private List<MenuStatsDto> popularMenus; // 인기 메뉴 DTO
    private List<CommentRankingDto> topCommentedMenus; // ✅ 추가: 댓글 많은 메뉴 TOP 5

    @Getter
    @Builder
    public static class ReportDto {
        private Long id;
        private String userName;
        private String userEmail;
        private String content;
        private String reason;
    }

    @Getter
    @Builder
    public static class MenuStatsDto {
        private String name;
        private String date;
        private String type;
        private long votes;
    }

    // ✅ 추가: 댓글 순위를 담기 위한 내부 클래스
    @Getter
    @Builder
    public static class CommentRankingDto {
        private String mealKey; // 예: 20260203-LUNCH
        private long count;     // 댓글 개수
    }

    private List<DailyStatsDto> dailyStats; // ✅ 요일별 통계 데이터 추가

    @Getter
    @Builder
    public static class DailyStatsDto {
        private String dayOfWeek; // 월, 화, 수, 목, 금
        private long commentCount;
        private long likeCount;
    }
}