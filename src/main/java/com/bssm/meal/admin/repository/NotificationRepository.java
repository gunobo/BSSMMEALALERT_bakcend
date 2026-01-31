package com.bssm.meal.admin.repository;

import com.bssm.meal.admin.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 최신 알림을 생성일(createdAt) 역순으로 정렬하여 가져옵니다.
     * 프론트엔드 메인 페이지나 공지사항 목록에서 사용됩니다.
     */
    List<Notification> findAllByOrderByCreatedAtDesc();

    /**
     * (선택사항) 가장 최근의 공지사항 딱 1개만 가져오고 싶을 때 사용합니다.
     */
    Notification findFirstByOrderByCreatedAtDesc();
    Optional<Notification> findFirstByOrderByIdDesc();
}