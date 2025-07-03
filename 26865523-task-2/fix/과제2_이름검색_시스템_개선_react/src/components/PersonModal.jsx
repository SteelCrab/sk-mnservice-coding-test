import React, { useState, useEffect } from 'react';

const PersonModal = ({ person, isOpen, onClose }) => {
  const [detailInfo, setDetailInfo] = useState(null);
  const [loading, setLoading] = useState(false);

  // 모의 API 호출
  const fetchPersonDetail = async (person) => {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    return {
      ...person,
      email: `${person.name.toLowerCase().replace(/\s+/g, '.')}@example.com`,
      phone: `010-${Math.floor(Math.random() * 9000) + 1000}-${Math.floor(Math.random() * 9000) + 1000}`,
      department: ['Engineering', 'Marketing', 'Sales', 'HR', 'Finance'][Math.floor(Math.random() * 5)],
      joinDate: new Date(2020 + Math.floor(Math.random() * 4), Math.floor(Math.random() * 12), Math.floor(Math.random() * 28) + 1).toLocaleDateString('ko-KR')
    };
  };

  useEffect(() => {
    if (isOpen && person) {
      setLoading(true);
      setDetailInfo(null);
      
      fetchPersonDetail(person)
        .then(setDetailInfo)
        .catch(console.error)
        .finally(() => setLoading(false));
    }
  }, [isOpen, person]);

  if (!isOpen) return null;

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div className={`modal-overlay ${isOpen ? 'show' : ''}`} onClick={handleOverlayClick}>
      <div className="modal">
        <div className="modal-header">
          <h2 className="modal-title">상세 정보</h2>
          <button className="modal-close" onClick={onClose}>
            &times;
          </button>
        </div>
        <div className="modal-content">
          {loading ? (
            <div className="loading">정보를 불러오는 중입니다...</div>
          ) : detailInfo ? (
            <div className="person-detail">
              <div className="detail-item">
                <div className="detail-icon">👤</div>
                <div className="detail-info">
                  <div className="detail-label">이름</div>
                  <div className="detail-value">{detailInfo.name}</div>
                </div>
              </div>
              <div className="detail-item">
                <div className="detail-icon">🎂</div>
                <div className="detail-info">
                  <div className="detail-label">나이</div>
                  <div className="detail-value">{detailInfo.age}세</div>
                </div>
              </div>
              <div className="detail-item">
                <div className="detail-icon">💼</div>
                <div className="detail-info">
                  <div className="detail-label">직업</div>
                  <div className="detail-value">{detailInfo.job}</div>
                </div>
              </div>
              <div className="detail-item">
                <div className="detail-icon">🏢</div>
                <div className="detail-info">
                  <div className="detail-label">부서</div>
                  <div className="detail-value">{detailInfo.department}</div>
                </div>
              </div>
              <div className="detail-item">
                <div className="detail-icon">🏙️</div>
                <div className="detail-info">
                  <div className="detail-label">도시</div>
                  <div className="detail-value">{detailInfo.city}</div>
                </div>
              </div>
              <div className="detail-item">
                <div className="detail-icon">📧</div>
                <div className="detail-info">
                  <div className="detail-label">이메일</div>
                  <div className="detail-value">{detailInfo.email}</div>
                </div>
              </div>
              <div className="detail-item">
                <div className="detail-icon">📱</div>
                <div className="detail-info">
                  <div className="detail-label">전화번호</div>
                  <div className="detail-value">{detailInfo.phone}</div>
                </div>
              </div>
              <div className="detail-item">
                <div className="detail-icon">📅</div>
                <div className="detail-info">
                  <div className="detail-label">입사일</div>
                  <div className="detail-value">{detailInfo.joinDate}</div>
                </div>
              </div>
            </div>
          ) : (
            <div>정보를 불러올 수 없습니다.</div>
          )}
        </div>
      </div>
    </div>
  );
};

export default PersonModal;
