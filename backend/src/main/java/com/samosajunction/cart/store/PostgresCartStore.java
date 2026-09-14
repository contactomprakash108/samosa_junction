package com.samosajunction.cart.store;

import com.samosajunction.cart.entity.CartRecord;
import com.samosajunction.cart.repository.CartRecordRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class PostgresCartStore {

    private final CartRecordRepository cartRecordRepository;

    public PostgresCartStore(CartRecordRepository cartRecordRepository) {
        this.cartRecordRepository = cartRecordRepository;
    }

    @Transactional(readOnly = true)
    public CartPayload load(UUID userId) {
        return cartRecordRepository.findById(userId)
                .map(CartRecord::getPayload)
                .orElse(null);
    }

    @Transactional
    public void save(UUID userId, CartPayload payload) {
        Instant now = Instant.now();
        cartRecordRepository.findById(userId).ifPresentOrElse(
                record -> record.replace(payload, now),
                () -> cartRecordRepository.save(new CartRecord(userId, payload, now))
        );
    }

    @Transactional
    public void delete(UUID userId) {
        cartRecordRepository.deleteById(userId);
    }
}
