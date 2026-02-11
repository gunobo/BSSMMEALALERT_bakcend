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
                        .username(c.getUsername())
                        .email(c.getEmail())
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
     * 프론트에서 넘어온 검색 조건에 따라 분기 처리하여 Repository 호출
     */
    @Transactional(readOnly = true)
    public Page<CommentDto> searchComments(String type, String keyword, String mealDate, String mealType, Pageable pageable) {
        // 검색어(keyword)가 비어있으면 null로 처리하여 쿼리에서 무시되도록 함
        String searchKeyword = (keyword == null || keyword.trim().isEmpty()) ? null : keyword;

        // type 값에 따라 이름(username) 또는 이메일(email) 필터 결정
        String username = "username".equals(type) ? searchKeyword : null;
        String email = "email".equals(type) ? searchKeyword : null;

        // 리포지토리에서 @Query로 작성한 동적 필터 메서드 호출
        Page<Comment> commentPage = commentRepository.findAdminComments(
                username,
                email,
                (mealDate == null || mealDate.isEmpty()) ? null : mealDate,
                (mealType == null || mealType.isEmpty()) ? null : mealType,
                pageable
        );

        // 엔티티 페이지를 DTO 페이지로 변환
        return commentPage.map(c -> CommentDto.builder()
                .id(c.getId())
                .content(c.getContent())
                .mealDate(c.getMealDate())
                .mealType(c.getMealType())
                .username(c.getUsername())
                .email(c.getEmail())
                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : "")
                .build());
    }

    /**
     * 관리자용: 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long id) {
        commentRepository.deleteById(id);
    }

    /**
     * 통계 기능
     */
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