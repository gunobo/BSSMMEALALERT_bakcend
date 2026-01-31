package com.bssm.meal.auth.service;

import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.repository.UserRepository; // ✅ 반드시 추가
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findByGoogleId(String googleId) {
        Optional<User> userOpt = userRepository.findByGoogleId(googleId);
        return userOpt.orElse(null);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
