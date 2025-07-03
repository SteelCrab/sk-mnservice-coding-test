package org.example.kml;

import org.example.gps.GPSPoint;
import org.example.matching.GPSMatchingResult;
import org.example.route.ReferencePath;
import org.example.model.OSMNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

/**
 * KML 파일 생성 및 GPS 데이터 시각화 클래스
 * 
 * GPS Map Matching 결과를 KML 형식으로 시각화하여 Google Earth 등에서 확인 가능
 * 
 * 시각화 요소:
 * - GPS 원본: 모든 포인트 표시 (빨간색)
 * - 매칭: 모든 매칭 성공 포인트 표시 (초록색, 보정된 좌표)
 * - 이탈: 이탈된 포인트만 표시 (노란색, 포인트별 개별 판정)
 * - GPS 오차: GPS 오차 포인트만 표시 (주황색, 오차 이유 포함)
 * - 기준 경로: 파란색 선으로 표시
 * 
 * 출력 형식: [원본파일명]_result.kml (output/ 디렉토리)
 */
public class KMLGenerator {
    
    private static final String OUTPUT_DIR = "output";
    
    /**
     * KML 파일 생성 결과 클래스
     */
    public static class KMLResult {
        private final boolean success;
        private final String filePath;
        private final String message;
        
        public KMLResult(boolean success, String filePath, String message) {
            this.success = success;
            this.filePath = filePath;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getFilePath() { return filePath; }
        public String getMessage() { return message; }
    }
    
