package com.bssm.meal.favorite.entity;

import com.bssm.meal.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter // ✅ 외부에서 필드 제어가 필요한 경우를 위해 추가 (allowNotifications 등)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "fcm_tokens")
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String token;

    // ✅ 기기 구분 필드 (MOBILE, WEB)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType deviceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // DB의 FK 컬럼명
    private User user;

    @Column(name = "allow_notifications", nullable = false)
    @Builder.Default
    private boolean allowNotifications = true;

    /**
     * ✅ 연관관계 편의 메서드
     * User 객체를 설정함과 동시에 User 엔티티의 리스트에도 자신을 추가하여
     * 메모리 상에서 양방향 연동을 동기화합니다.
     */
    public void setUser(User user) {
        // 기존 관계 제거
        if (this.user != null) {
            this.user.getFcmTokens().remove(this);
        }
        this.user = user;
        // 새로운 관계 설정
        if (user != null && !user.getFcmTokens().contains(this)) {
            user.getFcmTokens().add(String.valueOf(this));
        }
    }

    /**
     * ✅ 토큰 값 업데이트
     */
    public void updateToken(String token) {
        this.token = token;
    }

    /**
     * ✅ 알림 허용 여부 설정 (명시적 메서드)
     */
    public void updateAllowNotifications(boolean allowNotifications) {
        this.allowNotifications = allowNotifications;
    }

    /**
     * ✅ 알림 허용 여부 Setter
     * UserController 등에서 호출할 때 실제 필드에 값이 할당되도록 보장합니다.
     */
    public void setAllowNotifications(boolean allowNotifications) {
        this.allowNotifications = allowNotifications;
    }

    /**
     * ✅ 기기 타입 Enum
     */
    public enum DeviceType {
        MOBILE, WEB
    }
}