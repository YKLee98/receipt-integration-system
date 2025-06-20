package com.company.receipt.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class CardNumberUtil {
    
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^[0-9]{16}$");
    private static final Pattern MASKED_CARD_PATTERN = Pattern.compile("^[0-9]{4}-\\*{4}-\\*{4}-[0-9]{4}$");
    
    /**
     * 카드번호 마스킹 처리
     * 1234567890123456 -> 1234-****-****-3456
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || !isValidCardNumber(cardNumber)) {
            throw new IllegalArgumentException("Invalid card number");
        }
        
        return String.format("%s-****-****-%s",
            cardNumber.substring(0, 4),
            cardNumber.substring(12)
        );
    }
    
    /**
     * 카드번호 일부 마스킹 (뒤 4자리만 표시)
     * 1234567890123456 -> ************3456
     */
    public static String maskCardNumberPartial(String cardNumber) {
        if (cardNumber == null || !isValidCardNumber(cardNumber)) {
            throw new IllegalArgumentException("Invalid card number");
        }
        
        return "*".repeat(12) + cardNumber.substring(12);
    }
    
    /**
     * 카드번호 유효성 검증 (Luhn 알고리즘)
     */
    public static boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || !CARD_NUMBER_PATTERN.matcher(cardNumber).matches()) {
            return false;
        }
        
        return checkLuhn(cardNumber);
    }
    
    /**
     * 마스킹된 카드번호 형식 검증
     */
    public static boolean isValidMaskedCardNumber(String maskedCardNumber) {
        return maskedCardNumber != null && 
               MASKED_CARD_PATTERN.matcher(maskedCardNumber).matches();
    }
    
    /**
     * 카드사 식별 (BIN 기반)
     */
    public static String identifyCardCompany(String cardNumber) {
        if (!isValidCardNumber(cardNumber)) {
            return "UNKNOWN";
        }
        
        String bin = cardNumber.substring(0, 6);
        
        // 실제 BIN 데이터베이스를 사용해야 하지만, 간단한 예시
        if (bin.startsWith("4")) {
            return "VISA";
        } else if (bin.startsWith("5")) {
            return "MASTERCARD";
        } else if (bin.startsWith("37")) {
            return "AMEX";
        } else if (bin.startsWith("62")) {
            return "UNIONPAY";
        }
        
        // 국내 카드사 BIN 예시
        if (bin.startsWith("404117") || bin.startsWith("438676")) {
            return "SHINHAN";
        } else if (bin.startsWith("457973") || bin.startsWith("450823")) {
            return "KB";
        } else if (bin.startsWith("365154") || bin.startsWith("520121")) {
            return "SAMSUNG";
        } else if (bin.startsWith("433026") || bin.startsWith("433027")) {
            return "HYUNDAI";
        }
        
        return "UNKNOWN";
    }
    
    /**
     * 카드번호에서 마지막 4자리 추출
     */
    public static String getLast4Digits(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "";
        }
        
        // 마스킹된 카드번호 처리
        if (isValidMaskedCardNumber(cardNumber)) {
            return cardNumber.substring(cardNumber.length() - 4);
        }
        
        // 일반 카드번호 처리
        if (isValidCardNumber(cardNumber)) {
            return cardNumber.substring(cardNumber.length() - 4);
        }
        
        return "";
    }
    
    /**
     * 두 카드번호가 같은지 비교 (마스킹된 번호도 처리)
     */
    public static boolean isSameCard(String cardNumber1, String cardNumber2) {
        if (cardNumber1 == null || cardNumber2 == null) {
            return false;
        }
        
        // 둘 다 마스킹된 경우
        if (isValidMaskedCardNumber(cardNumber1) && isValidMaskedCardNumber(cardNumber2)) {
            return cardNumber1.equals(cardNumber2);
        }
        
        // 하나만 마스킹된 경우
        if (isValidMaskedCardNumber(cardNumber1) && isValidCardNumber(cardNumber2)) {
            return cardNumber1.equals(maskCardNumber(cardNumber2));
        }
        
        if (isValidCardNumber(cardNumber1) && isValidMaskedCardNumber(cardNumber2)) {
            return maskCardNumber(cardNumber1).equals(cardNumber2);
        }
        
        // 둘 다 마스킹되지 않은 경우
        if (isValidCardNumber(cardNumber1) && isValidCardNumber(cardNumber2)) {
            return cardNumber1.equals(cardNumber2);
        }
        
        return false;
    }
    
    /**
     * Luhn 알고리즘 체크
     */
    private static boolean checkLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            
            sum += n;
            alternate = !alternate;
        }
        
        return (sum % 10 == 0);
    }
}
