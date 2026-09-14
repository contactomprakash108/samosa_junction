package com.samosajunction.cart.repository;

import com.samosajunction.cart.entity.CartRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CartRecordRepository extends JpaRepository<CartRecord, UUID> {
}
