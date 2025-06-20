package com.company.receipt.repository;

import com.company.receipt.domain.ElectronicReceipt;
import com.company.receipt.dto.ReceiptSearchDto;
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
public interface ElectronicReceiptRepository extends JpaRepository<ElectronicReceipt, Long>, ElectronicReceiptRepositoryCustom {
    
    @Query("SELECT r FROM ElectronicReceipt r " +
           "JOIN FETCH r.transactionRecord t " +
           "JOIN FETCH t.cardInfo " +
           "WHERE r.receiptId = :receiptId")
    Optional<ElectronicReceipt> findByIdWithDetails(@Param("receiptId") Long receiptId);
    
    @Query("SELECT r FROM ElectronicReceipt r " +
           "JOIN r.transactionRecord t " +
           "WHERE t.approvalNumber = :approvalNumber")
    Optional<ElectronicReceipt> findByApprovalNumber(@Param("approvalNumber") String approvalNumber);
    
    @Query("SELECT r FROM ElectronicReceipt r " +
           "JOIN r.transactionRecord t " +
           "WHERE t.cardInfo.userId = :userId " +
           "AND r.issueDate BETWEEN :startDate AND :endDate")
    List<ElectronicReceipt> findByUserAndDateRange(
        @Param("userId") Long userId, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT r FROM ElectronicReceipt r " +
           "LEFT JOIN r.accountingMatches m " +
           "WHERE m IS NULL OR m.matchStatus != 'MATCHED'")
    List<ElectronicReceipt> findUnmatchedReceipts();
    
    @Query("SELECT r FROM ElectronicReceipt r " +
           "JOIN r.transactionRecord t " +
           "WHERE t.cardInfo.cardId = :cardId " +
           "AND r.isVerified = false")
    List<ElectronicReceipt> findUnverifiedReceiptsByCard(@Param("cardId") Long cardId);
    
    @Query("SELECT COUNT(r) FROM ElectronicReceipt r " +
           "JOIN r.transactionRecord t " +
           "WHERE t.cardInfo.userId = :userId " +
           "AND r.issueDate >= :date")
    long countReceiptsByUserSince(@Param("userId") Long userId, @Param("date") LocalDateTime date);
    
    @Query("SELECT SUM(t.amount) FROM ElectronicReceipt r " +
           "JOIN r.transactionRecord t " +
           "WHERE t.cardInfo.userId = :userId " +
           "AND r.issueDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserAndDateRange(
        @Param("userId") Long userId, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT r.receiptType, COUNT(r), SUM(t.amount) " +
           "FROM ElectronicReceipt r " +
           "JOIN r.transactionRecord t " +
           "WHERE r.issueDate BETWEEN :startDate AND :endDate " +
           "GROUP BY r.receiptType")
    List<Object[]> getReceiptStatsByType(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT DISTINCT t.merchantCategory " +
           "FROM ElectronicReceipt r " +
           "JOIN r.transactionRecord t " +
           "WHERE t.merchantCategory IS NOT NULL " +
           "ORDER BY t.merchantCategory")
    List<String> findAllMerchantCategories();
    
    @Query(value = "SELECT r FROM ElectronicReceipt r " +
           "JOIN FETCH r.transactionRecord t " +
           "JOIN FETCH t.cardInfo c " +
           "LEFT JOIN FETCH r.accountingMatches m " +
           "WHERE r.receiptId IN :ids")
    List<ElectronicReceipt> findByIdsWithDetails(@Param("ids") List<Long> ids);
    
    @Query("SELECT r FROM ElectronicReceipt r " +
           "JOIN r.transactionRecord t " +
           "WHERE LOWER(t.merchantName) LIKE LOWER(CONCAT('%', :merchantName, '%'))")
    Page<ElectronicReceipt> findByMerchantNameContaining(
        @Param("merchantName") String merchantName, 
        Pageable pageable
    );
    
    @Query("SELECT r FROM ElectronicReceipt r " +
           "WHERE r.createdAt >= :date " +
           "AND r.receiptImageUrl IS NULL " +
           "AND r.receiptPdfUrl IS NULL")
    List<ElectronicReceipt> findReceiptsWithoutDocuments(@Param("date") LocalDateTime date);
    
    @Query("SELECT r FROM ElectronicReceipt r " +
           "JOIN r.transactionRecord t " +
           "JOIN r.accountingMatches m " +
           "WHERE m.accountCode = :accountCode " +
           "AND m.matchStatus = 'MATCHED' " +
           "AND r.issueDate BETWEEN :startDate AND :endDate")
    List<ElectronicReceipt> findByAccountCodeAndDateRange(
        @Param("accountCode") String accountCode,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}

// Custom Repository Interface
interface ElectronicReceiptRepositoryCustom {
    Page<ElectronicReceipt> searchReceipts(ReceiptSearchDto searchDto, Pageable pageable);
    List<ElectronicReceipt> findReceiptsForAutoMatch(LocalDateTime startDate, LocalDateTime endDate);
}