package com.bssm.meal.admin.repository;

import com.bssm.meal.admin.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 1. 특정 타입(NOTICE/ALARM)의 모든 공지를 최신순으로 조회
     * 게시판 목록에서 'NOTICE' 타입만 필터링할 때 사용합니다.
     */
    List<Notification> findAllByTypeOrderByCreatedAtDesc(String type);

    /**
     * 2. 특정 타입의 가장 최근 공지사항 딱 1개만 조회
     * 홈화면의 최신 공지 띠배너 등에서 사용됩니다.
     */
    Optional<Notification> findFirstByTypeOrderByCreatedAtDesc(String type);

    /**
     * 기존 메서드 유지 (필요 시 전체 조용)
     */
    List<Notification> findAllByOrderByCreatedAtDesc();

    /**
     * ID 기준 최신 단건 조회
     */
    Optional<Notification> findFirstByOrderByIdDesc();

//    long countByMealDate(String mealDate);
}