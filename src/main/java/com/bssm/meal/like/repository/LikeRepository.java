package com.bssm.meal.like.repository;

import com.bssm.meal.like.entity.MealLike;
import com.bssm.meal.like.dto.RankingResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // 추가
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<MealLike, Long> {

    // [기존 1번] 좋아요 토글 및 중복 체크용
    Optional<MealLike> findByUserIdAndMealDateAndMealTypeAndMealKey(
            String userId, String mealDate, String mealType, String mealKey);

    // [기존 2번] 유저별 하트 키 조회
    @Query("SELECT CONCAT(l.mealDate, '_', l.mealType, '_', l.mealKey) " +
            "FROM MealLike l " +
            "WHERE l.userId = :userId AND l.userId IS NOT NULL")
    List<String> findLikedKeysByUserId(@Param("userId") String userId);

    // [기존 3번] 통계용: 특정 메뉴의 전체 좋아요 수 (MealService 에러 해결용)
    long countByMealKey(String mealKey);

    // [기존 4번] 특정 유저의 좋아요 여부
    boolean existsByUserIdAndMealDateAndMealTypeAndMealKey(
            String userId, String mealDate, String mealType, String mealKey);

    // [기존 5번] 실시간 랭킹 TOP 5
    @Query("SELECT new com.bssm.meal.like.dto.RankingResponse(l.mealDate, l.mealType, l.mealKey, COUNT(l)) " +
            "FROM MealLike l " +
            "GROUP BY l.mealDate, l.mealType, l.mealKey " +
            "ORDER BY COUNT(l) DESC")
    List<RankingResponse> findTopRanking(Pageable pageable);

    // [기존 6번] 관리자용: 오늘 생성된 전체 좋아요 수
    long countByCreatedAtAfter(LocalDateTime dateTime);

    // [기존 7번] 관리자용: 인기 메뉴 TOP 5
    @Query("SELECT new com.bssm.meal.like.dto.RankingResponse(l.mealDate, l.mealType, l.mealKey, COUNT(l)) " +
            "FROM MealLike l " +
            "GROUP BY l.mealDate, l.mealType, l.mealKey " +
            "ORDER BY COUNT(l) DESC")
    List<RankingResponse> findTop5PopularMenus(Pageable pageable);

    // -----------------------------------------------------------
    // ✨ [추가된 메서드들]
    // -----------------------------------------------------------

    /**
     * ✅ 성능 최적화용: 최근 n일간의 랭킹만 조회
     */
    @Query("SELECT new com.bssm.meal.like.dto.RankingResponse(l.mealDate, l.mealType, l.mealKey, COUNT(l)) " +
            "FROM MealLike l " +
            "WHERE l.createdAt >= :startDate " +
            "GROUP BY l.mealDate, l.mealType, l.mealKey " +
            "ORDER BY COUNT(l) DESC")
    List<RankingResponse> findRecentRanking(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    /**
     * ✅ 특정 날짜의 메뉴별 좋아요 수 조회 (Object[] 대신 DTO 활용 권장하나 일단 유지)
     */
    @Query("SELECT l.mealKey, COUNT(l) " +
            "FROM MealLike l " +
            "WHERE l.mealDate = :mealDate " +
            "GROUP BY l.mealKey")
    List<Object[]> countLikesByDate(@Param("mealDate") String mealDate);

    /**
     * ✅ 회원 탈퇴 처리 시 사용: 유저의 모든 좋아요 데이터 삭제
     * @Modifying 과 @Transactional(서비스단)이 필요합니다.
     */
    @Modifying
    @Query("DELETE FROM MealLike l WHERE l.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);

    /**
     * ✅ 유저 ID로 모든 좋아요 엔티티 조회
     */
    List<MealLike> findByUserId(String userId);
}