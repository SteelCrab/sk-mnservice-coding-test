package org.example.matching;

import org.example.gps.GPSPoint;
import org.example.gps.GPSAccuracy;
import org.example.route.ReferencePath;
import org.example.route.RouteSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * GPS Map Matching 알고리즘 클래스
 * 
 * GPS 좌표를 기준 경로(도로)에 매칭하고 경로 이탈을 판정
 * - 거리 기반: GPS 포인트와 도로 세그먼트 간 최단거리 계산
 * - 각도 기반: GPS 진행 방향과 도로 방향의 일치성 검증
 * - 가중치 기반: 거리(60%) + 각도(20%) + 속도(20%) 종합 평가
 * - GPS 정확도 고려: HDOP 값에 따른 매칭 임계값 동적 조정
 */
public class GPSMapMatcher {
    
    // 매칭 설정 상수들 (개선된 값)
    private static final double DEFAULT_MATCHING_THRESHOLD = 30.0;      // 기본 매칭 임계값 (30m로 감소)
    private static final double DEFAULT_OFF_ROUTE_THRESHOLD = 50.0;     // 기본 경로 이탈 임계값 (50m로 감소)
    private static final double ANGLE_TOLERANCE = 60.0;                 // 방향 허용 오차 (60도로 증가)
    private static final double SPEED_WEIGHT = 0.2;                     // 속도 가중치
    private static final double DISTANCE_WEIGHT = 0.6;                  // 거리 가중치 (증가)
    private static final double ANGLE_WEIGHT = 0.2;                     // 각도 가중치
    
    private ReferencePath referencePath;        // 기준 경로
    private double matchingThreshold;           // 매칭 임계값 (미터)
    private double offRouteThreshold;           // 경로 이탈 임계값 (미터)
    private MatchingStrategy strategy;          // 매칭 전략
    
    /**
     * GPSMapMatcher 생성자 (기본 설정)
     * @param referencePath 기준 경로
     */
    public GPSMapMatcher(ReferencePath referencePath) {
        this.referencePath = referencePath;
        this.matchingThreshold = DEFAULT_MATCHING_THRESHOLD;
        this.offRouteThreshold = DEFAULT_OFF_ROUTE_THRESHOLD;
        this.strategy = MatchingStrategy.WEIGHTED_DISTANCE;  // 기본 전략
    }
    
    /**
     * GPSMapMatcher 생성자 (상세 설정)
     * @param referencePath 기준 경로
     * @param matchingThreshold 매칭 임계값 (미터)
     * @param offRouteThreshold 경로 이탈 임계값 (미터)
     * @param strategy 매칭 전략
     */
    public GPSMapMatcher(ReferencePath referencePath, double matchingThreshold, 
                        double offRouteThreshold, MatchingStrategy strategy) {
        this.referencePath = referencePath;
        this.matchingThreshold = matchingThreshold;
        this.offRouteThreshold = offRouteThreshold;
        this.strategy = strategy;
    }
    
    /**
     * 단일 GPS 포인트를 기준 경로에 매칭
     * 
     * 매칭 프로세스:
     * 1. GPS 포인트 유효성 검사
     * 2. GPS 정확도에 따른 임계값 조정
     * 3. 가장 가까운 도로 세그먼트 탐색
     * 4. 거리/각도/속도 기반 종합 검증
     * 
     * @param gpsPoint 매칭할 GPS 포인트
     * @return GPSMatchingResult 매칭 결과 (성공시 보정된 좌표, 실패시 실패 사유)
     */
    public GPSMatchingResult matchPoint(GPSPoint gpsPoint) {
        if (gpsPoint == null || !gpsPoint.isValid()) {
            return null;  // 유효하지 않은 GPS 포인트
        }
        
        // GPS 정확도에 따른 임계값 조정
        double adjustedThreshold = adjustThresholdByAccuracy(gpsPoint.getAccuracy());
        
        // 가장 가까운 세그먼트 찾기
        RouteSegment nearestSegment = referencePath.findNearestSegment(
            gpsPoint.getLatitude(), gpsPoint.getLongitude()
        );
        
        if (nearestSegment == null) {
            return createFailedResult(gpsPoint, "기준 경로가 비어있음");
        }
        
        // 세그먼트까지의 거리 계산
        double distanceToSegment = nearestSegment.getDistanceToPoint(
            gpsPoint.getLatitude(), gpsPoint.getLongitude()
        );
        
        // 매칭 가능 여부 판단
        if (distanceToSegment > adjustedThreshold) {
            return createFailedResult(gpsPoint, 
                String.format("거리 임계값 초과: %.2fm > %.2fm", distanceToSegment, adjustedThreshold));
        }
        
        // 매칭 전략에 따른 추가 검증
        if (!validateMatchingByStrategy(gpsPoint, nearestSegment)) {
            return createFailedResult(gpsPoint, "매칭 전략 검증 실패");
        }
        
        // 성공적인 매칭 결과 생성
        return createSuccessfulResult(gpsPoint, nearestSegment, distanceToSegment);
    }
    
