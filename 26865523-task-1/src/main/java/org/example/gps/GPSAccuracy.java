package org.example.gps;

/**
 * GPS 정확도 등급을 나타내는 열거형
 * 역할: HDOP 값을 기반으로 GPS 신호의 품질을 분류
 * 구조: 5단계 정확도 등급과 각각의 설명 포함
 */
public enum GPSAccuracy {
    /**
     * 매우 우수한 정확도 (HDOP ≤ 1.0)
     * 예상 오차: 1m 이하
     * 사용 가능: 정밀한 내비게이션, 측량
     */
    EXCELLENT("매우 우수", 1.0, "1m 이하 오차"),
    
    /**
     * 우수한 정확도 (1.0 < HDOP ≤ 2.0)
     * 예상 오차: 1-3m
     * 사용 가능: 일반적인 내비게이션, Map Matching
     */
    GOOD("우수", 2.0, "1-3m 오차"),
    
    /**
     * 보통 정확도 (2.0 < HDOP ≤ 5.0)
     * 예상 오차: 3-8m
     * 사용 가능: 기본적인 위치 서비스
     */
    MODERATE("보통", 5.0, "3-8m 오차"),
    
    /**
     * 나쁜 정확도 (5.0 < HDOP ≤ 10.0)
     * 예상 오차: 8-15m
     * 사용 제한: 정밀도가 필요한 서비스에는 부적합
     */
    FAIR("나쁨", 10.0, "8-15m 오차"),
    
    /**
     * 매우 나쁜 정확도 (HDOP > 10.0)
     * 예상 오차: 15m 이상
     * 사용 불가: 대부분의 위치 서비스에 부적합
     */
    POOR("매우 나쁨", Double.MAX_VALUE, "15m 이상 오차");
    
    private final String description;    // 정확도 설명 (한글)
    private final double maxHdop;        // 해당 등급의 최대 HDOP 값
    private final String errorRange;     // 예상 오차 범위 설명
    
    /**
     * GPSAccuracy 생성자
     * @param description 정확도 설명
     * @param maxHdop 최대 HDOP 값
     * @param errorRange 예상 오차 범위
     */
    GPSAccuracy(String description, double maxHdop, String errorRange) {
        this.description = description;
        this.maxHdop = maxHdop;
        this.errorRange = errorRange;
    }
    
    /**
     * HDOP 값으로부터 GPS 정확도 등급 결정
     * @param hdop 수평 정밀도 희석값
     * @return 해당하는 GPS 정확도 등급
     */
    public static GPSAccuracy fromHdop(double hdop) {
        if (hdop <= EXCELLENT.maxHdop) {
            return EXCELLENT;
        } else if (hdop <= GOOD.maxHdop) {
            return GOOD;
        } else if (hdop <= MODERATE.maxHdop) {
            return MODERATE;
        } else if (hdop <= FAIR.maxHdop) {
            return FAIR;
        } else {
            return POOR;
        }
    }
    
    /**
     * Map Matching에 적합한 정확도인지 판단
     * @return true = Map Matching 사용 가능, false = 사용 부적합
     */
    public boolean isSuitableForMapMatching() {
        // GOOD 이상의 정확도만 Map Matching에 적합
        return this == EXCELLENT || this == GOOD;
    }
    
    /**
     * 경로 이탈 판정에 적합한 정확도인지 판단
     * @return true = 경로 이탈 판정 가능, false = 판정 부적합
     */
    public boolean isSuitableForOffRouteDetection() {
        // MODERATE 이상의 정확도면 경로 이탈 판정 가능
        return this != FAIR && this != POOR;
    }
    
    /**
     * 정확도에 따른 권장 매칭 임계값 반환
     * GPS 좌표를 도로에 매칭할 때 사용할 최대 허용 거리
     * @return 권장 임계값 (미터 단위)
     */
    public double getRecommendedMatchingThreshold() {
        switch (this) {
            case EXCELLENT:
                return 10.0;   // 매우 정확하므로 10m 이내만 매칭
            case GOOD:
                return 20.0;   // 20m 이내 매칭
            case MODERATE:
                return 50.0;   // 50m 이내 매칭
            case FAIR:
                return 100.0;  // 100m 이내 매칭 (신뢰도 낮음)
            case POOR:
                return 200.0;  // 200m 이내 매칭 (매우 낮은 신뢰도)
            default:
                return 50.0;   // 기본값
        }
    }
    
    /**
     * 정확도 설명 반환
     * @return 정확도 설명 (한글)
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 최대 HDOP 값 반환
     * @return 해당 등급의 최대 HDOP 값
     */
    public double getMaxHdop() {
        return maxHdop;
    }
    
    /**
     * 예상 오차 범위 반환
     * @return 오차 범위 설명
     */
    public String getErrorRange() {
        return errorRange;
    }
    
    /**
     * GPS 정확도 정보를 문자열로 변환
     * @return "GOOD(우수): 1-3m 오차" 형태
     */
    @Override
    public String toString() {
        return String.format("%s(%s): %s", name(), description, errorRange);
    }
}
