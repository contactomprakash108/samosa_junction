package com.samosajunction.wallet.controller;

import com.samosajunction.auth.security.UserPrincipal;
import com.samosajunction.common.response.PageResponse;
import com.samosajunction.wallet.dto.AddMoneyRequest;
import com.samosajunction.wallet.dto.WalletResponse;
import com.samosajunction.wallet.dto.WalletTransactionResponse;
import com.samosajunction.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public WalletResponse get(@AuthenticationPrincipal UserPrincipal principal) {
        return walletService.getWallet(principal.getId());
    }

    @PostMapping("/add-money")
    public WalletResponse addMoney(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AddMoneyRequest request
    ) {
        return walletService.addMoney(
                principal.getId(),
                request.amount(),
                idempotencyKey,
                request.referenceId()
        );
    }

    @GetMapping("/transactions")
    public PageResponse<WalletTransactionResponse> transactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(walletService.listTransactions(principal.getId(), pageable));
    }
}
