package com.company.receipt.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.company.receipt.dto.*;
import com.company.receipt.service.ReceiptMatchingService;
import com.company.receipt.service.ReceiptService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
@Tag(name = "Receipt Management", description = "전자영수증 관리 API")
public class ReceiptController {
    
    private final ReceiptService receiptService;
    private final ReceiptMatchingService matchingService;
    
    @GetMapping
    @Operation(summary = "영수증 목록 조회", description = "조건에 따른 영수증 목록을 조회합니다.")
    public ResponseEntity<Page<ReceiptResponseDto>> getReceipts(
            @Valid ReceiptSearchDto searchDto,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<ReceiptResponseDto> receipts = receiptService.searchReceipts(searchDto, pageable);
        return ResponseEntity.ok(receipts);
    }
    
    @GetMapping("/{receiptId}")
    @Operation(summary = "영수증 상세 조회", description = "특정 영수증의 상세 정보를 조회합니다.")
    public ResponseEntity<ReceiptDetailDto> getReceiptDetail(@PathVariable Long receiptId) {
        ReceiptDetailDto receipt = receiptService.getReceiptDetail(receiptId);
        return ResponseEntity.ok(receipt);
    }
    
    @PostMapping("/sync")
    @Operation(summary = "영수증 동기화", description = "등록된 카드의 영수증을 동기화합니다.")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SyncResponseDto> syncReceipts(@Valid @RequestBody SyncRequestDto syncRequest) {
        SyncResponseDto response = receiptService.syncReceipts(syncRequest);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{receiptId}/match")
    @Operation(summary = "영수증 매칭", description = "영수증을 회계 장부와 매칭합니다.")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MatchResponseDto> matchReceipt(
            @PathVariable Long receiptId,
            @Valid @RequestBody MatchRequestDto matchRequest) {
        
        MatchResponseDto response = matchingService.matchReceipt(receiptId, matchRequest);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/match/auto")
    @Operation(summary = "자동 매칭", description = "미매칭 영수증을 자동으로 매칭합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AutoMatchResponseDto> autoMatch(
            @Valid @RequestBody AutoMatchRequestDto autoMatchRequest) {
        
        AutoMatchResponseDto response = matchingService.autoMatch(autoMatchRequest);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{receiptId}/download")
    @Operation(summary = "영수증 다운로드", description = "영수증 원본을 다운로드합니다.")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long receiptId) {
        byte[] receiptData = receiptService.downloadReceipt(receiptId);
        
        return ResponseEntity.ok()
            .header("Content-Type", "application/pdf")
            .header("Content-Disposition", "attachment; filename=receipt_" + receiptId + ".pdf")
            .body(receiptData);
    }
    
    @PostMapping("/export")
    @Operation(summary = "영수증 내보내기", description = "선택한 영수증을 Excel/PDF로 내보냅니다.")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<byte[]> exportReceipts(@Valid @RequestBody ExportRequestDto exportRequest) {
        byte[] exportData = receiptService.exportReceipts(exportRequest);
        
        String contentType = exportRequest.getFormat().equals("EXCEL") 
            ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            : "application/pdf";
        
        return ResponseEntity.ok()
            .header("Content-Type", contentType)
            .header("Content-Disposition", "attachment; filename=receipts." + 
                (exportRequest.getFormat().equals("EXCEL") ? "xlsx" : "pdf"))
            .body(exportData);
    }
}