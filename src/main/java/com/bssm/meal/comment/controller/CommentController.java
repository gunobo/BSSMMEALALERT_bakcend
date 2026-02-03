package com.bssm.meal.comment.controller;

import com.bssm.meal.comment.dto.CommentDto;
import com.bssm.meal.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 특정 식단의 댓글 목록 조회
     */
    @GetMapping
    public List<CommentDto> getComments(
            @RequestParam String mealDate,
            @RequestParam String mealType,
            @RequestParam String mealKey) {
        return commentService.findComments(mealDate, mealType, mealKey);
    }

    /**
     * 댓글 저장 (로그인 유저)
     */
    @PostMapping
    public ResponseEntity<String> saveComment(@RequestBody CommentDto dto, Authentication authentication) {
        // authentication.getName()은 유저의 이메일을 반환함
        commentService.save(dto, authentication.getName());
        return ResponseEntity.ok("success");
    }

    /**
     * 관리자용 댓글 검색 및 필터링 조회
     * 프론트엔드 AdminCommentManager.jsx에서 보낸 파라미터를 받습니다.
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<CommentDto> getAllCommentsForAdmin(
            @RequestParam(required = false) String type,       // username 또는 email
            @RequestParam(required = false) String keyword,    // 검색어
            @RequestParam(required = false) String mealDate,   // 날짜 (20260203)
            @RequestParam(required = false) String mealType,   // 식사 구분 (조식/중식/석식)
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // 검색 조건이 있으면 검색 서비스를 호출하고, 없으면 전체 조회를 수행하도록 Service에서 처리
        return commentService.searchComments(type, keyword, mealDate, mealType, pageable);
    }

    /**
     * 관리자 전용 댓글 삭제
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.ok("삭제되었습니다.");
    }
}