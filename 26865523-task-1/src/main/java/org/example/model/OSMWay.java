package org.example.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * OSM Way 클래스
 *  Map Matching과 경로 이탈 판정에 필요한 모든 태그 정보
 */
public class OSMWay {
    private long id;               // Way의 고유 식별자 (예: 521766182)
    private List<Long> nodeIds;    // 이 Way를 구성하는 노드 ID들의 순서 목록

    private String name;           // 도로명 (예: "강남대로", null 가능)
    private String highway;        // 도로 타입 (예: "primary", "secondary", null 가능)

    // Map Matching 태그
    private String maxspeed;       // 최대 속도 제한 (50, 60 등)
    private String lanes;          // 차선 수 (2, 4 등)
    private String oneway;         // 일방통행 (yes, no, -1)
    private String access;         // 접근 제한 (public, private, no)
    private String width;          // 도로 폭 (미터 단위)
    private String surface;        // 도로 표면 (paved, unpaved, asphalt)
    private String junction;       // 교차로 타입 (roundabout 등)

    // GPS 오차 판정용 태그
    private String bridge;         // 교량 여부 (yes, no)
    private String tunnel;         // 터널 여부 (yes, no)
    private String level;          // 층수/레벨 (지하, 지상, 고가)

    /**
     * OSMWay 생성자
     * @param id OSM에서 제공하는 Way 고유 ID
     */
    public OSMWay(long id) {
        this.id = id;
        this.nodeIds = new ArrayList<>();
    }

    /**
     * Way에 노드 ID 추가 (순서대로 추가됨)
     * @param nodeId 추가할 노드의 ID
     */
    public void addNodeId(long nodeId) {
        nodeIds.add(nodeId);  // 리스트 끝에 추가 (순서 중요!)
    }

    // ===== 기본 정보 Getters =====

    /**
     * Way ID 반환
     * @return OSM Way 고유 식별자
     */
    public long getId() {
        return id;
    }

    /**
     * Way를 구성하는 노드 ID 목록 반환
     * @return 노드 ID 목록의 복사본 (Defensive Copy로 원본 보호)
     */
    public List<Long> getNodeIds() {
        return new ArrayList<>(nodeIds);  // 원본 수정 방지를 위한 복사본 반환
    }

    /**
     * 도로명 반환
     * @return 도로 이름 (없으면 null)
     */
    public String getName() {
        return name;
    }

    /**
     * 도로 타입 반환
     * @return 도로 분류 (primary, secondary 등, 없으면 null)
     */
    public String getHighway() {
        return highway;
    }

    // ===== 기본 정보 Setters =====

    /**
     * 도로명 설정
     * @param name 도로 이름 (예: "강남대로")
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 도로 타입 설정
     * @param highway 도로 분류 (예: "primary", "secondary")
     */
    public void setHighway(String highway) {
        this.highway = highway;  // 🔧 수정: 기존 코드에서 this.name = highway 오타 수정
    }

    // ===== 속도 관련 =====

    /**
     * 최대 속도 반환
     * @return 최대 속도 문자열 (예: "50", "60 km/h")
     */
    public String getMaxspeed() {  // 🔧 수정: getMaxSpeed → getMaxspeed (일관성)
        return maxspeed;
    }

    /**
     * 최대 속도 설정
     * @param maxspeed 최대 속도 (예: "50", "60 km/h")
     */
    public void setMaxspeed(String maxspeed) {
        this.maxspeed = maxspeed;
    }

    /**
     * 최대 속도를 정수로 반환 (파싱 실패 시 기본값)
     * @param defaultSpeed 파싱 실패 시 기본 속도
     * @return 최대 속도 (km/h)
     */
    public int getMaxspeedAsInt(int defaultSpeed) {
        if (maxspeed == null || maxspeed.isEmpty()) {
            return defaultSpeed;
        }
        try {
            // "50 km/h" → "50" 추출
            String speedStr = maxspeed.replaceAll("[^0-9]", "");
            return speedStr.isEmpty() ? defaultSpeed : Integer.parseInt(speedStr);
        } catch (NumberFormatException e) {
            return defaultSpeed;
        }
    }

    // ===== 차선 관련 =====

    /**
     * 차선 수 반환
     * @return 차선 수 문자열 (예: "2", "4")
     */
    public String getLanes() {
        return lanes;
    }

