package org.example.parser;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.example.model.*;
import org.dom4j.Document;

import java.io.File;
import java.util.List;

/**
 * <p>OSM XML 파일을 파싱하여 OSMData 객체로 변환하는 파서</p>
 * <p>DOM4J 라이브러리를 사용하여 XML 구조를 분석하고 Java 객체로 매핑</p>
 *
 * <p>주요 기능:</p>
 * <p>- OSM XML 파일 로드 및 파싱</p>
 * <p>- Node 요소들을 OSMNode 객체로 변환</p>
 * <p>- Way 요소들을 OSMWay 객체로 변환</p>
 * <p>- 기준 경로 Way들의 유효성 검증</p>
 * <p>- 상세한 파싱 진행 상황 로깅</p>
 */
public class OSMParser {

    /**
     * 기준 경로를 구성하는 Way ID들 (문제에서 제공된 순서)
     * 차량이 따라야 할 정상 경로의 순서대로 정렬
     * 521766182 → 990628459 → 472042763 → 218864485 → 520307304
     */
    private static final String[] REFERENCE_WAY_IDS = {
            "521766182", "990628459", "472042763", "218864485", "520307304"
    };

    /**
     * OSM 파일을 파싱하여 OSMData 객체로 반환하는 메인 메소드
     *
     * 처리 순서:
     * 1. XML 문서 로드 (DOM4J SAXReader 사용)
     * 2. 루트 요소에서 모든 <node> 요소 파싱
     * 3. 루트 요소에서 모든 <way> 요소 파싱
     * 4. 기준 경로 Way들의 존재 여부 검증
     *
     * @param osmFilePath OSM XML 파일의 절대 또는 상대 경로
     * @return 파싱된 노드와 Way들을 포함하는 OSMData 객체
     * @throws DocumentException XML 파일 읽기 실패 시 (파일 없음, 잘못된 XML 구조 등)
     */
    public OSMData parseOSMFile(String osmFilePath) throws DocumentException {
        System.out.println("🗺️ OSM 파일 파싱 시작: " + osmFilePath);

        // DOM4J를 사용하여 XML 문서 로드
        SAXReader reader = new SAXReader();
        Document document = reader.read(new File(osmFilePath));
        Element root = document.getRootElement();

        // 파싱 결과를 담을 컨테이너 생성
        OSMData osmData = new OSMData();

        // 1단계: Node 요소들 파싱 (교차점, 도로 꺾이는 지점 등)
        parseNodes(root, osmData);

        // 2단계: Way 요소들 파싱 (도로, 강, 건물 경계 등)
        parseWays(root, osmData);

        // 3단계: 기준 경로 Way들이 모두 존재하는지 검증
        validateReferencePath(osmData);

        System.out.println("✅ OSM 파싱 완료: " + osmData);
        return osmData;
    }

    /**
     * XML 루트에서 모든 <node> 요소들을 파싱하여 OSMNode 객체로 변환
     *
     * <node> 요소 구조 예시:
     * <node id="521766182" lat="37.4963" lon="127.0283"/>
     *
     * @param root OSM XML의 루트 요소 (<osm> 태그)
     * @param osmData 파싱 결과를 저장할 OSMData 컨테이너
     */
    private void parseNodes(Element root, OSMData osmData) {
        List<Element> nodeElements = root.elements("node");
        System.out.println("📍 Node 파싱 중... (" + nodeElements.size() + "개)");

        int successCount = 0;
        int errorCount = 0;

        for (Element nodeElement : nodeElements) {
            try {
                long id = Long.parseLong(nodeElement.attributeValue("id"));
                double lat = Double.parseDouble(nodeElement.attributeValue("lat"));
                double lon = Double.parseDouble(nodeElement.attributeValue("lon"));

                // 좌표 유효성 검증
                if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                    System.err.println("⚠️ 잘못된 좌표 범위: lat=" + lat + ", lon=" + lon);
                    errorCount++;
                    continue;
                }

                OSMNode node = new OSMNode(id, lat, lon);
                osmData.addNode(node);
                successCount++;

                if (successCount <= 5) {
                    System.out.println("  ✅ Node 파싱 성공 " + successCount + ": " + node);
                }

            } catch (NumberFormatException e) {
                errorCount++;
                if (errorCount <= 5) {
                    System.err.println("⚠️ Node 파싱 오류 " + errorCount + ": " + e.getMessage());
                }
            } catch (Exception e) {
                errorCount++;
                if (errorCount <= 3) {
                    System.err.println("⚠️ Node 파싱 예외 " + errorCount + ": " + e.getMessage());
                }
            }
        }

        System.out.println("✅ Node 파싱 완료: 성공 " + successCount + "개, 실패 " + errorCount + "개");

