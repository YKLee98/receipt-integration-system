package com.company.receipt.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptResponseDto {
    
    private Long receiptId;
    
    private String receiptNumber;
    
    private String receiptType;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issueDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime transactionDate;
    
    private String approvalNumber;
    
    // 가맹점 정보
    private String merchantName;
    
    private String merchantBizNumber;
    
    private String merchantCategory;
    
    // 금액 정보
    private BigDecimal amount;
    
    private BigDecimal vatAmount;
    
    private BigDecimal serviceFee;
    
    private BigDecimal totalAmount;
    
    private String currency;
    
    // 카드 정보
    private Long cardId;
    
    private String cardCompany;
    
    private String cardNumberMasked;
    
    private String cardAlias;
    
    // 매칭 정보
    private String matchStatus;
    
    private List<MatchInfo> matches;
    
    // 검증 정보
    private Boolean isVerified;
    
    private LocalDateTime verificationDate;
    
    private String verificationMethod;
    
    // 문서 정보
    private String receiptImageUrl;
    
    private String receiptPdfUrl;
    
    private Boolean hasDocument;
    
    // 추가 정보
    private List<ReceiptItemDto> items;
    
    private Map<String, Object> additionalData;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // 중첩 DTO 클래스들
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchInfo {
        private Long matchId;
        private String erpLedgerId;
        private String accountCode;
        private String accountName;
        private String costCenter;
        private BigDecimal matchedAmount;
        private String matchStatus;
        private String matchType;
        private String matchedByName;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime matchedAt;
        
        private String approvalStatus;
        private String notes;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReceiptItemDto {
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private BigDecimal taxAmount;
        private String itemCategory;
    }
    
    // 편의 메소드
    public BigDecimal getNetAmount() {
        if (totalAmount != null && vatAmount != null) {
            return totalAmount.subtract(vatAmount);
        }
        return amount != null ? amount : BigDecimal.ZERO;
    }
    
    public boolean isFullyMatched() {
        if (matches == null || matches.isEmpty()) {
            return false;
        }
        
        BigDecimal totalMatched = matches.stream()
            .filter(m -> "MATCHED".equals(m.getMatchStatus()))
            .map(MatchInfo::getMatchedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalMatched.compareTo(totalAmount != null ? totalAmount : amount) >= 0;
    }
    
    public String getDisplayStatus() {
        if (!isVerified) {
            return "검증대기";
        } else if (matches == null || matches.isEmpty()) {
            return "미매칭";
        } else if (isFullyMatched()) {
            return "매칭완료";
        } else {
            return "부분매칭";
        }
    }
}
    
