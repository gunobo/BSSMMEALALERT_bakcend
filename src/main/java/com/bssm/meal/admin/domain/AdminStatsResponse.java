package com.bssm.meal.admin.domain;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class AdminStatsResponse {
    private long totalUsers;
    private long todayLikes;
    private List<ReportedReviewResponse> reportedReviews;
    private List<PopularMenuResponse> popularMenus;

    @Getter
    @Builder
    public static class ReportedReviewResponse {
        private Long id;
        private String userName;
        private String userEmail;
        private String content;
        private String reason;
    }

    @Getter
    @Builder
    public static class PopularMenuResponse {
        private String name;
        private long votes;
    }
}