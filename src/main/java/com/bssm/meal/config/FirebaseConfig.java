package com.bssm.meal.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        // 1. 리소스 폴더의 인증 키 파일 로드
        ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
        InputStream serviceAccount = resource.getInputStream();

        // 2. Firebase 옵션 설정
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        // 3. FirebaseApp 초기화 (이미 있으면 기존 앱 사용)
        FirebaseApp firebaseApp;
        if (FirebaseApp.getApps().isEmpty()) {
            firebaseApp = FirebaseApp.initializeApp(options);
        } else {
            firebaseApp = FirebaseApp.getInstance();
        }

        // 4. FirebaseMessaging 인스턴스를 빈(Bean)으로 등록
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}