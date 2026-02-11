package com.bssm.meal.user.repository;

import com.bssm.meal.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isBanned = false WHERE u.isBanned = true AND u.banExpiresAt <= :now")
    int updateExpiredBans(@Param("now") LocalDateTime now);

    List<User> findAllByIsBannedTrueAndBanExpiresAtBefore(LocalDateTime now);

    @Query("SELECT u FROM User u WHERE " +
            "(:name IS NULL OR u.name LIKE %:name%) AND " +
            "(:email IS NULL OR u.email LIKE %:email%)")
    List<User> findUsersByFilter(
            @Param("name") String name,
            @Param("email") String email
    );

    // ✅ 수정: Java 필드명(camelCase) 사용
    @Query("SELECT u FROM User u WHERE u.allowNotifications = true")
    List<User> findByAllowNotificationsTrue();

    // ✅ 추가: 알레르기 알림 활성화 유저 조회
    @Query("SELECT u FROM User u WHERE u.allowAllergyNotifications = true")
    List<User> findByAllowAllergyNotificationsTrue();

    // ✅ 추가: 선호 메뉴 알림 활성화 유저 조회
    @Query("SELECT u FROM User u WHERE u.allowFavoriteNotifications = true")
    List<User> findByAllowFavoriteNotificationsTrue();

    List<User> findByEmailIn(List<String> emails);
}