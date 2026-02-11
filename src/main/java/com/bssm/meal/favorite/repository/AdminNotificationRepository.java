package com.bssm.meal.favorite.repository;

import com.bssm.meal.favorite.entity.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {
    // 아직 발송되지 않았고, 예약 시간이 현재 시간보다 이전인 알림 조회
    List<AdminNotification> findBySentFalseAndScheduledTimeBefore(LocalDateTime time);

    List<AdminNotification> findTop10ByOrderByCreatedAtDesc();
}