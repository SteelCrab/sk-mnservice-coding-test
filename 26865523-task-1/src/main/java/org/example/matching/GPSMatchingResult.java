package org.example.matching;

import org.example.gps.GPSPoint;
import org.example.route.RouteSegment;

/**
 * GPS Map Matching 결과를 담는 클래스
 * 역할: 단일 GPS 포인트의 매칭 결과 정보를 저장하고 관리
 * 구조: 원본 GPS 포인트, 매칭된 세그먼트, 매칭 품질 정보 등을 포함
 */
public class GPSMatchingResult {
    private GPSPoint originalPoint;      // 원본 GPS 포인트
    private RouteSegment matchedSegment; // 매칭된 도로 세그먼트 (매칭 실패 시 null)
    private double matchingDistance;     // GPS 포인트와 매칭된 세그먼트 간의 거리 (미터)
    private double segmentProgress;      // 매칭된 세그먼트 내에서의 진행률 (0.0 ~ 1.0)
    private double overallProgress;      // 전체 기준 경로에서의 진행률 (0.0 ~ 1.0)
    private boolean isMatched;           // 매칭 성공 여부
    private String message;              // 매칭 결과 메시지 (성공/실패 사유)
    private long processingTime;         // 매칭 처리 시간 (나노초)
    
    /**
     * GPSMatchingResult 생성자
     * @param originalPoint 원본 GPS 포인트
     * @param matchedSegment 매칭된 세그먼트 (실패 시 null)
     * @param matchingDistance 매칭 거리 (미터)
     * @param segmentProgress 세그먼트 내 진행률
     * @param overallProgress 전체 경로 진행률
     * @param isMatched 매칭 성공 여부
     * @param message 결과 메시지
     */
    public GPSMatchingResult(GPSPoint originalPoint, RouteSegment matchedSegment,
                            double matchingDistance, double segmentProgress, double overallProgress,
                            boolean isMatched, String message) {
        this.originalPoint = originalPoint;
        this.matchedSegment = matchedSegment;
        this.matchingDistance = matchingDistance;
        this.segmentProgress = segmentProgress;
        this.overallProgress = overallProgress;
        this.isMatched = isMatched;
        this.message = message;
        this.processingTime = 0;  // 기본값
    }
    
    /**
     * 매칭 품질 평가
     * 매칭 거리와 GPS 정확도를 기반으로 매칭 품질을 평가
     * @return 매칭 품질 등급
     */
    public MatchingQuality getMatchingQuality() {
        if (!isMatched) {
            return MatchingQuality.FAILED;  // 매칭 실패
        }
        
        // GPS 정확도에 따른 기준 거리 설정
        double excellentThreshold = originalPoint.getAccuracy().getRecommendedMatchingThreshold() * 0.3;
        double goodThreshold = originalPoint.getAccuracy().getRecommendedMatchingThreshold() * 0.6;
        double moderateThreshold = originalPoint.getAccuracy().getRecommendedMatchingThreshold();
        
        if (matchingDistance <= excellentThreshold) {
            return MatchingQuality.EXCELLENT;  // 매우 우수한 매칭
        } else if (matchingDistance <= goodThreshold) {
            return MatchingQuality.GOOD;       // 우수한 매칭
        } else if (matchingDistance <= moderateThreshold) {
            return MatchingQuality.MODERATE;   // 보통 매칭
        } else {
            return MatchingQuality.POOR;       // 낮은 품질 매칭
        }
    }
    
    /**
     * 매칭된 위치의 보정된 GPS 좌표 계산
     * 원본 GPS 좌표를 도로 위의 가장 가까운 지점으로 보정
     * @return 보정된 GPS 좌표 [위도, 경도] (매칭 실패 시 원본 좌표)
     */
    public double[] getCorrectedCoordinates() {
        if (!isMatched || matchedSegment == null) {
            // 매칭 실패 시 원본 좌표 반환
            return new double[]{originalPoint.getLatitude(), originalPoint.getLongitude()};
        }
        
        // 세그먼트 상의 가장 가까운 지점 계산 (간단한 근사)
        var nodes = matchedSegment.getNodes();
        if (nodes.size() < 2) {
            // 노드가 부족한 경우 첫 번째 노드 좌표 반환
            if (!nodes.isEmpty()) {
                var node = nodes.get(0);
                return new double[]{node.getLatitude(), node.getLongitude()};
            } else {
                return new double[]{originalPoint.getLatitude(), originalPoint.getLongitude()};
            }
        }
        
        // 진행률을 기반으로 세그먼트 상의 위치 계산
        int segmentIndex = (int) (segmentProgress * (nodes.size() - 1));
        segmentIndex = Math.max(0, Math.min(segmentIndex, nodes.size() - 2));
        
        var startNode = nodes.get(segmentIndex);
        var endNode = nodes.get(segmentIndex + 1);
        
        // 선형 보간으로 정확한 위치 계산
        double localProgress = (segmentProgress * (nodes.size() - 1)) - segmentIndex;
        localProgress = Math.max(0.0, Math.min(1.0, localProgress));
        
        double correctedLat = startNode.getLatitude() + 
                             (endNode.getLatitude() - startNode.getLatitude()) * localProgress;
        double correctedLon = startNode.getLongitude() + 
                             (endNode.getLongitude() - startNode.getLongitude()) * localProgress;
        
        return new double[]{correctedLat, correctedLon};
    }
    
