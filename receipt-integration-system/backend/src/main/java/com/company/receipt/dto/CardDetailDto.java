package com.company.receipt.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardDetailDto {
    
    private Long cardId;
    
    private String cardCompany;
    
    private String cardNumberMasked;
    
    private String cardAlias;
    
    private String cardType;
    
    private String authType;
    
    private Boolean isActive;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSyncDate;
    
    private String syncStatus;
    
    // 소유자 정보
    private String ownerName;
    
    private String ownerEmail;
    
    private String ownerDepartment;
    
    // 통계 정보
    private CardUsageStatistics statistics;
    
    // 최근 동기화 이력
    private List<SyncHistory> syncHistories;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CardUsageStatistics {
        private Integer totalTransactions;
        private Integer currentMonthTransactions;
        private Double totalAmount;
        private Double currentMonthAmount;
        private Map<String, Integer> transactionsByCategory;
        private Map<String, Double> amountsByCategory;
        private List<TopMerchant> topMerchants;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopMerchant {
        private String merchantName;
        private Integer transactionCount;
        private Double totalAmount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SyncHistory {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime syncDate;
        private String status;
        private Integer transactionCount;
        private Integer receiptCount;
        private String errorMessage;
    }
}
