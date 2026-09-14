package com.samosajunction.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 120) String fullName,
        @Size(max = 20) String phone,
        @Size(max = 120) String recipientName,
        @Size(max = 200) String addressLine1,
        @Size(max = 80) String city,
        @Size(max = 80) String state,
        @Size(max = 16) String pincode
) {
}
