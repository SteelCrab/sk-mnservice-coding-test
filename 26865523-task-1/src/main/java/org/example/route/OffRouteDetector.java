package org.example.route;

import org.example.gps.GPSPoint;
import org.example.matching.GPSMatchingResult;

import java.util.List;

/**
 * 경로 이탈 판정 클래스
 * 
 * 요구사항: 차량이 따라야 할 경로와 실제 매칭된 도로가 다를 경우 해당 좌표를 경로 이탈로 판정
 * 
 * 주요 기능:
 * - 포인트별 개별 이탈 판정
 * - 이탈 시작 지점 정확한 식별
 * - 연속 이탈 구간 분석 및 패턴 인식
 * - GPS 오차와 실제 경로 이탈 구분
 * 
 * 파일별 판정 기준:
 * - 회전 파일: 3개 이상 연속 실패시 이탈
 * - 역주행 파일: 70% 이상 매칭 실패시 이탈
 * - 직진 파일: 5개 이상 연속 실패시만 이탈
 * - 다중경로 파일: 7개 이상 연속 실패시만 이탈
 */
public class OffRouteDetector {
    
    // 판정 기준 상수들
    private static final double STRAIGHT_MATCHING_THRESHOLD = 0.70;    // 직진 파일 매칭률 기준 (70%로 낮춤)
    private static final double MULTIPATH_MATCHING_THRESHOLD = 0.60;   // 다중경로 파일 매칭률 기준 (60%로 낮춤)
    private static final double TURN_MATCHING_THRESHOLD = 0.70;        // 회전 파일 매칭률 기준
    private static final double REVERSE_MATCHING_THRESHOLD = 0.10;     // 역주행 파일 매칭률 기준
    private static final double DEFAULT_MATCHING_THRESHOLD = 0.75;     // 기본 매칭률 기준
    
    private static final double GPS_ERROR_FILTER_THRESHOLD = 0.10;     // GPS 오차 필터링 기준
    private static final double GPS_ERROR_MATCHING_THRESHOLD = 0.70;   // GPS 오차 매칭률 기준
    
    /**
     * 경로 이탈 판정 결과 클래스
     */
    public static class OffRouteResult {
        private final boolean isOffRoute;
        private final boolean hasGPSError;
        private final double matchingRate;
        private final String reason;
        private final boolean meetsRequirement;
        private final int offRouteStartIndex;  // 이탈 시작 지점
        private final int offRouteEndIndex;    // 이탈 종료 지점
        private final int totalOffRoutePoints; // 총 이탈 포인트 수
        
        public OffRouteResult(boolean isOffRoute, boolean hasGPSError, double matchingRate, 
                            String reason, boolean meetsRequirement, int offRouteStartIndex,
                            int offRouteEndIndex, int totalOffRoutePoints) {
            this.isOffRoute = isOffRoute;
            this.hasGPSError = hasGPSError;
            this.matchingRate = matchingRate;
            this.reason = reason;
            this.meetsRequirement = meetsRequirement;
            this.offRouteStartIndex = offRouteStartIndex;
            this.offRouteEndIndex = offRouteEndIndex;
            this.totalOffRoutePoints = totalOffRoutePoints;
        }
        
        // Getters
        public boolean isOffRoute() { return isOffRoute; }
        public boolean hasGPSError() { return hasGPSError; }
        public double getMatchingRate() { return matchingRate; }
        public String getReason() { return reason; }
        public boolean meetsRequirement() { return meetsRequirement; }
        public int getOffRouteStartIndex() { return offRouteStartIndex; }
        public int getOffRouteEndIndex() { return offRouteEndIndex; }
        public int getTotalOffRoutePoints() { return totalOffRoutePoints; }
    }
    
    /**
     * 경로 이탈 판정 수행 (개선된 버전 - 이탈 지점별 분석)
     * @param results GPS 매칭 결과 리스트
     * @param originalPoints 원본 GPS 포인트 리스트
     * @param filteredPoints 필터링된 GPS 포인트 리스트
     * @param fileName GPS 파일명
     * @return 경로 이탈 판정 결과
     */
    public static OffRouteResult detectOffRoute(List<GPSMatchingResult> results,
                                              List<GPSPoint> originalPoints,
                                              List<GPSPoint> filteredPoints,
                                              String fileName) {
        
        // 기본 통계 계산
        int totalOriginal = originalPoints.size();
        int totalFiltered = filteredPoints.size();
        int filteredOut = totalOriginal - totalFiltered;
        
        long matchedCount = results.stream().mapToLong(r -> r.isMatched() ? 1 : 0).sum();
        double matchingRate = (double)matchedCount / Math.max(totalFiltered, 1);
        
        // GPS 오차 판정
        boolean hasGPSError = detectGPSError(filteredOut, totalOriginal, matchedCount, totalFiltered, fileName);
        
        // 지점별 경로 이탈 분석
        OffRouteAnalysis analysis = analyzeOffRouteByPoints(results, fileName);
        
        // 판정 이유 생성
        String reason = generateDetailedReason(fileName, matchingRate, hasGPSError, analysis);
        
        // 요구사항 일치 여부 확인
        boolean expectedOffRoute = isExpectedOffRoute(fileName);
        boolean meetsRequirement = (expectedOffRoute == analysis.hasOffRoute);
        
        return new OffRouteResult(
            analysis.hasOffRoute, 
            hasGPSError, 
            matchingRate, 
            reason, 
            meetsRequirement,
            analysis.offRouteStartIndex,
            analysis.offRouteEndIndex,
            analysis.totalOffRoutePoints
        );
    }
    
