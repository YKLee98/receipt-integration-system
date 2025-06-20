-- 사용자 테이블 (중앙ERP 연동용)
CREATE TABLE users (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    erp_user_id VARCHAR(50) NOT NULL COMMENT '중앙ERP 사용자 ID',
    username VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    department VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY idx_erp_user_id (erp_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 카드 정보 테이블
CREATE TABLE card_info (
    card_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    card_company VARCHAR(50) NOT NULL COMMENT '카드사명',
    card_number_masked VARCHAR(20) NOT NULL COMMENT '마스킹된 카드번호',
    card_alias VARCHAR(50) COMMENT '카드 별칭',
    card_type ENUM('CORPORATE', 'PERSONAL') NOT NULL DEFAULT 'CORPORATE' COMMENT '카드 유형',
    auth_type VARCHAR(50) COMMENT '인증 방식',
    auth_credentials TEXT COMMENT '암호화된 인증 정보',
    last_sync_date DATETIME COMMENT '마지막 동기화 일시',
    sync_status ENUM('SUCCESS', 'FAILED', 'IN_PROGRESS') DEFAULT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (card_id),
    KEY idx_user_id (user_id),
    KEY idx_card_company (card_company),
    CONSTRAINT fk_card_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 거래 내역 테이블 (원본 데이터)
CREATE TABLE transaction_records (
    transaction_id BIGINT NOT NULL AUTO_INCREMENT,
    card_id BIGINT NOT NULL,
    transaction_datetime DATETIME NOT NULL COMMENT '거래일시',
    approval_number VARCHAR(50) NOT NULL COMMENT '승인번호',
    merchant_name VARCHAR(200) NOT NULL COMMENT '가맹점명',
    merchant_biz_number VARCHAR(20) COMMENT '사업자번호',
    merchant_category VARCHAR(100) COMMENT '업종',
    amount DECIMAL(15,2) NOT NULL COMMENT '거래금액',
    vat_amount DECIMAL(15,2) DEFAULT 0 COMMENT '부가세',
    service_fee DECIMAL(15,2) DEFAULT 0 COMMENT '봉사료',
    currency VARCHAR(10) DEFAULT 'KRW',
    payment_type ENUM('CREDIT', 'CHECK') DEFAULT 'CREDIT',
    installment_months INT DEFAULT 0 COMMENT '할부개월',
    transaction_status ENUM('APPROVED', 'CANCELLED') DEFAULT 'APPROVED',
    raw_data JSON COMMENT 'API 원본 데이터',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (transaction_id),
    UNIQUE KEY idx_approval_number (approval_number, card_id),
    KEY idx_card_id (card_id),
    KEY idx_transaction_date (transaction_datetime),
    KEY idx_merchant_name (merchant_name),
    KEY idx_amount (amount),
    CONSTRAINT fk_transaction_card FOREIGN KEY (card_id) REFERENCES card_info(card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 전자영수증 테이블
CREATE TABLE electronic_receipts (
    receipt_id BIGINT NOT NULL AUTO_INCREMENT,
    transaction_id BIGINT NOT NULL,
    receipt_type ENUM('CARD_SLIP', 'TAX_INVOICE', 'CASH_RECEIPT') NOT NULL,
    receipt_number VARCHAR(100) COMMENT '영수증 번호',
    issue_date DATETIME NOT NULL COMMENT '발행일시',
    receipt_image_url VARCHAR(500) COMMENT '영수증 이미지 URL',
    receipt_pdf_url VARCHAR(500) COMMENT '영수증 PDF URL',
    receipt_data JSON COMMENT '구조화된 영수증 데이터',
    ocr_text TEXT COMMENT 'OCR 추출 텍스트',
    is_verified BOOLEAN DEFAULT FALSE COMMENT '검증 여부',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (receipt_id),
    KEY idx_transaction_id (transaction_id),
    KEY idx_receipt_type (receipt_type),
    KEY idx_issue_date (issue_date),
    CONSTRAINT fk_receipt_transaction FOREIGN KEY (transaction_id) REFERENCES transaction_records(transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 회계 연동 테이블 (중앙ERP 장부와 매칭)
CREATE TABLE accounting_matches (
    match_id BIGINT NOT NULL AUTO_INCREMENT,
    receipt_id BIGINT NOT NULL,
    erp_ledger_id VARCHAR(50) COMMENT '중앙ERP 전표번호',
    account_code VARCHAR(20) COMMENT '계정과목코드',
    account_name VARCHAR(100) COMMENT '계정과목명',
    cost_center VARCHAR(50) COMMENT '코스트센터',
    matched_amount DECIMAL(15,2) NOT NULL,
    match_status ENUM('AUTO', 'MANUAL', 'PENDING') NOT NULL,
    matched_by BIGINT COMMENT '매칭 처리자',
    matched_at DATETIME,
    notes TEXT COMMENT '비고',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (match_id),
    KEY idx_receipt_id (receipt_id),
    KEY idx_erp_ledger_id (erp_ledger_id),
    KEY idx_match_status (match_status),
    CONSTRAINT fk_match_receipt FOREIGN KEY (receipt_id) REFERENCES electronic_receipts(receipt_id),
    CONSTRAINT fk_match_user FOREIGN KEY (matched_by) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- API 호출 로그 테이블
CREATE TABLE api_call_logs (
    log_id BIGINT NOT NULL AUTO_INCREMENT,
    api_provider VARCHAR(50) NOT NULL,
    api_method VARCHAR(100) NOT NULL,
    request_time DATETIME NOT NULL,
    response_time DATETIME,
    response_status INT,
    error_message TEXT,
    request_data JSON,
    response_data JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_api_provider (api_provider),
    KEY idx_request_time (request_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 시스템 설정 테이블
CREATE TABLE system_configs (
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT NOT NULL,
    config_type VARCHAR(50) NOT NULL,
    description TEXT,
    is_encrypted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;