# SK엠앤서비스 코딩테스트

## 지원자 정보
- **이름**: 박용현
- **이메일**: pyh5523@gmail.com
- **개발 기간**: 2025년 7월 1일 ~ 2025년 7월 3일

---

## 프로젝트 개요

본 저장소는 SK엠앤서비스 코딩테스트를 위한 두 가지 과제를 포함합니다:

### Task 1: GPS Map Matching 시스템
**위치**: `26865523-task-1/`
**기술 스택**: Java 17, Maven
**핵심 기능**: GPS 좌표를 도로 네트워크에 매칭하고 경로 이탈을 판정하는 시스템

### Task 2: 이름 검색 시스템 개선
**위치**: `26865523-task-2/`
**기술 스택**: JavaScript (ES6+), React
**핵심 기능**: 기존 검색 시스템의 문제점 분석 및 성능 최적화

---

## 빠른 실행 가이드

### Task 1 - GPS Map Matching 시스템
```bash
# Task 1 디렉토리로 이동
cd 26865523-task-1

# Maven을 통한 실행
mvn compile exec:java -Dexec.mainClass="org.example.Main"

# 또는 간단 실행
mvn exec:java

# 또는 IntelliJ에서 실행 가능
```

**실행 결과**:
- 콘솔에 매칭 결과 및 통계 출력
- `output/` 폴더에 KML 파일 생성 (Google Earth에서 시각화 가능)

### Task 2 - 이름 검색 시스템
```bash
# Task 2 디렉토리로 이동
cd 26865523-task-2

# 바닐라 JS 버전 (브라우저에서 직접 실행)
open fix/과제2_이름검색_시스템_개선.html

# React 버전 실행
cd fix/과제2_이름검색_시스템_개선_react
npm install
npm run dev
# http://localhost:3000 접속
```

---

## Task 1: GPS Map Matching 시스템

### 주요 기능
- **GPS 오차 필터링**: HDOP, 속도, 좌표-각도 불일치 등 다중 기준 적용
- **Map Matching**: 거리(60%) + 각도(20%) + 속도(20%) 가중치 기반 알고리즘
- **경로 이탈 판정**: 포인트별 개별 판정으로 이탈 시점 정확히 식별
- **KML 시각화**: 4색 구분으로 GPS 원본, 매칭, 이탈, 오차 지점 표시

### 처리 결과
- **총 GPS 포인트**: 169개 → 153개 (16개 오차 필터링)
- **매칭 성공률**: 72.8% (123/169 포인트)
- **경로 이탈 감지**: 5개 파일에서 이탈 구간 식별
- **시각화**: 10개 KML 파일 생성

### 기술적 특징
- Java 17 기반 객체지향 설계
- OSM 데이터 파싱 및 도로 네트워크 구축
- 동적 임계값 적용으로 GPS 정확도별 차별화
- Google Earth 연동 KML 생성

---

## Task 2: 이름 검색 시스템 개선

### 기존 시스템 문제점
- **성능 문제**: 매번 전체 DOM 재생성
- **코드 가독성**: 의미없는 함수명 (`hQuery`, `i`, `c1` 등)
- **확장성 부족**: 하드코딩된 데이터, 키보드 네비게이션 미지원
- **잠재적 보안**: XSS 취약점, 메모리 누수 가능성

### 개선사항
- **성능 최적화**: DOM 재사용, 150ms 디바운싱, 효율적 검색 알고리즘
- **코드 품질**: 클래스 기반 구조, 명확한 네이밍, 모듈화
- **새로운 기능**: 키보드 네비게이션, 상세정보 모달, CSV 파일 지원
- **대용량 지원**: 10만개 데이터 처리 최적화

### 구현 결과
- **바닐라 JS 버전**: 단일 HTML 파일로 완전 동작
- **React 버전**: 컴포넌트 기반, 커스텀 훅, 성능 최적화
- **성능 개선**: 검색 속도 50-100ms → 5-15ms
- **사용자 경험**: 키보드 네비게이션, 실시간 성능 표시

---

## 프로젝트 구조

```
sk-mnservice-coding-test/
├── README.md                           # 메인 프로젝트 설명서
├── 26865523-task-1/                    # GPS Map Matching 시스템
│   ├── src/main/java/                  # Java 소스 코드
│   ├── data/                           # OSM 파일 및 GPS 데이터
│   ├── output/                         # KML 결과 파일
│   ├── feedback/README.md              # 상세 문서 및 다이어그램
│   └── pom.xml                         # Maven 설정
└── 26865523-task-2/                    # 이름 검색 시스템 개선
    ├── fix/                            # 개선된 구현체
    │   ├── 과제2_이름검색_시스템_개선.html
    │   ├── names.csv
    │   └── 과제2_이름검색_시스템_개선_react/
    ├── data/                           # 원본 파일
    └── feedback/README.md              # 상세 분석 및 개선사항
```

---

## 개발 환경 요구사항

### Task 1 (GPS Map Matching)
- **Java**: 17 이상
- **Maven**: 3.6 이상
- **메모리**: 최소 512MB
- **IDE**: IntelliJ IDEA 권장

### Task 2 (이름 검색 시스템)
- **브라우저**: Chrome, Firefox, Safari 등 모던 브라우저
- **Node.js**: 16 이상 (React 버전)
- **npm**: 8 이상 (React 버전)

---

## 상세 문서

각 프로젝트의 상세한 구현 내용, 알고리즘 설명, 다이어그램은 다음 문서를 참조하세요:

- **Task 1 상세 문서**: [`26865523-task-1/feedback/README.md`](26865523-task-1/feedback/README.md)
- **Task 2 상세 문서**: [`26865523-task-2/feedback/README.md`](26865523-task-2/feedback/README.md)

---

*본 프로젝트는 SK엠앤서비스 코딩테스트를 위해 개발되었습니다.*