    /**
     * GPS 포인트 목록을 순차적으로 매칭
     * @param gpsPoints 매칭할 GPS 포인트 목록
     * @return 매칭 결과 목록
     */
    public List<GPSMatchingResult> matchPoints(List<GPSPoint> gpsPoints) {
        List<GPSMatchingResult> results = new ArrayList<>();
        
        if (gpsPoints == null || gpsPoints.isEmpty()) {
            return results;  // 빈 목록 반환
        }
        
        System.out.printf("GPS Map Matching 시작: %d개 포인트 처리%n", gpsPoints.size());
        
        int successCount = 0;
        int failCount = 0;
        
        // 각 GPS 포인트를 순차적으로 매칭
        for (int i = 0; i < gpsPoints.size(); i++) {
            GPSPoint gpsPoint = gpsPoints.get(i);
            GPSMatchingResult result = matchPoint(gpsPoint);
            
            if (result != null) {
                results.add(result);
                if (result.isMatched()) {
                    successCount++;
                } else {
                    failCount++;
                }
            } else {
                // null 결과인 경우 실패 결과 생성
                results.add(createFailedResult(gpsPoint, "매칭 처리 실패"));
                failCount++;
            }
            
            // 진행률 출력 (10% 단위)
            if ((i + 1) % Math.max(1, gpsPoints.size() / 10) == 0) {
                double progress = (double) (i + 1) / gpsPoints.size() * 100;
                System.out.printf("진행률: %.0f%% (%d/%d)%n", progress, i + 1, gpsPoints.size());
            }
        }
        
        System.out.printf("GPS Map Matching 완료: 성공 %d개, 실패 %d개 (성공률: %.1f%%)%n", 
                         successCount, failCount, (double) successCount / (successCount + failCount) * 100);
        
        return results;
    }
    
    /**
     * 경로 이탈 판정
     * @param gpsPoint 판정할 GPS 포인트
     * @return true = 경로 이탈, false = 경로 내 위치
     */
    public boolean isOffRoute(GPSPoint gpsPoint) {
        if (gpsPoint == null || !gpsPoint.isValid()) {
            return true;  // 유효하지 않은 GPS는 이탈로 판정
        }
        
        return referencePath.isOffRoute(
            gpsPoint.getLatitude(), 
            gpsPoint.getLongitude(), 
            offRouteThreshold
        );
    }
    
    /**
     * GPS 정확도에 따른 매칭 임계값 조정 (개선된 버전)
     * @param accuracy GPS 정확도
     * @return 조정된 임계값 (미터)
     */
    private double adjustThresholdByAccuracy(GPSAccuracy accuracy) {
        double baseThreshold = this.matchingThreshold;
        
        switch (accuracy) {
            case EXCELLENT:
                return baseThreshold * 0.7;   // 30% 감소 (더 엄격한 매칭)
            case GOOD:
                return baseThreshold * 0.9;   // 10% 감소
            case MODERATE:
                return baseThreshold;         // 기본값 유지
            case FAIR:
                return baseThreshold * 1.3;   // 30% 증가 (더 관대한 매칭)
            case POOR:
                return baseThreshold * 1.8;   // 80% 증가
            default:
                return baseThreshold;
        }
    }
    
    /**
     * 매칭 전략에 따른 추가 검증
     * @param gpsPoint GPS 포인트
     * @param segment 매칭 대상 세그먼트
     * @return true = 검증 통과, false = 검증 실패
     */
    private boolean validateMatchingByStrategy(GPSPoint gpsPoint, RouteSegment segment) {
        switch (strategy) {
            case DISTANCE_ONLY:
                return true;  // 거리만 고려하므로 추가 검증 없음
                
            case ANGLE_AWARE:
                return validateAngleMatching(gpsPoint, segment);
                
            case WEIGHTED_DISTANCE:
                return validateWeightedMatching(gpsPoint, segment);
                
            case SPEED_AWARE:
                return validateSpeedMatching(gpsPoint, segment);
                
            default:
                return true;
        }
    }
    
    /**
     * 각도 기반 매칭 검증
     * GPS 진행 방향과 도로 방향의 일치성 확인
     * @param gpsPoint GPS 포인트
     * @param segment 세그먼트
     * @return true = 방향 일치, false = 방향 불일치
     */
    private boolean validateAngleMatching(GPSPoint gpsPoint, RouteSegment segment) {
        // 세그먼트의 방향 계산 (시작점 -> 끝점)
        if (segment.getNodes().size() < 2) {
            return true;  // 방향 계산 불가능한 경우 통과
        }
        
        // 세그먼트 방향 계산 (간단한 근사)
        var nodes = segment.getNodes();
        var startNode = nodes.get(0);
        var endNode = nodes.get(nodes.size() - 1);
        
        double segmentAngle = calculateBearing(
            startNode.getLatitude(), startNode.getLongitude(),
            endNode.getLatitude(), endNode.getLongitude()
        );
        
        // GPS 각도와 세그먼트 각도의 차이 계산
        double angleDiff = Math.abs(gpsPoint.getAngle() - segmentAngle);
        angleDiff = Math.min(angleDiff, 360.0 - angleDiff);  // 최소 각도 차이
        
        return angleDiff <= ANGLE_TOLERANCE;
    }
    
