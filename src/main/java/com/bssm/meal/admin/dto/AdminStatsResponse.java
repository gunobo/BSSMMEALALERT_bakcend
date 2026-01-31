package com.bssm.meal.admin.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class AdminStatsResponse {
    private long totalUsers;
    private long todayLikes;
    private List<ReportDto> reportedReviews; // 신고 내역 DTO
    private List<MenuStatsDto> popularMenus; // 인기 메뉴 DTO

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
}