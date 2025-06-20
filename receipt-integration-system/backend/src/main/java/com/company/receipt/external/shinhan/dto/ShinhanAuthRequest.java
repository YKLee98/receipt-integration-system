package com.company.receipt.external.shinhan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShinhanAuthRequest {
    
    @JsonProperty("client_id")
    private String clientId;
    
    @JsonProperty("client_secret")
    private String clientSecret;
    
    @JsonProperty("grant_type")
    private String grantType;
    
    @JsonProperty("scope")
    private String scope;
    
    @JsonProperty("user_auth")
    private String userAuth; // 사용자 인증 정보 (공동인증서 등)
    
    @JsonProperty("card_no")
    private String cardNo;
    
    @JsonProperty("auth_type")
    private String authType; // CERT, PIN, BIO
}
