package com.company.receipt.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;

// CardRegistrationDto.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardRegistrationDto {
    
    @NotNull(message = "카드사는 필수입니다")
    @Pattern(regexp = "^(SHINHAN|KB|SAMSUNG|HYUNDAI|LOTTE|WOORI|HANA|BC)$", 
             message = "지원하지 않는 카드사입니다")
    private String cardCompany;
    
    @NotNull(message = "카드번호는 필수입니다")
    @Pattern(regexp = "^[0-9]{16}$", message = "카드번호는 16자리 숫자여야 합니다")
    private String cardNumber;
    
    @Size(max = 50, message = "카드 별칭은 50자를 초과할 수 없습니다")
    private String cardAlias;
    
    @NotNull(message = "카드 유형은 필수입니다")
    @Pattern(regexp = "^(CORPORATE|PERSONAL)$", message = "카드 유형이 올바르지 않습니다")
    private String cardType;
    
    @NotNull(message = "인증 방식은 필수입니다")
    private String authType; // CERTIFICATE, PASSWORD, BIO
    
    @NotNull(message = "인증 정보는 필수입니다")
    private String authCredentials; // 암호화될 인증 정보
    
    // 추가 옵션
    private CardOptions options;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CardOptions {
        private Boolean enableAutoSync = true;
        private Integer syncIntervalHours = 24;
        private Boolean enableNotification = true;
        private String preferredCurrency = "KRW";
    }
}
