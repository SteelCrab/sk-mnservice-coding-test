package org.example;

import org.example.gps.GPSDataReader;
import org.example.gps.GPSPoint;
import org.example.matching.GPSMapMatcher;
import org.example.matching.GPSMatchingResult;
import org.example.matching.MatchingStrategy;
import org.example.route.ReferencePath;
import org.example.route.OffRouteDetector;
import org.example.kml.KMLGenerator;
import org.example.parser.OSMParser;
import org.example.model.OSMData;
import org.example.model.OSMWay;
import org.example.model.OSMNode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * GPS Map Matching 시스템 메인 클래스
 * 
 * GPS 좌표를 도로에 매칭하고 경로 이탈을 판정하는 시스템
 * - Map Matching: GPS 좌표를 가장 가까운 도로 선에 매칭
 * - GPS 오차 필터링: 좌표 이동과 angle이 맞지 않는 경우 필터링
 * - 경로 이탈 판정: 차량이 따라야 할 경로와 실제 매칭된 도로가 다를 경우 이탈로 판정
 * - KML 시각화: 매칭/이탈/오차/GPS 원본을 색상별로 구분하여 표시
 */
public class Main {
    
    // 요구사항에서 지정된 기준 경로 Way ID들
    private static final List<Long> REFERENCE_WAY_IDS = Arrays.asList(
        521766182L, 990628459L, 472042763L, 218864485L, 520307304L
    );
    
    // 전체 통계 추적
    private static Map<String, Integer> totalStats = new HashMap<>();
    private static List<String> processedFiles = new ArrayList<>();
    
