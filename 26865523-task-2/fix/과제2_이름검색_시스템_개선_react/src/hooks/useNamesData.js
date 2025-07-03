import { useState, useEffect } from 'react';
import { parseCSV, getEmbeddedData } from '../utils/dataUtils';

export const useNamesData = () => {
  const [names, setNames] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);
        
        // CSV 파일에서 데이터 로드 시도
        try {
          const response = await fetch('/names.csv');
          if (response.ok) {
            const csvText = await response.text();
            const parsedNames = parseCSV(csvText);
            setNames(parsedNames);
            console.log(`CSV에서 ${parsedNames.length}개 데이터 로드`);
          } else {
            throw new Error('CSV 파일을 찾을 수 없습니다.');
          }
        } catch (csvError) {
          // CSV 로드 실패 시 내장 데이터 사용
          console.warn('CSV 로드 실패, 내장 데이터 사용:', csvError.message);
          const embeddedNames = getEmbeddedData();
          setNames(embeddedNames);
          console.log(`내장 데이터 ${embeddedNames.length}개 로드`);
        }
        
      } catch (err) {
        setError(err.message);
        console.error('데이터 로딩 오류:', err);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, []);

  return { names, loading, error };
};
