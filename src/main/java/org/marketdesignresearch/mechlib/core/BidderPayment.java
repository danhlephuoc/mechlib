package org.marketdesignresearch.mechlib.core;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.PersistenceConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor(onConstructor = @__({@PersistenceConstructor}))
@ToString
@EqualsAndHashCode
public final class BidderPayment implements Comparable<BidderPayment> {
    public static final BidderPayment ZERO_PAYMENT = new BidderPayment(BigDecimal.ZERO);
    @Getter
    private final BigDecimal amount;

    @Override
    public int compareTo(BidderPayment o) {
        return getAmount().compareTo(o.getAmount());
    }
}
