package com.samosajunction.wallet.dto;

import com.samosajunction.product.support.InrMoney;
import com.samosajunction.wallet.entity.Wallet;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(UUID id, BigDecimal balance, String currency) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                InrMoney.toRupees(wallet.getBalancePaise()),
                wallet.getCurrency()
        );
    }
}
