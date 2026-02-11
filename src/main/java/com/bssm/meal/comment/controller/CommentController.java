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

    @GetMapping
    public List<CommentDto> getComments(
            @RequestParam String mealDate,
            @RequestParam String mealType,
            @RequestParam String mealKey) {
        return commentService.findComments(mealDate, mealType, mealKey);
    }

    @PostMapping
    public ResponseEntity<String> saveComment(@RequestBody CommentDto dto, Authentication authentication) {
        commentService.save(dto, authentication.getName());
        return ResponseEntity.ok("success");
    }

    /**
     * 관리자용 댓글 검색 및 필터링 조회
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<CommentDto> getAllCommentsForAdmin(
            @RequestParam(required = false) String type,       // username 또는 email
            @RequestParam(required = false) String keyword,    // 검색어
            @RequestParam(required = false) String mealDate,   // 날짜 (20260203)
            @RequestParam(required = false) String mealType,   // 식사 구분 (조식/중식/석식)
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        return commentService.searchComments(type, keyword, mealDate, mealType, pageable);
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.ok("삭제되었습니다.");
    }
}