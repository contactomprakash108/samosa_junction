package com.samosajunction.product.support;

import com.samosajunction.common.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class InrMoney {

    private InrMoney() {
    }

    public static int toPaise(BigDecimal rupees) {
        try {
            return rupees.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).intValueExact();
        } catch (ArithmeticException ex) {
            throw new InvalidRequestException("Price must be a non-negative amount with at most two decimal places");
        }
    }

    public static BigDecimal toRupees(int paise) {
        return BigDecimal.valueOf(paise).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY);
    }
}
