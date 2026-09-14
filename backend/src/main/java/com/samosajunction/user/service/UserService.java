package com.samosajunction.user.service;

import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.order.dto.DeliveryAddressRequest;
import com.samosajunction.user.dto.UpdateProfileRequest;
import com.samosajunction.user.dto.UserResponse;
import com.samosajunction.user.entity.User;
import com.samosajunction.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        return UserResponse.from(requireUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        user.updateProfile(
                request.fullName(),
                request.phone(),
                request.recipientName(),
                request.addressLine1(),
                request.city(),
                request.state(),
                request.pincode()
        );
        return UserResponse.from(user);
    }

    @Transactional
    public void rememberDelivery(UUID userId, DeliveryAddressRequest delivery) {
        User user = requireUser(userId);
        user.replaceDefaultAddress(
                delivery.recipientName(),
                delivery.line1(),
                delivery.city(),
                delivery.state(),
                delivery.pincode()
        );
    }

    @Transactional(readOnly = true)
    public DeliveryAddressRequest requireDefaultAddress(UUID userId) {
        User user = requireUser(userId);
        if (!user.hasDefaultAddress()) {
            throw new InvalidRequestException(
                    "No default address on your profile. Save one under Profile, then I can place the order."
            );
        }
        return new DeliveryAddressRequest(
                user.getRecipientName(),
                user.getAddressLine1(),
                user.getCity(),
                user.getState(),
                user.getPincode()
        );
    }

    private User requireUser(UUID userId) {
        return userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
