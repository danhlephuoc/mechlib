package org.marketdesignresearch.mechlib.mechanism.auctions.cca;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.marketdesignresearch.mechlib.instrumentation.AuctionInstrumentation;
import org.marketdesignresearch.mechlib.instrumentation.MipInstrumentation;
import org.marketdesignresearch.mechlib.mechanism.auctions.Auction;
import org.marketdesignresearch.mechlib.mechanism.auctions.AuctionRoundBuilder;
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.bidcollection.ClockPhaseBidCollector;
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.bidcollection.SupplementaryBidCollector;
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.bidcollection.supplementaryround.SupplementaryRound;
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.priceupdate.PriceUpdater;
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.priceupdate.SimpleRelativePriceUpdate;
import org.marketdesignresearch.mechlib.core.BundleBid;
import org.marketdesignresearch.mechlib.core.Domain;
import org.marketdesignresearch.mechlib.core.Good;
import org.marketdesignresearch.mechlib.core.bid.Bid;
import org.marketdesignresearch.mechlib.core.bid.Bids;
import org.marketdesignresearch.mechlib.core.bidder.Bidder;
import org.marketdesignresearch.mechlib.core.price.LinearPrices;
import org.marketdesignresearch.mechlib.core.price.Prices;
import org.marketdesignresearch.mechlib.core.Outcome;
import org.marketdesignresearch.mechlib.outcomerules.OutcomeRuleGenerator;
import org.marketdesignresearch.mechlib.outcomerules.ccg.MechanismFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.marketdesignresearch.mechlib.mechanism.auctions.cca.CCARound.Type.CLOCK;
import static org.marketdesignresearch.mechlib.mechanism.auctions.cca.CCARound.Type.SUPPLEMENTARY;

@Slf4j
public class CCAuction extends Auction {

    @Getter
    private Prices currentPrices;

    @Setter
    private PriceUpdater priceUpdater = new SimpleRelativePriceUpdate();
    private final List<SupplementaryRound> supplementaryRounds = new ArrayList<>();
    private Queue<SupplementaryRound> supplementaryRoundQueue = new LinkedList<>();

    @Getter
    private boolean clockPhaseCompleted = false;

    public CCAuction(Domain domain) {
        this(domain, org.marketdesignresearch.mechlib.outcomerules.OutcomeRuleGenerator.CCG);
    }

    public CCAuction(Domain domain, OutcomeRuleGenerator outcomeRuleGenerator) {
        this(domain, outcomeRuleGenerator, false);
    }

    public CCAuction(Domain domain, OutcomeRuleGenerator mechanismType, Prices currentPrices) {
        this(domain, mechanismType, false);
        this.currentPrices = currentPrices;
    }

    public CCAuction(Domain domain, OutcomeRuleGenerator mechanismType, boolean proposeStartingPrices) {
        this(domain, mechanismType, proposeStartingPrices, new MipInstrumentation(), new AuctionInstrumentation());
    }

    public CCAuction(Domain domain, OutcomeRuleGenerator mechanismType, boolean proposeStartingPrices, MipInstrumentation mipInstrumentation) {
        this(domain, mechanismType, proposeStartingPrices, mipInstrumentation, new AuctionInstrumentation());
    }

    public CCAuction(Domain domain, OutcomeRuleGenerator mechanismType, boolean proposeStartingPrices, MipInstrumentation mipInstrumentation, AuctionInstrumentation auctionInstrumentation) {
        super(domain, mechanismType, mipInstrumentation, auctionInstrumentation);
        setMaxRounds(100);
        if (proposeStartingPrices) {
            this.currentPrices = getDomain().proposeStartingPrices();
        } else {
            this.currentPrices = new LinearPrices(getDomain().getGoods());
        }
    }

    @Override
    public int allowedNumberOfBids() {
        if (!clockPhaseCompleted) return 1;
        SupplementaryRound next = supplementaryRoundQueue.peek();
        return next == null ? 0 : next.getNumberOfSupplementaryBids();
    }

    public ImmutableList<SupplementaryRound> getSupplementaryRounds() {
        return ImmutableList.copyOf(supplementaryRounds);
    }

    public void addSupplementaryRound(SupplementaryRound supplementaryRound) {
        supplementaryRounds.add(supplementaryRound);
        supplementaryRoundQueue.add(supplementaryRound);
    }

    /**
     * Overrides the default method to have outcomes only based on each round's bids
     */
    @Override
    public Outcome getOutcomeAtRound(int index) {
        if (getBidsAt(index).isEmpty()) return Outcome.NONE;
        if (getRound(index).getOutcome() == null) {
            getRound(index).setOutcome(getOutcomeRuleType().getOutcomeRule(getBidsAt(index), getMipInstrumentation()).getOutcome());
        }
        return getRound(index).getOutcome();
    }

