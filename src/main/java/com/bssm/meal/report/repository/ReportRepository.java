package com.bssm.meal.report.repository;

import com.bssm.meal.report.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // ✅ AdminService에서 호출하는 메서드: 신고된 리스트 전체 조회
    // 서비스 코드에서 reviewRepository.findByIsReportedTrue()를 쓰기로 했으므로 추가합니다.
    List<Report> findByIsReportedTrue();

    // 최신 신고순으로 정렬이 필요한 경우 (추천)
    List<Report> findByIsReportedTrueOrderByIdDesc();

    // 기존에 사용하던 전체 조회 메서드 (필요시 유지)
    List<Report> findAllByOrderByIdDesc();
}