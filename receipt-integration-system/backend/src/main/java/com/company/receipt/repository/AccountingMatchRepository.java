package com.company.receipt.repository;

import com.company.receipt.domain.AccountingMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountingMatchRepository extends JpaRepository<AccountingMatch, Long> {
    
    @Query("SELECT m FROM AccountingMatch m " +
           "JOIN FETCH m.electronicReceipt r " +
           "JOIN FETCH r.transactionRecord t " +
           "WHERE m.matchId = :matchId")
    Optional<AccountingMatch> findByIdWithDetails(@Param("matchId") Long matchId);
    
    @Query("SELECT m FROM AccountingMatch m WHERE m.erpLedgerId = :erpLedgerId")
    List<AccountingMatch> findByErpLedgerId(@Param("erpLedgerId") String erpLedgerId);
    
    @Query("SELECT m FROM AccountingMatch m " +
           "WHERE m.electronicReceipt.receiptId = :receiptId " +
           "AND m.matchStatus != 'CANCELLED' " +
           "ORDER BY m.createdAt DESC")
    List<AccountingMatch> findActiveMatchesByReceiptId(@Param("receiptId") Long receiptId);
    
    @Query("SELECT m FROM AccountingMatch m " +
           "WHERE m.matchedBy.userId = :userId " +
           "AND m.createdAt BETWEEN :startDate AND :endDate")
    List<AccountingMatch> findByUserAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT m FROM AccountingMatch m " +
           "WHERE m.approvalStatus = :status " +
           "AND m.createdAt >= :dateThreshold " +
           "ORDER BY m.createdAt ASC")
    List<AccountingMatch> findByApprovalStatusSince(
        @Param("status") AccountingMatch.ApprovalStatus status,
        @Param("dateThreshold") LocalDateTime dateThreshold
    );
    
    @Query("SELECT m FROM AccountingMatch m " +
           "WHERE m.accountCode = :accountCode " +
           "AND m.matchStatus = 'MATCHED' " +
           "AND m.matchedAt BETWEEN :startDate AND :endDate")
    List<AccountingMatch> findMatchedByAccountCodeAndDateRange(
        @Param("accountCode") String accountCode,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT m.accountCode, m.accountName, COUNT(m), SUM(m.matchedAmount) " +
           "FROM AccountingMatch m " +
           "WHERE m.matchStatus = 'MATCHED' " +
           "AND m.matchedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY m.accountCode, m.accountName " +
           "ORDER BY SUM(m.matchedAmount) DESC")
    List<Object[]> getMatchingSummaryByAccount(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT m FROM AccountingMatch m " +
           "WHERE m.matchType = :matchType " +
           "AND m.confidenceScore < :confidenceThreshold " +
           "AND m.approvalStatus = 'PENDING'")
    List<AccountingMatch> findLowConfidenceMatches(
        @Param("matchType") AccountingMatch.MatchType matchType,
        @Param("confidenceThreshold") Double confidenceThreshold
    );
    
    @Query("SELECT COUNT(m) FROM AccountingMatch m " +
           "WHERE m.matchedBy.userId = :userId " +
           "AND m.matchStatus = 'MATCHED' " +
           "AND m.matchedAt >= :dateThreshold")
    long countUserMatchesSince(
        @Param("userId") Long userId,
        @Param("dateThreshold") LocalDateTime dateThreshold
    );
    
    @Query("SELECT m.costCenter, COUNT(m), SUM(m.matchedAmount) " +
           "FROM AccountingMatch m " +
           "WHERE m.matchStatus = 'MATCHED' " +
           "AND m.costCenter IS NOT NULL " +
           "AND m.matchedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY m.costCenter")
    List<Object[]> getMatchingSummaryByCostCenter(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT m FROM AccountingMatch m " +
           "WHERE m.approvalStatus = 'REJECTED' " +
           "AND m.approvedAt >= :dateThreshold " +
           "ORDER BY m.approvedAt DESC")
    List<AccountingMatch> findRecentRejectedMatches(@Param("dateThreshold") LocalDateTime dateThreshold);
    
    @Query("SELECT m.matchType, COUNT(m), AVG(m.confidenceScore) " +
           "FROM AccountingMatch m " +
           "WHERE m.matchStatus = 'MATCHED' " +
           "AND m.matchedAt >= :dateThreshold " +
           "GROUP BY m.matchType")
    List<Object[]> getMatchingStatsByType(@Param("dateThreshold") LocalDateTime dateThreshold);
    
    @Query("SELECT EXISTS(SELECT 1 FROM AccountingMatch m " +
           "WHERE m.electronicReceipt.receiptId = :receiptId " +
           "AND m.matchStatus IN ('MATCHED', 'PARTIAL'))")
    boolean hasActiveMatch(@Param("receiptId") Long receiptId);
}
