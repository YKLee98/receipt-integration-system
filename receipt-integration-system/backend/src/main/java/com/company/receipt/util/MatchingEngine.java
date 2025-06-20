package com.company.receipt.util;

import com.company.receipt.domain.ElectronicReceipt;
import com.company.receipt.domain.TransactionRecord;
import com.company.receipt.service.ErpLedgerInfo;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MatchingEngine {
    
    @Value("${matching.date.tolerance.days:3}")
    private int dateToleranceDays;
    
    @Value("${matching.amount.tolerance.percentage:0.01}")
    private double amountTolerancePercentage;
    
    @Value("${matching.min.confidence.score:70.0}")
    private double minConfidenceScore;
    
    // 가맹점명 매칭 패턴
    private static final Map<String, Pattern> MERCHANT_PATTERNS = new HashMap<>();
    
    static {
        // 일반적인 가맹점 패턴
        MERCHANT_PATTERNS.put("TAXI", Pattern.compile(".*(택시|TAXI|대리운전).*", Pattern.CASE_INSENSITIVE));
        MERCHANT_PATTERNS.put("MEAL", Pattern.compile(".*(식당|레스토랑|김밥|분식|치킨|피자|카페|커피|RESTAURANT|CAFE).*", Pattern.CASE_INSENSITIVE));
        MERCHANT_PATTERNS.put("FUEL", Pattern.compile(".*(주유소|충전소|GS칼텍스|SK에너지|현대오일뱅크|S-OIL).*", Pattern.CASE_INSENSITIVE));
        MERCHANT_PATTERNS.put("HOTEL", Pattern.compile(".*(호텔|모텔|펜션|리조트|HOTEL|RESORT).*", Pattern.CASE_INSENSITIVE));
        MERCHANT_PATTERNS.put("OFFICE", Pattern.compile(".*(문구|사무용품|오피스|복사|인쇄).*", Pattern.CASE_INSENSITIVE));
    }
    
    // 계정과목 매핑 규칙
    private static final Map<String, List<String>> ACCOUNT_MAPPING = new HashMap<>();
    
    static {
        ACCOUNT_MAPPING.put("TAXI", Arrays.asList("51110", "51111")); // 교통비
        ACCOUNT_MAPPING.put("MEAL", Arrays.asList("51210", "51211")); // 접대비, 복리후생비
        ACCOUNT_MAPPING.put("FUEL", Arrays.asList("51310", "51311")); // 차량유지비
        ACCOUNT_MAPPING.put("HOTEL", Arrays.asList("51410", "51411")); // 출장비
        ACCOUNT_MAPPING.put("OFFICE", Arrays.asList("51510", "51511")); // 사무용품비
    }
    
    /**
     * 영수증에 대한 최적 매칭 찾기
     */
    public MatchResult findBestMatch(ElectronicReceipt receipt, 
                                    List<ErpLedgerInfo> candidates,
                                    double minScore) {
        if (receipt == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        
        TransactionRecord transaction = receipt.getTransactionRecord();
        
        List<MatchResult> potentialMatches = candidates.stream()
            .map(ledger -> calculateMatchScore(transaction, ledger))
            .filter(result -> result.getConfidenceScore() >= minScore)
            .sorted(Comparator.comparingDouble(MatchResult::getConfidenceScore).reversed())
            .collect(Collectors.toList());
        
        if (potentialMatches.isEmpty()) {
            log.debug("No matches found for receipt: {} with min score: {}", 
                receipt.getReceiptId(), minScore);
            return null;
        }
        
        MatchResult bestMatch = potentialMatches.get(0);
        log.info("Best match found for receipt: {} with score: {}", 
            receipt.getReceiptId(), bestMatch.getConfidenceScore());
        
        return bestMatch;
    }
    
    /**
     * 거래내역과 ERP 전표의 매칭 점수 계산
     */
    private MatchResult calculateMatchScore(TransactionRecord transaction, ErpLedgerInfo ledger) {
        MatchResult.MatchResultBuilder resultBuilder = MatchResult.builder()
            .erpLedgerId(ledger.getLedgerId())
            .accountCode(ledger.getAccountCode())
            .accountName(ledger.getAccountName())
            .costCenter(ledger.getCostCenter())
            .matchedAmount(ledger.getAmount());
        
        double totalScore = 0.0;
        double maxScore = 0.0;
        List<String> matchReasons = new ArrayList<>();
        List<String> mismatchReasons = new ArrayList<>();
        
        // 1. 금액 매칭 (40%)
        double amountScore = calculateAmountScore(transaction.getAmount(), ledger.getAmount());
        totalScore += amountScore * 40;
        maxScore += 40;
        
        if (amountScore >= 0.95) {
            matchReasons.add("금액 일치");
        } else if (amountScore >= 0.8) {
            matchReasons.add("금액 유사");
        } else {
            mismatchReasons.add("금액 불일치");
        }
        
        // 2. 날짜 매칭 (30%)
        double dateScore = calculateDateScore(
            transaction.getTransactionDateTime(), 
            ledger.getAccountingDate()
        );
        totalScore += dateScore * 30;
        maxScore += 30;
        
        if (dateScore >= 0.9) {
            matchReasons.add("날짜 일치");
        } else if (dateScore >= 0.7) {
            matchReasons.add("날짜 근접");
        } else {
            mismatchReasons.add("날짜 차이 큼");
        }
        
        // 3. 가맹점/계정과목 매칭 (20%)
        double merchantScore = calculateMerchantAccountScore(
            transaction.getMerchantName(),
            transaction.getMerchantCategory(),
            ledger.getAccountCode()
        );
        totalScore += merchantScore * 20;
        maxScore += 20;
        
        if (merchantScore >= 0.8) {
            matchReasons.add("가맹점-계정과목 매칭");
        } else if (merchantScore >= 0.5) {
            matchReasons.add("가맹점-계정과목 부분 매칭");
        }
        
        // 4. 설명/비고 매칭 (10%)
        double descriptionScore = calculateDescriptionScore(
            transaction.getMerchantName(),
            ledger.getDescription()
        );
        totalScore += descriptionScore * 10;
        maxScore += 10;
        
        if (descriptionScore >= 0.7) {
            matchReasons.add("설명 일치");
        }
        
        double confidenceScore = (totalScore / maxScore) * 100;
        
        // 매칭 규칙 결정
        String matchingRule = determineMatchingRule(amountScore, dateScore, merchantScore);
        
        return resultBuilder
            .confidenceScore(confidenceScore)
            .matchingRule(matchingRule)
            .matchReasons(matchReasons)
            .mismatchReasons(mismatchReasons)
            .build();
    }
    
    /**
     * 금액 매칭 점수 계산
     */
    private double calculateAmountScore(BigDecimal transactionAmount, BigDecimal ledgerAmount) {
        if (transactionAmount == null || ledgerAmount == null) {
            return 0.0;
        }
        
        if (transactionAmount.compareTo(ledgerAmount) == 0) {
            return 1.0; // 정확히 일치
        }
        
        BigDecimal difference = transactionAmount.subtract(ledgerAmount).abs();
        BigDecimal toleranceAmount = transactionAmount.multiply(
            BigDecimal.valueOf(amountTolerancePercentage)
        );
        
        if (difference.compareTo(toleranceAmount) <= 0) {
            // 허용 오차 범위 내
            double diffPercentage = difference.doubleValue() / transactionAmount.doubleValue();
            return 1.0 - (diffPercentage / amountTolerancePercentage);
        }
        
        // 허용 오차 초과
        double diffRatio = difference.doubleValue() / transactionAmount.doubleValue();
        return Math.max(0, 1.0 - diffRatio);
    }
    
    /**
     * 날짜 매칭 점수 계산
     */
    private double calculateDateScore(LocalDateTime transactionDate, LocalDateTime ledgerDate) {
        if (transactionDate == null || ledgerDate == null) {
            return 0.0;
        }
        
        long daysDiff = Math.abs(ChronoUnit.DAYS.between(transactionDate, ledgerDate));
        
        if (daysDiff == 0) {
            return 1.0; // 같은 날
        } else if (daysDiff <= dateToleranceDays) {
            // 허용 범위 내
            return 1.0 - (daysDiff * 0.1); // 하루당 10% 감소
        } else if (daysDiff <= dateToleranceDays * 2) {
            // 허용 범위의 2배 이내
            return 0.5 - ((daysDiff - dateToleranceDays) * 0.05);
        }
        
        return 0.0;
    }
    
    /**
     * 가맹점-계정과목 매칭 점수 계산
     */
    private double calculateMerchantAccountScore(String merchantName, 
                                                String merchantCategory,
                                                String accountCode) {
        if (merchantName == null || accountCode == null) {
            return 0.0;
        }
        
        // 가맹점 유형 식별
        String merchantType = identifyMerchantType(merchantName, merchantCategory);
        if (merchantType == null) {
            return 0.3; // 기본 점수
        }
        
        // 예상 계정과목 확인
        List<String> expectedAccounts = ACCOUNT_MAPPING.get(merchantType);
        if (expectedAccounts != null && expectedAccounts.contains(accountCode)) {
            return 1.0; // 완벽한 매칭
        }
        
        // 계정과목 그룹 매칭 (앞 3자리)
        if (expectedAccounts != null) {
            String accountGroup = accountCode.substring(0, Math.min(3, accountCode.length()));
            boolean groupMatch = expectedAccounts.stream()
                .anyMatch(expected -> expected.startsWith(accountGroup));
            if (groupMatch) {
                return 0.7; // 그룹 매칭
            }
        }
        
        return 0.3; // 기본 점수
    }
    
    /**
     * 설명 매칭 점수 계산
     */
    private double calculateDescriptionScore(String merchantName, String description) {
        if (merchantName == null || description == null) {
            return 0.0;
        }
        
        String normalizedMerchant = normalizeText(merchantName);
        String normalizedDescription = normalizeText(description);
        
        // 완전 포함
        if (normalizedDescription.contains(normalizedMerchant) || 
            normalizedMerchant.contains(normalizedDescription)) {
            return 1.0;
        }
        
        // 부분 매칭 (토큰 기반)
        Set<String> merchantTokens = tokenize(normalizedMerchant);
        Set<String> descriptionTokens = tokenize(normalizedDescription);
        
        Set<String> intersection = new HashSet<>(merchantTokens);
        intersection.retainAll(descriptionTokens);
        
        if (intersection.isEmpty()) {
            return 0.0;
        }
        
        double jaccardSimilarity = (double) intersection.size() / 
            (merchantTokens.size() + descriptionTokens.size() - intersection.size());
        
        return jaccardSimilarity;
    }
    
    /**
     * 가맹점 유형 식별
     */
    private String identifyMerchantType(String merchantName, String merchantCategory) {
        if (merchantCategory != null) {
            // 카테고리 코드 기반 매핑 (MCC 코드 등)
            // 실제 구현에서는 MCC 코드 매핑 테이블 사용
        }
        
        // 가맹점명 패턴 매칭
        for (Map.Entry<String, Pattern> entry : MERCHANT_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(merchantName).matches()) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    /**
     * 매칭 규칙 결정
     */
    private String determineMatchingRule(double amountScore, double dateScore, double merchantScore) {
        if (amountScore >= 0.95 && dateScore >= 0.9) {
            return "EXACT_MATCH";
        } else if (amountScore >= 0.8 && dateScore >= 0.7 && merchantScore >= 0.7) {
            return "HIGH_CONFIDENCE";
        } else if (amountScore >= 0.7 && dateScore >= 0.5) {
            return "MEDIUM_CONFIDENCE";
        } else {
            return "LOW_CONFIDENCE";
        }
    }
    
    /**
     * 텍스트 정규화
     */
    private String normalizeText(String text) {
        return text.toLowerCase()
            .replaceAll("[^가-힣a-z0-9\\s]", "")
            .replaceAll("\\s+", " ")
            .trim();
    }
    
    /**
     * 텍스트 토큰화
     */
    private Set<String> tokenize(String text) {
        return Arrays.stream(text.split("\\s+"))
            .filter(token -> token.length() > 1)
            .collect(Collectors.toSet());
    }
    
    /**
     * 매칭 결과 클래스
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchResult {
        private String erpLedgerId;
        private String accountCode;
        private String accountName;
        private String costCenter;
        private BigDecimal matchedAmount;
        private Double confidenceScore;
        private String matchingRule;
        private List<String> matchReasons;
        private List<String> mismatchReasons;
    }
}
