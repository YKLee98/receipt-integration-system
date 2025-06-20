ppackage com.company.receipt.external.shinhan;

import com.company.receipt.domain.CardInfo;
import com.company.receipt.domain.TransactionRecord;
import com.company.receipt.external.common.CardApiService;
import com.company.receipt.external.shinhan.dto.ShinhanAuthRequest;
import com.company.receipt.external.shinhan.dto.ShinhanAuthResponse;
import com.company.receipt.external.shinhan.dto.ShinhanTransactionRequest;
import com.company.receipt.external.shinhan.dto.ShinhanTransactionResponse;
import com.company.receipt.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShinhanCardApiService implements CardApiService {
    
    private final RestTemplate restTemplate;
    private final EncryptionUtil encryptionUtil;
    
    @Value("${external.api.shinhan.base-url}")
    private String baseUrl;
    
    @Value("${external.api.shinhan.client-id}")
    private String clientId;
    
    @Value("${external.api.shinhan.client-secret}")
    private String clientSecret;
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    @Override
    public boolean supports(String cardCompany) {
        return "SHINHAN".equalsIgnoreCase(cardCompany);
    }
    
    @Override
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public List<TransactionRecord> fetchTransactions(CardInfo cardInfo, LocalDateTime fromDate, LocalDateTime toDate) {
        log.info("Fetching Shinhan card transactions for card: {}", cardInfo.getCardId());
        
        try {
            // 1. 인증 토큰 획득
            String accessToken = authenticate(cardInfo);
            
            // 2. 거래내역 조회
            ShinhanTransactionRequest request = ShinhanTransactionRequest.builder()
                .cardNo(decryptCardNumber(cardInfo))
                .startDate(fromDate.format(DATE_FORMAT))
                .endDate(toDate.format(DATE_FORMAT))
                .build();
            
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<ShinhanTransactionRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<ShinhanTransactionResponse> response = restTemplate.exchange(
                baseUrl + "/api/v1/card/transaction/list",
                HttpMethod.POST,
                entity,
                ShinhanTransactionResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return convertToTransactionRecords(response.getBody(), cardInfo);
            }
            
            throw new RuntimeException("Failed to fetch transactions from Shinhan API");
            
        } catch (Exception e) {
            log.error("Error fetching Shinhan card transactions", e);
            throw new RuntimeException("Shinhan API call failed", e);
        }
    }
    
    @Override
    public String downloadReceiptDocument(TransactionRecord transaction) {
        log.info("Downloading receipt for transaction: {}", transaction.getApprovalNumber());
        
        try {
            String accessToken = authenticate(transaction.getCardInfo());
            
            HttpHeaders headers = createHeaders(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String url = String.format("%s/api/v1/card/receipt/%s", 
                baseUrl, transaction.getApprovalNumber());
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                byte[].class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 파일 저장 로직 (S3, 로컬 스토리지 등)
                String fileUrl = saveReceiptFile(response.getBody(), transaction);
                return fileUrl;
            }
            
        } catch (Exception e) {
            log.error("Error downloading receipt document", e);
        }
        
        return null;
    }
    
    private String authenticate(CardInfo cardInfo) {
        ShinhanAuthRequest authRequest = ShinhanAuthRequest.builder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .grantType("client_credentials")
            .scope("card.transaction.read card.receipt.read")
            .build();
        
        // 사용자 인증 정보 추가 (공동인증서 등)
        String decryptedAuth = encryptionUtil.decrypt(cardInfo.getAuthCredentials());
        authRequest.setUserAuth(decryptedAuth);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<ShinhanAuthRequest> entity = new HttpEntity<>(authRequest, headers);
        
        ResponseEntity<ShinhanAuthResponse> response = restTemplate.exchange(
            baseUrl + "/api/v1/auth/token",
            HttpMethod.POST,
            entity,
            ShinhanAuthResponse.class
        );
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().getAccessToken();
        }
        
        throw new RuntimeException("Authentication failed");
    }
    
    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-API-Version", "1.0");
        return headers;
    }
    
    private List<TransactionRecord> convertToTransactionRecords(
            ShinhanTransactionResponse response, CardInfo cardInfo) {
        
        return response.getTransactions().stream()
            .map(tx -> TransactionRecord.builder()
                .cardInfo(cardInfo)
                .transactionDateTime(LocalDateTime.parse(tx.getTransactionDate(), DATETIME_FORMAT))
                .approvalNumber(tx.getApprovalNo())
                .merchantName(tx.getMerchantName())
                .merchantBizNumber(tx.getMerchantBizNo())
                .merchantCategory(tx.getMerchantCategory())
                .amount(new BigDecimal(tx.getAmount()))
                .vatAmount(new BigDecimal(tx.getVatAmount()))
                .currency(tx.getCurrency())
                .paymentType(TransactionRecord.PaymentType.valueOf(tx.getPaymentType()))
                .installmentMonths(tx.getInstallmentMonths())
                .transactionStatus(TransactionRecord.TransactionStatus.valueOf(tx.getStatus()))
                .rawData(tx.toMap())
                .build())
            .collect(Collectors.toList());
    }
    
    private String decryptCardNumber(CardInfo cardInfo) {
        // 실제 구현에서는 암호화된 카드번호를 복호화
        return "1234567890123456";
    }
    
    private String saveReceiptFile(byte[] fileData, TransactionRecord transaction) {
        // 실제 구현에서는 S3 또는 로컬 스토리지에 저장
        String fileName = String.format("receipt_%s_%s.pdf", 
            transaction.getApprovalNumber(), 
            System.currentTimeMillis());
        
        // 파일 저장 로직
        
        return "/receipts/" + fileName;
    }
}
