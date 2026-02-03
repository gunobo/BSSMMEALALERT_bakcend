package com.bssm.meal.comment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mealDate; // 20240520
    private String mealType; // 중식
    private String mealKey;  // 돈까스

    @Column(columnDefinition = "TEXT")
    private String content;

    private String username;
    private String email;

    private LocalDateTime createdAt = LocalDateTime.now();
}