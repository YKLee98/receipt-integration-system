package com.company.receipt.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.company.receipt.domain.CardInfo;
import com.company.receipt.service.CardService;
import com.company.receipt.service.ReceiptService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReceiptSyncScheduler {
    
    private final CardService cardService;
    private final ReceiptService receiptService;
    
    @Scheduled(cron = "0 0 2 * * *") // 매일 새벽 2시
    @SchedulerLock(name = "receiptDailySync", lockAtMostFor = "1h", lockAtLeastFor = "5m")
    public void syncDailyReceipts() {
        log.info("Starting daily receipt sync at {}", LocalDateTime.now());
        
        List<CardInfo> activeCards = cardService.getActiveCards();
        log.info("Found {} active cards for sync", activeCards.size());
        
        activeCards.parallelStream().forEach(card -> {
            try {
                receiptService.syncReceiptsForCard(card);
            } catch (Exception e) {
                log.error("Failed to sync receipts for card: {}", card.getCardId(), e);
            }
        });
        
        log.info("Daily receipt sync completed at {}", LocalDateTime.now());
    }
    
    @Scheduled(cron = "0 */30 * * * *") // 30분마다
    @SchedulerLock(name = "receiptRealtimeSync", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void syncRealtimeReceipts() {
        // 실시간 동기화가 필요한 카드만 처리
        List<CardInfo> realtimeSyncCards = cardService.getRealtimeSyncCards();
        
        if (!realtimeSyncCards.isEmpty()) {
            log.info("Starting realtime sync for {} cards", realtimeSyncCards.size());
            
            realtimeSyncCards.forEach(card -> {
                try {
                    receiptService.syncReceiptsForCard(card);
                } catch (Exception e) {
                    log.error("Failed realtime sync for card: {}", card.getCardId(), e);
                }
            });
        }
    }
}