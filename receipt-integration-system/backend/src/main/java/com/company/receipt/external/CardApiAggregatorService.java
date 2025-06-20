package com.company.receipt.external;

import com.company.receipt.domain.CardInfo;
import com.company.receipt.domain.TransactionRecord;
import com.company.receipt.external.common.CardApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardApiAggregatorService {
    
    private final List<CardApiService> cardApiServices;
    private final ExecutorService apiCallExecutor;
    
    public List<TransactionRecord> fetchTransactions(CardInfo cardInfo, 
                                                    LocalDateTime fromDate, 
                                                    LocalDateTime toDate) {
        CardApiService apiService = getApiService(cardInfo.getCardCompany());
        
        if (apiService == null) {
            throw new RuntimeException("Unsupported card company: " + cardInfo.getCardCompany());
        }
        
        try {
            return apiService.fetchTransactions(cardInfo, fromDate, toDate);
        } catch (Exception e) {
            log.error("Failed to fetch transactions for card: {}", cardInfo.getCardId(), e);
            throw new RuntimeException("Transaction fetch failed", e);
        }
    }
    
    public CompletableFuture<List<TransactionRecord>> fetchTransactionsAsync(
            CardInfo cardInfo, 
            LocalDateTime fromDate, 
            LocalDateTime toDate) {
        
        return CompletableFuture.supplyAsync(() -> 
            fetchTransactions(cardInfo, fromDate, toDate), 
            apiCallExecutor
        );
    }
    
    public Map<Long, List<TransactionRecord>> fetchTransactionsForMultipleCards(
            List<CardInfo> cardInfos,
            LocalDateTime fromDate,
            LocalDateTime toDate) {
        
        List<CompletableFuture<CardTransactionResult>> futures = cardInfos.stream()
            .map(card -> CompletableFuture.supplyAsync(() -> {
                try {
                    List<TransactionRecord> transactions = fetchTransactions(card, fromDate, toDate);
                    return new CardTransactionResult(card.getCardId(), transactions, null);
                } catch (Exception e) {
                    log.error("Failed to fetch transactions for card: {}", card.getCardId(), e);
                    return new CardTransactionResult(card.getCardId(), List.of(), e.getMessage());
                }
            }, apiCallExecutor))
            .collect(Collectors.toList());
        
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        return allFutures.thenApply(v -> 
            futures.stream()
                .map(CompletableFuture::join)
                .filter(result -> result.error == null)
                .collect(Collectors.toMap(
                    result -> result.cardId,
                    result -> result.transactions
                ))
        ).join();
    }
    
    public String downloadReceiptDocument(TransactionRecord transaction) {
        CardApiService apiService = getApiService(transaction.getCardInfo().getCardCompany());
        
        if (apiService == null) {
            throw new RuntimeException("Unsupported card company: " + 
                transaction.getCardInfo().getCardCompany());
        }
        
        try {
            return apiService.downloadReceiptDocument(transaction);
        } catch (Exception e) {
            log.error("Failed to download receipt for transaction: {}", 
                transaction.getApprovalNumber(), e);
            return null;
        }
    }
    
    public boolean validateCard(String cardCompany, String cardNumber, String authCredentials) {
        CardApiService apiService = getApiService(cardCompany);
        
        if (apiService == null) {
            log.warn("No API service available for card company: {}", cardCompany);
            return false;
        }
        
        try {
            return apiService.validateCard(cardNumber, authCredentials);
        } catch (Exception e) {
            log.error("Card validation failed", e);
            return false;
        }
    }
    
    public List<String> getSupportedCardCompanies() {
        return cardApiServices.stream()
            .map(CardApiService::getCardCompany)
            .sorted()
            .collect(Collectors.toList());
    }
    
    private CardApiService getApiService(String cardCompany) {
        return cardApiServices.stream()
            .filter(service -> service.supports(cardCompany))
            .findFirst()
            .orElse(null);
    }
    
    private static class CardTransactionResult {
        final Long cardId;
        final List<TransactionRecord> transactions;
        final String error;
        
        CardTransactionResult(Long cardId, List<TransactionRecord> transactions, String error) {
            this.cardId = cardId;
            this.transactions = transactions;
            this.error = error;
        }
    }
}
