package com.company.receipt.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "card_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Long cardId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "card_company", nullable = false, length = 50)
    private String cardCompany;
    
    @Column(name = "card_number_masked", nullable = false, length = 20)
    private String cardNumberMasked;
    
    @Column(name = "card_alias", length = 50)
    private String cardAlias;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType cardType = CardType.CORPORATE;
    
    @Column(name = "auth_type", length = 50)
    private String authType;
    
    @Column(name = "auth_credentials", columnDefinition = "TEXT")
    private String authCredentials; // 암호화된 인증 정보
    
    @Column(name = "last_sync_date")
    private LocalDateTime lastSyncDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status")
    private SyncStatus syncStatus;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum CardType {
        CORPORATE, PERSONAL
    }
    
    public enum SyncStatus {
        SUCCESS, FAILED, IN_PROGRESS
    }
}