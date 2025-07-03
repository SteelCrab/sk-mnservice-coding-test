import React from 'react';

const ResultItem = ({ person, isSelected, onClick }) => {
  return (
    <li
      className={isSelected ? 'selected' : ''}
      onClick={onClick}
    >
      {person.name}
    </li>
  );
};

export default ResultItem;
