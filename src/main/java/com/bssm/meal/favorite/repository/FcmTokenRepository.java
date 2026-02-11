package com.bssm.meal.favorite.repository;

import com.bssm.meal.favorite.entity.FcmToken;
import com.bssm.meal.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    List<FcmToken> findAllByUserId(Long userId);

    List<FcmToken> findByUserEmailIn(List<String> emails);

    void deleteByUserId(Long userId);

    Optional<FcmToken> findByToken(String token);

    // ✅ 수정: Java 필드명(camelCase) 사용
    @Query("SELECT t FROM FcmToken t JOIN t.user u WHERE u.allowNotifications = true")
    List<FcmToken> findByUserAllowNotificationsTrue();

    // ✅ 추가: 알레르기 알림 활성화 유저의 토큰 조회
    @Query("SELECT t FROM FcmToken t JOIN t.user u WHERE u.allowAllergyNotifications = true")
    List<FcmToken> findByUserAllowAllergyNotificationsTrue();

    // ✅ 추가: 선호 메뉴 알림 활성화 유저의 토큰 조회
    @Query("SELECT t FROM FcmToken t JOIN t.user u WHERE u.allowFavoriteNotifications = true")
    List<FcmToken> findByUserAllowFavoriteNotificationsTrue();
}