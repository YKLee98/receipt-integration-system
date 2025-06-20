package com.company.receipt.external.common;

import com.company.receipt.domain.CardInfo;
import com.company.receipt.domain.TransactionRecord;

import java.time.LocalDateTime;
import java.util.List;

public interface CardApiService {
    
    /**
     * 지원하는 카드사인지 확인
     */
    boolean supports(String cardCompany);
    
    /**
     * 카드사명 반환
     */
    String getCardCompany();
    
    /**
     * 거래내역 조회
     */
    List<TransactionRecord> fetchTransactions(
        CardInfo cardInfo, 
        LocalDateTime fromDate, 
        LocalDateTime toDate
    ) throws Exception;
    
    /**
     * 영수증 문서 다운로드
     */
    String downloadReceiptDocument(TransactionRecord transaction) throws Exception;
    
    /**
     * 카드 유효성 검증
     */
    boolean validateCard(String cardNumber, String authCredentials) throws Exception;
    
    /**
     * 거래내역 실시간 조회 지원 여부
     */
    default boolean supportsRealtimeSync() {
        return false;
    }
    
    /**
     * 배치 조회 최대 기간 (일)
     */
    default int getMaxBatchPeriodDays() {
        return 90;
    }
    
    /**
     * API 호출 제한 정보
     */
    default RateLimitInfo getRateLimitInfo() {
        return new RateLimitInfo(100, 3600); // 기본값: 시간당 100회
    }
    
    class RateLimitInfo {
        private final int maxCalls;
        private final int periodSeconds;
        
        public RateLimitInfo(int maxCalls, int periodSeconds) {
            this.maxCalls = maxCalls;
            this.periodSeconds = periodSeconds;
        }
        
        public int getMaxCalls() {
            return maxCalls;
        }
        
        public int getPeriodSeconds() {
            return periodSeconds;
        }
    }
}
