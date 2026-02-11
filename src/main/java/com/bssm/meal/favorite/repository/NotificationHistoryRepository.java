package com.bssm.meal.favorite.repository;

import com.bssm.meal.favorite.entity.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {
    List<NotificationHistory> findTop10ByOrderBySentAtDesc();
    // 기본 저장 기능(save)을 상속받습니다.
}