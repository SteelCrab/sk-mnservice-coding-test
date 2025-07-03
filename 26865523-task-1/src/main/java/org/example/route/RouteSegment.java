package org.example.route;

import org.example.model.OSMNode;
import org.example.model.OSMWay;

import java.util.ArrayList;
import java.util.List;

/**
 * 기준 경로의 세그먼트(구간)를 나타내는 클래스
 * 역할: OSM Way 하나를 기반으로 한 도로 구간 정보를 관리하고 GPS 매칭 계산 수행
 * 구조: OSM Way 정보, 노드 목록, 거리 계산, GPS 매칭 기능 등을 포함
 */
public class RouteSegment {
    private OSMWay way;                        // 이 세그먼트가 기반하는 OSM Way 정보
    private List<OSMNode> nodes;               // 세그먼트를 구성하는 노드들의 순서 목록
    private double distance;                   // 세그먼트의 총 길이 (미터 단위)
    private List<Double> cumulativeDistances;  // 각 노드까지의 누적 거리 목록
    
    /**
     * RouteSegment 생성자
     * OSM Way와 해당 노드들을 기반으로 세그먼트 생성
     * @param way OSM Way 객체 (도로 정보)
     * @param nodes Way를 구성하는 노드들의 목록 (순서대로)
     */
    public RouteSegment(OSMWay way, List<OSMNode> nodes) {
        this.way = way;
        this.nodes = new ArrayList<>(nodes);           // 노드 목록 복사 (원본 보호)
        this.cumulativeDistances = new ArrayList<>();  // 누적 거리 목록 초기화
        calculateDistances();                          // 거리 계산 수행
    }
    
    /**
     * 세그먼트의 거리 정보 계산
     * 각 노드 간의 거리를 계산하고 누적 거리를 저장
     * 생성자에서 자동으로 호출됨
     */
    private void calculateDistances() {
        this.distance = 0.0;                          // 총 거리 초기화
        this.cumulativeDistances.clear();             // 누적 거리 목록 초기화
        
        if (nodes.size() < 2) {
            // 노드가 2개 미만이면 거리 계산 불가
            cumulativeDistances.add(0.0);
            return;
        }
        
        // 첫 번째 노드의 누적 거리는 0
        cumulativeDistances.add(0.0);
        
        // 연속된 노드들 간의 거리를 계산하여 누적
        for (int i = 1; i < nodes.size(); i++) {
            OSMNode prevNode = nodes.get(i - 1);       // 이전 노드
            OSMNode currNode = nodes.get(i);           // 현재 노드
            
            // 두 노드 간의 거리 계산 (하버사인 공식 사용)
            double segmentDistance = calculateHaversineDistance(
                prevNode.getLatitude(), prevNode.getLongitude(),
                currNode.getLatitude(), currNode.getLongitude()
            );
            
            this.distance += segmentDistance;          // 총 거리에 추가
            cumulativeDistances.add(this.distance);    // 누적 거리 저장
        }
    }
    