    /**
     * 매칭 결과의 신뢰도 계산
     * GPS 정확도, 매칭 거리, 세그먼트 품질 등을 종합하여 신뢰도 산출
     * @return 신뢰도 (0.0 ~ 1.0, 높을수록 신뢰할 만함)
     */
    public double getConfidenceScore() {
        if (!isMatched) {
            return 0.0;  // 매칭 실패 시 신뢰도 0
        }
        
        // GPS 정확도 점수 (0.0 ~ 1.0)
        double accuracyScore = switch (originalPoint.getAccuracy()) {
            case EXCELLENT -> 1.0;
            case GOOD -> 0.8;
            case MODERATE -> 0.6;
            case FAIR -> 0.4;
            case POOR -> 0.2;
        };
        
        // 매칭 거리 점수 (0.0 ~ 1.0, 가까울수록 높음)
        double maxDistance = originalPoint.getAccuracy().getRecommendedMatchingThreshold();
        double distanceScore = Math.max(0.0, 1.0 - (matchingDistance / maxDistance));
        
        // 세그먼트 품질 점수 (노드 수가 많을수록 높음)
        double segmentScore = 1.0;
        if (matchedSegment != null && matchedSegment.getNodes().size() >= 2) {
            segmentScore = Math.min(1.0, matchedSegment.getNodes().size() / 10.0);
        }
        
        // 가중 평균으로 최종 신뢰도 계산
        return (accuracyScore * 0.4) + (distanceScore * 0.5) + (segmentScore * 0.1);
    }
    
    /**
     * 경로 이탈 여부 판정
     * 매칭 거리와 임계값을 비교하여 경로 이탈 여부 결정
     * @param offRouteThreshold 경로 이탈 임계값 (미터)
     * @return true = 경로 이탈, false = 경로 내 위치
     */
    public boolean isOffRoute(double offRouteThreshold) {
        if (!isMatched) {
            return true;  // 매칭 실패는 경로 이탈로 간주
        }
        
        return matchingDistance > offRouteThreshold;
    }
    
    /**
     * 매칭 결과 요약 정보 생성
     * @return 매칭 결과 요약 문자열
     */
    public String getSummary() {
        if (!isMatched) {
            return String.format("매칭 실패: %s", message);
        }
        
        return String.format("매칭 성공 - 거리: %.2fm, 품질: %s, 신뢰도: %.1f%%, 진행률: %.1f%%",
                           matchingDistance, 
                           getMatchingQuality().getDisplayName(),
                           getConfidenceScore() * 100,
                           overallProgress * 100);
    }
    
    /**
     * 상세한 매칭 결과 보고서 생성
     * @return 상세 매칭 결과 문자열
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== GPS 매칭 결과 상세 보고서 ===\n");
        report.append(String.format("원본 GPS: %s\n", originalPoint.toString()));
        report.append(String.format("매칭 상태: %s\n", isMatched ? "성공" : "실패"));
        report.append(String.format("결과 메시지: %s\n", message));
        
        if (isMatched && matchedSegment != null) {
            report.append(String.format("매칭된 세그먼트: %s\n", matchedSegment.toString()));
            report.append(String.format("매칭 거리: %.2fm\n", matchingDistance));
            report.append(String.format("매칭 품질: %s\n", getMatchingQuality().getDisplayName()));
            report.append(String.format("신뢰도: %.1f%%\n", getConfidenceScore() * 100));
            report.append(String.format("세그먼트 진행률: %.1f%%\n", segmentProgress * 100));
            report.append(String.format("전체 경로 진행률: %.1f%%\n", overallProgress * 100));
            
            double[] corrected = getCorrectedCoordinates();
            report.append(String.format("보정된 좌표: %.6f, %.6f\n", corrected[0], corrected[1]));
        }
        
        if (processingTime > 0) {
            report.append(String.format("처리 시간: %.3fms\n", processingTime / 1_000_000.0));
        }
        
        return report.toString();
    }
    
    // ===== Getters and Setters =====
    
    public GPSPoint getOriginalPoint() { return originalPoint; }
    public RouteSegment getMatchedSegment() { return matchedSegment; }
    public double getMatchingDistance() { return matchingDistance; }
    public double getSegmentProgress() { return segmentProgress; }
    public double getOverallProgress() { return overallProgress; }
    public boolean isMatched() { return isMatched; }
    public String getMessage() { return message; }
    public long getProcessingTime() { return processingTime; }
    
    public void setProcessingTime(long processingTime) { 
        this.processingTime = processingTime; 
    }
    
    /**
     * 매칭 결과를 간단한 문자열로 변환
     * @return "GPSMatchingResult{matched=true, distance=15.2m, quality=GOOD}" 형태
     */
    @Override
    public String toString() {
        if (isMatched) {
            return String.format("GPSMatchingResult{matched=true, distance=%.1fm, quality=%s}",
                               matchingDistance, getMatchingQuality());
        } else {
            return String.format("GPSMatchingResult{matched=false, reason='%s'}", message);
        }
    }
}

/**
 * 매칭 품질을 나타내는 열거형
 */
enum MatchingQuality {
    EXCELLENT("매우 우수"),
    GOOD("우수"),
    MODERATE("보통"),
    POOR("낮음"),
    FAILED("실패");
    
    private final String displayName;
    
    MatchingQuality(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
