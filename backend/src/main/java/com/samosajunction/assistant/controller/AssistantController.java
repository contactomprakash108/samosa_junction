package com.samosajunction.assistant.controller;

import com.samosajunction.assistant.dto.AssistantChatRequest;
import com.samosajunction.assistant.dto.AssistantChatResponse;
import com.samosajunction.assistant.service.AssistantService;
import com.samosajunction.auth.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    public AssistantChatResponse chat(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AssistantChatRequest request
    ) {
        return assistantService.chat(principal.getId(), request);
    }
}
