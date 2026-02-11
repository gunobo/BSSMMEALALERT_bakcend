package com.bssm.meal.user.service;

import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.dto.UserResponse;
import com.bssm.meal.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j; // ✨ 로그 출력을 위해 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j // ✨ 로그를 찍기 위한 어노테이션
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

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

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 유저 정보 업데이트 (알레르기, 선호메뉴, 알림 설정)
     */
    @Transactional
    public void updateUserInfo(String email, UserResponse request) {
        // 1. 요청 데이터 확인 로그
        log.info("📢 유저 정보 업데이트 시작 - 대상 이메일: {}", email);
        log.info("📢 요청 데이터 -> 알림허용: {}, 선호메뉴: {}, 알레르기: {}",
                request.isAllow_notifications(), request.getFavoriteMenus(), request.getAllergies());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 엔티티 데이터 업데이트
        user.updateInfo(
                request.getAllergies(),
                request.getFavoriteMenus(),
                request.isAllow_notifications(),
                request.isAllow_allergy_notifications(),
                request.isAllow_favorite_notifications()
        );

        // 3. 수동 save 호출 (Dirty Checking이 작동하지만, 확실한 로그 확인을 위해 추가)
        userRepository.save(user);

        log.info("✅ 유저 정보 업데이트 완료 - 현재 DB 저장 값(알림): {}", user.isAllow_notifications());
    }
}