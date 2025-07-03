import React from 'react';
import ResultItem from './ResultItem';

const ResultsList = ({ results, query, selectedIndex, onPersonClick }) => {
  if (!query.trim()) {
    return (
      <ul className="results-list">
        <li className="no-results">검색어를 입력해주세요</li>
      </ul>
    );
  }

  if (results.length === 0) {
    return (
      <ul className="results-list">
        <li className="no-results">검색 결과가 없습니다</li>
      </ul>
    );
  }

  return (
    <ul className="results-list">
      {results.map((person, index) => (
        <ResultItem
          key={`${person.name}-${index}`}
          person={person}
          isSelected={index === selectedIndex}
          onClick={() => onPersonClick(person)}
        />
      ))}
    </ul>
  );
};

export default ResultsList;
