import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Provider } from 'react-redux';
import { ConfigProvider } from 'antd';
import koKR from 'antd/locale/ko_KR';
import { store } from './store';
import { AuthProvider } from './contexts/AuthContext';
import PrivateRoute from './components/common/PrivateRoute';
import MainLayout from './components/layout/MainLayout';

// Pages
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ReceiptManagementPage from './pages/ReceiptManagementPage';
import CardManagementPage from './pages/CardManagementPage';
import ReceiptDetailPage from './pages/ReceiptDetailPage';
import AccountingMatchPage from './pages/AccountingMatchPage';
import ReportPage from './pages/ReportPage';

import 'antd/dist/reset.css';
import './styles/global.css';

function App() {
  return (
    <Provider store={store}>
      <ConfigProvider locale={koKR}>
        <AuthProvider>
          <Router>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/" element={<PrivateRoute><MainLayout /></PrivateRoute>}>
                <Route index element={<Navigate to="/dashboard" replace />} />
                <Route path="dashboard" element={<DashboardPage />} />
                <Route path="receipts" element={<ReceiptManagementPage />} />
                <Route path="receipts/:id" element={<ReceiptDetailPage />} />
                <Route path="cards" element={<CardManagementPage />} />
                <Route path="matching" element={<AccountingMatchPage />} />
                <Route path="reports" element={<ReportPage />} />
              </Route>
            </Routes>
          </Router>
        </AuthProvider>
      </ConfigProvider>
    </Provider>
  );
}

export default App;