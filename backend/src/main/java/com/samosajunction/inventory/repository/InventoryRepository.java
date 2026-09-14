package com.samosajunction.inventory.repository;

import com.samosajunction.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    long countByQuantityLessThanEqual(int quantity);
}
