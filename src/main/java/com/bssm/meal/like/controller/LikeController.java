package com.bssm.meal.like.controller;

import com.bssm.meal.like.entity.MealLike;
import com.bssm.meal.like.dto.RankingResponse;
import com.bssm.meal.like.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime; // 추가
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class LikeController {

    private final LikeRepository likeRepository;

    /**
     * ✅ 1. 좋아요 토글
     */
    @PostMapping("/toggle")
    @Transactional // 쓰기 작업이므로 일반 트랜잭션
    public ResponseEntity<String> toggleLike(@RequestBody MealLike request) {
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("userId is required");
        }

        Optional<MealLike> existing = likeRepository.findByUserIdAndMealDateAndMealTypeAndMealKey(
                request.getUserId(), request.getMealDate(), request.getMealType(), request.getMealKey());

        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            return ResponseEntity.ok("unliked");
        } else {
            // 저장 시점 시간 명시 (엔티티에 PrePersist가 없다면 유용)
            request.setCreatedAt(LocalDateTime.now());
            likeRepository.save(request);
            return ResponseEntity.ok("liked");
        }
    }

    /**
     * ✅ 2. 특정 유저의 좋아요 목록 조회 (객체 리스트 반환)
     */
    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MealLike>> getUserLikes(@PathVariable String userId) { // String -> MealLike
        try {
            if (userId == null || userId.equals("undefined") || userId.equals("null")) {
                return ResponseEntity.ok(List.of());
            }
            // findLikedKeysByUserId 대신 엔티티 전체를 가져오는 메소드 사용
            List<MealLike> likes = likeRepository.findByUserId(userId);
            return ResponseEntity.ok(likes);
        } catch (Exception e) {
            System.err.println("❌ User Likes Query Error: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ✅ 3. 실시간 랭킹 TOP 5 조회
     */
    @GetMapping("/ranking")
    @Transactional(readOnly = true)
    public ResponseEntity<List<RankingResponse>> getRanking() {
        try {
            // PageRequest를 통해 DB 부하 최소화
            List<RankingResponse> ranking = likeRepository.findTopRanking(PageRequest.of(0, 5));
            return ResponseEntity.ok(ranking);
        } catch (Exception e) {
            System.err.println("❌ Ranking Query Error: " + e.getMessage());
            // 타임아웃 방지를 위해 빈 리스트라도 반환하여 프론트엔드가 멈추지 않게 함
            return ResponseEntity.ok(List.of());
        }
    }
}