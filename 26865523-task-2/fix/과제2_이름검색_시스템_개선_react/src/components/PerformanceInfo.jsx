import React from 'react';

const PerformanceInfo = ({ message }) => {
  if (!message) return null;

  return (
    <div className={`performance-info ${message ? 'show' : ''}`}>
      {message}
    </div>
  );
};

export default PerformanceInfo;
