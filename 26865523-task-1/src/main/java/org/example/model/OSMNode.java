package org.example.model;

/**
 * 역할 : 위도,경도 죄표 점을 나타냄
 * 구조 : Felds, Constructor, Getters, Utils
 */

import java.util.List;
import java.util.Objects;

// 노드 식별,  위도,경도
public class OSMNode {
    private long id;            // 노드 ID
    private double latitude;    // 위도 좌표
    private double longitude;   // 경도 좌표

    /*
     * OSMNode 생성자
     * @param id OSM에서 제공하는 노드 고유 ID
     * @param latitude 위도
     * @param longitude 경도
     */
    public OSMNode(long id, double latitude, double longitude) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    //노드 ID 반환
    //@return OSM 노드 ID
    public long getId() {
        return id;
    }

    //위도 반환
    //@return 위도
    public double getLatitude() {
        return latitude;
    }

    //경도 반환
    //@return 경도
    public double getLongitude() {
        return longitude;
    }


    /*
     * 노드 정보를 문자열로 변환
     * @return "Node{id=123, lat=37.4963000, lon=127.0283000}" 형태
     */
    @Override
    public String toString() {
        return String.format("Node{id=%d, lat=%.7f, lon=%.7f}", id, latitude, longitude);
    }

    /*
     * 두 노드가 같은지 비교 (ID)
     * @param obj 비교할 객체
     * @return ID가 같으면 true, 다르면 false
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if ( obj == null || getClass() != obj.getClass() )
            return false;
            OSMNode osmNode = (OSMNode) obj;
            return id == osmNode.id;
    }

    /*
     * HashSet, HashMap에서 사용할 해시코드 생성
     * @return ID 기반 해시코드
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}