    /**
     * 가중치 기반 매칭 검증
     * 거리, 각도, 속도를 종합적으로 고려
     * @param gpsPoint GPS 포인트
     * @param segment 세그먼트
     * @return true = 종합 점수 통과, false = 점수 미달
     */
    private boolean validateWeightedMatching(GPSPoint gpsPoint, RouteSegment segment) {
        // 거리 점수 (0.0 ~ 1.0, 가까울수록 높음)
        double distance = segment.getDistanceToPoint(gpsPoint.getLatitude(), gpsPoint.getLongitude());
        double distanceScore = Math.max(0.0, 1.0 - (distance / matchingThreshold));
        
        // 각도 점수 (0.0 ~ 1.0, 방향이 일치할수록 높음)
        double angleScore = validateAngleMatching(gpsPoint, segment) ? 1.0 : 0.0;
        
        // 속도 점수 (현재는 단순화, 실제로는 도로 제한속도와 비교)
        double speedScore = gpsPoint.getSpeed() > 0 ? 1.0 : 0.5;
        
        // 가중 평균 계산
        double totalScore = (distanceScore * DISTANCE_WEIGHT) + 
                           (angleScore * ANGLE_WEIGHT) + 
                           (speedScore * SPEED_WEIGHT);
        
        return totalScore >= 0.6;  // 60% 이상이면 매칭 성공
    }
    
    /**
     * 속도 기반 매칭 검증
     * @param gpsPoint GPS 포인트
     * @param segment 세그먼트
     * @return true = 속도 적합, false = 속도 부적합
     */
    private boolean validateSpeedMatching(GPSPoint gpsPoint, RouteSegment segment) {
        // 현재는 단순한 속도 범위 검증
        // 실제로는 도로의 제한속도 정보를 활용해야 함
        double speed = gpsPoint.getSpeed();
        
        // 일반적인 도로 속도 범위 (5 ~ 120 km/h)
        return speed >= 5.0 && speed <= 120.0;
    }
    
    /**
     * 성공적인 매칭 결과 생성
     * @param gpsPoint 원본 GPS 포인트
     * @param segment 매칭된 세그먼트
     * @param distance 매칭 거리
     * @return 성공 매칭 결과
     */
    private GPSMatchingResult createSuccessfulResult(GPSPoint gpsPoint, RouteSegment segment, double distance) {
        // 세그먼트 내 진행률 계산
        double progress = segment.getProgressAtPoint(gpsPoint.getLatitude(), gpsPoint.getLongitude());
        
        // 전체 경로에서의 진행률 계산
        double overallProgress = referencePath.getProgressAtPoint(gpsPoint.getLatitude(), gpsPoint.getLongitude());
        
        return new GPSMatchingResult(
            gpsPoint,           // 원본 GPS 포인트
            segment,            // 매칭된 세그먼트
            distance,           // 매칭 거리
            progress,           // 세그먼트 내 진행률
            overallProgress,    // 전체 경로 진행률
            true,               // 매칭 성공
            "매칭 성공"         // 성공 메시지
        );
    }
    
    /**
     * 실패한 매칭 결과 생성
     * @param gpsPoint 원본 GPS 포인트
     * @param reason 실패 사유
     * @return 실패 매칭 결과
     */
    private GPSMatchingResult createFailedResult(GPSPoint gpsPoint, String reason) {
        return new GPSMatchingResult(
            gpsPoint,           // 원본 GPS 포인트
            null,               // 매칭된 세그먼트 없음
            Double.MAX_VALUE,   // 매칭 거리 (최대값)
            0.0,                // 세그먼트 내 진행률
            0.0,                // 전체 경로 진행률
            false,              // 매칭 실패
            reason              // 실패 사유
        );
    }
    
    /**
     * 두 지점 간의 방위각 계산
     * @param lat1 시작점 위도
     * @param lon1 시작점 경도
     * @param lat2 끝점 위도
     * @param lon2 끝점 경도
     * @return 방위각 (0~360도)
     */
    private double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLonRad = Math.toRadians(lon2 - lon1);
        
        double y = Math.sin(deltaLonRad) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                   Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad);
        
        double bearingRad = Math.atan2(y, x);
        double bearingDeg = Math.toDegrees(bearingRad);
        
        return (bearingDeg + 360.0) % 360.0;
    }
    
    // ===== Getters and Setters =====
    
    public ReferencePath getReferencePath() { return referencePath; }
    public double getMatchingThreshold() { return matchingThreshold; }
    public double getOffRouteThreshold() { return offRouteThreshold; }
    public MatchingStrategy getStrategy() { return strategy; }
    
    public void setMatchingThreshold(double matchingThreshold) { 
        this.matchingThreshold = matchingThreshold; 
    }
    
    public void setOffRouteThreshold(double offRouteThreshold) { 
        this.offRouteThreshold = offRouteThreshold; 
    }
    
    public void setStrategy(MatchingStrategy strategy) { 
        this.strategy = strategy; 
    }
}
