package com.ecommerce.library.repository;

import com.ecommerce.library.model.IncomeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncomeRecordRepository extends JpaRepository<IncomeRecord, Long> {
    IncomeRecord findByOrderId(Long orderId);
}
