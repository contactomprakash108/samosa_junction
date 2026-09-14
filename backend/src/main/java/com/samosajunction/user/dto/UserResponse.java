package com.samosajunction.user.dto;

import com.samosajunction.user.entity.User;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String recipientName,
        String addressLine1,
        String city,
        String state,
        String pincode,
        boolean hasDefaultAddress,
        Set<String> roles,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRecipientName(),
                user.getAddressLine1(),
                user.getCity(),
                user.getState(),
                user.getPincode(),
                user.hasDefaultAddress(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toUnmodifiableSet()),
                user.getCreatedAt()
        );
    }
}
