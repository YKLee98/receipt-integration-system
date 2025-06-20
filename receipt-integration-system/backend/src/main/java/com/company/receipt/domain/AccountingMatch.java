package com.company.receipt.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "accounting_matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"electronicReceipt", "matchedBy"})
public class AccountingMatch {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private Long matchId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private ElectronicReceipt electronicReceipt;
    
    @Column(name = "erp_ledger_id", length = 50)
    private String erpLedgerId;
    
    @Column(name = "account_code", length = 20)
    private String accountCode;
    
    @Column(name = "account_name", length = 100)
    private String accountName;
    
    @Column(name = "cost_center", length = 50)
    private String costCenter;
    
    @Column(name = "project_code", length = 50)
    private String projectCode;
    
    @Column(name = "matched_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal matchedAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false)
    private MatchStatus matchStatus = MatchStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false)
    private MatchType matchType = MatchType.MANUAL;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_by")
    private User matchedBy;
    
    @Column(name = "matched_at")
    private LocalDateTime matchedAt;
    
    @Column(name = "approval_status")
    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
    
    @Column(name = "approved_by")
    private Long approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @Column(name = "confidence_score")
    private Double confidenceScore;
    
    @Column(name = "match_criteria", length = 500)
    private String matchCriteria;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 버전 관리를 위한 필드
    @Version
    @Column(name = "version")
    private Long version;
    
    public enum MatchStatus {
        PENDING("대기"),
        MATCHED("매칭완료"),
        CANCELLED("취소"),
        PARTIAL("부분매칭");
        
        private final String description;
        
        MatchStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public enum MatchType {
        AUTO("자동"),
        MANUAL("수동"),
        RULE_BASED("규칙기반"),
        AI_SUGGESTED("AI추천");
        
        private final String description;
        
        MatchType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public enum ApprovalStatus {
        PENDING("승인대기"),
        APPROVED("승인완료"),
        REJECTED("반려"),
        REVIEW_REQUIRED("검토필요");
        
        private final String description;
        
        ApprovalStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 비즈니스 메소드
    public void approve(Long approverId) {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = LocalDateTime.now();
        this.matchStatus = MatchStatus.MATCHED;
    }
    
    public void reject(Long rejectorId, String reason) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.approvedBy = rejectorId;
        this.approvedAt = LocalDateTime.now();
        this.rejectionReason = reason;
        this.matchStatus = MatchStatus.CANCELLED;
    }
    
    public void cancel(String reason) {
        this.matchStatus = MatchStatus.CANCELLED;
        this.notes = (this.notes != null ? this.notes + "\n" : "") + 
                    "Cancelled: " + reason;
    }
    
    public boolean isEditable() {
        return matchStatus == MatchStatus.PENDING && 
               approvalStatus == ApprovalStatus.PENDING;
    }
    
    public boolean canApprove() {
        return matchStatus == MatchStatus.MATCHED && 
               approvalStatus == ApprovalStatus.PENDING;
    }
    
    public BigDecimal getRemainingAmount() {
        if (electronicReceipt == null || electronicReceipt.getTransactionRecord() == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalAmount = electronicReceipt.getTransactionRecord().getAmount();
        BigDecimal matchedTotal = electronicReceipt.getAccountingMatches().stream()
            .filter(match -> match.getMatchStatus() == MatchStatus.MATCHED && 
                           !match.getMatchId().equals(this.matchId))
            .map(AccountingMatch::getMatchedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalAmount.subtract(matchedTotal).subtract(this.matchedAmount);
    }
    
    public void updateMatchInfo(String accountCode, String accountName, 
                               String costCenter, BigDecimal amount, String notes) {
        if (!isEditable()) {
            throw new IllegalStateException("Cannot update match in current status");
        }
        
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.costCenter = costCenter;
        this.matchedAmount = amount;
        this.notes = notes;
        this.matchedAt = LocalDateTime.now();
    }
}