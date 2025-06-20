package com.company.receipt.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoMatchResponseDto {
    
    private String batchId; // 배치 작업 ID
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executionTime;
    
    private Long processingTimeMillis;
    
    private String status; // COMPLETED, FAILED, PARTIAL_SUCCESS
    
    // 전체 통계
    private MatchingStatistics statistics;
    
    // 개별 매칭 결과
    @Builder.Default
    private List<MatchResult> matchResults = new ArrayList<>();
    
    // 매칭 실패 목록
    @Builder.Default
    private List<UnmatchedReceipt> unmatchedReceipts = new ArrayList<>();
    
    // 오류 및 경고
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchingStatistics {
        private Integer totalReceipts; // 전체 영수증 수
        private Integer eligibleReceipts; // 매칭 대상 영수증 수
        private Integer successfulMatches; // 성공적으로 매칭된 수
        private Integer failedMatches; // 매칭 실패 수
        private Integer lowConfidenceMatches; // 낮은 신뢰도 매칭 수
        private Integer multipleMatches; // 복수 매칭 수
        private Double averageConfidenceScore; // 평균 신뢰도
        private Map<String, Integer> matchesByAccountCode; // 계정과목별 매칭 수
        private Map<String, Integer> matchesByStrategy; // 전략별 매칭 수
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchResult {
        private Long receiptId;
        private String receiptNumber;
        private Long matchId;
        private String erpLedgerId;
        private String accountCode;
        private String accountName;
        private Double confidenceScore;
        private String matchingStrategy;
        private String matchingRule;
        private List<String> matchReasons;
        private Boolean requiresApproval;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime matchedAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnmatchedReceipt {
        private Long receiptId;
        private String receiptNumber;
        private String merchantName;
        private LocalDateTime transactionDate;
        private List<String> failureReasons;
        private List<PotentialMatch> potentialMatches; // 가능성 있는 매칭들
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PotentialMatch {
        private String erpLedgerId;
        private String accountCode;
        private String accountName;
        private Double confidenceScore;
        private List<String> missingCriteria; // 부족한 매칭 조건
    }
    
    // 편의 메소드
    public boolean isSuccess() {
        return "COMPLETED".equals(status) && 
               (errors == null || errors.isEmpty());
    }
    
    public double getSuccessRate() {
        if (statistics == null || statistics.getEligibleReceipts() == null || 
            statistics.getEligibleReceipts() == 0) {
            return 0.0;
        }
        return (statistics.getSuccessfulMatches() * 100.0) / statistics.getEligibleReceipts();
    }
}
