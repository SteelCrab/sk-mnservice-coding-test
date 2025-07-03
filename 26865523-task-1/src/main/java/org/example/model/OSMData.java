package org.example.model;

import java.util.ArrayList;
import java.util.List;

/*
 * OSM 데이터 전체를 담는 컨테이너 클래스
 * 파싱된 모든 노드와 Way들을 관리하고 검색 기능 제공
 */
public class OSMData {
    private List<OSMNode> nodes; //파싱된 모든 노드들의 목록
    private List<OSMWay> ways;   //파싱된 모든 Way들의 목록

    /*
     * OSMData 생성자
     * (빈 컨테이너)
     */
    public OSMData() {
        this.nodes = new ArrayList<>(); // 빈 노드 목록
        this.ways = new ArrayList<>();  // 빈 way 목록
    }

    /*
     * 노드를 컨테이너에 추가
     * @param node 추가한 OSMNode 객체
     */
    public void addNode(OSMNode node) {
        nodes.add(node); // 노드 목록 끝에 추가
    }

    /*
     * Way를 컨테이너에 추가
     * @param way 추가할 OSMWay 객체
     */
    public void addWay(OSMWay way) {
        ways.add(way); // way 목록 추가
    }

    /*
     * 모든 노드 목록 반환
     * @return 노드 목록의 복사본
     * (Defensive Copy로 원본 보호)
     */
    public List<OSMNode> getNodes() {
        return new ArrayList<>(nodes);
        //원본 수정 방지를 위한 복사본 반환
    }

    /*
     * 모든 way 목록 반환
     * @return Way 목록의 복사본
     * (Defensive Copy로 원본 보호)ㄴ
     */
    public List<OSMWay> getWays() {
        return  new ArrayList<>(ways);
        //원본 수정 방지를 위한 복사본 반환
    }

    /*
     * ID로 노드 검색 (stream_api)
     * @param nodeId 찾을 노드의 ID
     * @return 해당 ID의 노드 (없으면 null)
     */
    public OSMNode findNodeById(long nodeId) {
        return nodes.stream()                                       // 노드 목록을 스트림으로 반환
                .filter(node -> node.getId() == nodeId)   // ID가 일치하는 노드만 필터링
                .findFirst()                                        // 첫 번째 일치하는 요소 찾기
                .orElse(null);                                // 없으면 null 반환
    }

    /*
     * ID로 Way 검색 (stream_api)
     * @param wayId 찾을 노드의 ID
     * @return 해당 ID의 Way (없으면 null)
     */
    public OSMWay findWayById(long wayId) {
        return ways.stream()                                       // way 목록을 스트림으로 반환
                .filter(way -> way.getId() == wayId)      // ID가 일치하는 way만 필터링
                .findFirst()                                       // 첫 번째 일치하는 요소 찾기
                .orElse(null);                               // 없으면 null 반환
    }

    /*
     * 전체 데이터 요약 정보를 문자열로 반환
     * @param
     * @return "OSMData{nodes=113개, ways=31개}"
     */
    @Override
    public String toString() {
        return String.format("OSMData{nodes=%d개 ways=%d개}",nodes.size(), ways.size());
    }

    /*
     * @param
     * @return
     */


}


