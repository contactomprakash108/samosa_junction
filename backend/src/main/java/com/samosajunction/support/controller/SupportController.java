package com.samosajunction.support.controller;

import com.samosajunction.auth.security.UserPrincipal;
import com.samosajunction.common.response.PageResponse;
import com.samosajunction.support.dto.CreateSupportMessageRequest;
import com.samosajunction.support.dto.SupportMessageResponse;
import com.samosajunction.support.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupportMessageResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateSupportMessageRequest request
    ) {
        return supportService.create(principal.getId(), request);
    }

    @GetMapping
    public PageResponse<SupportMessageResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(supportService.list(principal.getId(), pageable));
    }
}
