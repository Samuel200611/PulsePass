package com.pulsepass.service.pricing;

import com.pulsepass.domain.TicketType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TicketPricingPolicy no tiene colaboradores (no llama repositories ni hace
 * I/O), asi que no necesita Mockito ni @ExtendWith(MockitoExtension.class):
 * es una funcion pura y se prueba como tal.
 */
class TicketPricingPolicyTest {

    @Test
    void generalTicketCostsBasePrice() {
        assertThat(TicketPricingPolicy.calculatePrice(TicketType.GENERAL))
                .isEqualByComparingTo(TicketPricingPolicy.BASE_PRICE);
    }

    @Test
    void studentTicketIsCheaperThanGeneral() {
        BigDecimal student = TicketPricingPolicy.calculatePrice(TicketType.STUDENT);
        BigDecimal general = TicketPricingPolicy.calculatePrice(TicketType.GENERAL);

        assertThat(student).isLessThan(general);
        assertThat(student).isEqualByComparingTo("50000.00");
    }

    @Test
    void vipTicketIsMoreExpensiveThanGeneral() {
        BigDecimal vip = TicketPricingPolicy.calculatePrice(TicketType.VIP);
        BigDecimal general = TicketPricingPolicy.calculatePrice(TicketType.GENERAL);

        assertThat(vip).isGreaterThan(general);
        assertThat(vip).isEqualByComparingTo("200000.00");
    }

    @Test
    void backstageTicketIsTheMostExpensiveTier() {
        BigDecimal backstage = TicketPricingPolicy.calculatePrice(TicketType.BACKSTAGE);
        BigDecimal vip = TicketPricingPolicy.calculatePrice(TicketType.VIP);

        assertThat(backstage).isGreaterThan(vip);
        assertThat(backstage).isEqualByComparingTo("350000.00");
    }

    @Test
    void priceIsNeverNegativeForAnyTicketType() {
        for (TicketType type : TicketType.values()) {
            assertThat(TicketPricingPolicy.calculatePrice(type))
                    .isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }
    }

    @Test
    void priceAlwaysHasExactlyTwoDecimalPlaces() {
        for (TicketType type : TicketType.values()) {
            assertThat(TicketPricingPolicy.calculatePrice(type).scale())
                    .isEqualTo(2);
        }
    }
}
