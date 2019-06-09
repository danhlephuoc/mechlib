package org.marketdesignresearch.mechlib.domain.auction;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.marketdesignresearch.mechlib.domain.Domain;
import org.marketdesignresearch.mechlib.domain.Good;
import org.marketdesignresearch.mechlib.domain.bid.Bid;
import org.marketdesignresearch.mechlib.domain.bidder.Bidder;
import org.marketdesignresearch.mechlib.domain.bid.Bids;
import org.marketdesignresearch.mechlib.mechanisms.AuctionMechanism;
import org.marketdesignresearch.mechlib.mechanisms.AuctionResult;
import org.marketdesignresearch.mechlib.mechanisms.MechanismType;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RequiredArgsConstructor
public class Auction implements AuctionMechanism {

    @Getter
    private final Domain domain;
    @Getter
    private final MechanismType mechanismType;
    private List<AuctionRound> rounds = new ArrayList<>();

    public Bidder getBidder(UUID id) {
        return domain.getBidders().stream().filter(b -> b.getId().equals(id)).findFirst().orElseThrow(NoSuchElementException::new);
    }

    public Good getGood(String id) {
        return domain.getGoods().stream().filter(b -> b.getId().equals(id)).findFirst().orElseThrow(NoSuchElementException::new);
    }

    public int addRound(Bids bids) {
        return addRound(new AuctionRound(rounds.size() + 1, bids));
    }

    public int addRound(AuctionRound round) {
        int roundNumber = rounds.size() + 1;
        Preconditions.checkArgument(round.getRoundNumber() == roundNumber);
        Preconditions.checkArgument(domain.getBidders().containsAll(round.getBids().getBidders()));
        Preconditions.checkArgument(domain.getGoods().containsAll(round.getBids().getGoods()));
        rounds.add(round);
        return roundNumber;
    }

    public Bids getBidsAt(int round) {
        Preconditions.checkArgument(round >= 0 && round < rounds.size());
        return rounds.stream()
                .map(AuctionRound::getBids)
                .reduce(new Bids(), Bids::join);
    }

    public Bid getBidsAt(Bidder bidder, int round) {
        Preconditions.checkArgument(round >= 0 && round < rounds.size());
        return rounds.stream()
                .map(AuctionRound::getBids)
                .map(bids -> bids.getBid(bidder))
                .reduce(new Bid(), Bid::join);
    }

    public Bids getLatestBids() {
        if (rounds.size() == 0) return new Bids();
        return getBidsAt(rounds.size() - 1);
    }

    public Bid getLatestBids(Bidder bidder) {
        return getBidsAt(bidder, rounds.size() - 1);
    }

    public AuctionRound getRound(int index) {
        Preconditions.checkArgument(index >= 0 && index < rounds.size());
        return rounds.get(index);
    }

    public int getRounds() {
        return rounds.size();
    }

    public void resetToRound(int index) {
        Preconditions.checkArgument(index < rounds.size());
        rounds = rounds.subList(0, index);
    }

    public AuctionResult getAuctionResultAtRound(int index) {
        if (getRound(index).getAuctionResult() == null) {
            getRound(index).setAuctionResult(mechanismType.getMechanism(getBidsAt(index)).getAuctionResult());
        }
        return getRound(index).getAuctionResult();
    }

    @Override
    public AuctionResult getAuctionResult() {
        if (rounds.size() == 0) return AuctionResult.NONE;
        return getAuctionResultAtRound(rounds.size() - 1);
    }
}