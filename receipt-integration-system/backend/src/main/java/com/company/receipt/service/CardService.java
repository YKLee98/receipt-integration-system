package com.company.receipt.service;

import com.company.receipt.domain.CardInfo;
import com.company.receipt.domain.User;
import com.company.receipt.dto.*;
import com.company.receipt.exception.CardAlreadyExistsException;
import com.company.receipt.exception.CardNotFoundException;
import com.company.receipt.exception.InvalidCardCredentialsException;
import com.company.receipt.external.CardApiAggregatorService;
import com.company.receipt.repository.CardInfoRepository;
import com.company.receipt.repository.UserRepository;
import com.company.receipt.util.CardNumberUtil;
import com.company.receipt.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CardService {
    
    private final CardInfoRepository cardInfoRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    private final CardApiAggregatorService cardApiAggregatorService;
    
    @Transactional
    public CardResponseDto registerCard(Long userId, CardRegistrationDto registrationDto) {
        log.info("Registering new card for user: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 중복 카드 체크
        String maskedCardNumber = CardNumberUtil.maskCardNumber(registrationDto.getCardNumber());
        if (cardInfoRepository.existsByUserUserIdAndCardNumberMasked(userId, maskedCardNumber)) {
            throw new CardAlreadyExistsException("이미 등록된 카드입니다");
        }
        
        // 카드 유효성 검증 (카드사 API 호출)
        validateCardWithProvider(registrationDto);
        
        // 카드 정보 생성
        CardInfo cardInfo = CardInfo.builder()
            .user(user)
            .cardCompany(registrationDto.getCardCompany())
            .cardNumberMasked(maskedCardNumber)
            .cardAlias(registrationDto.getCardAlias())
            .cardType(CardInfo.CardType.valueOf(registrationDto.getCardType()))
            .authType(registrationDto.getAuthType())
            .authCredentials(encryptionUtil.encrypt(registrationDto.getAuthCredentials()))
            .isActive(true)
            .build();
        
        CardInfo savedCard = cardInfoRepository.save(cardInfo);
        
        log.info("Card registered successfully: {}", savedCard.getCardId());
        
        return convertToDto(savedCard);
    }
    
    @Transactional
    @CacheEvict(value = "userCards", key = "#userId")
    public void updateCard(Long userId, Long cardId, CardUpdateDto updateDto) {
        CardInfo cardInfo = cardInfoRepository.findById(cardId)
            .orElseThrow(() -> new CardNotFoundException("Card not found"));
        
        if (!cardInfo.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to card");
        }
        
        if (updateDto.getCardAlias() != null) {
            cardInfo.setCardAlias(updateDto.getCardAlias());
        }
        
        if (updateDto.getAuthCredentials() != null) {
            // 새로운 인증 정보 검증
            validateAuthCredentials(cardInfo.getCardCompany(), updateDto.getAuthCredentials());
            cardInfo.setAuthCredentials(encryptionUtil.encrypt(updateDto.getAuthCredentials()));
            cardInfo.setSyncStatus(null); // 재인증 필요
        }
        
        cardInfoRepository.save(cardInfo);
        log.info("Card updated: {}", cardId);
    }
    
    @Transactional
    @CacheEvict(value = "userCards", key = "#userId")
    public void deactivateCard(Long userId, Long cardId) {
        CardInfo cardInfo = cardInfoRepository.findById(cardId)
            .orElseThrow(() -> new CardNotFoundException("Card not found"));
        
        if (!cardInfo.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to card");
        }
        
        cardInfo.setIsActive(false);
        cardInfoRepository.save(cardInfo);
        
        log.info("Card deactivated: {}", cardId);
    }
    
    @Cacheable(value = "userCards", key = "#userId")
    public List<CardResponseDto> getUserCards(Long userId) {
        List<CardInfo> cards = cardInfoRepository.findActiveCardsByUserId(userId);
        return cards.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    public CardDetailDto getCardDetail(Long userId, Long cardId) {
        CardInfo cardInfo = cardInfoRepository.findByIdWithUser(cardId)
            .orElseThrow(() -> new CardNotFoundException("Card not found"));
        
        if (!cardInfo.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to card");
        }
        
        return convertToDetailDto(cardInfo);
    }
    
    public List<CardInfo> getActiveCards() {
        return cardInfoRepository.findAll().stream()
            .filter(CardInfo::getIsActive)
            .collect(Collectors.toList());
    }
    
    public List<CardInfo> getRealtimeSyncCards() {
        // 실시간 동기화가 필요한 카드 조회
        // 예: 법인카드, 최근 거래가 많은 카드 등
        LocalDateTime recentThreshold = LocalDateTime.now().minusHours(1);
        return cardInfoRepository.findActiveCardsWithRecentTransactions(recentThreshold);
    }
    
    public List<CardInfo> getCardsNeedingSync() {
        LocalDateTime syncThreshold = LocalDateTime.now().minusHours(24);
        return cardInfoRepository.findCardsNeedingSync(syncThreshold);
    }
    
    @Transactional
    public void updateSyncStatus(Long cardId, CardInfo.SyncStatus status) {
        cardInfoRepository.updateSyncStatus(cardId, status, LocalDateTime.now());
    }
    
    public CardStatisticsDto getCardStatistics(Long userId) {
        List<CardInfo> userCards = cardInfoRepository.findActiveCardsByUserId(userId);
        
        return CardStatisticsDto.builder()
            .totalCards(userCards.size())
            .activeCards((int) userCards.stream().filter(CardInfo::getIsActive).count())
            .cardsByCompany(
                userCards.stream()
                    .collect(Collectors.groupingBy(
                        CardInfo::getCardCompany,
                        Collectors.counting()
                    ))
            )
            .lastSyncDates(
                userCards.stream()
                    .collect(Collectors.toMap(
                        CardInfo::getCardId,
                        card -> card.getLastSyncDate() != null ? card.getLastSyncDate() : null
                    ))
            )
            .build();
    }
    
    private void validateCardWithProvider(CardRegistrationDto registrationDto) {
        try {
            boolean isValid = cardApiAggregatorService.validateCard(
                registrationDto.getCardCompany(),
                registrationDto.getCardNumber(),
                registrationDto.getAuthCredentials()
            );
            
            if (!isValid) {
                throw new InvalidCardCredentialsException("카드 정보가 유효하지 않습니다");
            }
        } catch (Exception e) {
            log.error("Card validation failed", e);
            throw new RuntimeException("카드 검증 중 오류가 발생했습니다");
        }
    }
    
    private void validateAuthCredentials(String cardCompany, String authCredentials) {
        // 카드사별 인증 정보 검증 로직
        // 실제 구현시 카드사 API 호출
    }
    
    private CardResponseDto convertToDto(CardInfo cardInfo) {
        return CardResponseDto.builder()
            .cardId(cardInfo.getCardId())
            .cardCompany(cardInfo.getCardCompany())
            .cardNumberMasked(cardInfo.getCardNumberMasked())
            .cardAlias(cardInfo.getCardAlias())
            .cardType(cardInfo.getCardType().name())
            .isActive(cardInfo.getIsActive())
            .lastSyncDate(cardInfo.getLastSyncDate())
            .syncStatus(cardInfo.getSyncStatus() != null ? cardInfo.getSyncStatus().name() : null)
            .build();
    }
    
    private CardDetailDto convertToDetailDto(CardInfo cardInfo) {
        CardDetailDto dto = new CardDetailDto();
        dto.setCardId(cardInfo.getCardId());
        dto.setCardCompany(cardInfo.getCardCompany());
        dto.setCardNumberMasked(cardInfo.getCardNumberMasked());
        dto.setCardAlias(cardInfo.getCardAlias());
        dto.setCardType(cardInfo.getCardType().name());
        dto.setAuthType(cardInfo.getAuthType());
        dto.setIsActive(cardInfo.getIsActive());
        dto.setLastSyncDate(cardInfo.getLastSyncDate());
        dto.setSyncStatus(cardInfo.getSyncStatus() != null ? cardInfo.getSyncStatus().name() : null);
        dto.setCreatedAt(cardInfo.getCreatedAt());
        dto.setUpdatedAt(cardInfo.getUpdatedAt());
        
        // 사용자 정보
        User user = cardInfo.getUser();
        dto.setOwnerName(user.getUsername());
        dto.setOwnerEmail(user.getEmail());
        dto.setOwnerDepartment(user.getDepartment());
        
        return dto;
    }
}