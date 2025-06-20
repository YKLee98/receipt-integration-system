package com.company.receipt.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transaction_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private CardInfo cardInfo;
    
    @Column(name = "transaction_datetime", nullable = false)
    private LocalDateTime transactionDateTime;
    
    @Column(name = "approval_number", nullable = false, length = 50)
    private String approvalNumber;
    
    @Column(name = "merchant_name", nullable = false, length = 200)
    private String merchantName;
    
    @Column(name = "merchant_biz_number", length = 20)
    private String merchantBizNumber;
    
    @Column(name = "merchant_category", length = 100)
    private String merchantCategory;
    
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "vat_amount", precision = 15, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;
    
    @Column(name = "service_fee", precision = 15, scale = 2)
    private BigDecimal serviceFee = BigDecimal.ZERO;
    
    @Column(name = "currency", length = 10)
    private String currency = "KRW";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentType paymentType = PaymentType.CREDIT;
    
    @Column(name = "installment_months")
    private Integer installmentMonths = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status")
    private TransactionStatus transactionStatus = TransactionStatus.APPROVED;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "JSON")
    private Map<String, Object> rawData;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @OneToOne(mappedBy = "transactionRecord", cascade = CascadeType.ALL)
    private ElectronicReceipt electronicReceipt;
    
    public enum PaymentType {
        CREDIT, CHECK
    }
    
    public enum TransactionStatus {
        APPROVED, CANCELLED
    }
}