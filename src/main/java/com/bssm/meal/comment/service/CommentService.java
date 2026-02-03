package com.bssm.meal.comment.service;

import com.bssm.meal.comment.dto.CommentDto;
import com.bssm.meal.comment.domain.Comment;
import com.bssm.meal.comment.repository.CommentRepository;
import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    /**
     * 특정 식단의 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CommentDto> findComments(String mealDate, String mealType, String mealKey) {
        List<Comment> comments = commentRepository.findByMealDateAndMealTypeAndMealKeyOrderByCreatedAtDesc(
                mealDate, mealType, mealKey
        );

        return comments.stream()
                .map(c -> CommentDto.builder()
                        .id(c.getId())
                        .mealDate(c.getMealDate())
                        .mealType(c.getMealType())
                        .mealKey(c.getMealKey())
                        .content(c.getContent())
                        .username(c.getUsername()) // 엔티티에 저장된 값 직접 사용
                        .email(c.getEmail())       // 엔티티에 저장된 값 직접 사용
                        .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 댓글 저장
     */
    @Transactional
    public void save(CommentDto dto, String email) {
        String realName = userRepository.findByEmail(email)
                .map(User::getName)
                .orElse("익명");

        Comment comment = new Comment();
        comment.setMealDate(dto.getMealDate());
        comment.setMealType(dto.getMealType());
        comment.setMealKey(dto.getMealKey());
        comment.setContent(dto.getContent());
        comment.setUsername(realName);
        comment.setEmail(email);

        commentRepository.save(comment);
    }

    /**
     * 관리자용: 필터링 및 검색 조회
     */
    @Transactional(readOnly = true)
    public Page<CommentDto> searchComments(String type, String keyword, String mealDate, String mealType, Pageable pageable) {
        // 1. 리포지토리에서 필터링된 엔티티 페이지를 가져옴
        Page<Comment> commentPage = commentRepository.findAllWithFilters(type, keyword, mealDate, mealType, pageable);

        // 2. 엔티티를 DTO로 변환
        // comment.getUser().getName() 대신 엔티티의 getUsername()을 사용하는 것이 데이터 일관성에 좋습니다.
        return commentPage.map(c -> CommentDto.builder()
                .id(c.getId())
                .content(c.getContent())
                .mealDate(c.getMealDate())
                .mealType(c.getMealType())
                .username(c.getUsername()) // Comment 엔티티의 필드 사용
                .email(c.getEmail())       // Comment 엔티티의 필드 사용
                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : "")
                .build());
    }

    @Transactional
    public void deleteComment(Long id) {
        commentRepository.deleteById(id);
    }

    // 통계 기능 (필요시 AdminService와 통합 검토)
    @Transactional(readOnly = true)
    public Map<String, Object> getCommentStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", commentRepository.count());
        stats.put("todayCount", commentRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay()));

        List<Object[]> topMeals = commentRepository.findTopMealFeedback(PageRequest.of(0, 3));
        stats.put("topMeals", topMeals);

        return stats;
    }
}