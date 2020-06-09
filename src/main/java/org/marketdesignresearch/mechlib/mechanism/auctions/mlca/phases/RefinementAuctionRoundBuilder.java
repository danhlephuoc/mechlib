package org.marketdesignresearch.mechlib.mechanism.auctions.mlca.phases;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.marketdesignresearch.mechlib.core.Allocation;
import org.marketdesignresearch.mechlib.core.Outcome;
import org.marketdesignresearch.mechlib.core.bid.bundle.BundleBoundValueBid;
import org.marketdesignresearch.mechlib.core.bid.bundle.BundleBoundValueBids;
import org.marketdesignresearch.mechlib.core.bidder.Bidder;
import org.marketdesignresearch.mechlib.core.price.Prices;
import org.marketdesignresearch.mechlib.mechanism.auctions.Auction;
import org.marketdesignresearch.mechlib.mechanism.auctions.AuctionRound;
import org.marketdesignresearch.mechlib.mechanism.auctions.AuctionRoundBuilder;
import org.marketdesignresearch.mechlib.mechanism.auctions.interactions.RefinementQuery;
import org.marketdesignresearch.mechlib.mechanism.auctions.interactions.RefinementType;
import org.marketdesignresearch.mechlib.mechanism.auctions.interactions.impl.DefaultRefinementQueryInteraction;
import org.marketdesignresearch.mechlib.mechanism.auctions.mlca.refinement.validator.ICEValidator;
import org.marketdesignresearch.mechlib.outcomerules.OutcomeRuleGenerator;

import com.google.common.base.Preconditions;

import lombok.Getter;

public class RefinementAuctionRoundBuilder extends AuctionRoundBuilder<BundleBoundValueBids> {

	@Getter
	private final Map<UUID, RefinementQuery> interactions;

	private final Prices prices;
	private final Allocation alphaAllocation;
	private final Set<RefinementType> types;

	private final Map<UUID, BundleBoundValueBid> original = new LinkedHashMap<>();

	public RefinementAuctionRoundBuilder(Auction<BundleBoundValueBids> auction, Prices prices,
			Allocation alphaAllocation) {
		super(auction);
		this.prices = prices;
		this.alphaAllocation = alphaAllocation;

		BundleBoundValueBids latestAggregatedBids = auction.getLatestAggregatedBids();

		this.types = RefinementHelper.getMRPARAndDIAR(latestAggregatedBids);

		this.interactions = new LinkedHashMap<>();

		for (Bidder bidder : auction.getDomain().getBidders()) {
			interactions.put(bidder.getId(),new DefaultRefinementQueryInteraction(bidder.getId(), auction, types,
					alphaAllocation.allocationOf(bidder).getBundle(), prices,
					latestAggregatedBids.getBid(bidder)));
			this.original.put(bidder.getId(), latestAggregatedBids.getBid(bidder).copy());
		}
	}

	@Override
	public AuctionRound<BundleBoundValueBids> build() {
		// Validate submissions
		interactions.entrySet()
				.forEach(e -> ICEValidator.validateRefinement(original.get(e.getKey()), e.getValue().getBid(),
						e.getValue().getPrices(), e.getValue().getProvisonalAllocation(),
						e.getValue().getRefinementTypes()));
		// check if no new bids were added
		// consistency of the bid was already validated before (i.e. if for every bundle of the original bid a bid was submitted)
		interactions.entrySet().forEach(e -> Preconditions.checkState(e.getValue().getBid().getBundleBids().size() == original.get(e.getKey()).getBundleBids().size()));
		
		return new RefinementAuctionRound(this.getAuction(), prices,
				new BundleBoundValueBids(interactions.entrySet().stream().collect(
						Collectors.toMap(e -> this.getAuction().getBidder(e.getKey()), e -> e.getValue().getBid()))),
				types, alphaAllocation);
	}

	@Override
	protected Outcome computeTemporaryResult(OutcomeRuleGenerator outcomeRuleGenerator) {
		// TODO Auto-generated method stub
		return null;
	}

}