package com.bssm.meal.admin.repository;

import com.bssm.meal.admin.domain.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    // 최근 발송된 순서대로 상위 10개를 가져오는 메서드 (관리자 대시보드용)
    List<NotificationLog> findTop10ByOrderBySentAtDesc();
}