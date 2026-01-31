package com.bssm.meal.report.dto;

import com.bssm.meal.report.domain.ReportType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportRequest {
    private String reason;    // 신고 사유
    private String content;   // 상세 내용
    private Long targetId;    // 대상 ID
    private String type;      // REVIEW 또는 MENU (String으로 받아 처리)
}