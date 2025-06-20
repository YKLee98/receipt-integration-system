package com.company.receipt.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchRequestDto {
    
    @NotNull(message = "ERP 전표번호는 필수입니다")
    private String erpLedgerId;
    
    @NotNull(message = "계정과목 코드는 필수입니다")
    @Pattern(regexp = "^[0-9]{4,8}$", message = "계정과목 코드 형식이 올바르지 않습니다")
    private String accountCode;
    
    @NotNull(message = "계정과목명은 필수입니다")
    private String accountName;
    
    private String costCenter;
    
    private String projectCode;
    
    @NotNull(message = "매칭 금액은 필수입니다")
    @DecimalMin(value = "0.01", message = "매칭 금액은 0보다 커야 합니다")
    private BigDecimal matchedAmount;
    
    @Size(max = 1000, message = "비고는 1000자를 초과할 수 없습니다")
    private String notes;
    
    private String matchType; // MANUAL, AUTO, RULE_BASED
    
    // 부분 매칭 정보
    private Boolean isPartialMatch;
    
    private List<PartialMatchItem> partialMatchItems;
    
    // 매칭 규칙 정보 (자동 매칭용)
    private MatchingRule matchingRule;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PartialMatchItem {
        private String description;
        private BigDecimal amount;
        private String accountCode;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchingRule {
        private String ruleName;
        private Map<String, String> conditions;
        private Double minConfidenceScore;
    }
    
    // 유효성 검증
    public void validate() {
        if (matchType == null) {
            matchType = "MANUAL";
        }
        
        if (isPartialMatch != null && isPartialMatch) {
            if (partialMatchItems == null || partialMatchItems.isEmpty()) {
                throw new IllegalArgumentException("부분 매칭 항목이 필요합니다");
            }
            
            BigDecimal totalPartialAmount = partialMatchItems.stream()
                .map(PartialMatchItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (totalPartialAmount.compareTo(matchedAmount) != 0) {
                throw new IllegalArgumentException("부분 매칭 금액의 합이 전체 매칭 금액과 일치하지 않습니다");
            }
        }
    }
}

