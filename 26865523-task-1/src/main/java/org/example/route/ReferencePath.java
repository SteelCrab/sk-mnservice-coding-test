package org.example.route;

import org.example.model.OSMNode;
import org.example.model.OSMWay;

import java.util.ArrayList;
import java.util.List;

/**
 * 기준 경로를 나타내는 클래스
 * 역할: OSM 도로 데이터를 기반으로 한 기준 경로 정보를 관리하고 GPS 매칭에 사용
 * 구조: 경로 ID, 이름, 세그먼트 목록, 총 거리 등을 포함
 */
public class ReferencePath {
    private String pathId;                     // 기준 경로 고유 식별자 (예: "PATH_001")
    private String name;                       // 기준 경로 이름 (예: "강남대로 북행")
    private List<RouteSegment> segments;       // 경로를 구성하는 세그먼트들의 순서 목록
    private double totalDistance;              // 전체 경로 길이 (미터 단위)
    
    /**
     * ReferencePath 생성자
     * @param pathId 기준 경로 고유 식별자
     * @param name 기준 경로 이름
     */
    public ReferencePath(String pathId, String name) {
        this.pathId = pathId;
        this.name = name;
        this.segments = new ArrayList<>();     // 빈 세그먼트 목록으로 초기화
        this.totalDistance = 0.0;              // 초기 거리는 0
    }
    
    /**
     * 기준 경로에 세그먼트 추가
     * 세그먼트가 추가될 때마다 총 거리도 자동으로 업데이트됨
     * @param segment 추가할 경로 세그먼트
     */
    public void addSegment(RouteSegment segment) {
        this.segments.add(segment);                    // 세그먼트 목록 끝에 추가
        this.totalDistance += segment.getDistance();   // 총 거리에 세그먼트 거리 추가
    }
    
    /**
     * OSM Way를 기반으로 경로 세그먼트 생성 및 추가
     * OSM 데이터에서 직접 기준 경로를 구성할 때 사용
     * @param way OSM Way 객체 (도로 정보)
     * @param nodes Way를 구성하는 노드들의 목록 (순서대로)
     */
    public void addWayAsSegment(OSMWay way, List<OSMNode> nodes) {
        RouteSegment segment = new RouteSegment(way, nodes);  // Way와 노드로 세그먼트 생성
        addSegment(segment);                                  // 생성된 세그먼트를 기준 경로에 추가
    }
    
    /**
     * 특정 GPS 지점에서 가장 가까운 경로 세그먼트 찾기
     * Map Matching의 핵심 기능 - GPS 좌표를 가장 가까운 도로에 매칭
     * @param lat GPS 위도
     * @param lon GPS 경도
     * @return 가장 가까운 세그먼트 (경로가 비어있으면 null)
     */
    public RouteSegment findNearestSegment(double lat, double lon) {
        RouteSegment nearestSegment = null;    // 가장 가까운 세그먼트
        double minDistance = Double.MAX_VALUE; // 최소 거리 (초기값은 최대값)
        
        // 모든 세그먼트를 순회하며 가장 가까운 것 찾기
        for (RouteSegment segment : segments) {
            double distance = segment.getDistanceToPoint(lat, lon);  // 세그먼트까지의 거리 계산
            if (distance < minDistance) {                            // 더 가까운 세그먼트 발견
                minDistance = distance;
                nearestSegment = segment;
            }
        }
        
        return nearestSegment;  // 가장 가까운 세그먼트 반환 (없으면 null)
    }
    
    /**
     * 기준 경로 상의 특정 GPS 위치에서의 진행률 계산
     * 전체 경로에서 현재 위치가 몇 % 지점인지 계산 (0.0 ~ 1.0)
     * @param lat GPS 위도
     * @param lon GPS 경도
     * @return 진행률 (0.0 = 시작점, 1.0 = 끝점)
     */
    public double getProgressAtPoint(double lat, double lon) {
        RouteSegment nearestSegment = findNearestSegment(lat, lon);  // 가장 가까운 세그먼트 찾기
        if (nearestSegment == null) {
            return 0.0;  // 세그먼트가 없으면 진행률 0%
        }
        
        // 현재 세그먼트까지의 누적 거리 계산
        double distanceToSegment = 0.0;
        for (RouteSegment segment : segments) {
            if (segment == nearestSegment) {
                break;  // 현재 세그먼트에 도달하면 중단
            }
            distanceToSegment += segment.getDistance();  // 이전 세그먼트들의 거리 누적
        }
        
        // 현재 세그먼트 내에서의 진행률 추가 계산
        double segmentProgress = nearestSegment.getProgressAtPoint(lat, lon);
        distanceToSegment += nearestSegment.getDistance() * segmentProgress;
        
        // 전체 거리 대비 진행률 반환 (0.0 ~ 1.0)
        return totalDistance > 0 ? distanceToSegment / totalDistance : 0.0;
    }
    
    /**
     * GPS 좌표가 기준 경로에서 이탈했는지 판정
     * Map Matching에서 경로 이탈 감지에 사용
     * @param lat GPS 위도
     * @param lon GPS 경도
     * @param threshold 이탈 판정 임계값 (미터 단위, 예: 50m)
     * @return true = 경로 이탈, false = 경로 내 위치
     */
    public boolean isOffRoute(double lat, double lon, double threshold) {
        RouteSegment nearestSegment = findNearestSegment(lat, lon);  // 가장 가까운 세그먼트 찾기
        if (nearestSegment == null) {
            return true;  // 세그먼트가 없으면 이탈로 판정
        }
        
        double distance = nearestSegment.getDistanceToPoint(lat, lon);  // 세그먼트까지의 거리
        return distance > threshold;  // 임계값보다 멀면 이탈로 판정
    }
    
    // ===== Getters =====
    
    /**
     * 기준 경로 ID 반환
     * @return 기준 경로 고유 식별자
     */
    public String getPathId() {
        return pathId;
    }
    
    /**
     * 기준 경로 이름 반환
     * @return 기준 경로 이름
     */
    public String getName() {
        return name;
    }
    
    /**
     * 모든 세그먼트 목록 반환
     * @return 세그먼트 목록 (원본 반환 - 수정 가능)
     */
    public List<RouteSegment> getSegments() {
        return segments;
    }
    
    /**
     * 전체 기준 경로 길이 반환
     * @return 총 거리 (미터 단위)
     */
    public double getTotalDistance() {
        return totalDistance;
    }
    
    /**
     * 세그먼트 개수 반환
     * @return 기준 경로를 구성하는 세그먼트 수
     */
    public int getSegmentCount() {
        return segments.size();
    }
    
    /**
     * 기준 경로 정보를 문자열로 변환
     * @return "ReferencePath{id='PATH_001', name='강남대로 북행', segments=5개, distance=1250.50m}" 형태
     */
    @Override
    public String toString() {
        return String.format("ReferencePath{id='%s', name='%s', segments=%d개, distance=%.2fm}", 
                           pathId, name, segments.size(), totalDistance);
    }
}
