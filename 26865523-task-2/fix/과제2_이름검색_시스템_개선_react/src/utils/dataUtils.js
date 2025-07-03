/**
 * CSV 텍스트를 파싱하여 객체 배열로 변환
 */
export const parseCSV = (csvText) => {
  const lines = csvText.trim().split('\n');
  const headers = lines[0].split(',');
  const names = [];

  for (let i = 1; i < lines.length; i++) {
    const values = lines[i].split(',');
    const person = {};
    headers.forEach((header, index) => {
      person[header.trim()] = values[index]?.trim() || '';
    });
    names.push(person);
  }

  return names;
};

/**
 * 내장 데이터 반환 (CSV 로드 실패 시 사용)
 */
export const getEmbeddedData = () => {
  const csvData = `Alexander,Alice,Amanda,Andrew,Angela,Anna,Anthony,Ashley,Barbara,Benjamin,Betty,Brian,Carol,Charles,Christopher,Daniel,David,Deborah,Donald,Donna,Dorothy,Edward,Elizabeth,Emily,Emma,Eric,Frances,Frank,Gary,George,Helen,Henry,James,Jason,Jennifer,Jessica,John,Joseph,Joshua,Karen,Kimberly,Larry,Laura,Linda,Lisa,Margaret,Maria,Mark,Mary,Matthew,Michael,Michelle,Nancy,Nicole,Noah,Olivia,Patricia,Paul,Rachel,Rebecca,Richard,Robert,Ronald,Ruth,Sandra,Sarah,Scott,Sharon,Sophia,Stephen,Steven,Susan,Thomas,Timothy,Victoria,Virginia,Walter,Wayne,William,Abigail,Adam,Adrian,Albert,Andrea,Annie,Arthur,Austin,Brenda,Bruce,Carl,Catherine,Christine,Cynthia, Diane,Douglas,Eugene,Hannah,Harold,Isabella,Jack,Jacob,Jane,Janet,Jerry,Joan,Jonathan,Juan,Julie,Justin,Katherine,Keith,Kevin,Laura,Louis,Martha,Mason,Nathan,Noah,Peter,Ralph,Raymond,Roger,Ryan,Samantha,Sean,Shirley,Tyler,Virginia,Willie`;
  
  return csvData.split(',').map((name, index) => ({
    name: name.trim(),
    age: 25 + (index % 20),
    job: ['Engineer', 'Designer', 'Teacher', 'Manager', 'Developer'][index % 5],
    city: ['Seoul', 'Busan', 'Incheon', 'Daegu', 'Gwangju'][index % 5]
  }));
};

/**
 * 효율적인 검색 알고리즘 (대용량 데이터 지원)
 */
export const searchNames = (names, query) => {
  if (!query.trim()) {
    return [];
  }

  const lowerQuery = query.toLowerCase();
  const exactMatches = [];
  const startMatches = [];
  const containsMatches = [];

  for (const person of names) {
    const name = person.name.toLowerCase();
    
    if (name === lowerQuery) {
      exactMatches.push(person);
    } else if (name.startsWith(lowerQuery)) {
      startMatches.push(person);
    } else if (name.includes(lowerQuery)) {
      containsMatches.push(person);
    }
    
    // 대용량 데이터 처리를 위한 결과 제한
    if (exactMatches.length + startMatches.length + containsMatches.length >= 100) {
      break;
    }
  }

  return [...exactMatches, ...startMatches, ...containsMatches].slice(0, 100);
};
