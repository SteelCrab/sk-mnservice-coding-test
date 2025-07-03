import React from 'react';

const AutocompleteOverlay = ({ query, hint }) => {
  if (!query || !hint) return null;

  return (
    <div className="autocomplete-overlay">
      <span style={{ color: 'transparent' }}>{query}</span>
      <span className="autocomplete-hint">{hint}</span>
    </div>
  );
};

export default AutocompleteOverlay;
