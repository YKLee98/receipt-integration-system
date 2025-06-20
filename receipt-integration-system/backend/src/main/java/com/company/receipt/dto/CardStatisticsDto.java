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
public class CardStatisticsDto {
    
    private Integer totalCards;
    
    private Integer activeCards;
    
    private Integer inactiveCards;
    
    private Map<String, Long> cardsByCompany;
    
    private Map<String, Long> cardsByType;
    
    private Map<Long, LocalDateTime> lastSyncDates;
    
    private List<CardSyncStatus> syncStatuses;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CardSyncStatus {
        private Long cardId;
        private String cardAlias;
        private String status;
        private LocalDateTime lastSync;
        private Integer pendingTransactions;
    }
}
