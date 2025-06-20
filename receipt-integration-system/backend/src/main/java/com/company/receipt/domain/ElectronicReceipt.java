package com.company.receipt.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "electronic_receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"transactionRecord", "accountingMatches"})
public class ElectronicReceipt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id")
    private Long receiptId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private TransactionRecord transactionRecord;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_type", nullable = false)
    private ReceiptType receiptType;
    
    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;
    
    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;
    
    @Column(name = "receipt_image_url", length = 500)
    private String receiptImageUrl;
    
    @Column(name = "receipt_pdf_url", length = 500)
    private String receiptPdfUrl;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "receipt_data", columnDefinition = "JSON")
    private Map<String, Object> receiptData;
    
    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;
    
    @Column(name = "is_verified")
    private Boolean isVerified = false;
    
    @Column(name = "verification_date")
    private LocalDateTime verificationDate;
    
    @Column(name = "verification_method", length = 50)
    private String verificationMethod;
    
    @ElementCollection
    @CollectionTable(
        name = "receipt_items",
        joinColumns = @JoinColumn(name = "receipt_id")
    )
    private List<ReceiptItem> items = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 연관관계
    @OneToMany(mappedBy = "electronicReceipt", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<AccountingMatch> accountingMatches = new ArrayList<>();
    
    // 임베디드 타입
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReceiptItem {
        
        @Column(name = "item_name", length = 200)
        private String itemName;
        
        @Column(name = "quantity")
        private Integer quantity;
        
        @Column(name = "unit_price")
        private Double unitPrice;
        
        @Column(name = "amount")
        private Double amount;
        
        @Column(name = "tax_amount")
        private Double taxAmount;
        
        @Column(name = "item_category", length = 100)
        private String itemCategory;
    }
    
    public enum ReceiptType {
        CARD_SLIP("카드전표"),
        TAX_INVOICE("세금계산서"),
        CASH_RECEIPT("현금영수증"),
        SIMPLE_RECEIPT("간이영수증");
        
        private final String description;
        
        ReceiptType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 비즈니스 메소드
    public void verify(String method) {
        this.isVerified = true;
        this.verificationDate = LocalDateTime.now();
        this.verificationMethod = method;
    }
    
    public boolean isMatched() {
        return !accountingMatches.isEmpty() && 
               accountingMatches.stream().anyMatch(match -> 
                   match.getMatchStatus() != AccountingMatch.MatchStatus.CANCELLED);
    }
    
    public AccountingMatch getLatestMatch() {
        return accountingMatches.stream()
            .filter(match -> match.getMatchStatus() != AccountingMatch.MatchStatus.CANCELLED)
            .max(Comparator.comparing(AccountingMatch::getCreatedAt))
            .orElse(null);
    }
    
    public Double getTotalAmount() {
        if (transactionRecord != null) {
            return transactionRecord.getAmount().doubleValue();
        }
        return items.stream()
            .mapToDouble(item -> item.amount != null ? item.amount : 0.0)
            .sum();
    }
    
    public Double getTotalTaxAmount() {
        if (transactionRecord != null && transactionRecord.getVatAmount() != null) {
            return transactionRecord.getVatAmount().doubleValue();
        }
        return items.stream()
            .mapToDouble(item -> item.taxAmount != null ? item.taxAmount : 0.0)
            .sum();
    }
    
    public void addItem(ReceiptItem item) {
        this.items.add(item);
    }
    
    public void removeItem(ReceiptItem item) {
        this.items.remove(item);
    }
    
    public Map<String, Object> toSummaryMap() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("receiptId", receiptId);
        summary.put("receiptNumber", receiptNumber);
        summary.put("receiptType", receiptType.getDescription());
        summary.put("issueDate", issueDate);
        summary.put("totalAmount", getTotalAmount());
        summary.put("merchantName", transactionRecord != null ? 
            transactionRecord.getMerchantName() : null);
        summary.put("isVerified", isVerified);
        summary.put("isMatched", isMatched());
        return summary;
    }
}