    /**
     * 특정 GPS 지점과 이 세그먼트 간의 최단 거리 계산
     * Map Matching에서 가장 가까운 세그먼트를 찾을 때 사용
     * @param lat GPS 위도
     * @param lon GPS 경도
     * @return 세그먼트까지의 최단 거리 (미터 단위)
     */
    public double getDistanceToPoint(double lat, double lon) {
        if (nodes.size() < 2) {
            // 노드가 2개 미만이면 첫 번째 노드까지의 거리 반환
            if (nodes.size() == 1) {
                OSMNode node = nodes.get(0);
                return calculateHaversineDistance(lat, lon, node.getLatitude(), node.getLongitude());
            }
            return Double.MAX_VALUE;  // 노드가 없으면 최대값 반환
        }
        
        double minDistance = Double.MAX_VALUE;  // 최소 거리 (초기값은 최대값)
        
        // 모든 연속된 노드 쌍에 대해 점-선분 간 거리 계산
        for (int i = 0; i < nodes.size() - 1; i++) {
            OSMNode startNode = nodes.get(i);      // 선분 시작점
            OSMNode endNode = nodes.get(i + 1);    // 선분 끝점
            
            // GPS 점과 선분 간의 최단 거리 계산
            double distance = calculatePointToLineDistance(
                lat, lon,
                startNode.getLatitude(), startNode.getLongitude(),
                endNode.getLatitude(), endNode.getLongitude()
            );
            
            // 더 가까운 거리 발견 시 업데이트
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        
        return minDistance;
    }
    
    /**
     * 세그먼트 내에서 특정 GPS 지점의 진행률 계산
     * 세그먼트 시작점을 0.0, 끝점을 1.0으로 하는 진행률 반환
     * @param lat GPS 위도
     * @param lon GPS 경도
     * @return 세그먼트 내 진행률 (0.0 ~ 1.0)
     */
    public double getProgressAtPoint(double lat, double lon) {
        if (nodes.size() < 2 || distance <= 0) {
            return 0.0;  // 세그먼트가 유효하지 않으면 0% 반환
        }
        
        double minDistance = Double.MAX_VALUE;  // 최소 거리
        double bestProgress = 0.0;              // 최적 진행률
        
        // 모든 연속된 노드 쌍에 대해 진행률 계산
        for (int i = 0; i < nodes.size() - 1; i++) {
            OSMNode startNode = nodes.get(i);      // 선분 시작점
            OSMNode endNode = nodes.get(i + 1);    // 선분 끝점
            
            // GPS 점과 선분 간의 거리 및 진행률 계산
            double[] result = calculatePointToLineProgressAndDistance(
                lat, lon,
                startNode.getLatitude(), startNode.getLongitude(),
                endNode.getLatitude(), endNode.getLongitude()
            );
            
            double distance = result[0];    // 점-선분 간 거리
            double segmentProgress = result[1];  // 선분 내 진행률 (0.0 ~ 1.0)
            
            // 더 가까운 선분 발견 시 진행률 업데이트
            if (distance < minDistance) {
                minDistance = distance;
                // 전체 세그먼트에서의 진행률 계산
                double segmentStartDistance = cumulativeDistances.get(i);
                double segmentLength = cumulativeDistances.get(i + 1) - segmentStartDistance;
                bestProgress = (segmentStartDistance + segmentLength * segmentProgress) / this.distance;
            }
        }
        
        return Math.max(0.0, Math.min(1.0, bestProgress));  // 0.0 ~ 1.0 범위로 제한
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
    
    /**
     * 점과 선분 간의 최단 거리 계산
     * @param pointLat 점의 위도
     * @param pointLon 점의 경도
     * @param line1Lat 선분 시작점의 위도
     * @param line1Lon 선분 시작점의 경도
     * @param line2Lat 선분 끝점의 위도
     * @param line2Lon 선분 끝점의 경도
     * @return 점과 선분 간의 최단 거리 (미터 단위)
     */
    private double calculatePointToLineDistance(double pointLat, double pointLon,
                                              double line1Lat, double line1Lon,
                                              double line2Lat, double line2Lon) {
        double[] result = calculatePointToLineProgressAndDistance(
            pointLat, pointLon, line1Lat, line1Lon, line2Lat, line2Lon
        );
        return result[0];  // 거리만 반환
    }
    
    /**
     * 점과 선분 간의 거리 및 선분 내 진행률 동시 계산
     * @param pointLat 점의 위도
     * @param pointLon 점의 경도
     * @param line1Lat 선분 시작점의 위도
     * @param line1Lon 선분 시작점의 경도
     * @param line2Lat 선분 끝점의 위도
     * @param line2Lon 선분 끝점의 경도
     * @return [거리(미터), 진행률(0.0~1.0)] 배열
     */
    private double[] calculatePointToLineProgressAndDistance(double pointLat, double pointLon,
                                                           double line1Lat, double line1Lon,
                                                           double line2Lat, double line2Lon) {
        // 선분의 길이 계산
        double lineLength = calculateHaversineDistance(line1Lat, line1Lon, line2Lat, line2Lon);
        
        if (lineLength < 0.001) {  // 선분이 거의 점인 경우 (1mm 미만)
            double distance = calculateHaversineDistance(pointLat, pointLon, line1Lat, line1Lon);
            return new double[]{distance, 0.0};
        }
        
        // 벡터 계산을 위한 좌표 변환 (간단한 평면 근사)
        double x1 = 0.0;
        double y1 = 0.0;
        double x2 = (line2Lon - line1Lon) * Math.cos(Math.toRadians(line1Lat)) * 111320.0;
        double y2 = (line2Lat - line1Lat) * 111320.0;
        double px = (pointLon - line1Lon) * Math.cos(Math.toRadians(line1Lat)) * 111320.0;
        double py = (pointLat - line1Lat) * 111320.0;
        
        // 점에서 선분으로의 투영 계산
        double dx = x2 - x1;
        double dy = y2 - y1;
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        
        // 투영점이 선분 범위를 벗어나는 경우 제한
        t = Math.max(0.0, Math.min(1.0, t));
        
        // 투영점 좌표 계산
        double projX = x1 + t * dx;
        double projY = y1 + t * dy;
        
        // 점과 투영점 간의 거리 계산
        double distance = Math.sqrt((px - projX) * (px - projX) + (py - projY) * (py - projY));
        
        return new double[]{distance, t};  // [거리, 진행률] 반환
    }
    
    // ===== Getters =====
    
    /**
     * 이 세그먼트의 OSM Way 정보 반환
     * @return OSM Way 객체
     */
    public OSMWay getWay() {
        return way;
    }
    
    /**
     * 세그먼트를 구성하는 노드 목록 반환
     * @return 노드 목록의 복사본 (원본 보호)
     */
    public List<OSMNode> getNodes() {
        return new ArrayList<>(nodes);
    }
    
    /**
     * 세그먼트의 총 길이 반환
     * @return 세그먼트 길이 (미터 단위)
     */
    public double getDistance() {
        return distance;
    }
    
    /**
     * 세그먼트의 시작 노드 반환
     * @return 첫 번째 노드 (없으면 null)
     */
    public OSMNode getStartNode() {
        return nodes.isEmpty() ? null : nodes.get(0);
    }
    
    /**
     * 세그먼트의 끝 노드 반환
     * @return 마지막 노드 (없으면 null)
     */
    public OSMNode getEndNode() {
        return nodes.isEmpty() ? null : nodes.get(nodes.size() - 1);
    }
    
    /**
     * 세그먼트 정보를 문자열로 변환
     * @return "RouteSegment{wayId=123456, nodes=5개, distance=245.67m}" 형태
     */
    @Override
    public String toString() {
        return String.format("RouteSegment{wayId=%d, nodes=%d개, distance=%.2fm}", 
                           way.getId(), nodes.size(), distance);
    }
}