        if (successCount == 0) {
            System.err.println("❌ 심각한 문제: 노드가 하나도 파싱되지 않았습니다!");
        }
    }

    /**
     * XML 루트에서 모든 <way> 요소들을 파싱하여 OSMWay 객체로 변환
     *
     * <way> 요소 구조 예시:
     * <way id="521766182">
     *   <nd ref="123"/>                  <!-- 구성 노드 참조 -->
     *   <nd ref="456"/>
     *   <tag k="highway" v="primary"/>   <!-- 도로 타입 -->
     *   <tag k="name" v="강남대로"/>       <!-- 도로명 -->
     * </way>
     *
     * @param root OSM XML의 루트 요소 (<osm> 태그)
     * @param osmData 파싱 결과를 저장할 OSMData 컨테이너
     */
    private void parseWays(Element root, OSMData osmData) {
        List<Element> wayElements = root.elements("way");
        System.out.println("🛣️ Way 파싱 중... (" + wayElements.size() + "개)");

        int successCount = 0;

        for (Element wayElement : wayElements) {
            try {
                long id = Long.parseLong(wayElement.attributeValue("id"));
                OSMWay way = new OSMWay(id);

                // <nd> 요소들에서 구성 노드 ID들 추출
                List<Element> ndElements = wayElement.elements("nd");
                for (Element ndElement : ndElements) {
                    long nodeId = Long.parseLong(ndElement.attributeValue("ref"));
                    way.addNodeId(nodeId);
                }

                // <tag> 요소 파싱 - Map Matching에 필요한 모든 태그
                List<Element> tagElements = wayElement.elements("tag");
                int parsedTags = 0;

                for (Element tagElement : tagElements) {
                    String key = tagElement.attributeValue("k");
                    String value = tagElement.attributeValue("v");

                    // Map Matching 핵심 태그들만 파싱
                    switch (key) {
                        case "highway":
                            way.setHighway(value);
                            parsedTags++;
                            break;
                        case "name":
                            way.setName(value);
                            parsedTags++;
                            break;
                        case "maxspeed":
                            way.setMaxspeed(value);
                            parsedTags++;
                            break;
                        case "lanes":
                            way.setLanes(value);
                            parsedTags++;
                            break;
                        case "oneway":
                            way.setOneway(value);
                            parsedTags++;
                            break;
                        case "access":
                            way.setAccess(value);
                            parsedTags++;
                            break;
                        case "width":
                            way.setWidth(value);
                            parsedTags++;
                            break;
                        case "surface":
                            way.setSurface(value);
                            parsedTags++;
                            break;
                        case "junction":
                            way.setJunction(value);
                            parsedTags++;
                            break;
                        case "bridge":
                            way.setBridge(value);
                            parsedTags++;
                            break;
                        case "tunnel":
                            way.setTunnel(value);
                            parsedTags++;
                            break;
                        case "level":
                            way.setLevel(value);
                            parsedTags++;
                            break;
                        default:
                            // 기타 태그들은 무시
                            break;
                    }
                }

                osmData.addWay(way);
                successCount++;

                // 기준 경로 Way인 경우 상세 정보 출력
                if (isReferenceWay(id)) {
                    System.out.println("  🎯 기준 경로 Way 발견: " + way);
                    printPureWayDetails(way, osmData, parsedTags);
                }

            } catch (NumberFormatException e) {
                System.err.println("⚠️ Way 파싱 오류: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("⚠️ Way 파싱 예외: " + e.getMessage());
            }
        }

        System.out.println("✅ Way 파싱 완료: " + successCount + "개");
    }

    /**
     * Way 상세 정보 출력 - 순수 OSM 데이터만 표시 (기본값 없음)
     */
    private void printPureWayDetails(OSMWay way, OSMData osmData, int parsedTags) {
        System.out.println("    📊 Way 상세 정보:");
        System.out.println("      - ID: " + way.getId());
        System.out.println("      - 노드 개수: " + way.getNodeIds().size());
        System.out.println("      - 파싱된 태그 수: " + parsedTags);

        // 기본 정보
        System.out.println("      🛣️ 기본 정보:");
        System.out.println("        - 도로 타입: " + way.getHighway());
        System.out.println("        - 이름: " + way.getName());

        // 🔧 순수 OSM 데이터만 표시 (기본값 제거)
        System.out.println("      📋 태그 정보:");
        System.out.println("        - 최대속도: " + (way.getMaxspeed() != null ? way.getMaxspeed() : "정보 없음"));
        System.out.println("        - 차선수: " + (way.getLanes() != null ? way.getLanes() : "정보 없음"));
        System.out.println("        - 일방통행: " + (way.getOneway() != null ? way.getOneway() : "정보 없음"));
        System.out.println("        - 접근성: " + (way.getAccess() != null ? way.getAccess() : "정보 없음"));

        // 물리적 특성 (데이터가 있을 때만 표시)
        boolean hasPhysicalInfo = way.getWidth() != null || way.getSurface() != null;
        if (hasPhysicalInfo) {
            System.out.println("      📏 물리적 특성:");
            if (way.getWidth() != null) {
                System.out.println("        - 도로폭: " + way.getWidth());
            }
            if (way.getSurface() != null) {
                System.out.println("        - 표면: " + way.getSurface());
            }
        }

        // 특수 구조물 (데이터가 있을 때만 표시)
        boolean hasSpecialStructure = way.getBridge() != null || way.getTunnel() != null || way.getLevel() != null;
        if (hasSpecialStructure) {
            System.out.println("      🏗️ 특수 구조물:");
            if (way.getBridge() != null) {
                System.out.println("        - 교량: " + way.getBridge());
            }
            if (way.getTunnel() != null) {
                System.out.println("        - 터널: " + way.getTunnel());
            }
            if (way.getLevel() != null) {
                System.out.println("        - 레벨: " + way.getLevel());
            }
        }

        // 시작점/끝점 정보
        List<Long> nodeIds = way.getNodeIds();
        if (!nodeIds.isEmpty()) {
            OSMNode firstNode = osmData.findNodeById(nodeIds.get(0));
            OSMNode lastNode = osmData.findNodeById(nodeIds.get(nodeIds.size() - 1));

            if (firstNode != null && lastNode != null) {
                System.out.println("      📍 경로 정보:");
                System.out.println("        - 시작점: " + firstNode);
                System.out.println("        - 끝점: " + lastNode);
            } else {
                System.err.println("      ⚠️ 참조된 노드 누락 감지");
            }
        }
    }

    /**
     * 주어진 Way ID가 기준 경로에 포함되는지 확인하는 유틸리티 메소드
     */
    private boolean isReferenceWay(long wayId) {
        for (String refWayId : REFERENCE_WAY_IDS) {
            if (Long.parseLong(refWayId) == wayId) {
                return true;
            }
        }
        return false;
    }

    /**
     * 기준 경로를 구성하는 5개 Way가 모두 파싱되었는지 검증
     */
    private void validateReferencePath(OSMData osmData) {
        System.out.println("\n🔍 기준 경로 검증 중...");

        int foundWays = 0;

        for (String wayIdStr : REFERENCE_WAY_IDS) {
            long wayId = Long.parseLong(wayIdStr);
            OSMWay way = osmData.findWayById(wayId);

            if (way == null) {
                System.err.println("❌ 기준 경로 Way 누락: " + wayId);
            } else {
                foundWays++;
                System.out.println("✅ 기준 경로 Way 확인: " + way);

                // 노드 참조 유효성 검증
                int validNodes = 0;
                for (Long nodeId : way.getNodeIds()) {
                    if (osmData.findNodeById(nodeId) != null) {
                        validNodes++;
                    }
                }

                if (validNodes == 0) {
                    System.err.println("    ❌ 이 Way의 모든 노드가 누락됨!");
                } else if (validNodes < way.getNodeIds().size()) {
                    System.err.println("    ⚠️ 일부 노드 누락: " + validNodes + "/" + way.getNodeIds().size());
                } else {
                    System.out.println("    ✅ 모든 참조 노드 존재");
                }
            }
        }

        System.out.println("📊 기준 경로 검증 결과: " + foundWays + "/" + REFERENCE_WAY_IDS.length + " Way 발견");
    }

    /**
     * OSM 파서 단독 테스트용 메인 메서드
     */
    public static void main(String[] args) {
        OSMParser parser = new OSMParser();

        try {
            System.out.println("🔧 순수 OSM 데이터 파서 테스트 시작");

            // 실제 OSM 파일 파싱 실행
            OSMData osmData = parser.parseOSMFile("data/roads.osm");

            // 파싱 결과 통계 출력
            System.out.println("\n📊 최종 파싱 결과:");
            System.out.println("- 총 노드 수: " + osmData.getNodes().size());
            System.out.println("- 총 Way 수: " + osmData.getWays().size());

            // 샘플 노드 출력
            if (osmData.getNodes().size() > 0) {
                System.out.println("\n📍 샘플 노드들:");
                List<OSMNode> nodes = osmData.getNodes();
                for (int i = 0; i < Math.min(5, nodes.size()); i++) {
                    System.out.println("  " + (i+1) + ". " + nodes.get(i));
                }
            } else {
                System.err.println("\n❌ 심각: 노드가 전혀 파싱되지 않았습니다!");
            }

            // 기준 경로 Way들의 상세 분석
            System.out.println("\n🛣️ 기준 경로 분석:");
            for (String wayIdStr : REFERENCE_WAY_IDS) {
                long wayId = Long.parseLong(wayIdStr);
                OSMWay way = osmData.findWayById(wayId);
                if (way != null) {
                    String wayName = way.getName() != null ? way.getName() : "이름없음";
                    System.out.println("  " + wayName + " (ID: " + wayId +
                            ", 노드: " + way.getNodeIds().size() + "개)");
                } else {
                    System.err.println("  ❌ Way " + wayId + " 파싱 실패");
                }
            }

        } catch (DocumentException e) {
            System.err.println("❌ OSM 파일 파싱 실패: " + e.getMessage());
            System.err.println("   - 파일 경로 확인: data/roads.osm");
            System.err.println("   - XML 구조 검증 필요");
            e.printStackTrace();
        }
    }
}