    /**
     * This is a shortcut to finish all rounds & calculate the final result
     */
    @Override
    public Outcome getOutcome() {
        log.info("Finishing all rounds...");
        while (!finished()) {
            advanceRound();
        }
        log.info("Collected all bids. Running {} Auction to determine allocation & payments.", getOutcomeRuleType());
        return getOutcomeRuleType().getOutcomeRule(getAggregatedBidsAt(rounds.size() - 1), getMipInstrumentation()).getOutcome();
    }

    public CCARound.Type getCurrentRoundType() {
        if (clockPhaseCompleted) return SUPPLEMENTARY;
        return CLOCK;
    }

    @Override
    public boolean currentPhaseFinished() {
        if (rounds.isEmpty()) return false;
        CCARound lastRound = (CCARound) rounds.get(rounds.size() - 1);
        if (CLOCK.equals(lastRound.getType()) && clockPhaseCompleted) {
            return true;
        } else {
            return finished();
        }
    }

    @Override
    public void closeRound() {
        Preconditions.checkState(!finished());
        Bids bids = current.getBids();
        Preconditions.checkArgument(getDomain().getBidders().containsAll(bids.getBidders()));
        Preconditions.checkArgument(getDomain().getGoods().containsAll(bids.getGoods()));
        int roundNumber = rounds.size() + 1;
        CCARound round;
        if (clockPhaseCompleted) {
            round = new CCARound(roundNumber, bids, getCurrentPrices(), SUPPLEMENTARY);
            supplementaryRoundQueue.poll();
        } else {
            round = new CCARound(roundNumber, bids, getCurrentPrices());
        }
        // if (current.hasOutcome()) {
        //     round.setOutcome(current.getOutcome());
        // }
        getAuctionInstrumentation().postRound(round);
        rounds.add(round);
        current = new AuctionRoundBuilder(getOutcomeRuleType(), getMipInstrumentation());
        updatePrices();
    }

    @Override
    public boolean finished() {
        return super.finished() || clockPhaseCompleted && !hasNextSupplementaryRound();
    }

    @Override
    public Bid proposeBid(Bidder bidder) {
        Bid bid = super.proposeBid(bidder);
        if (CLOCK.equals(getCurrentRoundType())) {
            Set<BundleBid> bundleBids = bid.getBundleBids().stream()
                    .map(bb -> new BundleBid(getCurrentPrices().getPrice(bb.getBundle()).getAmount(), bb.getBundle(), bb.getId()))
                    .collect(Collectors.toSet());
            bid = new Bid(bundleBids);
        }
        return bid;
    }

    private void updatePrices() {
        Map<Good, Integer> demand = new HashMap<>();
        getDomain().getGoods().forEach(good -> demand.put(good, getLatestBids().getDemand(good)));
        Prices updatedPrices = priceUpdater.updatePrices(getCurrentPrices(), demand);
        if (getCurrentPrices().equals(updatedPrices) || getNumberOfRounds() >= getMaxRounds()) {
            clockPhaseCompleted = true;
            return;
        }
        currentPrices = updatedPrices;
    }

    @Override
    public void advanceRound() {
        List<Bidder> biddersToQuery = getDomain().getBidders().stream().filter(b -> !current.getBids().getBidders().contains(b)).collect(Collectors.toList());
        if (!clockPhaseCompleted) {
            ClockPhaseBidCollector collector = new ClockPhaseBidCollector(getNumberOfRounds() + 1, getCurrentPrices(), biddersToQuery);
            log.debug("Starting clock round {}...", getNumberOfRounds() + 1);
            submitBids(collector.collectBids());
        } else {
            if (supplementaryRoundQueue.isEmpty()) {
                log.warn("No supplementary round found to run");
                return;
            }
            SupplementaryRound supplementaryRound = supplementaryRoundQueue.peek();
            SupplementaryBidCollector collector = new SupplementaryBidCollector(getNumberOfRounds() + 1, biddersToQuery, supplementaryRound, getCurrentPrices());
            log.debug("Starting supplementary round '{}'...", collector);
            submitBids(collector.collectBids());
        }
        closeRound();
    }

    public boolean hasNextSupplementaryRound() {
        return !supplementaryRoundQueue.isEmpty();
    }

    @Override
    public void resetToRound(int index) {
        CCARound round = (CCARound) getRound(index);
        currentPrices = round.getPrices();
        if (SUPPLEMENTARY.equals(round.getType())) {
            CCARound previous = (CCARound) getRound(index - 1);
            Preconditions.checkState(CLOCK.equals(previous.getType()),
                    "Currently, the implementation does not allow to reset to another supplementary round than the first one.");
            clockPhaseCompleted = true;
            supplementaryRoundQueue = new LinkedList<>(supplementaryRounds);
        } else {
            clockPhaseCompleted = false;
            supplementaryRoundQueue = new LinkedList<>(supplementaryRounds);
        }
        super.resetToRound(index);
    }

}