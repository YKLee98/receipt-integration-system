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
public class AutoMatchRequestDto {
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;
    
    private List<Long> cardIds; // 특정 카드만 처리
    
    private List<Long> receiptIds; // 특정 영수증만 처리
    
    @Min(value = 0, message = "신뢰도는 0 이상이어야 합니다")
    @Max(value = 100, message = "신뢰도는 100 이하여야 합니다")
    @Builder.Default
    private Double minConfidenceScore = 80.0;
    
    @Builder.Default
    private Integer maxMatchesPerReceipt = 1; // 영수증당 최대 매칭 수
    
    @Builder.Default
    private Boolean dryRun = false; // 실제 매칭하지 않고 시뮬레이션만
    
    @Builder.Default
    private Boolean requireApproval = true; // 자동 매칭 후 승인 필요 여부
    
    // 매칭 전략
    @Builder.Default
    private MatchingStrategy strategy = MatchingStrategy.CONSERVATIVE;
    
    // 매칭 규칙
    @Builder.Default
    private List<MatchingRuleDto> customRules = new ArrayList<>();
    
    // 제외 조건
    private ExclusionCriteria exclusions;
    
    public enum MatchingStrategy {
        CONSERVATIVE("보수적"), // 높은 정확도, 적은 매칭
        BALANCED("균형"), // 중간
        AGGRESSIVE("적극적"); // 낮은 정확도, 많은 매칭
        
        private final String description;
        
        MatchingStrategy(String description) {
            this.description = description;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchingRuleDto {
        private String ruleName;
        private String accountCodePattern; // 계정과목 코드 패턴
        private String merchantPattern; // 가맹점명 패턴
        private String categoryPattern; // 업종 패턴
        private Map<String, String> additionalConditions;
        private Integer priority; // 우선순위 (낮을수록 높은 우선순위)
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExclusionCriteria {
        private List<String> excludeAccountCodes;
        private List<String> excludeMerchants;
        private List<String> excludeCategories;
        private Boolean excludeWeekends;
        private Boolean excludeHolidays;
    }
}

