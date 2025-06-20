package com.company.receipt.service;

import com.company.receipt.domain.AccountingMatch;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErpIntegrationService {
    
    private final RestTemplate restTemplate;
    
    @Value("${erp.api.base-url}")
    private String erpBaseUrl;
    
    @Value("${erp.api.api-key}")
    private String erpApiKey;
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    /**
     * ERP 전표 정보 조회
     */
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ErpLedgerInfo getLedgerInfo(String ledgerId) {
        log.info("Fetching ERP ledger info: {}", ledgerId);
        
        try {
            String url = erpBaseUrl + "/api/ledger/" + ledgerId;
            
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ErpLedgerResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ErpLedgerResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return convertToLedgerInfo(response.getBody());
            }
            
        } catch (Exception e) {
            log.error("Failed to fetch ERP ledger info: {}", ledgerId, e);
        }
        
        return null;
    }
    
    /**
     * 미결 전표 목록 조회
     */
    public List<ErpLedgerInfo> getOpenLedgers(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching open ledgers from {} to {}", startDate, endDate);
        
        try {
            String url = erpBaseUrl + "/api/ledgers/open";
            
            Map<String, Object> params = new HashMap<>();
            params.put("startDate", startDate.format(DATE_FORMAT));
            params.put("endDate", endDate.format(DATE_FORMAT));
            params.put("status", "OPEN");
            params.put("pageSize", 1000);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);
            
            ResponseEntity<ErpLedgerListResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                ErpLedgerListResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getLedgers().stream()
                    .map(this::convertToLedgerInfo)
                    .collect(Collectors.toList());
            }
            
        } catch (Exception e) {
            log.error("Failed to fetch open ledgers", e);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 매칭 정보 ERP 전송
     */
    public void sendMatchingInfo(AccountingMatch match) {
        log.info("Sending matching info to ERP: {}", match.getMatchId());
        
        try {
            String url = erpBaseUrl + "/api/matching/create";
            
            ErpMatchingRequest request = ErpMatchingRequest.builder()
                .ledgerId(match.getErpLedgerId())
                .receiptId(match.getElectronicReceipt().getReceiptId().toString())
                .receiptNumber(match.getElectronicReceipt().getReceiptNumber())
                .matchedAmount(match.getMatchedAmount())
                .matchedDate(match.getMatchedAt())
                .matchedBy(match.getMatchedBy().getErpUserId())
                .matchType(match.getMatchType().name())
                .notes(match.getNotes())
                .build();
            
            HttpHeaders headers = createHeaders();
            HttpEntity<ErpMatchingRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<ErpApiResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                ErpApiResponse.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to send matching info to ERP: {}", response.getBody());
            }
            
        } catch (Exception e) {
            log.error("Error sending matching info to ERP", e);
            // 실패해도 로컬 처리는 계속 진행
        }
    }
    
    /**
     * 승인 정보 ERP 전송
     */
    public void sendApprovalInfo(AccountingMatch match) {
        log.info("Sending approval info to ERP: {}", match.getMatchId());
        
        Map<String, Object> approvalData = new HashMap<>();
        approvalData.put("matchId", match.getMatchId());
        approvalData.put("ledgerId", match.getErpLedgerId());
        approvalData.put("approvedBy", match.getApprovedBy());
        approvalData.put("approvedAt", match.getApprovedAt());
        approvalData.put("status", "APPROVED");
        
        sendStatusUpdate("/api/matching/approve", approvalData);
    }
    
    /**
     * 반려 정보 ERP 전송
     */
    public void sendRejectionInfo(AccountingMatch match) {
        log.info("Sending rejection info to ERP: {}", match.getMatchId());
        
        Map<String, Object> rejectionData = new HashMap<>();
        rejectionData.put("matchId", match.getMatchId());
        rejectionData.put("ledgerId", match.getErpLedgerId());
        rejectionData.put("rejectedBy", match.getApprovedBy());
        rejectionData.put("rejectedAt", match.getApprovedAt());
        rejectionData.put("reason", match.getRejectionReason());
        rejectionData.put("status", "REJECTED");
        
        sendStatusUpdate("/api/matching/reject", rejectionData);
    }
    
    /**
     * 취소 정보 ERP 전송
     */
    public void sendCancellationInfo(AccountingMatch match) {
        log.info("Sending cancellation info to ERP: {}", match.getMatchId());
        
        Map<String, Object> cancellationData = new HashMap<>();
        cancellationData.put("matchId", match.getMatchId());
        cancellationData.put("ledgerId", match.getErpLedgerId());
        cancellationData.put("cancelledAt", LocalDateTime.now());
        cancellationData.put("notes", match.getNotes());
        cancellationData.put("status", "CANCELLED");
        
        sendStatusUpdate("/api/matching/cancel", cancellationData);
    }
    
    private void sendStatusUpdate(String endpoint, Map<String, Object> data) {
        try {
            String url = erpBaseUrl + endpoint;
            
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(data, headers);
            
            restTemplate.exchange(url, HttpMethod.POST, entity, ErpApiResponse.class);
            
        } catch (Exception e) {
            log.error("Failed to send status update to ERP", e);
        }
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", erpApiKey);
        headers.set("X-Request-ID", UUID.randomUUID().toString());
        return headers;
    }
    
    private ErpLedgerInfo convertToLedgerInfo(ErpLedgerResponse response) {
        return ErpLedgerInfo.builder()
            .ledgerId(response.getLedgerId())
            .accountCode(response.getAccountCode())
            .accountName(response.getAccountName())
            .costCenter(response.getCostCenter())
            .amount(response.getAmount())
            .accountingDate(response.getAccountingDate())
            .description(response.getDescription())
            .status(response.getStatus())
            .build();
    }
}

// DTOs for ERP communication

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ErpLedgerInfo {
    private String ledgerId;
    private String accountCode;
    private String accountName;
    private String costCenter;
    private BigDecimal amount;
    private LocalDateTime accountingDate;
    private String description;
    private String status;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ErpLedgerResponse {
    private String ledgerId;
    private String accountCode;
    private String accountName;
    private String costCenter;
    private BigDecimal amount;
    private LocalDateTime accountingDate;
    private String description;
    private String status;
    private Map<String, Object> additionalInfo;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ErpLedgerListResponse {
    private List<ErpLedgerResponse> ledgers;
    private Integer totalCount;
    private Integer pageNo;
    private Integer pageSize;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ErpMatchingRequest {
    private String ledgerId;
    private String receiptId;
    private String receiptNumber;
    private BigDecimal matchedAmount;
    private LocalDateTime matchedDate;
    private String matchedBy;
    private String matchType;
    private String notes;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ErpApiResponse {
    private String status;
    private String message;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
}