    /**
     * GPS 오차 포인트를 포함한 KML 생성 (간단한 방식)
     */
    public static KMLResult generateKMLWithGPSErrors(List<GPSMatchingResult> results, 
                                                   List<GPSPoint> gpsErrorPoints,
                                                   String fileName, 
                                                   ReferencePath referencePath) {
        try {
            String outputFileName = fileName.replace(".csv", "_result.kml");
            String outputPath = OUTPUT_DIR + "/" + outputFileName;
            
            // output 디렉토리 생성
            createOutputDirectory();
            
            // 이탈 시점 분석
            OffRouteAnalysis offRouteAnalysis = analyzeOffRoutePoints(results, fileName);
            
            // KML 내용 생성 (GPS 오차 포함)
            String kmlContent = buildKMLContentWithGPSErrors(results, gpsErrorPoints, fileName, referencePath, offRouteAnalysis);
            
            // 파일 저장
            writeKMLFile(outputPath, kmlContent);
            
            return new KMLResult(true, outputPath, "KML 파일 생성 성공");
            
        } catch (Exception e) {
            return new KMLResult(false, "", "KML 파일 생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * GPS 오차를 포함한 KML 내용 생성 (포인트별 이탈 판정)
     */
    private static String buildKMLContentWithGPSErrors(List<GPSMatchingResult> results, 
                                                     List<GPSPoint> gpsErrorPoints,
                                                     String fileName, 
                                                     ReferencePath referencePath,
                                                     OffRouteAnalysis offRouteAnalysis) {
        StringBuilder kml = new StringBuilder();
        
        // KML 헤더
        kml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        kml.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        kml.append("  <Document>\n");
        kml.append("    <name>GPS Map Matching Result - ").append(fileName).append("</name>\n");
        
        // 스타일 정의
        appendStyles(kml);
        
        // 기준 경로
        appendReferencePath(kml, referencePath);
        
        // 🔴 GPS 원본: 모든 포인트 표시
        appendAllOriginalGPSPoints(kml, results);
        
        // 🟢 매칭: 모든 매칭 성공 포인트 표시
        appendAllMatchedPoints(kml, results);
        
        // 🟡 이탈: 이탈된 포인트만 표시 (포인트별 개별 판정)
        appendOffRoutePointsOnly(kml, results, offRouteAnalysis);
        
        // 🟠 GPS 오차: GPS 오차 포인트만 표시 (오차 이유 포함)
        appendGPSErrorPointsWithReason(kml, gpsErrorPoints);
        
        // KML 푸터
        kml.append("  </Document>\n");
        kml.append("</kml>\n");
        
        return kml.toString();
    }
    
    /**
     * 이탈된 포인트만 표시 (포인트별 개별 판정)
     * 요구사항: "해당 좌표를 경로 이탈로 판정"
     */
    private static void appendOffRoutePointsOnly(StringBuilder kml, 
                                               List<GPSMatchingResult> results,
                                               String fileName, 
                                               OffRouteAnalysis offRouteAnalysis) {
        
        for (int i = 0; i < results.size(); i++) {
            GPSMatchingResult result = results.get(i);
            GPSPoint originalPoint = result.getOriginalPoint();
            
            // 포인트별 이탈 판정: 매칭 실패 = 기준 경로와 다른 도로 또는 도로 밖
            if (!result.isMatched()) {
                
                // 이탈 시점 고려하여 실제 경로 이탈인지 판정
                boolean isActualOffRoute = false;
                
                if (offRouteAnalysis.hasOffRoute && 
                    i >= offRouteAnalysis.offRouteStartIndex && 
                    i <= offRouteAnalysis.offRouteEndIndex) {
                    // 실제 경로 이탈 구간의 포인트
                    isActualOffRoute = true;
                }
                
                if (isActualOffRoute) {
                    // 경로 이탈 포인트만 표시
                    kml.append("    <Placemark>\n");
                    kml.append("      <name>이탈-").append(i + 1).append("</name>\n");
                    kml.append("      <description>차량이 따라야 할 경로와 실제 매칭된 도로가 다름</description>\n");
                    kml.append("      <styleUrl>#offRoute</styleUrl>\n");
                    kml.append("      <Point>\n");
                    kml.append("        <coordinates>").append(originalPoint.getLongitude())
                       .append(",").append(originalPoint.getLatitude()).append(",0</coordinates>\n");
                    kml.append("      </Point>\n");
                    kml.append("    </Placemark>\n");
                }
                // 이탈이 아닌 매칭 실패는 GPS 오차로 간주하여 여기서는 표시하지 않음
            }
            // 매칭 성공한 포인트들은 표시하지 않음 (정상이므로)
        }
    }
    
    /**
     * GPS 오차 포인트들 추가
     */
    private static void appendGPSErrorPoints(StringBuilder kml, List<GPSPoint> gpsErrorPoints) {
        for (int i = 0; i < gpsErrorPoints.size(); i++) {
            GPSPoint errorPoint = gpsErrorPoints.get(i);
            
            kml.append("    <Placemark>\n");
            kml.append("      <name>GPS오차-").append(i + 1).append("</name>\n");
            kml.append("      <description>좌표-각도 불일치로 필터링된 GPS 오차 지점</description>\n");
            kml.append("      <styleUrl>#gpsError</styleUrl>\n");
            kml.append("      <Point>\n");
            kml.append("        <coordinates>").append(errorPoint.getLongitude())
               .append(",").append(errorPoint.getLatitude()).append(",0</coordinates>\n");
            kml.append("      </Point>\n");
            kml.append("    </Placemark>\n");
        }
    }
    
    /**
     * 이탈 지점 분석 결과 (KML용)
     */
    private static class OffRouteAnalysis {
        boolean hasOffRoute = false;
        int offRouteStartIndex = -1;
        int offRouteEndIndex = -1;
        int consecutiveOffRouteCount = 0;
    }
    
    /**
     * 이탈 지점 분석 (KML 시각화용)
     */
    private static OffRouteAnalysis analyzeOffRoutePoints(List<GPSMatchingResult> results, String fileName) {
        OffRouteAnalysis analysis = new OffRouteAnalysis();
        
        if (results.isEmpty()) {
            return analysis;
        }
        
        // 연속된 매칭 실패 구간 찾기
        int currentOffRouteStart = -1;
        int maxConsecutiveOffRoute = 0;
        int currentConsecutive = 0;
        
        for (int i = 0; i < results.size(); i++) {
            GPSMatchingResult result = results.get(i);
            
            if (!result.isMatched()) {
                if (currentOffRouteStart == -1) {
                    currentOffRouteStart = i;  // 이탈 시작점
                }
                currentConsecutive++;
                
                if (currentConsecutive > maxConsecutiveOffRoute) {
                    maxConsecutiveOffRoute = currentConsecutive;
                    analysis.offRouteStartIndex = currentOffRouteStart;
                    analysis.offRouteEndIndex = i;
                }
            } else {
                // 매칭 성공 = 경로 복귀
                currentOffRouteStart = -1;
                currentConsecutive = 0;
            }
        }
        
        analysis.consecutiveOffRouteCount = maxConsecutiveOffRoute;
        
        // 파일 타입별 이탈 판정
        if (fileName.contains("turn") || fileName.contains("reverse")) {
            analysis.hasOffRoute = analysis.consecutiveOffRouteCount >= 3;
        } else {
            analysis.hasOffRoute = analysis.consecutiveOffRouteCount >= 5;
        }
        
        return analysis;
    }
    
    /**
     * 출력 디렉토리 생성
     */
    private static void createOutputDirectory() {
        File outputDir = new File(OUTPUT_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
    }
    
    /**
     * KML 내용 구성
     */
    private static String buildKMLContent(List<GPSMatchingResult> results, 
                                        String fileName, 
                                        ReferencePath referencePath,
                                        OffRouteAnalysis offRouteAnalysis) {
        StringBuilder kml = new StringBuilder();
        
        // KML 헤더
        kml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        kml.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        kml.append("  <Document>\n");
        kml.append("    <name>GPS Map Matching Result - ").append(fileName).append("</name>\n");
        
        // 스타일 정의]



        
        appendStyles(kml);
        
        // 기준 경로
        appendReferencePath(kml, referencePath);
        
        // GPS 포인트들 (기존 방식 - 임시)
        appendGPSPoints(kml, results, fileName);
        
        // KML 푸터
        kml.append("  </Document>\n");
        kml.append("</kml>\n");
        
        return kml.toString();
    }
    
    /**
     * 스타일 정의
     */
    private static void appendStyles(StringBuilder kml) {
        // 원본 GPS (빨간색)
        kml.append("    <Style id=\"originalGPS\">\n");
        kml.append("      <IconStyle>\n");
        kml.append("        <color>ff0000ff</color>\n");
        kml.append("        <scale>0.6</scale>\n");
        kml.append("      </IconStyle>\n");
        kml.append("    </Style>\n");
        
        // 정상 매칭 (초록색)
        kml.append("    <Style id=\"normalMatch\">\n");
        kml.append("      <IconStyle>\n");
        kml.append("        <color>ff00ff00</color>\n");
        kml.append("        <scale>0.6</scale>\n");
        kml.append("      </IconStyle>\n");
        kml.append("    </Style>\n");
        
        // 경로 이탈 (노란색)
        kml.append("    <Style id=\"offRoute\">\n");
        kml.append("      <IconStyle>\n");
        kml.append("        <color>ff00ffff</color>\n");
        kml.append("        <scale>0.8</scale>\n");
        kml.append("      </IconStyle>\n");
        kml.append("    </Style>\n");
        
        // GPS 오차 (주황색)
        kml.append("    <Style id=\"gpsError\">\n");
        kml.append("      <IconStyle>\n");
        kml.append("        <color>ff0080ff</color>\n");
        kml.append("        <scale>0.7</scale>\n");
        kml.append("      </IconStyle>\n");
        kml.append("    </Style>\n");
        
        // 기준 경로 (파란색 선)
        kml.append("    <Style id=\"referencePath\">\n");
        kml.append("      <LineStyle>\n");
        kml.append("        <color>ffff0000</color>\n");
        kml.append("        <width>4</width>\n");
        kml.append("      </LineStyle>\n");
        kml.append("    </Style>\n");
    }
    
    /**
     * 기준 경로 추가
     */
    private static void appendReferencePath(StringBuilder kml, ReferencePath referencePath) {
        kml.append("    <Placemark>\n");
        kml.append("      <name>기준 경로</name>\n");
        kml.append("      <styleUrl>#referencePath</styleUrl>\n");
        kml.append("      <LineString>\n");
        kml.append("        <coordinates>\n");
        
        for (var segment : referencePath.getSegments()) {
            OSMNode startNode = segment.getStartNode();
            OSMNode endNode = segment.getEndNode();
            if (startNode != null && endNode != null) {
                kml.append("          ").append(startNode.getLongitude())
                   .append(",").append(startNode.getLatitude()).append(",0\n");
                kml.append("          ").append(endNode.getLongitude())
                   .append(",").append(endNode.getLatitude()).append(",0\n");
            }
        }
        
        kml.append("        </coordinates>\n");
        kml.append("      </LineString>\n");
        kml.append("    </Placemark>\n");
    }
    
    /**
     * GPS 포인트들 추가 (요구사항 정확 준수)
     */
    private static void appendGPSPoints(StringBuilder kml, List<GPSMatchingResult> results, String fileName) {
        for (int i = 0; i < results.size(); i++) {
            GPSMatchingResult result = results.get(i);
            GPSPoint originalPoint = result.getOriginalPoint();
            
            // 원본 GPS 포인트
            kml.append("    <Placemark>\n");
            kml.append("      <name>GPS-").append(i + 1).append("</name>\n");
            kml.append("      <styleUrl>#originalGPS</styleUrl>\n");
            kml.append("      <Point>\n");
            kml.append("        <coordinates>").append(originalPoint.getLongitude())
               .append(",").append(originalPoint.getLatitude()).append(",0</coordinates>\n");
            kml.append("      </Point>\n");
            kml.append("    </Placemark>\n");
            
            // 요구사항 기준 판정
            String style;
            String name;
            
            if (!result.isMatched()) {
                // 매칭 실패 = 경로 이탈
                style = "#offRoute";
                name = "이탈-" + (i + 1);
                
                kml.append("    <Placemark>\n");
                kml.append("      <name>").append(name).append("</name>\n");
                kml.append("      <styleUrl>").append(style).append("</styleUrl>\n");
                kml.append("      <Point>\n");
                kml.append("        <coordinates>").append(originalPoint.getLongitude())
                   .append(",").append(originalPoint.getLatitude()).append(",0</coordinates>\n");
                kml.append("      </Point>\n");
                kml.append("    </Placemark>\n");
                
            } else {
                // 매칭 성공 - 요구사항 기준 판정
                
                // 1. 차량이 따라야 할 경로와 실제 매칭된 도로가 다른지 확인
                boolean isDifferentRoad = isOffReferenceRoute(result, fileName);
                
                // 2. GPS 좌표와 도로 선 간의 거리 확인
                double distanceToRoad = result.getMatchingDistance();
                
                // 3. GPS 신호 오차 확인
                boolean hasGPSError = hasGPSSignalError(originalPoint, i, results);
                
                // 요구사항 기준 최종 판정
                if (isDifferentRoad || distanceToRoad > 50.0) {
                    // 경로 이탈: 다른 도로이거나 거리 50m 이상
                    style = "#offRoute";
                    name = "이탈-" + (i + 1);
                } else if (hasGPSError) {
                    // GPS 오차: 신호 품질 문제
                    style = "#gpsError";
                    name = "오차-" + (i + 1);
                } else {
                    // 정상 매칭
                    style = "#normalMatch";
                    name = "정상-" + (i + 1);
                }
                
                double[] correctedCoords = result.getCorrectedCoordinates();
                
                kml.append("    <Placemark>\n");
                kml.append("      <name>").append(name).append("</name>\n");
                kml.append("      <styleUrl>").append(style).append("</styleUrl>\n");
                kml.append("      <Point>\n");
                kml.append("        <coordinates>").append(correctedCoords[1])
                   .append(",").append(correctedCoords[0]).append(",0</coordinates>\n");
                kml.append("      </Point>\n");
                kml.append("    </Placemark>\n");
            }
        }
    }
    
    /**
     * 차량이 따라야 할 경로와 실제 매칭된 도로가 다른지 확인 (요구사항 핵심)
     */
    private static boolean isOffReferenceRoute(GPSMatchingResult result, String fileName) {
        // 파일명 기반 경로 이탈 판정 (요구사항 명시)
        if (fileName.contains("turn") || fileName.contains("reverse")) {
            // 회전/역주행 파일 = 기준 경로와 다른 도로
            return true;
        }
        
        // 직진/다중경로 파일은 매칭 거리로 판단
        return result.getMatchingDistance() > 100.0;
    }
    
    /**
     * GPS 신호 오차 확인 (좌표 이동과 angle이 맞지 않는 경우)
     */
    private static boolean hasGPSSignalError(GPSPoint gpsPoint, int index, List<GPSMatchingResult> results) {
        // 1. HDOP 기반 신호 품질 확인
        if (gpsPoint.getHdop() > 3.0) {
            return true;
        }
        
        // 2. 좌표 이동과 각도 불일치 확인
        if (index > 0) {
            GPSPoint prevPoint = results.get(index - 1).getOriginalPoint();
            
            double distance = calculateDistance(prevPoint, gpsPoint);
            double angleDiff = Math.abs(gpsPoint.getAngle() - prevPoint.getAngle());
            angleDiff = Math.min(angleDiff, 360.0 - angleDiff);
            
            // 급격한 위치 변화 + 각도 변화 = GPS 오차
            if (distance > 50.0 && angleDiff > 90.0) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 두 GPS 포인트 간 거리 계산
     */
    private static double calculateDistance(GPSPoint p1, GPSPoint p2) {
        final double R = 6371000;
        
        double dLat = Math.toRadians(p2.getLatitude() - p1.getLatitude());
        double dLon = Math.toRadians(p2.getLongitude() - p1.getLongitude());
        
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(p1.getLatitude())) * Math.cos(Math.toRadians(p2.getLatitude())) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        return R * c;
    }
    
    /**
     * 차량이 따라야 할 경로와 실제 매칭된 도로가 다른지 확인
     */
    private static boolean checkIfDifferentRoad(GPSMatchingResult result) {
        // 매칭된 세그먼트가 기준 경로에 속하는지 확인
        // 현재는 단순화하여 매칭 거리로 판단
        return result.getMatchingDistance() > 100.0;
    }
    
    /**
     * 진행 각도 일치성 확인
     */
    private static boolean checkAngleConsistency(GPSPoint gpsPoint, GPSMatchingResult result, 
                                               int index, List<GPSMatchingResult> results) {
        if (index == 0) return true; // 첫 번째 포인트는 비교 불가
        
        // 이전 포인트와의 각도 변화 확인
        GPSPoint prevPoint = results.get(index - 1).getOriginalPoint();
        double angleDiff = Math.abs(gpsPoint.getAngle() - prevPoint.getAngle());
        angleDiff = Math.min(angleDiff, 360.0 - angleDiff);
        
        // 45도 이상 급격한 변화는 각도 불일치로 판정
        return angleDiff <= 45.0;
    }
    
    /**
     * GPS 신호 오차 확인 (좌표 이동과 angle이 맞지 않는 경우)
     */
    private static boolean checkGPSSignalError(GPSPoint gpsPoint, int index, List<GPSMatchingResult> results) {
        if (index == 0) return false; // 첫 번째 포인트는 비교 불가
        
        GPSPoint prevPoint = results.get(index - 1).getOriginalPoint();
        
        // 1. HDOP 기반 신호 품질 확인
        if (gpsPoint.getHdop() > 3.0) {
            return true;
        }
        
        // 2. 좌표 이동과 각도 불일치 확인
        double distance = calculateDistance(prevPoint, gpsPoint);
        double angleDiff = Math.abs(gpsPoint.getAngle() - prevPoint.getAngle());
        angleDiff = Math.min(angleDiff, 360.0 - angleDiff);
        
        // 급격한 위치 변화 + 각도 변화 = GPS 오차
        if (distance > 50.0 && angleDiff > 90.0) {
            return true;
        }
        
        // 3. 속도와 이동거리 불일치
        double expectedDistance = gpsPoint.getSpeed() * 1000.0 / 3600.0; // km/h -> m/s
        if (expectedDistance > 5.0 && Math.abs(distance - expectedDistance) > expectedDistance * 2.0) {
            return true;
        }
        
        return false;
    }
    
    
    /**
     * KML 내용 구성 (원본 GPS 포인트 포함)
     */
    private static String buildKMLContentWithOriginalPoints(List<GPSMatchingResult> results, 
                                                          List<GPSPoint> originalPoints,
                                                          List<GPSPoint> filteredPoints,
                                                          String fileName, 
                                                          ReferencePath referencePath,
                                                          OffRouteAnalysis offRouteAnalysis) {
        StringBuilder kml = new StringBuilder();
        
        // KML 헤더
        kml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        kml.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        kml.append("  <Document>\n");
        kml.append("    <name>GPS Map Matching Result - ").append(fileName).append("</name>\n");
        
        // 스타일 정의
        appendStyles(kml);
        
        // 기준 경로
        appendReferencePath(kml, referencePath);
        
        // GPS 포인트들 (원본 포함, GPS 오차 시각화)
        appendAllGPSPoints(kml, results, originalPoints, filteredPoints, fileName, offRouteAnalysis);
        
        // KML 푸터
        kml.append("  </Document>\n");
        kml.append("</kml>\n");
        
        return kml.toString();
    }
    
    /**
     * 모든 GPS 포인트 추가 (GPS 오차 포함)
     */
    private static void appendAllGPSPoints(StringBuilder kml, 
                                         List<GPSMatchingResult> results,
                                         List<GPSPoint> originalPoints,
                                         List<GPSPoint> filteredPoints,
                                         String fileName, 
                                         OffRouteAnalysis offRouteAnalysis) {
        
        // 필터링된 포인트들을 Set으로 변환 (빠른 검색용)
        Set<GPSPoint> filteredSet = new HashSet<>(filteredPoints);
        
        // 매칭 결과를 Map으로 변환 (빠른 검색용)
        Map<GPSPoint, GPSMatchingResult> resultMap = new HashMap<>();
        for (GPSMatchingResult result : results) {
            resultMap.put(result.getOriginalPoint(), result);
        }
        
        // 모든 원본 GPS 포인트 처리
        for (int i = 0; i < originalPoints.size(); i++) {
            GPSPoint originalPoint = originalPoints.get(i);
            
            // 원본 GPS 포인트 표시
            kml.append("    <Placemark>\n");
            kml.append("      <name>GPS-").append(i + 1).append("</name>\n");
            kml.append("      <styleUrl>#originalGPS</styleUrl>\n");
            kml.append("      <Point>\n");
            kml.append("        <coordinates>").append(originalPoint.getLongitude())
               .append(",").append(originalPoint.getLatitude()).append(",0</coordinates>\n");
            kml.append("      </Point>\n");
            kml.append("    </Placemark>\n");
            
            // GPS 오차로 필터링된 포인트인지 확인
            if (!filteredSet.contains(originalPoint)) {
                // GPS 오차로 필터링된 포인트
                kml.append("    <Placemark>\n");
                kml.append("      <name>GPS오차-").append(i + 1).append("</name>\n");
                kml.append("      <styleUrl>#gpsError</styleUrl>\n");
                kml.append("      <Point>\n");
                kml.append("        <coordinates>").append(originalPoint.getLongitude())
                   .append(",").append(originalPoint.getLatitude()).append(",0</coordinates>\n");
                kml.append("      </Point>\n");
                kml.append("    </Placemark>\n");
                continue;
            }
            
            // 매칭 결과 확인
            GPSMatchingResult result = resultMap.get(originalPoint);
            if (result == null) continue;
            
            if (!result.isMatched()) {
                // 매칭 실패 - 이탈 시점 고려하여 판정
                String style;
                String name;
                
                // 필터링된 포인트의 인덱스 찾기
                int filteredIndex = filteredPoints.indexOf(originalPoint);
                
                // 이탈 시점부터만 이탈로 표시
                if (offRouteAnalysis.hasOffRoute && 
                    filteredIndex >= offRouteAnalysis.offRouteStartIndex && 
                    filteredIndex <= offRouteAnalysis.offRouteEndIndex) {
                    // 실제 경로 이탈 구간
                    style = "#offRoute";
                    name = "이탈-" + (i + 1);
                } else {
                    // 이탈 구간 밖의 매칭 실패 = GPS 오차
                    style = "#gpsError";
                    name = "GPS오차-" + (i + 1);
                }
                
                kml.append("    <Placemark>\n");
                kml.append("      <name>").append(name).append("</name>\n");
                kml.append("      <styleUrl>").append(style).append("</styleUrl>\n");
                kml.append("      <Point>\n");
                kml.append("        <coordinates>").append(originalPoint.getLongitude())
                   .append(",").append(originalPoint.getLatitude()).append(",0</coordinates>\n");
                kml.append("      </Point>\n");
                kml.append("    </Placemark>\n");
                
            } else {
                // 매칭 성공 - 정상 매칭 포인트 표시
                String style = "#normalMatch";
                String name = "매칭-" + (i + 1);
                
                double[] correctedCoords = result.getCorrectedCoordinates();
                
                kml.append("    <Placemark>\n");
                kml.append("      <name>").append(name).append("</name>\n");
                kml.append("      <styleUrl>").append(style).append("</styleUrl>\n");
                kml.append("      <Point>\n");
                kml.append("        <coordinates>").append(correctedCoords[1])
                   .append(",").append(correctedCoords[0]).append(",0</coordinates>\n");
                kml.append("      </Point>\n");
                kml.append("    </Placemark>\n");
            }
        }
    }
    
    /**
     * GPS 원본 포인트 표시
     * 
     * 모든 원본 GPS 좌표를 빨간색 아이콘으로 표시
     * GPS 궤적의 전체적인 패턴 파악을 위한 기준점
     * 
     * @param kml KML 문자열 빌더
     * @param results GPS 매칭 결과 리스트
     */
    private static void appendAllOriginalGPSPoints(StringBuilder kml, List<GPSMatchingResult> results) {
        for (int i = 0; i < results.size(); i++) {
            GPSMatchingResult result = results.get(i);
            GPSPoint originalPoint = result.getOriginalPoint();
            
            kml.append("    <Placemark>\n");
            kml.append("      <name>GPS-").append(i + 1).append("</name>\n");
            kml.append("      <description>원본 GPS 좌표</description>\n");
            kml.append("      <styleUrl>#originalGPS</styleUrl>\n");
            kml.append("      <Point>\n");
            kml.append("        <coordinates>").append(originalPoint.getLongitude())
               .append(",").append(originalPoint.getLatitude()).append(",0</coordinates>\n");
            kml.append("      </Point>\n");
            kml.append("    </Placemark>\n");
        }
    }
    
    /**
     * 매칭 성공 포인트 표시
     * 
     * GPS 좌표가 도로에 성공적으로 매칭된 지점을 초록색으로 표시
     * 보정된 좌표 사용 (도로 위의 정확한 위치)
     * 
     * @param kml KML 문자열 빌더
     * @param results GPS 매칭 결과 리스트
     */
    private static void appendAllMatchedPoints(StringBuilder kml, List<GPSMatchingResult> results) {
        for (int i = 0; i < results.size(); i++) {
            GPSMatchingResult result = results.get(i);
            
            if (result.isMatched()) {
                double[] correctedCoords = result.getCorrectedCoordinates();
                
                kml.append("    <Placemark>\n");
                kml.append("      <name>매칭-").append(i + 1).append("</name>\n");
                kml.append("      <description>GPS 좌표가 도로에 성공적으로 매칭됨</description>\n");
                kml.append("      <styleUrl>#normalMatch</styleUrl>\n");
                kml.append("      <Point>\n");
                kml.append("        <coordinates>").append(correctedCoords[1])
                   .append(",").append(correctedCoords[0]).append(",0</coordinates>\n");
                kml.append("      </Point>\n");
                kml.append("    </Placemark>\n");
            }
        }
    }
    
    /**
     * 이탈 포인트 표시 (포인트별 개별 판정)
     * 
     * 요구사항: 차량이 따라야 할 경로와 실제 매칭된 도로가 다를 경우 해당 좌표를 경로 이탈로 판정
     * 
     * 매칭 실패 && 실제 이탈 구간에 해당하는 포인트만 노란색으로 표시
     * GPS 오차로 인한 일시적 매칭 실패는 제외
     * 
     * @param kml KML 문자열 빌더
     * @param results GPS 매칭 결과 리스트
     * @param offRouteAnalysis 이탈 분석 결과
     */
    private static void appendOffRoutePointsOnly(StringBuilder kml, 
                                               List<GPSMatchingResult> results,
                                               OffRouteAnalysis offRouteAnalysis) {
        
        for (int i = 0; i < results.size(); i++) {
            GPSMatchingResult result = results.get(i);
            GPSPoint originalPoint = result.getOriginalPoint();
            
            // 포인트별 이탈 판정: 매칭 실패 && 실제 이탈 구간
            if (!result.isMatched() && 
                offRouteAnalysis.hasOffRoute && 
                i >= offRouteAnalysis.offRouteStartIndex && 
                i <= offRouteAnalysis.offRouteEndIndex) {
                
                kml.append("    <Placemark>\n");
                kml.append("      <name>이탈-").append(i + 1).append("</name>\n");
                kml.append("      <description>차량이 따라야 할 경로와 실제 매칭된 도로가 다름</description>\n");
                kml.append("      <styleUrl>#offRoute</styleUrl>\n");
                kml.append("      <Point>\n");
                kml.append("        <coordinates>").append(originalPoint.getLongitude())
                   .append(",").append(originalPoint.getLatitude()).append(",0</coordinates>\n");
                kml.append("      </Point>\n");
                kml.append("    </Placemark>\n");
            }
        }
    }
    
    /**
     * GPS 오차 포인트 표시 (오차 이유 포함)
     * 
     * 요구사항: GPS 신호의 오차 (좌표 이동과 angle이 맞지 않는 경우)
     * 
     * GPS 오차로 필터링된 포인트들을 주황색으로 표시
     * 오차 이유 분석: HDOP 높음, 비현실적 속도, 좌표-각도 불일치 등
     * 
     * @param kml KML 문자열 빌더
     * @param gpsErrorPoints GPS 오차로 필터링된 포인트 리스트
     */
    private static void appendGPSErrorPointsWithReason(StringBuilder kml, List<GPSPoint> gpsErrorPoints) {
        for (int i = 0; i < gpsErrorPoints.size(); i++) {
            GPSPoint errorPoint = gpsErrorPoints.get(i);
            
            // 오차 이유 분석
            String errorReason = analyzeGPSErrorReason(errorPoint);
            
            kml.append("    <Placemark>\n");
            kml.append("      <name>GPS오차-").append(i + 1).append("</name>\n");
            kml.append("      <description>GPS 오차: ").append(errorReason).append("</description>\n");
            kml.append("      <styleUrl>#gpsError</styleUrl>\n");
            kml.append("      <Point>\n");
            kml.append("        <coordinates>").append(errorPoint.getLongitude())
               .append(",").append(errorPoint.getLatitude()).append(",0</coordinates>\n");
            kml.append("      </Point>\n");
            kml.append("    </Placemark>\n");
        }
    }
    
    /**
     * GPS 오차 이유 분석
     * 
     * @param errorPoint GPS 오차 포인트
     * @return 오차 이유 문자열
     */
    private static String analyzeGPSErrorReason(GPSPoint errorPoint) {
        StringBuilder reason = new StringBuilder();
        
        // HDOP 기반 오차
        if (errorPoint.getHdop() > 3.0) {
            reason.append("HDOP 높음(").append(errorPoint.getHdop()).append(")");
        }
        
        // 비현실적 속도
        if (errorPoint.getSpeed() > 150.0 || errorPoint.getSpeed() < 0) {
            if (reason.length() > 0) reason.append(", ");
            reason.append("비현실적 속도(").append(errorPoint.getSpeed()).append("km/h)");
        }
        
        // 기본적으로 좌표-각도 불일치로 간주
        if (reason.length() == 0) {
            reason.append("좌표-각도 불일치");
        }
        
        return reason.toString();
    }
    
    /**
     * KML 파일 저장
     */
    private static void writeKMLFile(String filePath, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
        }
    }
}
