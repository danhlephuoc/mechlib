package org.marketdesignresearch.mechlib.mechanisms.cca.priceupdate;

import com.google.common.base.Preconditions;
import org.marketdesignresearch.mechlib.domain.price.LinearPrices;
import org.marketdesignresearch.mechlib.domain.price.Prices;
import org.marketdesignresearch.mechlib.domain.Good;
import org.marketdesignresearch.mechlib.domain.price.Price;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class SimpleRelativePriceUpdate implements PriceUpdater {

    private static final BigDecimal DEFAULT_PRICE_UPDATE = BigDecimal.valueOf(0.1);
    private static final BigDecimal DEFAULT_INITIAL_UPDATE = BigDecimal.valueOf(1e5);

    @Setter
    private BigDecimal priceUpdate = DEFAULT_PRICE_UPDATE;
    @Setter
    private BigDecimal initialUpdate = DEFAULT_INITIAL_UPDATE;

    @Override
    public Prices updatePrices(Prices oldPrices, Map<Good, Integer> demand) {
        Preconditions.checkArgument(oldPrices instanceof LinearPrices, "Simple relative price updater only works with linear prices.");
        LinearPrices oldLinearPrices = (LinearPrices) oldPrices;

        Map<Good, Price> newPrices = new HashMap<>();

        for (Map.Entry<Good, Price> oldPriceEntry : oldLinearPrices.entrySet()) {
            Good good = oldPriceEntry.getKey();
            if (good.available() < demand.getOrDefault(good, 0)) {
                if (oldPriceEntry.getValue().equals(Price.ZERO))
                    newPrices.put(good, new Price(initialUpdate));
                else
                    newPrices.put(good, new Price(oldPriceEntry.getValue().getAmount().add(oldPriceEntry.getValue().getAmount().multiply(priceUpdate))));
            } else {
                newPrices.put(good, oldPriceEntry.getValue());
            }

        }

        return new LinearPrices(newPrices);
    }

    public SimpleRelativePriceUpdate withPriceUpdate(BigDecimal priceUpdate) {
        setPriceUpdate(priceUpdate);
        return this;
    }

    public SimpleRelativePriceUpdate withInitialUpdate(BigDecimal initialUpdate) {
        setInitialUpdate(initialUpdate);
        return this;
    }
}