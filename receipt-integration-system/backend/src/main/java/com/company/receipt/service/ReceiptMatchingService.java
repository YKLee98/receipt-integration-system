package com.company.receipt.service;

import com.company.receipt.domain.AccountingMatch;
import com.company.receipt.domain.ElectronicReceipt;
import com.company.receipt.domain.User;
import com.company.receipt.dto.*;
import com.company.receipt.exception.InvalidMatchException;
import com.company.receipt.exception.ReceiptNotFoundException;
import com.company.receipt.repository.AccountingMatchRepository;
import com.company.receipt.repository.ElectronicReceiptRepository;
import com.company.receipt.repository.UserRepository;
import com.company.receipt.util.MatchingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReceiptMatchingService {
    
    private final ElectronicReceiptRepository receiptRepository;
    private final AccountingMatchRepository matchRepository;
    private final UserRepository userRepository;
    private final ErpIntegrationService erpIntegrationService;
    private final MatchingEngine matchingEngine;
    
    @Transactional
    public MatchResponseDto matchReceipt(Long receiptId, MatchRequestDto matchRequest) {
        log.info("Starting manual match for receipt: {}", receiptId);
        
        // 영수증 조회
        ElectronicReceipt receipt = receiptRepository.findByIdWithDetails(receiptId)
            .orElseThrow(() -> new ReceiptNotFoundException("Receipt not found: " + receiptId));
        
        // 유효성 검증
        validateMatchRequest(receipt, matchRequest);
        
        // ERP 전표 정보 확인
        ErpLedgerInfo ledgerInfo = erpIntegrationService.getLedgerInfo(matchRequest.getErpLedgerId());
        if (ledgerInfo == null) {
            throw new InvalidMatchException("ERP 전표를 찾을 수 없습니다: " + matchRequest.getErpLedgerId());
        }
        
        // 현재 사용자 정보
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 매칭 생성
        AccountingMatch match = AccountingMatch.builder()
            .electronicReceipt(receipt)
            .erpLedgerId(matchRequest.getErpLedgerId())
            .accountCode(matchRequest.getAccountCode())
            .accountName(matchRequest.getAccountName())
            .costCenter(matchRequest.getCostCenter())
            .projectCode(matchRequest.getProjectCode())
            .matchedAmount(matchRequest.getMatchedAmount())
            .matchStatus(AccountingMatch.MatchStatus.MATCHED)
            .matchType(AccountingMatch.MatchType.valueOf(
                matchRequest.getMatchType() != null ? matchRequest.getMatchType() : "MANUAL"
            ))
            .matchedBy(currentUser)
            .matchedAt(LocalDateTime.now())
            .notes(matchRequest.getNotes())
            .build();
        
        AccountingMatch savedMatch = matchRepository.save(match);
        
        // ERP에 매칭 정보 전송
        erpIntegrationService.sendMatchingInfo(savedMatch);
        
        log.info("Match created successfully: {}", savedMatch.getMatchId());
        
        return convertToMatchResponseDto(savedMatch);
    }
    
    @Transactional
    public AutoMatchResponseDto autoMatch(AutoMatchRequestDto request) {
        log.info("Starting auto-match process");
        
        LocalDateTime startTime = LocalDateTime.now();
        AutoMatchResponseDto.AutoMatchResponseDtoBuilder responseBuilder = AutoMatchResponseDto.builder()
            .batchId(UUID.randomUUID().toString())
            .executionTime(startTime);
        
        try {
            // 매칭 대상 영수증 조회
            List<ElectronicReceipt> receipts = findReceiptsForAutoMatch(request);
            log.info("Found {} receipts for auto-matching", receipts.size());
            
            // ERP 미결 전표 조회
            List<ErpLedgerInfo> openLedgers = erpIntegrationService.getOpenLedgers(
                request.getStartDate(), 
                request.getEndDate()
            );
            
            // 통계 초기화
            AutoMatchResponseDto.MatchingStatistics statistics = new AutoMatchResponseDto.MatchingStatistics();
            statistics.setTotalReceipts(receipts.size());
            statistics.setEligibleReceipts(receipts.size());
            
            List<AutoMatchResponseDto.MatchResult> matchResults = new ArrayList<>();
            List<AutoMatchResponseDto.UnmatchedReceipt> unmatchedReceipts = new ArrayList<>();
            
            // 매칭 실행
            for (ElectronicReceipt receipt : receipts) {
                try {
                    MatchingEngine.MatchResult engineResult = matchingEngine.findBestMatch(
                        receipt, 
                        openLedgers, 
                        request.getMinConfidenceScore()
                    );
                    
                    if (engineResult != null && engineResult.getConfidenceScore() >= request.getMinConfidenceScore()) {
                        if (!request.getDryRun()) {
                            // 실제 매칭 생성
                            AccountingMatch match = createAutoMatch(receipt, engineResult, request);
                            
                            matchResults.add(AutoMatchResponseDto.MatchResult.builder()
                                .receiptId(receipt.getReceiptId())
                                .receiptNumber(receipt.getReceiptNumber())
                                .matchId(match.getMatchId())
                                .erpLedgerId(engineResult.getErpLedgerId())
                                .accountCode(engineResult.getAccountCode())
                                .accountName(engineResult.getAccountName())
                                .confidenceScore(engineResult.getConfidenceScore())
                                .matchingStrategy(request.getStrategy().name())
                                .matchingRule(engineResult.getMatchingRule())
                                .matchReasons(engineResult.getMatchReasons())
                                .requiresApproval(request.getRequireApproval())
                                .matchedAt(LocalDateTime.now())
                                .build());
                        }
                        
                        statistics.setSuccessfulMatches(
                            (statistics.getSuccessfulMatches() != null ? statistics.getSuccessfulMatches() : 0) + 1
                        );
                    } else {
                        // 매칭 실패
                        unmatchedReceipts.add(createUnmatchedReceiptInfo(receipt, engineResult));
                        statistics.setFailedMatches(
                            (statistics.getFailedMatches() != null ? statistics.getFailedMatches() : 0) + 1
                        );
                    }
                } catch (Exception e) {
                    log.error("Error matching receipt: {}", receipt.getReceiptId(), e);
                    responseBuilder.errors(Arrays.asList(
                        String.format("Receipt %d: %s", receipt.getReceiptId(), e.getMessage())
                    ));
                }
            }
            
            // 응답 구성
            statistics.setAverageConfidenceScore(
                matchResults.stream()
                    .mapToDouble(AutoMatchResponseDto.MatchResult::getConfidenceScore)
                    .average()
                    .orElse(0.0)
            );
            
            responseBuilder
                .status("COMPLETED")
                .statistics(statistics)
                .matchResults(matchResults)
                .unmatchedReceipts(unmatchedReceipts);
            
        } catch (Exception e) {
            log.error("Auto-match process failed", e);
            responseBuilder
                .status("FAILED")
                .errors(Arrays.asList(e.getMessage()));
        }
        
        AutoMatchResponseDto response = responseBuilder
            .processingTimeMillis(System.currentTimeMillis() - startTime.toInstant().toEpochMilli())
            .build();
        
        log.info("Auto-match completed: {} successful, {} failed", 
            response.getStatistics() != null ? response.getStatistics().getSuccessfulMatches() : 0,
            response.getStatistics() != null ? response.getStatistics().getFailedMatches() : 0
        );
        
        return response;
    }
    
    @Transactional
    public void approveMatch(Long matchId, String notes) {
        AccountingMatch match = matchRepository.findById(matchId)
            .orElseThrow(() -> new RuntimeException("Match not found"));
        
        if (!match.canApprove()) {
            throw new InvalidMatchException("매칭을 승인할 수 없는 상태입니다");
        }
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User approver = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        match.approve(approver.getUserId());
        if (notes != null) {
            match.setNotes(match.getNotes() != null ? match.getNotes() + "\n승인: " + notes : "승인: " + notes);
        }
        
        matchRepository.save(match);
        
        // ERP에 승인 정보 전송
        erpIntegrationService.sendApprovalInfo(match);
    }
    
    @Transactional
    public void rejectMatch(Long matchId, String reason) {
        AccountingMatch match = matchRepository.findById(matchId)
            .orElseThrow(() -> new RuntimeException("Match not found"));
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User rejector = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        match.reject(rejector.getUserId(), reason);
        matchRepository.save(match);
        
        // ERP에 반려 정보 전송
        erpIntegrationService.sendRejectionInfo(match);
    }
    
    @Transactional
    public void cancelMatch(Long matchId, String reason) {
        AccountingMatch match = matchRepository.findById(matchId)
            .orElseThrow(() -> new RuntimeException("Match not found"));
        
        match.cancel(reason);
        matchRepository.save(match);
        
        // ERP에 취소 정보 전송
        erpIntegrationService.sendCancellationInfo(match);
    }
    
    private void validateMatchRequest(ElectronicReceipt receipt, MatchRequestDto request) {
        // 요청 유효성 검증
        request.validate();
        
        // 이미 매칭된 영수증인지 확인
        if (receipt.isMatched() && receipt.isFullyMatched()) {
            throw new InvalidMatchException("이미 전액 매칭된 영수증입니다");
        }
        
        // 매칭 금액 검증
        BigDecimal totalAmount = receipt.getTotalAmount() != null ? 
            BigDecimal.valueOf(receipt.getTotalAmount()) : BigDecimal.ZERO;
        BigDecimal alreadyMatched = receipt.getAccountingMatches().stream()
            .filter(m -> m.getMatchStatus() == AccountingMatch.MatchStatus.MATCHED)
            .map(AccountingMatch::getMatchedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal remaining = totalAmount.subtract(alreadyMatched);
        
        if (request.getMatchedAmount().compareTo(remaining) > 0) {
            throw new InvalidMatchException(
                String.format("매칭 금액이 잔액을 초과합니다. 잔액: %s, 요청: %s", 
                    remaining, request.getMatchedAmount())
            );
        }
    }
    
    private List<ElectronicReceipt> findReceiptsForAutoMatch(AutoMatchRequestDto request) {
        if (request.getReceiptIds() != null && !request.getReceiptIds().isEmpty()) {
            return receiptRepository.findByIdsWithDetails(request.getReceiptIds());
        }
        
        return receiptRepository.findReceiptsForAutoMatch(
            request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now().minusMonths(1),
            request.getEndDate() != null ? request.getEndDate() : LocalDateTime.now()
        );
    }
    
    private AccountingMatch createAutoMatch(
            ElectronicReceipt receipt, 
            MatchingEngine.MatchResult engineResult,
            AutoMatchRequestDto request) {
        
        User systemUser = userRepository.findByUsername("SYSTEM")
            .orElseThrow(() -> new RuntimeException("System user not found"));
        
        AccountingMatch match = AccountingMatch.builder()
            .electronicReceipt(receipt)
            .erpLedgerId(engineResult.getErpLedgerId())
            .accountCode(engineResult.getAccountCode())
            .accountName(engineResult.getAccountName())
            .costCenter(engineResult.getCostCenter())
            .matchedAmount(engineResult.getMatchedAmount())
            .matchStatus(AccountingMatch.MatchStatus.MATCHED)
            .matchType(AccountingMatch.MatchType.AUTO)
            .matchedBy(systemUser)
            .matchedAt(LocalDateTime.now())
            .approvalStatus(request.getRequireApproval() ? 
                AccountingMatch.ApprovalStatus.PENDING : AccountingMatch.ApprovalStatus.APPROVED)
            .confidenceScore(engineResult.getConfidenceScore())
            .matchCriteria(String.join(", ", engineResult.getMatchReasons()))
            .notes("자동 매칭: " + engineResult.getMatchingRule())
            .build();
        
        return matchRepository.save(match);
    }
    
    private AutoMatchResponseDto.UnmatchedReceipt createUnmatchedReceiptInfo(
            ElectronicReceipt receipt, 
            MatchingEngine.MatchResult engineResult) {
        
        List<String> failureReasons = new ArrayList<>();
        if (engineResult == null) {
            failureReasons.add("매칭 가능한 전표를 찾을 수 없음");
        } else {
            failureReasons.add("신뢰도 부족: " + engineResult.getConfidenceScore());
            failureReasons.addAll(engineResult.getMismatchReasons());
        }
        
        return AutoMatchResponseDto.UnmatchedReceipt.builder()
            .receiptId(receipt.getReceiptId())
            .receiptNumber(receipt.getReceiptNumber())
            .merchantName(receipt.getTransactionRecord().getMerchantName())
            .transactionDate(receipt.getTransactionRecord().getTransactionDateTime())
            .failureReasons(failureReasons)
            .build();
    }
    
    private MatchResponseDto convertToMatchResponseDto(AccountingMatch match) {
        ElectronicReceipt receipt = match.getElectronicReceipt();
        
        return MatchResponseDto.builder()
            .matchId(match.getMatchId())
            .receiptId(receipt.getReceiptId())
            .receiptNumber(receipt.getReceiptNumber())
            .erpLedgerId(match.getErpLedgerId())
            .accountCode(match.getAccountCode())
            .accountName(match.getAccountName())
            .costCenter(match.getCostCenter())
            .projectCode(match.getProjectCode())
            .matchedAmount(match.getMatchedAmount())
            .receiptAmount(BigDecimal.valueOf(receipt.getTotalAmount()))
            .remainingAmount(match.getRemainingAmount())
            .matchStatus(match.getMatchStatus().name())
            .matchType(match.getMatchType().name())
            .approvalStatus(match.getApprovalStatus().name())
            .confidenceScore(match.getConfidenceScore())
            .matchCriteria(match.getMatchCriteria())
            .matchedAt(match.getMatchedAt())
            .matchedByName(match.getMatchedBy().getUsername())
            .matchedByEmail(match.getMatchedBy().getEmail())
            .notes(match.getNotes())
            .receiptSummary(MatchResponseDto.ReceiptSummary.builder()
                .merchantName(receipt.getTransactionRecord().getMerchantName())
                .transactionDate(receipt.getTransactionRecord().getTransactionDateTime())
                .amount(receipt.getTransactionRecord().getAmount())
                .approvalNumber(receipt.getTransactionRecord().getApprovalNumber())
                .cardAlias(receipt.getTransactionRecord().getCardInfo().getCardAlias())
                .isVerified(receipt.getIsVerified())
                .build())
            .build();
    }
}
    