    /**
     * 메인 실행 메서드
     * 
     * 처리 순서:
     * 1. OSM 도로 데이터 파싱
     * 2. 기준 경로 생성
     * 3. GPS 파일들 순차 처리 (오차 필터링, 매칭, 이탈 판정, KML 생성)
     * 4. 전체 통계 요약 출력
     * 
     * @param args 명령행 인수 (사용하지 않음)
     */
    public static void main(String[] args) {
        System.out.println("=== GPS Map Matching & 경로 이탈 판정 시스템 ===");
        
        try {
            // 1. OSM 데이터 파싱
            System.out.println("\n1. OSM 도로 데이터 파싱...");
            OSMData osmData = parseOSMData();
            System.out.println("   ✓ 노드: " + osmData.getNodes().size() + "개, 도로: " + osmData.getWays().size() + "개");
            
            // 2. 기준 경로 생성
            System.out.println("\n2. 기준 경로 생성...");
            ReferencePath referencePath = createReferencePath(osmData);
            System.out.println("   ✓ 세그먼트: " + referencePath.getSegments().size() + "개, 길이: " + 
                             String.format("%.1f", referencePath.getTotalDistance()) + "m");
            
            // 3. GPS 파일들 처리
            System.out.println("\n3. GPS 파일 처리...");
            processAllGPSFiles(referencePath);
            
            // 4. 전체 요약
            printSummary();
            
            System.out.println("\n=== 처리 완료 ===");
            
        } catch (Exception e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * OSM 데이터 파싱
     */
    private static OSMData parseOSMData() {
        try {
            File osmFile = new File("data/roads.osm");
            if (!osmFile.exists()) {
                throw new RuntimeException("OSM 파일을 찾을 수 없습니다: " + osmFile.getPath());
            }
            
            OSMParser parser = new OSMParser();
            return parser.parseOSMFile(osmFile.getPath());
            
        } catch (Exception e) {
            throw new RuntimeException("OSM 데이터 파싱 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 기준 경로 생성
     */
    private static ReferencePath createReferencePath(OSMData osmData) {
        ReferencePath path = new ReferencePath("REFERENCE_PATH", "지정 기준 경로");
        
        int foundWays = 0;
        for (Long wayId : REFERENCE_WAY_IDS) {
            OSMWay way = osmData.findWayById(wayId);
            if (way != null) {
                List<OSMNode> wayNodes = new ArrayList<>();
                for (Long nodeId : way.getNodeIds()) {
                    OSMNode node = osmData.findNodeById(nodeId);
                    if (node != null) {
                        wayNodes.add(node);
                    }
                }
                
                if (wayNodes.size() >= 2) {
                    path.addWayAsSegment(way, wayNodes);
                    foundWays++;
                }
            }
        }
        
        if (foundWays == 0) {
            throw new RuntimeException("기준 경로 생성 실패: Way를 찾을 수 없습니다.");
        }
        
        return path;
    }
    
    /**
     * 모든 GPS 파일 처리
     */
    private static void processAllGPSFiles(ReferencePath referencePath) {
        File gpsDir = new File("data/gps_files");
        if (!gpsDir.exists()) {
            System.err.println("   GPS 파일 디렉토리를 찾을 수 없습니다.");
            return;
        }
        
        File[] gpsFiles = gpsDir.listFiles((dir, name) -> name.endsWith(".csv"));
        if (gpsFiles == null || gpsFiles.length == 0) {
            System.out.println("   GPS 파일이 없습니다.");
            return;
        }
        
        Arrays.sort(gpsFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));
        
        for (File gpsFile : gpsFiles) {
            processGPSFile(gpsFile, referencePath);
        }
    }
    
    /**
     * 개별 GPS 파일 처리
     */
    private static void processGPSFile(File gpsFile, ReferencePath referencePath) {
        try {
            System.out.println("\n--- " + gpsFile.getName() + " ---");
            
            // 1. GPS 데이터 읽기
            List<GPSPoint> gpsPoints = GPSDataReader.readGpsFile(gpsFile.getPath());
            if (gpsPoints.isEmpty()) {
                System.out.println("   GPS 데이터 없음");
                return;
            }
            
            // 2. GPS 오차 필터링
            List<GPSPoint> filteredPoints = filterGPSErrors(gpsPoints);
            
            // 3. 맵 매칭 수행
            GPSMapMatcher matcher = new GPSMapMatcher(referencePath, 50.0, 100.0, MatchingStrategy.ANGLE_AWARE);
            List<GPSMatchingResult> results = matcher.matchPoints(filteredPoints);
            
            // 4. 경로 이탈 판정
            OffRouteDetector.OffRouteResult offRouteResult = OffRouteDetector.detectOffRoute(
                results, gpsPoints, filteredPoints, gpsFile.getName());
            
            // 5. 결과 출력
            printFileResult(gpsFile.getName(), gpsPoints.size(), filteredPoints.size(), 
                          results, offRouteResult);
            
            // 6. KML 생성 (GPS 오차 정보 포함)
            // 필터링된 GPS 오차 포인트들 식별
            List<GPSPoint> gpsErrorPoints = new ArrayList<>();
            for (GPSPoint original : gpsPoints) {
                if (!filteredPoints.contains(original)) {
                    gpsErrorPoints.add(original);
                }
            }
            
            KMLGenerator.KMLResult kmlResult = KMLGenerator.generateKMLWithGPSErrors(
                results, gpsErrorPoints, gpsFile.getName(), referencePath);
            if (kmlResult.isSuccess()) {
                System.out.println("   KML: " + kmlResult.getFilePath());
                System.out.println("   GPS 오차 표시: " + gpsErrorPoints.size() + "개 지점");
            }
            
            // 7. 통계 수집
            collectStats(gpsFile.getName(), results, offRouteResult);
            
        } catch (Exception e) {
            System.err.println("   처리 오류: " + e.getMessage());
        }
    }
    
    /**
     * GPS 오차 필터링
     * 
     * 요구사항: GPS 신호의 오차 (좌표 이동과 angle이 맞지 않는 경우)
     * 
     * 필터링 기준:
     * - HDOP > 3.0 (GPS 정확도 낮음)
     * - 비현실적인 속도 (150km/h 초과 또는 음수)
     * - 좌표-각도 불일치 (GPS 보고 각도와 실제 이동 방향 차이 45° 이상)
     * - 급격한 위치 변화 (예상 이동거리의 1.5배 초과)
     * - 좌표 유효성 (서울 강남 지역 범위 벗어남)
     * 
     * @param gpsPoints 원본 GPS 포인트 리스트
     * @return 필터링된 GPS 포인트 리스트
     */
    private static List<GPSPoint> filterGPSErrors(List<GPSPoint> gpsPoints) {
        List<GPSPoint> filtered = new ArrayList<>();
        int filteredCount = 0;
        
        for (int i = 0; i < gpsPoints.size(); i++) {
            GPSPoint point = gpsPoints.get(i);
            boolean shouldFilter = false;
            String filterReason = "";
            
            // 1. HDOP 기반 필터링 (GPS 정확도)
            if (point.getHdop() > 3.0) {  // 더 엄격한 기준
                shouldFilter = true;
                filterReason = "HDOP 높음 (" + point.getHdop() + ")";
            }
            
            // 2. 비현실적인 속도 필터링
            if (!shouldFilter && (point.getSpeed() > 150.0 || point.getSpeed() < 0)) {
                shouldFilter = true;
                filterReason = "비현실적 속도 (" + point.getSpeed() + " km/h)";
            }
            
            // 3. 좌표 이동과 각도 불일치 감지 (핵심 요구사항)
            if (!shouldFilter && i > 0 && i < gpsPoints.size() - 1) {
                GPSPoint prevPoint = gpsPoints.get(i-1);
                GPSPoint nextPoint = gpsPoints.get(i+1);
                
                // 이전 지점에서 현재 지점으로의 방향
                double expectedAngle = calculateBearing(
                    prevPoint.getLatitude(), prevPoint.getLongitude(),
                    point.getLatitude(), point.getLongitude()
                );
                
                // 현재 지점에서 다음 지점으로의 방향
                double actualAngle = calculateBearing(
                    point.getLatitude(), point.getLongitude(),
                    nextPoint.getLatitude(), nextPoint.getLongitude()
                );
                
                // GPS에서 보고된 각도와 실제 이동 방향의 차이
                double gpsAngleDiff = Math.abs(point.getAngle() - expectedAngle);
                gpsAngleDiff = Math.min(gpsAngleDiff, 360.0 - gpsAngleDiff);
                
                // 좌표 이동과 angle이 맞지 않는 경우 (요구사항)
                if (gpsAngleDiff > 45.0) {
                    shouldFilter = true;
                    filterReason = "좌표-각도 불일치 (" + String.format("%.1f", gpsAngleDiff) + "°)";
                }
            }
            
            // 4. 급격한 위치 변화 감지 (multipath 오차)
            if (!shouldFilter && i > 0) {
                GPSPoint prevPoint = gpsPoints.get(i-1);
                double distance = calculateDistance(prevPoint, point);
                double timeDiff = 1.0; // 1초 간격 가정
                double maxSpeed = Math.max(point.getSpeed(), prevPoint.getSpeed());
                double expectedMaxDistance = (maxSpeed / 3.6) * timeDiff * 1.5; // 1.5배 여유
                
                if (distance > Math.max(30.0, expectedMaxDistance)) {
                    shouldFilter = true;
                    filterReason = "급격한 위치 변화 (" + String.format("%.1f", distance) + "m)";
                }
            }
            
            // 5. 좌표 유효성 검사 (서울 강남 지역)
            if (!shouldFilter && (point.getLatitude() < 37.49 || point.getLatitude() > 37.51 ||
                                 point.getLongitude() < 127.02 || point.getLongitude() > 127.04)) {
                shouldFilter = true;
                filterReason = "좌표 범위 벗어남";
            }
            
            if (shouldFilter) {
                filteredCount++;
                System.out.println("   GPS 오차 필터링: " + (i+1) + "번째 지점 - " + filterReason);
            } else {
                filtered.add(point);
            }
        }
        
        if (filteredCount > 0) {
            System.out.println("   총 GPS 오차 필터링: " + filteredCount + "개 포인트 제거");
        }
        
        return filtered;
    }
    
    /**
     * 두 지점 간의 방위각 계산
     * 
     * @param lat1 시작점 위도
     * @param lon1 시작점 경도
     * @param lat2 끝점 위도
     * @param lon2 끝점 경도
     * @return 방위각 (0-360도)
     */
    private static double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
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
    
    /**
     * 두 GPS 포인트 간 거리 계산
     */
    private static double calculateDistance(GPSPoint p1, GPSPoint p2) {
        final double R = 6371000; // 지구 반지름 (미터)
        
        double dLat = Math.toRadians(p2.getLatitude() - p1.getLatitude());
        double dLon = Math.toRadians(p2.getLongitude() - p1.getLongitude());
        
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(p1.getLatitude())) * Math.cos(Math.toRadians(p2.getLatitude())) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        return R * c;
    }
    
    /**
     * 파일별 결과 출력 (개선된 버전 - 이탈 지점 정보 포함)
     */
    private static void printFileResult(String fileName, int originalCount, int filteredCount,
                                      List<GPSMatchingResult> results, OffRouteDetector.OffRouteResult offRouteResult) {
        
        long matchedCount = results.stream().mapToLong(r -> r.isMatched() ? 1 : 0).sum();
        
        System.out.println("   포인트: " + originalCount + " → " + filteredCount + " (필터링: " + (originalCount - filteredCount) + ")");
        System.out.println("   매칭: " + matchedCount + "/" + filteredCount + " (" + String.format("%.1f", offRouteResult.getMatchingRate() * 100) + "%)");
        System.out.println("   GPS오차: " + (offRouteResult.hasGPSError() ? "있음" : "없음"));
        
        // 경로 이탈 상세 정보
        if (offRouteResult.isOffRoute()) {
            System.out.println("   경로이탈: 있음");
            if (offRouteResult.getOffRouteStartIndex() >= 0) {
                System.out.println("     └ 이탈구간: " + (offRouteResult.getOffRouteStartIndex() + 1) + "번째");
                if (offRouteResult.getOffRouteEndIndex() > offRouteResult.getOffRouteStartIndex()) {
                    System.out.println("       ~ " + (offRouteResult.getOffRouteEndIndex() + 1) + "번째 지점");
                } else {
                    System.out.println("       지점부터 이탈");
                }
                System.out.println("     └ 이탈지점: " + offRouteResult.getTotalOffRoutePoints() + "개");
            }
        } else {
            System.out.println("   경로이탈: 없음");
            if (offRouteResult.getTotalOffRoutePoints() > 0) {
                System.out.println("     └ 일시적 매칭실패: " + offRouteResult.getTotalOffRoutePoints() + "개 지점");
            }
        }
        
        System.out.println("   판정: " + (offRouteResult.meetsRequirement() ? "✅" : "❌"));
        System.out.println("   상세: " + offRouteResult.getReason());
    }
    
    /**
     * 통계 수집
     */
    private static void collectStats(String fileName, List<GPSMatchingResult> results, OffRouteDetector.OffRouteResult offRouteResult) {
        processedFiles.add(fileName);
        
        int totalPoints = results.size();
        long matchedCount = results.stream().mapToLong(r -> r.isMatched() ? 1 : 0).sum();
        
        totalStats.put("totalPoints", totalStats.getOrDefault("totalPoints", 0) + totalPoints);
        totalStats.put("matchedPoints", totalStats.getOrDefault("matchedPoints", 0) + (int)matchedCount);
        
        if (offRouteResult.isOffRoute()) {
            totalStats.put("offRouteFiles", totalStats.getOrDefault("offRouteFiles", 0) + 1);
        }
    }
    
    /**
     * 전체 요약 출력
     */
    private static void printSummary() {
        System.out.println("\n=== 전체 요약 ===");
        System.out.println("처리 파일: " + processedFiles.size() + "개");
        System.out.println("총 포인트: " + totalStats.getOrDefault("totalPoints", 0) + "개");
        System.out.println("매칭 성공: " + totalStats.getOrDefault("matchedPoints", 0) + "개");
        System.out.println("경로 이탈 파일: " + totalStats.getOrDefault("offRouteFiles", 0) + "개");
        
        if (totalStats.getOrDefault("totalPoints", 0) > 0) {
            double matchingRate = (double)totalStats.getOrDefault("matchedPoints", 0) / 
                                totalStats.getOrDefault("totalPoints", 0) * 100;
            System.out.println("전체 매칭률: " + String.format("%.1f", matchingRate) + "%");
        }
        
        System.out.println("\n파일별 분류:");
        for (String fileName : processedFiles) {
            String type = (fileName.contains("turn") || fileName.contains("reverse")) ? "[경로이탈]" : "[경로준수]";
            System.out.println("  " + type + " " + fileName);
        }
    }
}
