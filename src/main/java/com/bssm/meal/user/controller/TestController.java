package com.bssm.meal.user.controller;

import com.bssm.meal.favorite.service.MealNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final MealNotificationService mealNotificationService;

    @GetMapping("/test-meal")
    public String test() {
        return "테스트 실행 완료! IntelliJ 콘솔 로그를 확인하세요.";
    }
}