package org.marketdesignresearch.mechlib.mechanism.auctions.cca.bidcollection.supplementaryround;

import java.util.ArrayList;
import java.util.List;

import org.marketdesignresearch.mechlib.core.Bundle;
import org.marketdesignresearch.mechlib.core.bid.bundle.BundleValueBid;
import org.marketdesignresearch.mechlib.core.bid.bundle.BundleValuePair;
import org.marketdesignresearch.mechlib.core.bidder.Bidder;
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.CCAuction;

import com.google.common.collect.Sets;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class ProfitMaximizingSupplementaryRound implements SupplementaryRound {

    private static final int DEFAULT_NUMBER_OF_SUPPLEMENTARY_BIDS = 10;

    @Setter @Getter
    private int numberOfSupplementaryBids = DEFAULT_NUMBER_OF_SUPPLEMENTARY_BIDS;
    
    @Override
    public BundleValueBid getSupplementaryBids(CCAuction auction, Bidder bidder) {
        List<Bundle> bestBundles = bidder.getBestBundles(auction.getCurrentPrices(), numberOfSupplementaryBids, true);
        List<BundleValuePair> bestBundleBids = new ArrayList<>();
        // Add with true value for now
        int count = 0;
        for (Bundle bundle : bestBundles) {
            bestBundleBids.add(new BundleValuePair(bidder.getValue(bundle), bundle, "DQ_" + String.valueOf(auction.getNumberOfRounds()+1) + "-" + ++count + "_Bidder_" + bidder));
        }
        return new BundleValueBid(Sets.newHashSet(bestBundleBids));
    }

    public ProfitMaximizingSupplementaryRound withNumberOfSupplementaryBids(int numberOfSupplementaryBids) {
        setNumberOfSupplementaryBids(numberOfSupplementaryBids);
        return this;
    }

    @Override
    public String getDescription() {
        return "Profit Maximizing Supplementary round with " + numberOfSupplementaryBids + " bids per bidder";
    }

}
