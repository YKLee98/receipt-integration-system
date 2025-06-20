package com.company.receipt.repository;

import com.company.receipt.domain.ElectronicReceipt;
import com.company.receipt.dto.ReceiptSearchDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ElectronicReceiptRepositoryImpl implements ElectronicReceiptRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public Page<ElectronicReceipt> searchReceipts(ReceiptSearchDto searchDto, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ElectronicReceipt> query = cb.createQuery(ElectronicReceipt.class);
        Root<ElectronicReceipt> receipt = query.from(ElectronicReceipt.class);
        
        // Fetch joins for performance
        receipt.fetch("transactionRecord", JoinType.INNER)
              .fetch("cardInfo", JoinType.INNER);
        receipt.fetch("accountingMatches", JoinType.LEFT);
        
        List<Predicate> predicates = buildPredicates(searchDto, cb, receipt);
        
        query.where(predicates.toArray(new Predicate[0]));
        
        // Apply sorting
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                Path<?> path = getPath(receipt, order.getProperty());
                if (path != null) {
                    orders.add(order.isAscending() ? 
                        cb.asc(path) : cb.desc(path));
                }
            });
            query.orderBy(orders);
        } else {
            // Default sorting by issue date desc
            query.orderBy(cb.desc(receipt.get("issueDate")));
        }
        
        TypedQuery<ElectronicReceipt> typedQuery = entityManager.createQuery(query);
        
        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<ElectronicReceipt> countRoot = countQuery.from(ElectronicReceipt.class);
        countRoot.join("transactionRecord", JoinType.INNER);
        
        List<Predicate> countPredicates = buildPredicates(searchDto, cb, countRoot);
        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicates.toArray(new Predicate[0]));
        
        Long total = entityManager.createQuery(countQuery).getSingleResult();
        
        // Apply pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        List<ElectronicReceipt> results = typedQuery.getResultList();
        
        return new PageImpl<>(results, pageable, total);
    }
    
    @Override
    public List<ElectronicReceipt> findReceiptsForAutoMatch(LocalDateTime startDate, LocalDateTime endDate) {
        String jpql = "SELECT DISTINCT r FROM ElectronicReceipt r " +
                     "JOIN FETCH r.transactionRecord t " +
                     "JOIN FETCH t.cardInfo c " +
                     "LEFT JOIN r.accountingMatches m " +
                     "WHERE r.issueDate BETWEEN :startDate AND :endDate " +
                     "AND r.isVerified = true " +
                     "AND (m IS NULL OR m.matchStatus NOT IN ('MATCHED', 'PARTIAL')) " +
                     "ORDER BY r.issueDate DESC";
        
        return entityManager.createQuery(jpql, ElectronicReceipt.class)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .setMaxResults(1000) // Limit for performance
            .getResultList();
    }
    
    private List<Predicate> buildPredicates(ReceiptSearchDto searchDto, 
                                           CriteriaBuilder cb, 
                                           Root<ElectronicReceipt> receipt) {
        List<Predicate> predicates = new ArrayList<>();
        Join<?, ?> transaction = receipt.join("transactionRecord");
        Join<?, ?> card = transaction.join("cardInfo");
        
        // Date range filter
        if (searchDto.getStartDate() != null && searchDto.getEndDate() != null) {
            predicates.add(cb.between(receipt.get("issueDate"), 
                searchDto.getStartDate(), searchDto.getEndDate()));
        }
        
        // Card filter
        if (searchDto.getCardId() != null) {
            predicates.add(cb.equal(card.get("cardId"), searchDto.getCardId()));
        }
        
        // User filter
        if (searchDto.getUserId() != null) {
            predicates.add(cb.equal(card.get("user").get("userId"), searchDto.getUserId()));
        }
        
        // Merchant name filter
        if (StringUtils.hasText(searchDto.getMerchantName())) {
            predicates.add(cb.like(cb.lower(transaction.get("merchantName")), 
                "%" + searchDto.getMerchantName().toLowerCase() + "%"));
        }
        
        // Amount range filter
        if (searchDto.getMinAmount() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                transaction.get("amount"), searchDto.getMinAmount()));
        }
        if (searchDto.getMaxAmount() != null) {
            predicates.add(cb.lessThanOrEqualTo(
                transaction.get("amount"), searchDto.getMaxAmount()));
        }
        
        // Receipt type filter
        if (searchDto.getReceiptType() != null && !"ALL".equals(searchDto.getReceiptType())) {
            predicates.add(cb.equal(receipt.get("receiptType"), 
                ElectronicReceipt.ReceiptType.valueOf(searchDto.getReceiptType())));
        }
        
        // Match status filter
        if (StringUtils.hasText(searchDto.getMatchStatus()) && !"ALL".equals(searchDto.getMatchStatus())) {
            if ("MATCHED".equals(searchDto.getMatchStatus())) {
                Subquery<Long> matchedSubquery = cb.createQuery().subquery(Long.class);
                Root<?> matchRoot = matchedSubquery.from("AccountingMatch");
                matchedSubquery.select(cb.count(matchRoot))
                    .where(cb.and(
                        cb.equal(matchRoot.get("electronicReceipt"), receipt),
                        cb.equal(matchRoot.get("matchStatus"), "MATCHED")
                    ));
                predicates.add(cb.greaterThan(matchedSubquery, 0L));
            } else if ("UNMATCHED".equals(searchDto.getMatchStatus())) {
                Subquery<Long> unmatchedSubquery = cb.createQuery().subquery(Long.class);
                Root<?> matchRoot = unmatchedSubquery.from("AccountingMatch");
                unmatchedSubquery.select(cb.count(matchRoot))
                    .where(cb.and(
                        cb.equal(matchRoot.get("electronicReceipt"), receipt),
                        cb.equal(matchRoot.get("matchStatus"), "MATCHED")
                    ));
                predicates.add(cb.equal(unmatchedSubquery, 0L));
            }
        }
        
        // Verification status filter
        if (searchDto.getIsVerified() != null) {
            predicates.add(cb.equal(receipt.get("isVerified"), searchDto.getIsVerified()));
        }
        
        // Merchant category filter
        if (StringUtils.hasText(searchDto.getMerchantCategory())) {
            predicates.add(cb.equal(transaction.get("merchantCategory"), 
                searchDto.getMerchantCategory()));
        }
        
        // Approval number filter
        if (StringUtils.hasText(searchDto.getApprovalNumber())) {
            predicates.add(cb.equal(transaction.get("approvalNumber"), 
                searchDto.getApprovalNumber()));
        }
        
        return predicates;
    }
    
    private Path<?> getPath(Root<ElectronicReceipt> root, String property) {
        switch (property) {
            case "issueDate":
                return root.get("issueDate");
            case "amount":
                return root.get("transactionRecord").get("amount");
            case "merchantName":
                return root.get("transactionRecord").get("merchantName");
            case "transactionDate":
                return root.get("transactionRecord").get("transactionDateTime");
            default:
                return null;
        }
    }
}