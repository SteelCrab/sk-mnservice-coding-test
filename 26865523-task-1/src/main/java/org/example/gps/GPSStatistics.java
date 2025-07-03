package org.example.gps;

/**
 * GPS 데이터의 통계 정보를 담는 클래스
 * 역할: GPS 포인트 목록의 전반적인 품질과 특성을 분석한 결과를 저장
 * 구조: 포인트 수, 거리, 속도, HDOP, 정확도 분포 등의 통계 정보 포함
 */
public class GPSStatistics {
    private int totalPoints;           // 전체 GPS 포인트 수
    private double totalDistance;      // 전체 이동 거리 (미터)
    
    // 속도 통계
    private double minSpeed;           // 최소 속도 (km/h)
    private double maxSpeed;           // 최대 속도 (km/h)
    private double avgSpeed;           // 평균 속도 (km/h)
    
    // HDOP 통계
    private double minHdop;            // 최소 HDOP 값
    private double maxHdop;            // 최대 HDOP 값
    private double avgHdop;            // 평균 HDOP 값
    
    // 정확도별 포인트 수
    private int excellentCount;        // EXCELLENT 등급 포인트 수
    private int goodCount;             // GOOD 등급 포인트 수
    private int moderateCount;         // MODERATE 등급 포인트 수
    private int fairCount;             // FAIR 등급 포인트 수
    private int poorCount;             // POOR 등급 포인트 수
    
    /**
     * 빈 통계 객체 생성자 (데이터가 없을 때 사용)
     */
    public GPSStatistics() {
        this.totalPoints = 0;
        this.totalDistance = 0.0;
        this.minSpeed = 0.0;
        this.maxSpeed = 0.0;
        this.avgSpeed = 0.0;
        this.minHdop = 0.0;
        this.maxHdop = 0.0;
        this.avgHdop = 0.0;
        this.excellentCount = 0;
        this.goodCount = 0;
        this.moderateCount = 0;
        this.fairCount = 0;
        this.poorCount = 0;
    }
    
    /**
     * 전체 통계 정보를 포함한 생성자
     * @param totalPoints 전체 포인트 수
     * @param totalDistance 전체 거리 (미터)
     * @param minSpeed 최소 속도 (km/h)
     * @param maxSpeed 최대 속도 (km/h)
     * @param avgSpeed 평균 속도 (km/h)
     * @param minHdop 최소 HDOP
     * @param maxHdop 최대 HDOP
     * @param avgHdop 평균 HDOP
     * @param excellentCount EXCELLENT 등급 수
     * @param goodCount GOOD 등급 수
     * @param moderateCount MODERATE 등급 수
     * @param fairCount FAIR 등급 수
     * @param poorCount POOR 등급 수
     */
    public GPSStatistics(int totalPoints, double totalDistance,
                        double minSpeed, double maxSpeed, double avgSpeed,
                        double minHdop, double maxHdop, double avgHdop,
                        int excellentCount, int goodCount, int moderateCount,
                        int fairCount, int poorCount) {
        this.totalPoints = totalPoints;
        this.totalDistance = totalDistance;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        this.avgSpeed = avgSpeed;
        this.minHdop = minHdop;
        this.maxHdop = maxHdop;
        this.avgHdop = avgHdop;
        this.excellentCount = excellentCount;
        this.goodCount = goodCount;
        this.moderateCount = moderateCount;
        this.fairCount = fairCount;
        this.poorCount = poorCount;
    }
    
    /**
     * Map Matching에 적합한 포인트의 비율 계산
     * EXCELLENT, GOOD 등급 포인트들의 비율
     * @return Map Matching 적합 비율 (0.0 ~ 1.0)
     */
    public double getMapMatchingSuitabilityRatio() {
        if (totalPoints == 0) {
            return 0.0;
        }
        return (double) (excellentCount + goodCount) / totalPoints;
    }
    
    /**
     * 경로 이탈 판정에 적합한 포인트의 비율 계산
     * EXCELLENT, GOOD, MODERATE 등급 포인트들의 비율
     * @return 경로 이탈 판정 적합 비율 (0.0 ~ 1.0)
     */
    public double getOffRouteDetectionSuitabilityRatio() {
        if (totalPoints == 0) {
            return 0.0;
        }
        return (double) (excellentCount + goodCount + moderateCount) / totalPoints;
    }
    
    /**
     * 전체적인 GPS 데이터 품질 평가
     * @return GPS 데이터 품질 등급
     */
    public GPSDataQuality getOverallQuality() {
        double mapMatchingRatio = getMapMatchingSuitabilityRatio();
        double offRouteRatio = getOffRouteDetectionSuitabilityRatio();
        
        if (mapMatchingRatio >= 0.8) {
            return GPSDataQuality.EXCELLENT;      // 80% 이상이 고품질
        } else if (mapMatchingRatio >= 0.6) {
            return GPSDataQuality.GOOD;           // 60% 이상이 고품질
        } else if (offRouteRatio >= 0.7) {
            return GPSDataQuality.MODERATE;       // 70% 이상이 중간품질
        } else if (offRouteRatio >= 0.5) {
            return GPSDataQuality.FAIR;           // 50% 이상이 중간품질
        } else {
            return GPSDataQuality.POOR;           // 대부분이 저품질
        }
    }
    
