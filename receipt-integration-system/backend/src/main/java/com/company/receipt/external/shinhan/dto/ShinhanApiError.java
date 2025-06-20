package com.company.receipt.external.shinhan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShinhanApiError {
    
    @JsonProperty("error_code")
    private String errorCode;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("error_detail")
    private String errorDetail;
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("request_id")
    private String requestId;
}
