package com.samosajunction.complaint.controller;

import com.samosajunction.auth.security.UserPrincipal;
import com.samosajunction.complaint.dto.ComplaintImageResponse;
import com.samosajunction.complaint.service.ComplaintImageService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/complaints/{complaintId}/images")
public class ComplaintImageController {

    private final ComplaintImageService complaintImageService;

    public ComplaintImageController(ComplaintImageService complaintImageService) {
        this.complaintImageService = complaintImageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintImageResponse upload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID complaintId,
            @RequestPart("file") MultipartFile file
    ) {
        return complaintImageService.upload(principal.getId(), complaintId, isPrivileged(principal), file);
    }

    @GetMapping
    public List<ComplaintImageResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID complaintId
    ) {
        return complaintImageService.list(principal.getId(), complaintId, isPrivileged(principal));
    }

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID complaintId,
            @PathVariable UUID imageId
    ) {
        complaintImageService.delete(principal.getId(), complaintId, imageId, isPrivileged(principal));
    }

    private static boolean isPrivileged(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_STAFF") || authority.equals("ROLE_ADMIN"));
    }
}
