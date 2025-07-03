import React, { useState, useCallback, useMemo, useEffect } from 'react';
import { useDebounce } from '../hooks/useDebounce';
import { useNamesData } from '../hooks/useNamesData';
import { searchNames } from '../utils/dataUtils';
import AutocompleteOverlay from './AutocompleteOverlay';
import ResultsList from './ResultsList';
import PersonModal from './PersonModal';
import PerformanceInfo from './PerformanceInfo';

const NameSearchSystem = () => {
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const [selectedPerson, setSelectedPerson] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [performanceMessage, setPerformanceMessage] = useState('');

  // 커스텀 훅 사용
  const { names, loading, error } = useNamesData();
  const debouncedQuery = useDebounce(query, 150);

  // 검색 결과 메모이제이션
  const filteredNames = useMemo(() => {
    if (!debouncedQuery.trim() || !names.length) return [];
    
    const startTime = performance.now();
    const results = searchNames(names, debouncedQuery);
    const endTime = performance.now();
    
    setPerformanceMessage(`검색 완료: ${(endTime - startTime).toFixed(1)}ms`);
    return results;
  }, [names, debouncedQuery]);

  // 자동완성 힌트 계산
  const autocompleteHint = useMemo(() => {
    if (!query || !filteredNames.length) return '';
    
    const firstMatch = filteredNames[0].name;
    if (firstMatch.toLowerCase().startsWith(query.toLowerCase())) {
      return firstMatch.slice(query.length);
    }
    return '';
  }, [query, filteredNames]);

  // 키보드 네비게이션 처리
  const handleKeyDown = useCallback((e) => {
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setSelectedIndex(prev => Math.min(prev + 1, filteredNames.length - 1));
        break;
        
      case 'ArrowUp':
        e.preventDefault();
        setSelectedIndex(prev => Math.max(prev - 1, -1));
        break;
        
      case 'Enter':
        e.preventDefault();
        if (selectedIndex >= 0 && filteredNames[selectedIndex]) {
          handlePersonClick(filteredNames[selectedIndex]);
        }
        break;
        
      case 'Tab':
        if (autocompleteHint) {
          e.preventDefault();
          setQuery(query + autocompleteHint);
        }
        break;
        
      case 'Escape':
        if (isModalOpen) {
          setIsModalOpen(false);
        }
        break;
    }
  }, [selectedIndex, filteredNames, autocompleteHint, query, isModalOpen]);

  // 개인 정보 클릭 처리
  const handlePersonClick = useCallback((person) => {
    setSelectedPerson(person);
    setIsModalOpen(true);
  }, []);

  // 모달 닫기
  const handleCloseModal = useCallback(() => {
    setIsModalOpen(false);
    setSelectedPerson(null);
  }, []);

  // 검색어 변경 시 선택 인덱스 초기화
  useEffect(() => {
    setSelectedIndex(-1);
  }, [debouncedQuery]);

  // 성능 메시지 자동 숨김
  useEffect(() => {
    if (performanceMessage) {
      const timer = setTimeout(() => {
        setPerformanceMessage('');
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [performanceMessage]);

  if (loading) {
    return (
      <div className="container">
        <div className="loading">시스템을 초기화하는 중입니다...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container">
        <div style={{ color: 'red', textAlign: 'center', padding: '20px' }}>
          오류가 발생했습니다: {error}
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="container">
        <h1>이름 검색 시스템</h1>
        
        <div className="info">
          💡 이름을 입력하면 자동완성과 관련 결과를 확인할 수 있습니다. 결과를 클릭하면 상세 정보를 볼 수 있습니다.
        </div>

        <div className="search-container">
          <AutocompleteOverlay query={query} hint={autocompleteHint} />
          <input
            id="searchInput"
            type="text"
            placeholder="이름을 입력하세요..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            autoComplete="off"
          />
        </div>

        <div className="results-container">
          <div className="results-title">검색 결과:</div>
          <ResultsList
            results={filteredNames}
            query={debouncedQuery}
            selectedIndex={selectedIndex}
            onPersonClick={handlePersonClick}
          />
        </div>
      </div>

      <PersonModal
        person={selectedPerson}
        isOpen={isModalOpen}
        onClose={handleCloseModal}
      />

      <PerformanceInfo message={performanceMessage} />
    </>
  );
};

export default NameSearchSystem;
