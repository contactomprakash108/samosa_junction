package com.samosajunction.product.support;

import com.samosajunction.common.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InrMoneyTest {

    @Test
    void convertsRupeesToPaise() {
        assertThat(InrMoney.toPaise(new BigDecimal("30.00"))).isEqualTo(3000);
        assertThat(InrMoney.toPaise(new BigDecimal("30.5"))).isEqualTo(3050);
        assertThat(InrMoney.toRupees(4000)).isEqualByComparingTo("40.00");
    }

    @Test
    void rejectsMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(() -> InrMoney.toPaise(new BigDecimal("30.123")))
                .isInstanceOf(InvalidRequestException.class);
    }
}
