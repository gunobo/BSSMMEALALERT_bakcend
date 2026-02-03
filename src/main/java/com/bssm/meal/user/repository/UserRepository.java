package com.bssm.meal.user.repository;

import com.bssm.meal.user.domain.User; // ✅ 여기도 경로가 domain.User여야 함!
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
}