    /**
     * 차선 수 설정
     * @param lanes 차선 수 (예: "2", "4")
     */
    public void setLanes(String lanes) {
        this.lanes = lanes;
    }

    /**
     * 차선 수를 정수로 반환
     * @param defaultLanes 파싱 실패 시 기본 차선 수
     * @return 차선 수
     */
    public int getLanesAsInt(int defaultLanes) {
        if (lanes == null || lanes.isEmpty()) {
            return defaultLanes;
        }
        try {
            return Integer.parseInt(lanes);
        } catch (NumberFormatException e) {
            return defaultLanes;
        }
    }

    // ===== 일방통행 관련 =====

    /**
     * 일방통행 정보 반환
     * @return 일방통행 여부 (yes, no, -1 등)
     */
    public String getOneway() {
        return oneway;
    }

    /**
     * 일방통행 정보 설정
     * @param oneway 일방통행 여부 (yes, no, -1)
     */
    public void setOneway(String oneway) {
        this.oneway = oneway;
    }

    /**
     * 일방통행 여부 확인
     * @return true if 일방통행, false if 양방향
     */
    public boolean isOneway() {
        return "yes".equals(oneway) || "1".equals(oneway) || "-1".equals(oneway);
    }

    /**
     * 일방통행 방향 확인
     * @return 1: 정방향, -1: 역방향, 0: 양방향
     */
    public int getOnewayDirection() {
        if ("yes".equals(oneway) || "1".equals(oneway)) {
            return 1;  // 정방향
        } else if ("-1".equals(oneway)) {
            return -1; // 역방향
        } else {
            return 0;  // 양방향
        }
    }

    // ===== 접근 제한 관련 =====

    /**
     * 접근 제한 정보 반환
     * @return 접근 제한 (public, private, no 등)
     */
    public String getAccess() {
        return access;
    }

    /**
     * 접근 제한 정보 설정
     * @param access 접근 제한 (public, private, no)
     */
    public void setAccess(String access) {
        this.access = access;
    }

    /**
     * 일반 차량 접근 가능 여부
     * @return true if 접근 가능
     */
    public boolean isAccessible() {
        if (access == null || access.isEmpty()) {
            return true; // 기본값: 접근 가능
        }
        return !"no".equals(access) && !"private".equals(access);
    }

    // ===== 도로 물리적 특성 =====

    /**
     * 도로 폭 반환
     * @return 도로 폭 문자열 (예: "5.5", "6 m")
     */
    public String getWidth() {
        return width;
    }

    /**
     * 도로 폭 설정
     * @param width 도로 폭 (예: "5.5", "6 m")
     */
    public void setWidth(String width) {
        this.width = width;
    }

    /**
     * 도로 폭을 미터 단위로 반환
     * @param defaultWidth 기본 폭 (미터)
     * @return 도로 폭 (미터)
     */
    public double getWidthAsMeters(double defaultWidth) {
        if (width == null || width.isEmpty()) {
            return defaultWidth;
        }
        try {
            // "5.5 m" → "5.5" 추출
            String widthStr = width.replaceAll("[^0-9.]", "");
            return widthStr.isEmpty() ? defaultWidth : Double.parseDouble(widthStr);
        } catch (NumberFormatException e) {
            return defaultWidth;
        }
    }

    /**
     * 도로 표면 반환
     * @return 도로 표면 (paved, unpaved, asphalt 등)
     */
    public String getSurface() {
        return surface;
    }

    /**
     * 도로 표면 설정
     * @param surface 도로 표면 (paved, unpaved, asphalt)
     */
    public void setSurface(String surface) {
        this.surface = surface;
    }

    /**
     * 포장도로 여부 확인
     * @return true if 포장도로
     */
    public boolean isPaved() {
        if (surface == null || surface.isEmpty()) {
            return true; // 기본값: 포장도로로 가정
        }
        return "paved".equals(surface) || "asphalt".equals(surface) ||
                "concrete".equals(surface) || "cobblestone".equals(surface);
    }

    // ===== 교차로 관련 =====

    /**
     * 교차로 타입 반환
     * @return 교차로 타입 (roundabout 등)
     */
    public String getJunction() {
        return junction;
    }

    /**
     * 교차로 타입 설정
     * @param junction 교차로 타입 (roundabout)
     */
    public void setJunction(String junction) {
        this.junction = junction;
    }