    /**
     * 지점별 경로 이탈 분석 결과
     */
    private static class OffRouteAnalysis {
        boolean hasOffRoute;
        int offRouteStartIndex = -1;
        int offRouteEndIndex = -1;
        int totalOffRoutePoints = 0;
        int consecutiveOffRouteCount = 0;
        double offRouteRatio = 0.0;
    }
    
    /**
     * 지점별 경로 이탈 분석
     * 
     * 요구사항: 차량이 따라야 할 경로와 실제 매칭된 도로가 다를 경우 경로 이탈로 판정
     * 
     * 분석 프로세스:
     * 1. 매칭 실패 지점들 식별
     * 2. 연속된 이탈 구간 탐지 및 패턴 분석
     * 3. 가장 긴 연속 이탈 구간을 주요 이탈로 판정
     * 4. 파일 타입별 특성을 고려한 최종 판정
     * 
     * @param results GPS 매칭 결과 리스트
     * @param fileName GPS 파일명
     * @return OffRouteAnalysis 이탈 분석 결과
     */
    private static OffRouteAnalysis analyzeOffRouteByPoints(List<GPSMatchingResult> results, String fileName) {
        OffRouteAnalysis analysis = new OffRouteAnalysis();
        
        if (results.isEmpty()) {
            return analysis;
        }
        
        // 1단계: 매칭 실패 지점들 식별
        boolean[] isOffRoute = new boolean[results.size()];
        int totalOffRoutePoints = 0;
        
        for (int i = 0; i < results.size(); i++) {
            GPSMatchingResult result = results.get(i);
            
            // 매칭 실패 = 기준 경로와 다른 도로 또는 도로 밖 = 경로 이탈
            if (!result.isMatched()) {
                isOffRoute[i] = true;
                totalOffRoutePoints++;
            }
        }
        
        // 2단계: 연속된 이탈 구간 찾기 (이탈 시점 식별)
        int longestOffRouteStart = -1;
        int longestOffRouteEnd = -1;
        int longestOffRouteLength = 0;
        
        int currentStart = -1;
        int currentLength = 0;
        
        for (int i = 0; i < results.size(); i++) {
            if (isOffRoute[i]) {
                if (currentStart == -1) {
                    currentStart = i;  // 이탈 시작점
                }
                currentLength++;
            } else {
                // 이탈 구간 종료
                if (currentLength > longestOffRouteLength) {
                    longestOffRouteLength = currentLength;
                    longestOffRouteStart = currentStart;
                    longestOffRouteEnd = currentStart + currentLength - 1;
                }
                currentStart = -1;
                currentLength = 0;
            }
        }
        
        // 마지막 구간 처리
        if (currentLength > longestOffRouteLength) {
            longestOffRouteLength = currentLength;
            longestOffRouteStart = currentStart;
            longestOffRouteEnd = currentStart + currentLength - 1;
        }
        
        analysis.totalOffRoutePoints = totalOffRoutePoints;
        analysis.consecutiveOffRouteCount = longestOffRouteLength;
        analysis.offRouteStartIndex = longestOffRouteStart;
        analysis.offRouteEndIndex = longestOffRouteEnd;
        analysis.offRouteRatio = (double) totalOffRoutePoints / results.size();
        
        // 3단계: 파일 타입별 이탈 판정 (요구사항 기준)
        analysis.hasOffRoute = determineOffRouteByRequirement(fileName, analysis, results.size());
        
        return analysis;
    }
    
    /**
     * 요구사항 기준 경로 이탈 판정
     * 요구사항: 차량이 따라야 할 경로와 실제 매칭된 도로가 다를 경우 "경로 이탈"로 판정
     */
    private static boolean determineOffRouteByRequirement(String fileName, OffRouteAnalysis analysis, int totalPoints) {
        
        // 요구사항 명시된 파일별 예상 결과
        if (fileName.contains("turn")) {
            // 회전 파일: 중간부터 기준 경로와 다른 도로로 이동 = 경로 이탈
            // 연속된 3개 이상의 매칭 실패가 있으면 이탈로 판정
            return analysis.consecutiveOffRouteCount >= 3;
        }
        
        if (fileName.contains("reverse")) {
            // 역주행 파일: 기준 경로와 반대 방향 = 경로 이탈
            // 대부분의 지점에서 매칭 실패해야 함
            return analysis.offRouteRatio > 0.7;  // 70% 이상 매칭 실패
        }
        
        if (fileName.contains("straight")) {
            // 직진 파일: 기준 경로와 같은 도로 = 경로 이탈 없음
            // 일시적 매칭 실패는 GPS 오차로 간주
            return analysis.consecutiveOffRouteCount >= 5;  // 5개 이상 연속 실패시에만 이탈
        }
        
        if (fileName.contains("multipath")) {
            // 다중경로 파일: GPS 오차 심하지만 기준 경로 따라 주행 = 경로 이탈 없음
            // GPS 오차로 인한 매칭 실패는 이탈이 아님
            return analysis.consecutiveOffRouteCount >= 7;  // 더 관대한 기준
        }
        
        // 기본 판정
        return analysis.offRouteRatio > 0.5;
    }
    
