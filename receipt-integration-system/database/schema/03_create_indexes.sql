-- 복합 인덱스 추가
CREATE INDEX idx_transaction_search ON transaction_records(card_id, transaction_datetime, amount);
CREATE INDEX idx_receipt_search ON electronic_receipts(receipt_type, issue_date, is_verified);
CREATE INDEX idx_match_search ON accounting_matches(match_status, matched_at);