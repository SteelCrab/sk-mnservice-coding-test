package org.example.gps;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * GPS 포인트 데이터를 나타내는 클래스
 * 역할: CSV 파일에서 읽어온 GPS 좌표 정보를 저장하고 관리
 * 구조: 위도, 경도, 각도, 속도, HDOP, 타임스탬프 등을 포함
 */
public class GPSPoint {
    private double latitude;        // 위도 (예: 37.496633)
    private double longitude;       // 경도 (예: 127.02815)
    private double angle;           // 진행 방향 각도 (0~360도, 북쪽이 0도)
    private double speed;           // 속도 (km/h 단위)
    private double hdop;            // 수평 정밀도 희석 (Horizontal Dilution of Precision)
    private LocalDateTime timestamp; // GPS 데이터 수집 시간 (선택적)
    private int sequenceNumber;     // 데이터 순서 번호 (CSV 파일 내 순서)
    
    /**
     * GPSPoint 생성자 (기본 정보만)
     * @param latitude 위도
     * @param longitude 경도
     * @param angle 진행 방향 각도 (도 단위)
     * @param speed 속도 (km/h)
     * @param hdop 수평 정밀도 희석값
     */
    public GPSPoint(double latitude, double longitude, double angle, double speed, double hdop) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.angle = angle;
        this.speed = speed;
        this.hdop = hdop;
        this.timestamp = null;          // 기본값은 null
        this.sequenceNumber = 0;        // 기본값은 0
    }
    
    /**
     * GPSPoint 생성자 (전체 정보 포함)
     * @param latitude 위도
     * @param longitude 경도
     * @param angle 진행 방향 각도 (도 단위)
     * @param speed 속도 (km/h)
     * @param hdop 수평 정밀도 희석값
     * @param timestamp GPS 데이터 수집 시간
     * @param sequenceNumber 데이터 순서 번호
     */
    public GPSPoint(double latitude, double longitude, double angle, double speed, double hdop,
                   LocalDateTime timestamp, int sequenceNumber) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.angle = angle;
        this.speed = speed;
        this.hdop = hdop;
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
    }
    
    /**
     * 다른 GPS 포인트와의 거리 계산 (하버사인 공식 사용)
     * @param other 비교할 다른 GPS 포인트
     * @return 두 점 간의 거리 (미터 단위)
     */
    public double distanceTo(GPSPoint other) {
        return calculateHaversineDistance(
            this.latitude, this.longitude,
            other.latitude, other.longitude
        );
    }
    
    /**
     * 다른 GPS 포인트와의 방위각 계산
     * 이 점에서 다른 점으로의 방향을 계산 (북쪽 기준 시계방향 각도)
     * @param other 목표 GPS 포인트
     * @return 방위각 (0~360도, 북쪽이 0도)
     */
    public double bearingTo(GPSPoint other) {
        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double deltaLonRad = Math.toRadians(other.longitude - this.longitude);
        
        double y = Math.sin(deltaLonRad) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                   Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad);
        
        double bearingRad = Math.atan2(y, x);
        double bearingDeg = Math.toDegrees(bearingRad);
        
        // 0~360도 범위로 정규화
        return (bearingDeg + 360.0) % 360.0;
    }
    
    /**
     * GPS 포인트의 정확도 평가
     * HDOP 값을 기반으로 GPS 신호의 품질을 평가
     * @return GPS 정확도 등급 (EXCELLENT, GOOD, MODERATE, FAIR, POOR)
     */
    public GPSAccuracy getAccuracy() {
        if (hdop <= 1.0) {
            return GPSAccuracy.EXCELLENT;  // 매우 우수 (1m 이하 오차)
        } else if (hdop <= 2.0) {
            return GPSAccuracy.GOOD;       // 우수 (1-3m 오차)
        } else if (hdop <= 5.0) {
            return GPSAccuracy.MODERATE;   // 보통 (3-8m 오차)
        } else if (hdop <= 10.0) {
            return GPSAccuracy.FAIR;       // 나쁨 (8-15m 오차)
        } else {
            return GPSAccuracy.POOR;       // 매우 나쁨 (15m 이상 오차)
        }
    }
    
    /**
     * GPS 포인트가 유효한지 검증
     * 위도, 경도, 속도 등의 값이 유효한 범위 내에 있는지 확인
     * @return true = 유효한 GPS 데이터, false = 잘못된 GPS 데이터
     */
    public boolean isValid() {
        // 위도 범위 확인 (-90 ~ 90도)
        if (latitude < -90.0 || latitude > 90.0) {
            return false;
        }
        
        // 경도 범위 확인 (-180 ~ 180도)
        if (longitude < -180.0 || longitude > 180.0) {
            return false;
        }
        
        // 각도 범위 확인 (0 ~ 360도)
        if (angle < 0.0 || angle > 360.0) {
            return false;
        }
        
        // 속도 범위 확인 (0 ~ 300 km/h, 음수 불가)
        if (speed < 0.0 || speed > 300.0) {
            return false;
        }
        
        // HDOP 범위 확인 (0 이상, 일반적으로 50 이하)
        if (hdop < 0.0 || hdop > 50.0) {
            return false;
        }
        
        return true;  // 모든 검증 통과
    }
    
    /**
     * 하버사인 공식을 사용한 두 GPS 좌표 간의 거리 계산
     * @param lat1 첫 번째 지점의 위도
     * @param lon1 첫 번째 지점의 경도
     * @param lat2 두 번째 지점의 위도
     * @param lon2 두 번째 지점의 경도
     * @return 두 지점 간의 거리 (미터 단위)
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS = 6371000.0;  // 지구 반지름 (미터)
        
        // 위도와 경도를 라디안으로 변환
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);
        
        // 위도와 경도 차이 계산
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;
        
        // 하버사인 공식 적용
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;  // 거리 반환 (미터)
    }
    
    // ===== Getters and Setters =====
    
    /**
     * 위도 반환
     * @return GPS 위도
     */
    public double getLatitude() {
        return latitude;
    }
    
    /**
     * 경도 반환
     * @return GPS 경도
     */
    public double getLongitude() {
        return longitude;
    }
    
    /**
     * 진행 방향 각도 반환
     * @return 각도 (0~360도, 북쪽이 0도)
     */
    public double getAngle() {
        return angle;
    }
    
    /**
     * 속도 반환
     * @return 속도 (km/h)
     */
    public double getSpeed() {
        return speed;
    }
    
    /**
     * HDOP 값 반환
     * @return 수평 정밀도 희석값
     */
    public double getHdop() {
        return hdop;
    }
    
    /**
     * 타임스탬프 반환
     * @return GPS 데이터 수집 시간 (null 가능)
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * 순서 번호 반환
     * @return 데이터 순서 번호
     */
    public int getSequenceNumber() {
        return sequenceNumber;
    }
    
    /**
     * 타임스탬프 설정
     * @param timestamp GPS 데이터 수집 시간
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * 순서 번호 설정
     * @param sequenceNumber 데이터 순서 번호
     */
    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
    
    /**
     * GPS 포인트 정보를 문자열로 변환
     * @return "GPSPoint{lat=37.496633, lon=127.028150, angle=336.0°, speed=54.1km/h, hdop=1.0}" 형태
     */
    @Override
    public String toString() {
        return String.format("GPSPoint{lat=%.6f, lon=%.6f, angle=%.1f°, speed=%.1fkm/h, hdop=%.1f}",
                           latitude, longitude, angle, speed, hdop);
    }
    
    /**
     * 두 GPS 포인트가 같은지 비교 (좌표 기준)
     * @param obj 비교할 객체
     * @return 위도, 경도가 같으면 true
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        GPSPoint gpsPoint = (GPSPoint) obj;
        return Double.compare(gpsPoint.latitude, latitude) == 0 &&
               Double.compare(gpsPoint.longitude, longitude) == 0;
    }
    
    /**
     * HashSet, HashMap에서 사용할 해시코드 생성
     * @return 위도, 경도 기반 해시코드
     */
    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }
}
