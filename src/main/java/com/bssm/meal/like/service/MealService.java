package com.bssm.meal.like.service;

import com.bssm.meal.like.repository.LikeRepository;
import com.bssm.meal.like.repository.MealRepository; // 프로젝트 구조에 맞게 수정
import com.bssm.meal.like.dto.MealResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // ✅ 조회 성능 향상
public class MealService {

    private final LikeRepository likeRepository;
    private final MealRepository mealRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // ✅ 환경변수 주입
    @Value("${neis.api.key}")
    private String apiKey;

    @Value("${neis.api.atpt-code}")
    private String atptCode;

    @Value("${neis.api.schul-code}")
    private String sdCode;

    /**
     * 식단 상세 정보와 좋아요 상태를 함께 반환
     */
    public MealResponseDto getMealDetail(String userId, String date, String type, String mealKey, String mealName) {

        // 1. 해당 메뉴의 총 좋아요 수 조회
        long likeCount = likeRepository.countByMealKey(mealKey);

        // 2. 유저별 좋아요 여부 판단
        boolean isLiked = false;
        if (userId != null && !userId.trim().isEmpty() && !"null".equals(userId)) {
            isLiked = likeRepository.existsByUserIdAndMealDateAndMealTypeAndMealKey(
                    userId, date, type, mealKey);
        }

        return MealResponseDto.builder()
                .mealKey(mealKey)
                .name(mealName)
                .date(date)
                .type(type)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .build();
    }

    /**
     * 알림 필터링 및 스케줄러에서 사용하는 메뉴 리스트 반환 메서드
     */
    public List<String> getMenusByDate(String date) {
        // 1. 나이스 API로부터 가공되지 않은 전체 메뉴 문자열을 가져옴
        String rawMenuData = getRawMealStringFromNeis(date);

        if (rawMenuData == null || rawMenuData.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. <br/> 태그를 기준으로 나누어 리스트로 변환
        return Arrays.stream(rawMenuData.split("<br/>"))
                .map(String::trim)
                .filter(menu -> !menu.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 나이스 API를 직접 호출하여 DDISH_NM(식단명) 필드만 추출
     */
    private String getRawMealStringFromNeis(String date) {
        try {
            // 환경변수를 적용한 URL 생성
            String url = String.format(
                    "https://open.neis.go.kr/hub/mealServiceDietInfo?KEY=%s&Type=json&ATPT_OFCDC_SC_CODE=%s&SD_SCHUL_CODE=%s&MLSV_YMD=%s",
                    apiKey, atptCode, sdCode, date
            );

            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return "";

            // org.json 라이브러리로 통일하여 파싱
            JSONObject jsonResponse = new JSONObject(response);

            if (jsonResponse.has("mealServiceDietInfo")) {
                JSONArray mealArray = jsonResponse.getJSONArray("mealServiceDietInfo");

                // 나이스 API 구조상 index 1번에 실제 데이터(row)가 들어있음
                JSONObject rowContainer = mealArray.getJSONObject(1);
                JSONArray rowArray = rowContainer.getJSONArray("row");

                // 해당 날짜의 첫 번째 식단 데이터 추출
                JSONObject mealInfo = rowArray.getJSONObject(0);

                return mealInfo.getString("DDISH_NM");
            }
        } catch (Exception e) {
            log.error("❌ [MealService] 나이스 API 데이터 추출 실패 (날짜: {}): {}", date, e.getMessage());
        }
        return "";
    }
}