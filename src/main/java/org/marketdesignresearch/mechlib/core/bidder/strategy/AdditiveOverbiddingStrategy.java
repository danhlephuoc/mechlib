package org.marketdesignresearch.mechlib.core.bidder.strategy;

import org.marketdesignresearch.mechlib.core.Good;
import org.marketdesignresearch.mechlib.core.bid.bundle.BundleValueBid;
import org.marketdesignresearch.mechlib.core.bid.bundle.BundleValuePair;

import com.google.common.collect.ImmutableSet;
import org.marketdesignresearch.mechlib.core.bidder.valuefunction.ValueFunction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/**
 * Created by Benedikt on 30.07.15.
 */
public class AdditiveOverbiddingStrategy<S extends ComparableStrategy<S>> implements MultiDimensionalComparableStrategy<AdditiveOverbiddingStrategy<S>> {
    public static final AdditiveOverbiddingStrategy<ShaveStrategy> TRUTHFUL = new AdditiveOverbiddingStrategy<>(ShaveStrategy.TRUTHFUL, BigDecimal.ZERO, ImmutableSet.of());
    private final S normalStrategy;

    private final BigDecimal overBid;
    private final Set<Good> allGoods;

    public AdditiveOverbiddingStrategy(S normalStrategy, BigDecimal overBid, Set<Good> allGoods) {
        this.normalStrategy = normalStrategy;
        this.overBid = overBid;
        this.allGoods = allGoods;
    }


    public Set<Good> getAllGoods() {
        return allGoods;
    }

    public Strategy getNormalStrategy() {
        return normalStrategy;
    }


    @Override
    public BundleValueBid<BundleValuePair> apply(ValueFunction combinatorialValueFunction) {
        BundleValueBid<BundleValuePair> bid = normalStrategy.apply(combinatorialValueFunction);
        if (overBid.signum() != 0) {
            BigDecimal bidSoFar = bid.getBundleBids().stream().map(BundleValuePair::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BundleValuePair bundleBid = new BundleValuePair(bidSoFar.add(overBid), allGoods, "FakeBig");
            bid.addBundleBid(bundleBid);
        }
        return bid;

    }

    @Override
    public String toString() {
        return "Overbid[" + overBid + "]";
    }

    @Override
    public int dimensions() {
        return 2;
    }



    @Override
    public BigDecimal getExactStrategy(int dimension) {
        if (dimension == 0) {
            return normalStrategy.getExactStrategy();
        } else if (dimension == 1) {
            return overBid;

        }
        throw new IllegalArgumentException("Dimension out of range. 2 dimensions. Requested " + dimension);


    }

    @Override
    public AdditiveOverbiddingStrategy<S> moreTruthful(int dimension, BigDecimal strategyChange) {
        if (dimension == 0) {
            S newNormalStrategy = normalStrategy.moreTruthful(strategyChange);
            return new AdditiveOverbiddingStrategy<>(newNormalStrategy, overBid, allGoods);
        } else if (dimension == 1) {
            BigDecimal newOverBid = overBid.subtract(strategyChange);
            return new AdditiveOverbiddingStrategy<>(normalStrategy, newOverBid, allGoods);

        }
        throw new IllegalArgumentException("Dimension out of range. 2 dimensions. Requested " + dimension);
    }



    @Override
    public AdditiveOverbiddingStrategy<S> merge(AdditiveOverbiddingStrategy<S> other) {
        S mergedNormal = normalStrategy.merge(other.normalStrategy);
        BigDecimal mergedOverbid = overBid.add(other.overBid).divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
        return new AdditiveOverbiddingStrategy<>(mergedNormal, mergedOverbid, allGoods);
    }
}
