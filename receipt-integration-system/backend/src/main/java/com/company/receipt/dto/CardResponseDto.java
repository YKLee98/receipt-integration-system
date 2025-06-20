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
public class CardResponseDto {
    
    private Long cardId;
    
    private String cardCompany;
    
    private String cardNumberMasked;
    
    private String cardAlias;
    
    private String cardType;
    
    private Boolean isActive;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSyncDate;
    
    private String syncStatus;
    
    private Integer transactionCount;
    
    private Integer receiptCount;
    
    private Integer unmatchedCount;
}