    /**
     * 로터리/원형교차로 여부
     * @return true if 로터리
     */
    public boolean isRoundabout() {
        return "roundabout".equals(junction);
    }

    // ===== GPS 오차 관련 태그들 =====

    /**
     * 교량 여부 반환
     * @return 교량 여부 (yes, no)
     */
    public String getBridge() {
        return bridge;
    }

    /**
     * 교량 여부 설정
     * @param bridge 교량 여부 (yes, no)
     */
    public void setBridge(String bridge) {
        this.bridge = bridge;
    }

    /**
     * 터널 여부 반환
     * @return 터널 여부 (yes, no)
     */
    public String getTunnel() {
        return tunnel;
    }

    /**
     * 터널 여부 설정
     * @param tunnel 터널 여부 (yes, no)
     */
    public void setTunnel(String tunnel) {
        this.tunnel = tunnel;
    }

    /**
     * 레벨 정보 반환
     * @return 층수/레벨 (0: 지상, 1: 1층, -1: 지하 등)
     */
    public String getLevel() {
        return level;
    }

    /**
     * 레벨 정보 설정
     * @param level 층수/레벨 (0, 1, -1 등)
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * 특수 구조물 여부 (GPS 오차 가능성 높음)
     * @return true if 교량, 터널, 고가도로 등
     */
    public boolean isSpecialStructure() {
        return "yes".equals(bridge) || "yes".equals(tunnel) ||
                (level != null && !level.equals("0") && !level.isEmpty());
    }

    // ===== Map Matching 유틸리티 메소드들 =====

    /**
     * Map Matching 시 허용 거리 계산 (도로 폭 기반)
     * @return GPS 매칭 허용 거리 (미터)
     */
    public double getMatchingTolerance() {
        double roadWidth = getWidthAsMeters(6.0); // 기본 6미터
        int laneCount = getLanesAsInt(2);         // 기본 2차선

        // 차선당 3.5미터로 계산하여 도로 폭 추정
        double estimatedWidth = Math.max(roadWidth, laneCount * 3.5);

        // GPS 매칭 허용 거리 = 도로 폭의 절반 + 여유분
        return estimatedWidth / 2.0 + 5.0; // 최소 5미터 여유
    }

    /**
     * GPS 속도 오차 판정을 위한 제한속도 확인
     * @param gpsSpeed GPS에서 측정된 속도 (km/h)
     * @return true if GPS 속도가 비정상적으로 높음 (오차 가능성)
     */
    public boolean isSpeedAnomalous(double gpsSpeed) {
        int speedLimit = getMaxspeedAsInt(50); // 기본 제한속도 50km/h

        // GPS 속도가 제한속도의 1.8배 이상이면 오차로 판정
        return gpsSpeed > speedLimit * 1.8;
    }

    /**
     * 차량 진행 방향과 일방통행 방향 비교
     * @param vehicleDirection 차량 진행 방향 (1: 정방향, -1: 역방향)
     * @return true if 올바른 방향
     */
    public boolean isDirectionValid(int vehicleDirection) {
        if (!isOneway()) {
            return true; // 양방향 도로는 어느 방향이든 OK
        }

        int allowedDirection = getOnewayDirection();
        return vehicleDirection == allowedDirection;
    }

    // ===== Object 메소드들 =====

    /**
     * Way 정보를 읽기 쉬운 문자열로 변환
     * @return "Way{id=123, nodes=5개, highway='primary', name='강남대로', maxspeed='50', lanes='4'}" 형태
     */
    @Override
    public String toString() {
        return String.format("Way{id=%d, nodes=%d개, highway='%s', name='%s', maxspeed='%s', lanes='%s', oneway='%s'}",
                id, nodeIds.size(), highway, name, maxspeed, lanes, oneway);
    }

    /**
     * 두 Way가 같은지 비교 (ID 기준)
     * @param obj 비교할 객체
     * @return ID가 같으면 true, 다르면 false
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;                                    // 같은 객체 참조
        if (obj == null || getClass() != obj.getClass()) return false;  // null이거나 다른 클래스
        OSMWay osmWay = (OSMWay) obj;
        return id == osmWay.id;                                          // ID로만 비교
    }

    /**
     * HashSet, HashMap에서 사용할 해시코드 생성
     * @return ID 기반 해시코드
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);  // ID만 사용하여 해시코드 생성
    }
}
