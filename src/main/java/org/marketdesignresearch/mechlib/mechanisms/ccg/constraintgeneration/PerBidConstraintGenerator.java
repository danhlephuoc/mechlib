package org.marketdesignresearch.mechlib.mechanisms.ccg.constraintgeneration;

import com.google.common.collect.Sets;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.marketdesignresearch.mechlib.domain.*;
import org.marketdesignresearch.mechlib.domain.bid.Bid;
import org.marketdesignresearch.mechlib.domain.bid.Bids;
import org.marketdesignresearch.mechlib.domain.bidder.Bidder;
import org.marketdesignresearch.mechlib.mechanisms.AuctionResult;
import org.marketdesignresearch.mechlib.mechanisms.ccg.blockingallocation.BlockedBidders;
import org.marketdesignresearch.mechlib.mechanisms.ccg.paymentrules.CorePaymentRule;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class should not be instantiated alone as it will not generate all core
 * constraints
 *
 * @author Benedikt
 */
class PerBidConstraintGenerator implements PartialConstraintGenerator {
    @Override
    public void generateFirstRoundConstraints(Bids bids, AuctionResult referencePoint, Map<Good, PotentialCoalition> goodToBidderMap, CorePaymentRule corePaymentRule) {
        corePaymentRule.resetResult();
        Allocation allocation = referencePoint.getAllocation();
        Payment lowerBound = referencePoint.getPayment();
        for (Entry<Bidder, Bid> bid : bids) {
            for (BundleBid bundleBid : bid.getValue().getBundleBids()) {
                // TODO: assumes availability of 1
                Set<Bidder> blockedBiddersSet = bundleBid.getBundle().getBundleEntries().stream().map(BundleEntry::getGood).filter(goodToBidderMap::containsKey).map(good -> goodToBidderMap.get(good).getBidder()).collect(Collectors.toSet());
                BigDecimal currentValue = allocation.allocationOf(bid.getKey()) == null ? BigDecimal.ZERO : allocation.allocationOf(bid.getKey()).getValue();
                BigDecimal currentPayment = lowerBound.paymentOf(bid.getKey()) == null ? BigDecimal.ZERO : lowerBound.paymentOf(bid.getKey()).getAmount();
                BigDecimal blockingValue = bundleBid.getAmount().subtract(currentValue).add(currentPayment);
                if (blockingValue.signum() > 0) {
                    BlockedBidders blockedBidders = new BlockedBidders(blockedBiddersSet, Sets.newHashSet(bundleBid.getPotentialCoalition(bid.getKey())), blockingValue);
                    corePaymentRule.addBlockingConstraint(blockedBidders, lowerBound);
                }
            }
        }
    }

    @Override
    public void generateConstraint(CorePaymentRule corePaymentRule, Graph<PotentialCoalition, DefaultEdge> graph,
                                   ConnectivityInspector<PotentialCoalition, DefaultEdge> connectivityInspector, Allocation blockingCoalition, AuctionResult priorResult) {

    }

}