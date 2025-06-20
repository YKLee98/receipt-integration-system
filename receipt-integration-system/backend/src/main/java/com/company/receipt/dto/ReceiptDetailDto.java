package com.company.receipt.dto;

import java.util.List;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptDetailDto {
    
    // 기본 정보
    private Long receiptId;
    private String receiptNumber;
    private String receiptType;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issueDate;
    
    // 거래 정보
    private TransactionInfo transaction;
    
    // 카드 정보
    private CardInfo cardInfo;
    
    // 가맹점 정보
    private MerchantInfo merchant;
    
    // 금액 정보
    private AmountInfo amounts;
    
    // 영수증 항목 (상세)
    private List<ReceiptItemDetail> items;
    
    // 검증 정보
    private VerificationInfo verification;
    
    // 매칭 정보
    private List<MatchingInfo> matchings;
    
    // 문서 정보
    private DocumentInfo documents;
    
    // 추가 데이터
    private Map<String, Object> additionalData;
    
    // 감사 정보
    private AuditInfo audit;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransactionInfo {
        private Long transactionId;
        private LocalDateTime transactionDateTime;
        private String approvalNumber;
        private String paymentType;
        private Integer installmentMonths;
        private String transactionStatus;
        private Map<String, Object> rawData;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CardInfo {
        private Long cardId;
        private String cardCompany;
        private String cardNumberMasked;
        private String cardAlias;
        private String cardType;
        private String ownerName;
        private String ownerDepartment;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MerchantInfo {
        private String name;
        private String bizNumber;
        private String category;
        private String categoryCode;
        private String address;
        private String phoneNumber;
        private String representative;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AmountInfo {
        private BigDecimal supplyAmount;
        private BigDecimal vatAmount;
        private BigDecimal serviceFee;
        private BigDecimal totalAmount;
        private String currency;
        private BigDecimal exchangeRate; // 외화인 경우
        private BigDecimal krwAmount; // 원화 환산액
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReceiptItemDetail {
        private Integer seq;
        private String itemName;
        private String itemCode;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal supplyAmount;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private String category;
        private String remarks;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VerificationInfo {
        private Boolean isVerified;
        private LocalDateTime verificationDate;
        private String verificationMethod;
        private String verifiedBy;
        private Double ocrConfidence;
        private List<String> verificationNotes;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchingInfo {
        private Long matchId;
        private String erpLedgerId;
        private String accountCode;
        private String accountName;
        private String costCenter;
        private String projectCode;
        private BigDecimal matchedAmount;
        private String matchStatus;
        private String matchType;
        private String approvalStatus;
        private LocalDateTime matchedAt;
        private String matchedBy;
        private String notes;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentInfo {
        private String imageUrl;
        private String pdfUrl;
        private String imageThumbUrl;
        private Long fileSize;
        private String mimeType;
        private LocalDateTime uploadedAt;
        private Boolean hasOcrText;
        private String ocrText;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuditInfo {
        private LocalDateTime createdAt;
        private String createdBy;
        private LocalDateTime updatedAt;
        private String updatedBy;
        private Integer version;
        private List<ChangeLog> changeLogs;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChangeLog {
        private LocalDateTime changeDate;
        private String changedBy;
        private String changeType;
        private String fieldName;
        private String oldValue;
        private String newValue;
    }
    
    // 편의 메소드
    public BigDecimal getTotalMatchedAmount() {
        if (matchings == null || matchings.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return matchings.stream()
            .filter(m -> "MATCHED".equals(m.getMatchStatus()))
            .map(MatchingInfo::getMatchedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public BigDecimal getRemainingAmount() {
        BigDecimal total = amounts != null ? amounts.getTotalAmount() : BigDecimal.ZERO;
        return total.subtract(getTotalMatchedAmount());
    }
    
    public boolean isFullyMatched() {
        return getRemainingAmount().compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean hasDocument() {
        return documents != null && 
               (documents.getImageUrl() != null || documents.getPdfUrl() != null);
    }
}
