package com.company.receipt.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cardInfos", "accountingMatches"})
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "erp_user_id", nullable = false, unique = true, length = 50)
    private String erpUserId;
    
    @Column(name = "username", nullable = false, length = 100)
    private String username;
    
    @Column(name = "email", length = 100)
    private String email;
    
    @Column(name = "department", length = 100)
    private String department;
    
    @Column(name = "position", length = 50)
    private String position;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles = new HashSet<>();
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "login_failure_count")
    private Integer loginFailureCount = 0;
    
    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 연관관계
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<CardInfo> cardInfos = new ArrayList<>();
    
    @OneToMany(mappedBy = "matchedBy", fetch = FetchType.LAZY)
    private List<AccountingMatch> accountingMatches = new ArrayList<>();
    
    // UserDetails 구현
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
            .collect(Collectors.toList());
    }
    
    @Override
    public String getPassword() {
        // ERP 연동이므로 별도 패스워드 없음
        return null;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return accountLockedUntil == null || accountLockedUntil.isBefore(LocalDateTime.now());
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(isActive);
    }
    
    // 비즈니스 메소드
    public void recordLoginSuccess() {
        this.lastLoginAt = LocalDateTime.now();
        this.loginFailureCount = 0;
        this.accountLockedUntil = null;
    }
    
    public void recordLoginFailure() {
        this.loginFailureCount++;
        if (this.loginFailureCount >= 5) {
            this.accountLockedUntil = LocalDateTime.now().plusMinutes(30);
        }
    }
    
    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }
    
    public void addRole(UserRole role) {
        this.roles.add(role);
    }
    
    public void removeRole(UserRole role) {
        this.roles.remove(role);
    }
    
    public enum UserRole {
        USER,       // 일반 사용자
        MANAGER,    // 관리자
        ADMIN,      // 시스템 관리자
        ACCOUNTANT  // 회계 담당자
    }
}