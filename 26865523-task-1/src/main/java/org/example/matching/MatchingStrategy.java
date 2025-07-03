package org.example.matching;

/**
 * GPS Map Matching 전략을 나타내는 열거형
 * 역할: 다양한 매칭 알고리즘 전략을 정의하고 각각의 특성을 관리
 * 구조: 4가지 주요 매칭 전략과 각각의 설명 포함
 */
public enum MatchingStrategy {
    
    /**
     * 거리만 고려하는 단순 매칭 전략
     * 특징: GPS 포인트와 가장 가까운 도로 세그먼트에 매칭
     * 장점: 빠른 처리 속도, 단순한 로직
     * 단점: 방향이나 속도를 고려하지 않아 부정확할 수 있음
     * 적용: 정확도가 높은 GPS 데이터, 단순한 도로 구조
     */
    DISTANCE_ONLY("거리 기반", "GPS 포인트와 가장 가까운 세그먼트에 매칭"),
    
    /**
     * 거리와 방향을 함께 고려하는 매칭 전략
     * 특징: GPS 진행 방향과 도로 방향의 일치성을 추가로 검증
     * 장점: 방향 정보로 매칭 정확도 향상
     * 단점: 방향 정보가 부정확한 경우 매칭 실패 가능
     * 적용: 방향 정보가 신뢰할 만한 GPS 데이터
     */
    ANGLE_AWARE("방향 고려", "거리와 함께 GPS 진행 방향을 고려하여 매칭"),
    
    /**
     * 거리, 방향, 속도를 가중치로 종합 평가하는 매칭 전략
     * 특징: 여러 요소를 가중 평균으로 계산하여 최적 매칭 결정
     * 장점: 가장 정확하고 안정적인 매칭 결과
     * 단점: 복잡한 계산으로 처리 시간 증가
     * 적용: 고품질 GPS 데이터, 복잡한 도로 환경
     */
    WEIGHTED_DISTANCE("가중치 기반", "거리, 방향, 속도를 종합적으로 고려한 가중치 매칭"),
    
    /**
     * 속도 정보를 중점적으로 고려하는 매칭 전략
     * 특징: GPS 속도와 도로 제한속도의 적합성을 검증
     * 장점: 속도 기반 필터링으로 잘못된 매칭 방지
     * 단점: 도로 제한속도 정보가 필요
     * 적용: 속도 정보가 정확한 GPS 데이터, 제한속도 정보가 있는 도로
     */
    SPEED_AWARE("속도 고려", "GPS 속도와 도로 특성을 고려하여 매칭");
    
    private final String displayName;    // 전략 표시명 (한글)
    private final String description;    // 전략 상세 설명
    
    /**
     * MatchingStrategy 생성자
     * @param displayName 전략 표시명
     * @param description 전략 설명
     */
    MatchingStrategy(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * 전략의 복잡도 수준 반환
     * 처리 시간과 정확도의 트레이드오프 참고용
     * @return 복잡도 (1=단순, 5=복잡)
     */
    public int getComplexityLevel() {
        switch (this) {
            case DISTANCE_ONLY:
                return 1;  // 가장 단순
            case ANGLE_AWARE:
                return 2;  // 보통
            case SPEED_AWARE:
                return 3;  // 중간
            case WEIGHTED_DISTANCE:
                return 4;  // 복잡
            default:
                return 1;
        }
    }
    
    /**
     * 전략의 예상 정확도 수준 반환
     * @return 정확도 (1=낮음, 5=높음)
     */
    public int getAccuracyLevel() {
        switch (this) {
            case DISTANCE_ONLY:
                return 2;  // 낮은 정확도
            case ANGLE_AWARE:
                return 3;  // 보통 정확도
            case SPEED_AWARE:
                return 3;  // 보통 정확도
            case WEIGHTED_DISTANCE:
                return 5;  // 높은 정확도
            default:
                return 2;
        }
    }
    
    /**
     * GPS 데이터 품질에 따른 권장 전략 반환
     * @param hasReliableAngle 방향 정보 신뢰성
     * @param hasReliableSpeed 속도 정보 신뢰성
     * @param isComplexRoad 복잡한 도로 환경 여부
     * @return 권장 매칭 전략
     */
    public static MatchingStrategy getRecommendedStrategy(boolean hasReliableAngle, 
                                                        boolean hasReliableSpeed, 
                                                        boolean isComplexRoad) {
        if (isComplexRoad && hasReliableAngle && hasReliableSpeed) {
            return WEIGHTED_DISTANCE;  // 복잡한 환경에서는 종합 고려
        } else if (hasReliableAngle) {
            return ANGLE_AWARE;        // 방향 정보가 있으면 방향 고려
        } else if (hasReliableSpeed) {
            return SPEED_AWARE;        // 속도 정보가 있으면 속도 고려
        } else {
            return DISTANCE_ONLY;      // 기본적인 거리 기반
        }
    }
    
    /**
     * 특정 GPS 정확도에 적합한 전략인지 판단
     * @param gpsAccuracy GPS 정확도 등급
     * @return true = 적합한 전략, false = 부적합한 전략
     */
    public boolean isSuitableForAccuracy(org.example.gps.GPSAccuracy gpsAccuracy) {
        switch (gpsAccuracy) {
            case EXCELLENT:
            case GOOD:
                return true;  // 모든 전략 사용 가능
                
            case MODERATE:
                // 복잡한 전략은 피하고 단순한 전략 선호
                return this != WEIGHTED_DISTANCE;
                
            case FAIR:
            case POOR:
                // 낮은 정확도에서는 거리 기반만 사용
                return this == DISTANCE_ONLY;
                
            default:
                return true;
        }
    }
    
    /**
     * 전략별 권장 매칭 임계값 배수 반환
     * 기본 임계값에 곱할 배수 값
     * @return 임계값 배수 (1.0 = 기본값 유지)
     */
    public double getThresholdMultiplier() {
        switch (this) {
            case DISTANCE_ONLY:
                return 0.8;   // 거리만 고려하므로 더 엄격하게
            case ANGLE_AWARE:
                return 1.0;   // 기본값
            case SPEED_AWARE:
                return 1.2;   // 속도 고려로 약간 관대하게
            case WEIGHTED_DISTANCE:
                return 1.5;   // 종합 고려로 가장 관대하게
            default:
                return 1.0;
        }
    }
    
    /**
     * 전략 표시명 반환
     * @return 전략 표시명 (한글)
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 전략 설명 반환
     * @return 전략 상세 설명
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 전략 정보를 문자열로 변환
     * @return "WEIGHTED_DISTANCE(가중치 기반): 거리, 방향, 속도를 종합적으로 고려한 가중치 매칭" 형태
     */
    @Override
    public String toString() {
        return String.format("%s(%s): %s", name(), displayName, description);
    }
}
