// ShinhanReceiptRequest.java
package com.company.receipt.external.shinhan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShinhanReceiptRequest {
    
    @JsonProperty("approval_no")
    private String approvalNo;
    
    @JsonProperty("transaction_date")
    private String transactionDate;
    
    @JsonProperty("format")
    @Builder.Default
    private String format = "PDF"; // PDF, IMAGE
    
    @JsonProperty("include_details")
    @Builder.Default
    private Boolean includeDetails = true;
}
