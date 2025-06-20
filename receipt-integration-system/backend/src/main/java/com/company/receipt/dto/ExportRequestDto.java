package com.company.receipt.dto;

import java.util.List;

import jakarta.validation.constraints.*;
import lombok.*;

// ExportRequestDto.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportRequestDto {
    
    @NotNull(message = "내보내기 형식은 필수입니다")
    @Pattern(regexp = "^(EXCEL|PDF|CSV)$", message = "지원하지 않는 형식입니다")
    private String format;
    
    private List<Long> receiptIds; // 특정 영수증만 내보내기
    
    // 검색 조건 (receiptIds가 없을 경우 사용)
    private ReceiptSearchDto searchCriteria;
    
    @Builder.Default
    private Boolean includeImages = false; // 영수증 이미지 포함 (PDF만)
    
    @Builder.Default
    private Boolean includeDetails = true; // 상세 정보 포함
    
    @Builder.Default
    private Boolean includeMatches = true; // 매칭 정보 포함
    
    // 그룹화 옵션
    private GroupingOptions grouping;
    
    // 정렬 옵션
    @Builder.Default
    private List<SortOption> sortOptions = List.of(
        new SortOption("transactionDate", "DESC")
    );
    
    // 컬럼 선택 (Excel/CSV용)
    private List<String> selectedColumns;
    
    // 템플릿 (PDF용)
    private String templateName;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GroupingOptions {
        private String groupBy; // DATE, CARD, ACCOUNT_CODE, MERCHANT_CATEGORY
        private Boolean includeSubtotals;
        private Boolean includeGrandTotal;
        private String dateGrouping; // DAILY, WEEKLY, MONTHLY (DATE 그룹핑시)
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortOption {
        private String field;
        private String direction; // ASC, DESC
    }
    
    // 유효성 검증
    public void validate() {
        if (receiptIds == null || receiptIds.isEmpty()) {
            if (searchCriteria == null) {
                throw new IllegalArgumentException("내보낼 영수증을 선택하거나 검색 조건을 입력해주세요");
            }
        }
        
        if ("PDF".equals(format) && includeImages && receiptIds != null && receiptIds.size() > 100) {
            throw new IllegalArgumentException("이미지를 포함한 PDF는 최대 100개까지만 가능합니다");
        }
    }
}

