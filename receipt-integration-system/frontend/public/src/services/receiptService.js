import axios from 'axios';
import { getAuthToken } from '../utils/auth';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor for auth
api.interceptors.request.use(
  (config) => {
    const token = getAuthToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Handle unauthorized
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const receiptService = {
  // 영수증 목록 조회
  getReceipts: async (params) => {
    const response = await api.get('/receipts', { params });
    return response.data;
  },

  // 영수증 상세 조회
  getReceiptDetail: async (receiptId) => {
    const response = await api.get(`/receipts/${receiptId}`);
    return response.data;
  },

  // 영수증 동기화
  syncReceipts: async (cardIds = []) => {
    const response = await api.post('/receipts/sync', { cardIds });
    return response.data;
  },

  // 영수증 매칭
  matchReceipt: async (receiptId, matchData) => {
    const response = await api.post(`/receipts/${receiptId}/match`, matchData);
    return response.data;
  },

  // 자동 매칭
  autoMatch: async (params) => {
    const response = await api.post('/receipts/match/auto', params);
    return response.data;
  },

  // 영수증 다운로드
  downloadReceipt: async (receiptId) => {
    const response = await api.get(`/receipts/${receiptId}/download`, {
      responseType: 'blob'
    });
    
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `receipt_${receiptId}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  // 영수증 내보내기
  exportReceipts: async (params) => {
    const response = await api.post('/receipts/export', params, {
      responseType: 'blob'
    });
    return response.data;
  },

  // 통계 데이터 조회
  getStatistics: async (params) => {
    const response = await api.get('/receipts/statistics', { params });
    return response.data;
  }
};