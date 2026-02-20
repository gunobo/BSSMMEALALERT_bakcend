package com.bssm.meal.user.repository;

import com.bssm.meal.user.domain.DeleteRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeleteRequestRepository extends JpaRepository<DeleteRequest, Long> {
    /**
     * 상태별 삭제 요청 조회 (최신순)
     * ✅ DeleteRequest 엔티티에는 status 필드가 있음
     */
    // ✅ DeleteRequest 엔티티의 필드만 사용
    List<DeleteRequest> findByStatusOrderByRequestedAtDesc(String status);
    List<DeleteRequest> findByEmailOrderByRequestedAtDesc(String email);
    long countByRequestedAtBetween(LocalDateTime start, LocalDateTime end);
}