package com.samosajunction.support.service;

import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.support.dto.CreateSupportMessageRequest;
import com.samosajunction.support.dto.StaffSupportMessageResponse;
import com.samosajunction.support.dto.SupportMessageResponse;
import com.samosajunction.support.entity.SupportMessage;
import com.samosajunction.support.repository.SupportMessageRepository;
import com.samosajunction.user.entity.User;
import com.samosajunction.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SupportService {

    private final SupportMessageRepository supportMessageRepository;
    private final UserRepository userRepository;

    public SupportService(SupportMessageRepository supportMessageRepository, UserRepository userRepository) {
        this.supportMessageRepository = supportMessageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SupportMessageResponse create(UUID userId, CreateSupportMessageRequest request) {
        var saved = supportMessageRepository.save(new SupportMessage(userId, request.subject().trim(), request.body().trim()));
        return SupportMessageResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<SupportMessageResponse> list(UUID userId, Pageable pageable) {
        return supportMessageRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(SupportMessageResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<StaffSupportMessageResponse> listAll(Pageable pageable) {
        Page<SupportMessage> page = supportMessageRepository.findAllByOrderByCreatedAtDesc(pageable);
        Map<UUID, User> users = userRepository.findAllById(
                page.getContent().stream().map(SupportMessage::getUserId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return page.map(message -> {
            User user = users.get(message.getUserId());
            String email = user == null ? "unknown" : user.getEmail();
            String name = user == null ? "Customer" : user.getFullName();
            return StaffSupportMessageResponse.from(message, email, name);
        });
    }

    @Transactional
    public StaffSupportMessageResponse close(UUID messageId) {
        SupportMessage message = supportMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Support message not found"));
        message.close();
        User user = userRepository.findById(message.getUserId()).orElse(null);
        String email = user == null ? "unknown" : user.getEmail();
        String name = user == null ? "Customer" : user.getFullName();
        return StaffSupportMessageResponse.from(message, email, name);
    }
}
