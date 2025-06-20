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
public class CardUpdateDto {
    
    @Size(max = 50, message = "카드 별칭은 50자를 초과할 수 없습니다")
    private String cardAlias;
    
    private String authCredentials; // 새로운 인증 정보 (선택적)
    
    private Boolean isActive;
    
    private CardRegistrationDto.CardOptions options;
}
