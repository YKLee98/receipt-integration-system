package com.company.receipt.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptSearchDto {
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;
    
    private Long userId;
    
    private Long cardId;
    
    private String merchantName;
    
    private String merchantCategory;
    
    @Min(0)
    private BigDecimal minAmount;
    
    @Min(0)
    private BigDecimal maxAmount;
    
    private String receiptType; // ALL, CARD_SLIP, TAX_INVOICE, CASH_RECEIPT
    
    private String matchStatus; // ALL, MATCHED, UNMATCHED, PENDING
    
    private Boolean isVerified;
    
    private String approvalNumber;
    
    private String accountCode;
    
    private String costCenter;
    
    private String sortBy = "issueDate"; // issueDate, amount, merchantName
    
    private String sortDirection = "DESC"; // ASC, DESC
    
    // 고급 검색 옵션
    private Boolean hasDocument; // 영수증 이미지/PDF 유무
    
    private String searchText; // 통합 검색어 (가맹점명, 승인번호 등)
    
    private String[] cardCompanies; // 카드사 필터 (복수 선택)
    
    @Builder.Default
    private Boolean includeDeleted = false;
    
    // 검색 조건 유효성 검증
    public boolean isValid() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return false;
        }
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            return false;
        }
        return true;
    }
    
    // 기본값 설정
    public void setDefaults() {
        if (startDate == null) {
            startDate = LocalDateTime.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        if (matchStatus == null) {
            matchStatus = "ALL";
        }
        if (receiptType == null) {
            receiptType = "ALL";
        }
    }
}