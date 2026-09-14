package com.samosajunction.staff.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StaffDashboardResponse(
        Instant asOf,
        long ordersToday,
        BigDecimal revenueToday,
        long preparing,
        long ready,
        long pending,
        long openComplaints,
        long lowStock
) {
}
