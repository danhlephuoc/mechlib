package org.marketdesignresearch.mechlib.core.bid;

import com.google.common.collect.ImmutableSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.marketdesignresearch.mechlib.core.BundleBid;
import org.marketdesignresearch.mechlib.core.BundleEntry;
import org.marketdesignresearch.mechlib.core.Good;
import org.springframework.data.annotation.PersistenceConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor(onConstructor = @__({@PersistenceConstructor}))
@ToString @EqualsAndHashCode
public class Bid { // FIXME: Have it abstract enough to support all kind of bids
    public static Bid singleBundleBid(BundleBid bundleBid) {
        return new Bid(ImmutableSet.of(bundleBid));
    }

    @Getter
    private final Set<BundleBid> bundleBids;

    public Bid() {
        this(new LinkedHashSet<>());
    }

    public boolean addBundleBid(BundleBid bundleBid) {
        return bundleBids.add(bundleBid);
    }

    public Bid reducedBy(BigDecimal payoff) {
        LinkedHashSet<BundleBid> newBids = getBundleBids().stream().map(bid -> bid.reducedBy(payoff)).collect(Collectors.toCollection(LinkedHashSet::new));
        return new Bid(newBids);
    }

    public Set<Good> getGoods() {
        Set<Good> goods = new HashSet<>();
        for (BundleBid bundleBid : bundleBids) {
            goods.addAll(bundleBid.getBundle().getBundleEntries().stream().map(BundleEntry::getGood).collect(Collectors.toSet()));
        }
        return goods;
    }

    public Bid join(Bid other) {
        Bid result = new Bid();
        getBundleBids().forEach(result::addBundleBid);
        other.getBundleBids().forEach(result::addBundleBid);
        return result;
    }
}

