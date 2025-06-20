package com.company.receipt.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResponseDto {
    
    private Long matchId;
    
    private Long receiptId;
    
    private String receiptNumber;
    
    private String erpLedgerId;
    
    private String accountCode;
    
    private String accountName;
    
    private String costCenter;
    
    private String projectCode;
    
    private BigDecimal matchedAmount;
    
    private BigDecimal receiptAmount;
    
    private BigDecimal remainingAmount;
    
    private String matchStatus; // MATCHED, PARTIAL, PENDING, CANCELLED
    
    private String matchType;
    
    private String approvalStatus; // PENDING, APPROVED, REJECTED
    
    private Double confidenceScore;
    
    private String matchCriteria;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime matchedAt;
    
    private String matchedByName;
    
    private String matchedByEmail;
    
    private String notes;
    
    // 영수증 정보
    private ReceiptSummary receiptSummary;
    
    // 매칭 검증 정보
    private ValidationResult validationResult;
    
    // 관련 매칭 정보 (같은 영수증의 다른 매칭들)
    private List<RelatedMatch> relatedMatches;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReceiptSummary {
        private String merchantName;
        private LocalDateTime transactionDate;
        private BigDecimal amount;
        private String approvalNumber;
        private String cardAlias;
        private Boolean isVerified;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationResult {
        private Boolean isValid;
        private List<String> warnings;
        private List<String> errors;
        private Map<String, Object> validationDetails;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelatedMatch {
        private Long matchId;
        private String accountCode;
        private String accountName;
        private BigDecimal matchedAmount;
        private String matchStatus;
        private LocalDateTime matchedAt;
    }
    
    // 편의 메소드
    public boolean isFullyMatched() {
        return remainingAmount != null && remainingAmount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean canBeApproved() {
        return "MATCHED".equals(matchStatus) && "PENDING".equals(approvalStatus);
    }
    
    public boolean hasValidationErrors() {
        return validationResult != null && 
               validationResult.getErrors() != null && 
               !validationResult.getErrors().isEmpty();
    }
}