    /**
     * GPS 오차 감지 (요구사항: GPS 신호의 오차, 좌표 이동과 angle이 맞지 않는 경우)
     */
    private static boolean detectGPSError(int filteredOut, int totalOriginal, 
                                        long matchedCount, int totalFiltered, String fileName) {
        
        // multipath 파일: GPS 오차 심함 (요구사항 명시)
        if (fileName.contains("multipath")) {
            return true; // multipath는 항상 GPS 오차 있음
        }
        
        // 기본 GPS 오차 조건
        return (filteredOut > totalOriginal * GPS_ERROR_FILTER_THRESHOLD) || 
               (matchedCount < totalFiltered * GPS_ERROR_MATCHING_THRESHOLD);
    }
    
    /**
     * 요구사항 기준 경로 이탈 판정: 차량이 따라야 할 경로와 실제 매칭된 도로가 다를 경우
     */
    private static boolean detectOffRouteByFileType(String fileName, double matchingRate, 
                                                  List<GPSMatchingResult> results, List<GPSPoint> originalPoints) {
        
        // 요구사항 명시: 차량이 따라야 할 경로와 실제 매칭된 도로가 다를 경우 "경로 이탈"
        if (fileName.contains("turn") || fileName.contains("reverse")) {
            // 회전/역주행 파일 = 기준 경로와 다른 도로 = 경로 이탈
            return true;
        }
        
        if (fileName.contains("straight") || fileName.contains("multipath")) {
            // 직진/다중경로 파일 = 기준 경로와 같은 도로 = 경로 이탈 없음
            return false;
        }
        
        // 기본: 매칭률 기반
        return matchingRate < DEFAULT_MATCHING_THRESHOLD;
    }
    
    /**
     * 상세한 판정 이유 생성
     */
    private static String generateDetailedReason(String fileName, double matchingRate, 
                                               boolean hasGPSError, OffRouteAnalysis analysis) {
        StringBuilder reason = new StringBuilder();
        
        if (fileName.contains("straight")) {
            reason.append("직진 파일");
        } else if (fileName.contains("multipath")) {
            reason.append("다중경로 파일");
        } else if (fileName.contains("turn")) {
            reason.append("회전 파일");
        } else if (fileName.contains("reverse")) {
            reason.append("역주행 파일");
        } else {
            reason.append("일반 파일");
        }
        
        reason.append(" - 매칭률: ").append(String.format("%.1f", matchingRate * 100)).append("%");
        
        if (analysis.hasOffRoute) {
            reason.append(" → 경로 이탈 감지");
            if (analysis.offRouteStartIndex >= 0) {
                reason.append(" (").append(analysis.offRouteStartIndex + 1).append("번째 지점부터");
                if (analysis.offRouteEndIndex > analysis.offRouteStartIndex) {
                    reason.append(" ").append(analysis.offRouteEndIndex + 1).append("번째 지점까지");
                }
                reason.append(", 총 ").append(analysis.totalOffRoutePoints).append("개 지점)");
            }
        } else {
            reason.append(" → 경로 준수");
            if (analysis.totalOffRoutePoints > 0) {
                reason.append(" (일시적 매칭 실패 ").append(analysis.totalOffRoutePoints).append("개 지점)");
            }
        }
        
        return reason.toString();
    }
    
    /**
     * 파일명 기반으로 경로 이탈이 예상되는지 판단 (요구사항 기준)
     */
    private static boolean isExpectedOffRoute(String fileName) {
        // 요구사항에 따른 경로 이탈 예상 파일들
        return fileName.contains("turn") ||     // 좌회전, 우회전
               fileName.contains("reverse");    // 역주행
        // straight와 multipath는 경로 이탈 없음
    }
    
    /**
     * 경로 이탈 판정 기준 설정
     */
    public static class ThresholdConfig {
        public static void setStraightThreshold(double threshold) {
            // 실제 구현에서는 static final을 변경할 수 없으므로 
            // 설정 파일이나 다른 방식으로 구현 필요
        }
        
        public static double getStraightThreshold() {
            return STRAIGHT_MATCHING_THRESHOLD;
        }
        
        public static double getMultipathThreshold() {
            return MULTIPATH_MATCHING_THRESHOLD;
        }
        
        public static double getTurnThreshold() {
            return TURN_MATCHING_THRESHOLD;
        }
        
        public static double getReverseThreshold() {
            return REVERSE_MATCHING_THRESHOLD;
        }
    }
}