    /**
     * 평균 이동 속도 계산 (시간 기준)
     * 전체 거리를 평균 속도로 나눈 예상 이동 시간
     * @return 예상 이동 시간 (분 단위)
     */
    public double getEstimatedTravelTimeMinutes() {
        if (avgSpeed <= 0) {
            return 0.0;
        }
        // 거리(m) / 속도(km/h) * 60 = 시간(분)
        return (totalDistance / 1000.0) / avgSpeed * 60.0;
    }
    
    /**
     * 통계 정보를 상세한 문자열로 변환
     * @return 상세 통계 정보 문자열
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== GPS 데이터 통계 보고서 ===\n");
        report.append(String.format("전체 포인트 수: %,d개\n", totalPoints));
        report.append(String.format("전체 이동 거리: %.2fm (%.3fkm)\n", totalDistance, totalDistance / 1000.0));
        report.append(String.format("예상 이동 시간: %.1f분\n", getEstimatedTravelTimeMinutes()));
        report.append("\n--- 속도 통계 ---\n");
        report.append(String.format("최소 속도: %.1f km/h\n", minSpeed));
        report.append(String.format("최대 속도: %.1f km/h\n", maxSpeed));
        report.append(String.format("평균 속도: %.1f km/h\n", avgSpeed));
        report.append("\n--- HDOP 통계 ---\n");
        report.append(String.format("최소 HDOP: %.2f\n", minHdop));
        report.append(String.format("최대 HDOP: %.2f\n", maxHdop));
        report.append(String.format("평균 HDOP: %.2f\n", avgHdop));
        report.append("\n--- 정확도 분포 ---\n");
        report.append(String.format("EXCELLENT: %,d개 (%.1f%%)\n", excellentCount, getPercentage(excellentCount)));
        report.append(String.format("GOOD: %,d개 (%.1f%%)\n", goodCount, getPercentage(goodCount)));
        report.append(String.format("MODERATE: %,d개 (%.1f%%)\n", moderateCount, getPercentage(moderateCount)));
        report.append(String.format("FAIR: %,d개 (%.1f%%)\n", fairCount, getPercentage(fairCount)));
        report.append(String.format("POOR: %,d개 (%.1f%%)\n", poorCount, getPercentage(poorCount)));
        report.append("\n--- 품질 평가 ---\n");
        report.append(String.format("전체 품질: %s\n", getOverallQuality().getDescription()));
        report.append(String.format("Map Matching 적합도: %.1f%%\n", getMapMatchingSuitabilityRatio() * 100));
        report.append(String.format("경로 이탈 판정 적합도: %.1f%%\n", getOffRouteDetectionSuitabilityRatio() * 100));
        
        return report.toString();
    }
    
    /**
     * 특정 카운트의 전체 대비 백분율 계산
     * @param count 개별 카운트
     * @return 백분율 (0.0 ~ 100.0)
     */
    private double getPercentage(int count) {
        return totalPoints > 0 ? (double) count / totalPoints * 100.0 : 0.0;
    }
    
    // ===== Getters =====
    
    public int getTotalPoints() { return totalPoints; }
    public double getTotalDistance() { return totalDistance; }
    public double getMinSpeed() { return minSpeed; }
    public double getMaxSpeed() { return maxSpeed; }
    public double getAvgSpeed() { return avgSpeed; }
    public double getMinHdop() { return minHdop; }
    public double getMaxHdop() { return maxHdop; }
    public double getAvgHdop() { return avgHdop; }
    public int getExcellentCount() { return excellentCount; }
    public int getGoodCount() { return goodCount; }
    public int getModerateCount() { return moderateCount; }
    public int getFairCount() { return fairCount; }
    public int getPoorCount() { return poorCount; }
    
    /**
     * 통계 정보를 간단한 문자열로 변환
     * @return "GpsStatistics{points=1234, distance=5678.9m, quality=GOOD}" 형태
     */
    @Override
    public String toString() {
        return String.format("GpsStatistics{points=%d, distance=%.1fm, quality=%s}",
                           totalPoints, totalDistance, getOverallQuality());
    }
}

/**
 * GPS 데이터 전체 품질을 나타내는 열거형
 */
enum GPSDataQuality {
    EXCELLENT("매우 우수"),
    GOOD("우수"),
    MODERATE("보통"),
    FAIR("나쁨"),
    POOR("매우 나쁨");
    
    private final String description;
    
    GPSDataQuality(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
