package org.example.gps;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GPS CSV 파일을 읽고 처리하는 클래스
 * 역할: CSV 형식의 GPS 데이터 파일을 읽어서 GPSPoint 객체 목록으로 변환
 * 구조: 파일 읽기, 데이터 파싱, 유효성 검증, 통계 정보 제공 등을 포함
 */
public class GPSDataReader {
    private static final String DEFAULT_GPS_DATA_PATH = "data/gps_files";  // 기본 GPS 데이터 경로
    
    /**
     * 단일 GPS CSV 파일을 읽어서 GPSPoint 목록으로 변환
     * @param filePath GPS CSV 파일 경로
     * @return GPS 포인트 목록 (파싱 실패 시 빈 목록)
     */
    public static List<GPSPoint> readGpsFile(String filePath) {
        List<GPSPoint> gpsPoints = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            boolean isFirstLine = true;  // 헤더 라인 스킵용
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // 첫 번째 라인은 헤더이므로 스킵
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                // 빈 라인 스킵
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    // CSV 라인을 GPSPoint로 파싱
                    GPSPoint gpsPoint = parseGpsLine(line, lineNumber - 1);  // 헤더 제외한 순서
                    
                    // 유효한 GPS 데이터만 추가
                    if (gpsPoint != null && gpsPoint.isValid()) {
                        gpsPoints.add(gpsPoint);
                    } else {
                        System.err.printf("경고: %s 파일의 %d번째 라인에 잘못된 GPS 데이터가 있습니다: %s%n", 
                                        filePath, lineNumber, line);
                    }
                    
                } catch (Exception e) {
                    System.err.printf("오류: %s 파일의 %d번째 라인 파싱 실패: %s (%s)%n", 
                                    filePath, lineNumber, line, e.getMessage());
                }
            }
            
            System.out.printf("성공: %s 파일에서 %d개의 GPS 포인트를 읽었습니다.%n", filePath, gpsPoints.size());
            
        } catch (IOException e) {
            System.err.printf("파일 읽기 오류: %s (%s)%n", filePath, e.getMessage());
        }
        
        return gpsPoints;
    }
    
    /**
     * 지정된 디렉토리의 모든 GPS CSV 파일을 읽어서 통합 목록으로 반환
     * @param directoryPath GPS 파일들이 있는 디렉토리 경로
     * @return 모든 GPS 파일의 통합 포인트 목록
     */
    public static List<GPSPoint> readAllGpsFiles(String directoryPath) {
        List<GPSPoint> allGpsPoints = new ArrayList<>();
        
        try {
            Path dir = Paths.get(directoryPath);
            
            // 디렉토리가 존재하지 않으면 빈 목록 반환
            if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                System.err.printf("디렉토리가 존재하지 않습니다: %s%n", directoryPath);
                return allGpsPoints;
            }
            
            // .csv 확장자를 가진 파일들만 필터링
            List<Path> csvFiles = Files.list(dir)
                    .filter(path -> path.toString().toLowerCase().endsWith(".csv"))
                    .collect(Collectors.toList());
            
            System.out.printf("발견된 GPS 파일 수: %d개%n", csvFiles.size());
            
            // 각 CSV 파일을 순차적으로 처리
            for (Path csvFile : csvFiles) {
                System.out.printf("처리 중: %s%n", csvFile.getFileName());
                List<GPSPoint> filePoints = readGpsFile(csvFile.toString());
                allGpsPoints.addAll(filePoints);
            }
            
            System.out.printf("전체 GPS 포인트 수: %d개%n", allGpsPoints.size());
            
        } catch (IOException e) {
            System.err.printf("디렉토리 읽기 오류: %s (%s)%n", directoryPath, e.getMessage());
        }
        
        return allGpsPoints;
    }
    
    /**
     * 기본 GPS 데이터 디렉토리의 모든 파일 읽기
     * @return 기본 디렉토리의 모든 GPS 포인트 목록
     */
    public static List<GPSPoint> readDefaultGpsFiles() {
        return readAllGpsFiles(DEFAULT_GPS_DATA_PATH);
    }
    
    /**
     * CSV 라인을 파싱하여 GPSPoint 객체로 변환
     * CSV 형식: Latitude,Longitude,Angle,Speed (km/h),HDOP
     * @param line CSV 라인 문자열
     * @param sequenceNumber 순서 번호
     * @return 파싱된 GPSPoint 객체 (파싱 실패 시 null)
     */
    private static GPSPoint parseGpsLine(String line, int sequenceNumber) {
        try {
            // CSV 라인을 콤마로 분할
            String[] parts = line.split(",");
            
            // 필드 수 검증 (최소 5개: lat, lon, angle, speed, hdop)
            if (parts.length < 5) {
                throw new IllegalArgumentException("CSV 필드 수가 부족합니다. 필요: 5개, 실제: " + parts.length);
            }
            
            // 각 필드 파싱
            double latitude = Double.parseDouble(parts[0].trim());
            double longitude = Double.parseDouble(parts[1].trim());
            double angle = Double.parseDouble(parts[2].trim());
            double speed = Double.parseDouble(parts[3].trim());
            double hdop = Double.parseDouble(parts[4].trim());
            
            // GPSPoint 객체 생성 (타임스탬프는 현재 시간으로 설정)
            return new GPSPoint(latitude, longitude, angle, speed, hdop, 
                              LocalDateTime.now(), sequenceNumber);
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("숫자 파싱 오류: " + e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException("라인 파싱 오류: " + e.getMessage());
        }
    }
    
    /**
     * GPS 데이터 목록의 통계 정보 생성
     * @param gpsPoints GPS 포인트 목록
     * @return GPS 데이터 통계 정보
     */
    public static GPSStatistics calculateStatistics(List<GPSPoint> gpsPoints) {
        if (gpsPoints == null || gpsPoints.isEmpty()) {
            return new GPSStatistics();  // 빈 통계 반환
        }
        
        // 기본 통계 계산
        int totalPoints = gpsPoints.size();
        double totalDistance = 0.0;
        double minSpeed = Double.MAX_VALUE;
        double maxSpeed = Double.MIN_VALUE;
        double avgSpeed = 0.0;
        double minHdop = Double.MAX_VALUE;
        double maxHdop = Double.MIN_VALUE;
        double avgHdop = 0.0;
        
        // 정확도별 카운트
        int excellentCount = 0;
        int goodCount = 0;
        int moderateCount = 0;
        int fairCount = 0;
        int poorCount = 0;
        
        // 첫 번째 포인트 처리
        GPSPoint prevPoint = gpsPoints.get(0);
        double speedSum = prevPoint.getSpeed();
        double hdopSum = prevPoint.getHdop();
        
        minSpeed = Math.min(minSpeed, prevPoint.getSpeed());
        maxSpeed = Math.max(maxSpeed, prevPoint.getSpeed());
        minHdop = Math.min(minHdop, prevPoint.getHdop());
        maxHdop = Math.max(maxHdop, prevPoint.getHdop());
        
        // 정확도 카운트
        switch (prevPoint.getAccuracy()) {
            case EXCELLENT: excellentCount++; break;
            case GOOD: goodCount++; break;
            case MODERATE: moderateCount++; break;
            case FAIR: fairCount++; break;
            case POOR: poorCount++; break;
        }
        
        // 나머지 포인트들 처리
        for (int i = 1; i < gpsPoints.size(); i++) {
            GPSPoint currPoint = gpsPoints.get(i);
            
            // 거리 누적
            totalDistance += prevPoint.distanceTo(currPoint);
            
            // 속도 통계
            double speed = currPoint.getSpeed();
            speedSum += speed;
            minSpeed = Math.min(minSpeed, speed);
            maxSpeed = Math.max(maxSpeed, speed);
            
            // HDOP 통계
            double hdop = currPoint.getHdop();
            hdopSum += hdop;
            minHdop = Math.min(minHdop, hdop);
            maxHdop = Math.max(maxHdop, hdop);
            
            // 정확도 카운트
            switch (currPoint.getAccuracy()) {
                case EXCELLENT: excellentCount++; break;
                case GOOD: goodCount++; break;
                case MODERATE: moderateCount++; break;
                case FAIR: fairCount++; break;
                case POOR: poorCount++; break;
            }
            
            prevPoint = currPoint;
        }
        
        // 평균 계산
        avgSpeed = speedSum / totalPoints;
        avgHdop = hdopSum / totalPoints;
        
        // 통계 객체 생성 및 반환
        return new GPSStatistics(
            totalPoints, totalDistance, 
            minSpeed, maxSpeed, avgSpeed,
            minHdop, maxHdop, avgHdop,
            excellentCount, goodCount, moderateCount, fairCount, poorCount
        );
    }
    
    /**
     * GPS 데이터 목록을 품질별로 필터링
     * @param gpsPoints 원본 GPS 포인트 목록
     * @param minAccuracy 최소 요구 정확도
     * @return 필터링된 GPS 포인트 목록
     */
    public static List<GPSPoint> filterByAccuracy(List<GPSPoint> gpsPoints, GPSAccuracy minAccuracy) {
        return gpsPoints.stream()
                .filter(point -> point.getAccuracy().ordinal() <= minAccuracy.ordinal())
                .collect(Collectors.toList());
    }
    
    /**
     * GPS 데이터 목록을 속도 범위로 필터링
     * @param gpsPoints 원본 GPS 포인트 목록
     * @param minSpeed 최소 속도 (km/h)
     * @param maxSpeed 최대 속도 (km/h)
     * @return 필터링된 GPS 포인트 목록
     */
    public static List<GPSPoint> filterBySpeed(List<GPSPoint> gpsPoints, double minSpeed, double maxSpeed) {
        return gpsPoints.stream()
                .filter(point -> point.getSpeed() >= minSpeed && point.getSpeed() <= maxSpeed)
                .collect(Collectors.toList());
    }
}
