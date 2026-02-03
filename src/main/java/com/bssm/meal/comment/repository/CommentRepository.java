package com.bssm.meal.comment.repository;

import com.bssm.meal.admin.dto.AdminStatsResponse;
import com.bssm.meal.comment.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 특정 급식 메뉴에 달린 댓글을 최신순으로 가져옴
    List<Comment> findByMealDateAndMealTypeAndMealKeyOrderByCreatedAtDesc(
            String mealDate, String mealType, String mealKey
    );

    // 오늘 작성된 댓글 수
    long countByCreatedAtAfter(LocalDateTime startOfDay);

    // ✅ [추가] 요일별 추이 차트를 위한 날짜별 카운트 (AdminService 121라인 에러 해결)
    long countByMealDate(String mealDate);

    // ✅ [추가] 관리자 댓글 관리 페이지의 필터 검색 (CommentService 77라인 에러 해결)
    @Query("SELECT c FROM Comment c WHERE " +
            "(:mealDate IS NULL OR :mealDate = '' OR c.mealDate = :mealDate) AND " +
            "(:mealType IS NULL OR :mealType = '' OR c.mealType = :mealType) AND " +
            "( " +
            "  (:type = 'username' AND c.username LIKE %:keyword%) OR " +
            "  (:type = 'email' AND c.email LIKE %:keyword%) OR " +
            "  (:keyword IS NULL OR :keyword = '') " +
            ")")
    Page<Comment> findAllWithFilters(
            @Param("type") String type,
            @Param("keyword") String keyword,
            @Param("mealDate") String mealDate,
            @Param("mealType") String mealType,
            Pageable pageable);

    // 메뉴별 댓글 순위 (가장 피드백이 많은 메뉴)
    @Query("SELECT c.mealKey, COUNT(c) FROM Comment c GROUP BY c.mealKey ORDER BY COUNT(c) DESC")
    List<Object[]> findTopMealFeedback(Pageable pageable);

    Page<Comment> findByUsernameContainingOrderByCreatedAtDesc(String username, Pageable pageable);
    Page<Comment> findByEmailContainingOrderByCreatedAtDesc(String email, Pageable pageable);
    Page<Comment> findByMealDateAndMealTypeOrderByCreatedAtDesc(String mealDate, String mealType, Pageable pageable);

    Page<Comment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // ✅ 통계용: 댓글 순위 DTO 매핑
    @Query("SELECT new com.bssm.meal.admin.dto.AdminStatsResponse$CommentRankingDto(c.mealKey, COUNT(c)) " +
            "FROM Comment c " +
            "GROUP BY c.mealKey " +
            "ORDER BY COUNT(c) DESC")
    List<AdminStatsResponse.CommentRankingDto> findTopCommentedMenus(Pageable pageable);
}