package com.company.receipt.service;

import com.company.receipt.domain.CardInfo;
import com.company.receipt.domain.ElectronicReceipt;
import com.company.receipt.domain.TransactionRecord;
import com.company.receipt.dto.ReceiptSearchDto;
import com.company.receipt.dto.ReceiptResponseDto;
import com.company.receipt.exception.ReceiptNotFoundException;
import com.company.receipt.external.CardApiAggregatorService;
import com.company.receipt.repository.ElectronicReceiptRepository;
import com.company.receipt.repository.TransactionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReceiptService {
    
    private final ElectronicReceiptRepository receiptRepository;
    private final TransactionRecordRepository transactionRepository;
    private final CardApiAggregatorService cardApiAggregatorService;
    private final ReceiptMatchingService matchingService;
    
    @Transactional
    public void syncReceiptsForCard(CardInfo cardInfo) {
        log.info("Starting receipt sync for card: {}", cardInfo.getCardId());
        
        try {
            // 카드사 API에서 거래내역 조회
            LocalDateTime fromDate = cardInfo.getLastSyncDate() != null 
                ? cardInfo.getLastSyncDate() 
                : LocalDateTime.now().minusMonths(1);
            
            List<TransactionRecord> transactions = cardApiAggregatorService
                .fetchTransactions(cardInfo, fromDate, LocalDateTime.now());
            
            // 거래내역 저장 및 영수증 생성
            for (TransactionRecord transaction : transactions) {
                saveTransactionWithReceipt(transaction);
            }
            
            // 동기화 상태 업데이트
            cardInfo.setLastSyncDate(LocalDateTime.now());
            cardInfo.setSyncStatus(CardInfo.SyncStatus.SUCCESS);
            
            log.info("Receipt sync completed for card: {}", cardInfo.getCardId());
            
        } catch (Exception e) {
            log.error("Receipt sync failed for card: {}", cardInfo.getCardId(), e);
            cardInfo.setSyncStatus(CardInfo.SyncStatus.FAILED);
            throw new RuntimeException("Receipt sync failed", e);
        }
    }
    
    @Transactional
    public ElectronicReceipt saveTransactionWithReceipt(TransactionRecord transaction) {
        // 중복 체크
        if (transactionRepository.existsByApprovalNumberAndCardInfo(
                transaction.getApprovalNumber(), transaction.getCardInfo())) {
            log.debug("Transaction already exists: {}", transaction.getApprovalNumber());
            return null;
        }
        
        // 거래내역 저장
        TransactionRecord savedTransaction = transactionRepository.save(transaction);
        
        // 전자영수증 생성
        ElectronicReceipt receipt = ElectronicReceipt.builder()
            .transactionRecord(savedTransaction)
            .receiptType(ElectronicReceipt.ReceiptType.CARD_SLIP)
            .issueDate(transaction.getTransactionDateTime())
            .receiptNumber(generateReceiptNumber(transaction))
            .isVerified(false)
            .build();
        
        // 영수증 이미지/PDF 다운로드 (비동기)
        downloadReceiptDocument(receipt);
        
        return receiptRepository.save(receipt);
    }
    
    @Async("receiptProcessingExecutor")
    public CompletableFuture<Void> downloadReceiptDocument(ElectronicReceipt receipt) {
        try {
            // 카드사 API에서 영수증 이미지/PDF 다운로드
            String receiptUrl = cardApiAggregatorService
                .downloadReceiptDocument(receipt.getTransactionRecord());
            
            receipt.setReceiptImageUrl(receiptUrl);
            receipt.setIsVerified(true);
            receiptRepository.save(receipt);
            
        } catch (Exception e) {
            log.error("Failed to download receipt document: {}", receipt.getReceiptId(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    public Page<ReceiptResponseDto> searchReceipts(ReceiptSearchDto searchDto, Pageable pageable) {
        return receiptRepository.searchReceipts(searchDto, pageable)
            .map(this::convertToDto);
    }
    
    public ReceiptResponseDto getReceiptById(Long receiptId) {
        ElectronicReceipt receipt = receiptRepository.findById(receiptId)
            .orElseThrow(() -> new ReceiptNotFoundException("Receipt not found: " + receiptId));
        
        return convertToDto(receipt);
    }
    
    private ReceiptResponseDto convertToDto(ElectronicReceipt receipt) {
        return ReceiptResponseDto.builder()
            .receiptId(receipt.getReceiptId())
            .transactionDate(receipt.getTransactionRecord().getTransactionDateTime())
            .merchantName(receipt.getTransactionRecord().getMerchantName())
            .amount(receipt.getTransactionRecord().getAmount())
            .receiptType(receipt.getReceiptType().name())
            .receiptImageUrl(receipt.getReceiptImageUrl())
            .isVerified(receipt.getIsVerified())
            .matchStatus(receipt.getAccountingMatches().isEmpty() ? "UNMATCHED" : "MATCHED")
            .build();
    }
    
    private String generateReceiptNumber(TransactionRecord transaction) {
        return String.format("%s-%s-%s", 
            transaction.getCardInfo().getCardCompany(),
            transaction.getTransactionDateTime().toLocalDate().toString().replace("-", ""),
            transaction.getApprovalNumber()
        );
    }
}