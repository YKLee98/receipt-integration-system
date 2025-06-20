// ShinhanTransactionRequest.java
package com.company.receipt.external.shinhan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShinhanTransactionRequest {
    
    @JsonProperty("card_no")
    private String cardNo;
    
    @JsonProperty("start_date")
    private String startDate; // YYYYMMDD
    
    @JsonProperty("end_date")
    private String endDate; // YYYYMMDD
    
    @JsonProperty("transaction_type")
    private String transactionType; // ALL, APPROVED, CANCELLED
    
    @JsonProperty("page_no")
    @Builder.Default
    private Integer pageNo = 1;
    
    @JsonProperty("page_size")
    @Builder.Default
    private Integer pageSize = 100;
    
    @JsonProperty("sort_order")
    @Builder.Default
    private String sortOrder = "DESC"; // DESC, ASC
    
    @JsonProperty("include_details")
    @Builder.Default
    private Boolean includeDetails = true;
}
