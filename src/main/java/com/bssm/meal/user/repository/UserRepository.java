package com.bssm.meal.user.repository;

import com.bssm.meal.user.domain.User; // ✅ 여기도 경로가 domain.User여야 함!
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
}