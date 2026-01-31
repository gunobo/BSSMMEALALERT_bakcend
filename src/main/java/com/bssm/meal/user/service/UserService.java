package com.bssm.meal.user.service;

import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 에러 해결: UserController가 찾는 'getUserById' 메서드 추가
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // 모든 유저 가져오기
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 이메일로 유저 찾기 또는 새로 저장 (구글 로그인용)
    @Transactional
    public User saveOrUpdate(String email, String name, String picture) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    user.setName(name);
                    user.setPicture(picture);
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    return userRepository.save(User.builder()
                            .email(email)
                            .name(name)
                            .picture(picture)
                            .build());
                });
    }

    // 기존에 있던 findById (유지해도 무방합니다)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}