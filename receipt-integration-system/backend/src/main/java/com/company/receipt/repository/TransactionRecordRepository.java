package com.company.receipt.repository;

import com.company.receipt.domain.CardInfo;
import com.company.receipt.domain.TransactionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {
    
    boolean existsByApprovalNumberAndCardInfo(String approvalNumber, CardInfo cardInfo);
    
    Optional<TransactionRecord> findByApprovalNumberAndCardInfo(String approvalNumber, CardInfo cardInfo);
    
    @Query("SELECT t FROM TransactionRecord t " +
           "WHERE t.cardInfo.cardId = :cardId " +
           "AND t.transactionDateTime BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDateTime DESC")
    List<TransactionRecord> findByCardAndDateRange(
        @Param("cardId") Long cardId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT t FROM TransactionRecord t " +
           "WHERE t.cardInfo.user.userId = :userId " +
           "AND t.transactionDateTime BETWEEN :startDate AND :endDate")
    Page<TransactionRecord> findByUserAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    @Query("SELECT t FROM TransactionRecord t " +
           "WHERE LOWER(t.merchantName) LIKE LOWER(CONCAT('%', :merchantName, '%')) " +
           "AND t.cardInfo.user.userId = :userId")
    List<TransactionRecord> findByMerchantNameContainingAndUserId(
        @Param("merchantName") String merchantName,
        @Param("userId") Long userId
    );
    
    @Query("SELECT SUM(t.amount) FROM TransactionRecord t " +
           "WHERE t.cardInfo.cardId = :cardId " +
           "AND t.transactionDateTime BETWEEN :startDate AND :endDate " +
           "AND t.transactionStatus = 'APPROVED'")
    BigDecimal sumAmountByCardAndDateRange(
        @Param("cardId") Long cardId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT t.merchantCategory, COUNT(t), SUM(t.amount) " +
           "FROM TransactionRecord t " +
           "WHERE t.cardInfo.user.userId = :userId " +
           "AND t.transactionDateTime BETWEEN :startDate AND :endDate " +
           "GROUP BY t.merchantCategory " +
           "ORDER BY SUM(t.amount) DESC")
    List<Object[]> getSpendingByCategory(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT t FROM TransactionRecord t " +
           "WHERE t.amount >= :amount " +
           "AND t.cardInfo.user.userId = :userId " +
           "ORDER BY t.amount DESC")
    List<TransactionRecord> findLargeTransactions(
        @Param("userId") Long userId,
        @Param("amount") BigDecimal amount
    );
    
    @Query("SELECT DATE(t.transactionDateTime), COUNT(t), SUM(t.amount) " +
           "FROM TransactionRecord t " +
           "WHERE t.cardInfo.cardId = :cardId " +
           "AND t.transactionDateTime BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(t.transactionDateTime) " +
           "ORDER BY DATE(t.transactionDateTime)")
    List<Object[]> getDailyTransactionSummary(
        @Param("cardId") Long cardId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT t FROM TransactionRecord t " +
           "LEFT JOIN t.electronicReceipt r " +
           "WHERE t.cardInfo.cardId = :cardId " +
           "AND r IS NULL " +
           "AND t.transactionDateTime >= :dateThreshold")
    List<TransactionRecord> findTransactionsWithoutReceipts(
        @Param("cardId") Long cardId,
        @Param("dateThreshold") LocalDateTime dateThreshold
    );
    
    @Query("SELECT t.merchantName, COUNT(t), SUM(t.amount) " +
           "FROM TransactionRecord t " +
           "WHERE t.cardInfo.user.userId = :userId " +
           "AND t.transactionDateTime >= :dateThreshold " +
           "GROUP BY t.merchantName " +
           "HAVING COUNT(t) >= :minCount " +
           "ORDER BY COUNT(t) DESC")
    List<Object[]> getFrequentMerchants(
        @Param("userId") Long userId,
        @Param("dateThreshold") LocalDateTime dateThreshold,
        @Param("minCount") Long minCount
    );
    
    @Query("SELECT COUNT(DISTINCT t.merchantBizNumber) " +
           "FROM TransactionRecord t " +
           "WHERE t.cardInfo.user.userId = :userId " +
           "AND t.merchantBizNumber IS NOT NULL")
    long countDistinctMerchantsByUser(@Param("userId") Long userId);
    
    @Query(value = "SELECT * FROM transaction_records t " +
           "WHERE t.card_id = :cardId " +
           "AND NOT EXISTS (SELECT 1 FROM electronic_receipts r WHERE r.transaction_id = t.transaction_id) " +
           "AND t.transaction_datetime >= :dateThreshold " +
           "ORDER BY t.transaction_datetime DESC " +
           "LIMIT :limit", 
           nativeQuery = true)
    List<TransactionRecord> findRecentUnprocessedTransactions(
        @Param("cardId") Long cardId,
        @Param("dateThreshold") LocalDateTime dateThreshold,
        @Param("limit") int limit
    );
}