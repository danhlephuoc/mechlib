package org.marketdesignresearch.mechlib.outcomerules.itemlevel;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.marketdesignresearch.mechlib.mechanism.auctions.Auction;
import org.marketdesignresearch.mechlib.core.*;
import org.marketdesignresearch.mechlib.core.bid.Bid;
import org.marketdesignresearch.mechlib.core.bid.Bids;
import org.marketdesignresearch.mechlib.core.bid.SingleItemBids;
import org.marketdesignresearch.mechlib.core.bidder.Bidder;
import org.marketdesignresearch.mechlib.core.bidder.XORBidder;
import org.marketdesignresearch.mechlib.core.Outcome;
import org.marketdesignresearch.mechlib.outcomerules.OutcomeRuleGenerator;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SecondPriceRuleTest {

    private SimpleGood item;
    private XORBidder bidder1;
    private XORBidder bidder2;
    private XORBidder bidder3;

    @Before
    public void setUp() {
        item = new SimpleGood("item");
        bidder1 = new XORBidder("B" + 1);
        bidder2 = new XORBidder("B" + 2);
        bidder3 = new XORBidder("B" + 3);
    }

    @Test
    public void testSimpleSecondPriceAuction() {
        BundleBid bid1 = new BundleBid(BigDecimal.valueOf(7), Sets.newHashSet(item), "1");
        BundleBid bid2 = new BundleBid(BigDecimal.valueOf(10), Sets.newHashSet(item), "2");
        BundleBid bid3 = new BundleBid(BigDecimal.valueOf(8), Sets.newHashSet(item), "3");
        Bids bids = new Bids();
        bids.setBid(bidder1, new Bid(Sets.newHashSet(bid1)));
        bids.setBid(bidder2, new Bid(Sets.newHashSet(bid2)));
        bids.setBid(bidder3, new Bid(Sets.newHashSet(bid3)));
        SingleItemBids singleItemBids = new SingleItemBids(bids);
        Outcome outcome = new SecondPriceRule(singleItemBids).getOutcome();
        checkResult(outcome, bidder2, bid2, BigDecimal.valueOf(8));
    }

    @Test
    public void testSimpleSecondPriceAuctionWithWrapper() {
        SimpleXORDomain domain = new SimpleXORDomain(Lists.newArrayList(bidder1, bidder2, bidder3), Lists.newArrayList(item));
        Auction auction = new Auction(domain, OutcomeRuleGenerator.SECOND_PRICE);

        BundleBid bid1 = new BundleBid(BigDecimal.valueOf(7), Sets.newHashSet(item), "1");
        BundleBid bid2 = new BundleBid(BigDecimal.valueOf(10), Sets.newHashSet(item), "2");
        BundleBid bid3 = new BundleBid(BigDecimal.valueOf(8), Sets.newHashSet(item), "3");
        Bids bids = new Bids();
        bids.setBid(bidder1, new Bid(Sets.newHashSet(bid1)));
        bids.setBid(bidder2, new Bid(Sets.newHashSet(bid2)));
        bids.setBid(bidder3, new Bid(Sets.newHashSet(bid3)));
        SingleItemBids singleItemBids = new SingleItemBids(bids);

        auction.addRound(singleItemBids);
        Outcome outcome = auction.getOutcome();
        checkResult(outcome, bidder2, bid2, BigDecimal.valueOf(8));
    }

    @Test
    public void testSecondPriceAuctionTwoWinningBids() {
        BundleBid bid1 = new BundleBid(BigDecimal.valueOf(10), Sets.newHashSet(item), "1");
        BundleBid bid2 = new BundleBid(BigDecimal.valueOf(10), Sets.newHashSet(item), "2");
        BundleBid bid3 = new BundleBid(BigDecimal.valueOf(8), Sets.newHashSet(item), "3");
        Bids bids = new Bids();
        bids.setBid(bidder1, new Bid(Sets.newHashSet(bid1)));
        bids.setBid(bidder2, new Bid(Sets.newHashSet(bid2)));
        bids.setBid(bidder3, new Bid(Sets.newHashSet(bid3)));
        SingleItemBids singleItemBids = new SingleItemBids(bids);
        Outcome outcome = new SecondPriceRule(singleItemBids).getOutcome();
        checkResult(outcome, bidder1, bid1, BigDecimal.TEN);
    }

    @Test
    public void testSecondPriceAuctionNoBidder() {
        SingleItemBids bids = new SingleItemBids(new Bids());
        Outcome outcome = new SecondPriceRule(bids).getOutcome();
        Allocation allocation = outcome.getAllocation();
        assertThat(allocation.getTotalAllocationValue()).isZero();
        Payment payment = outcome.getPayment();
        assertThat(payment.getTotalPayments()).isZero();
        assertThat(payment.getPaymentMap()).isEmpty();
    }

    @Test
    public void testSecondPriceAuctionSingleBidder() {
        BundleBid bid = new BundleBid(BigDecimal.valueOf(10), Sets.newHashSet(item), "1");

        Bids bids = new Bids();
        bids.setBid(bidder1, new Bid(Sets.newHashSet(bid)));
        SingleItemBids singleItemBids = new SingleItemBids(bids);
        Outcome outcome = new SecondPriceRule(singleItemBids).getOutcome();
        checkResult(outcome, bidder1, bid, BigDecimal.ZERO); // FIXME: Had accepted bid
    }

    @Test
    public void testInvalidSingleGoodAuction() {
        BundleBid bid1 = new BundleBid(BigDecimal.valueOf(2), Sets.newHashSet(item), "1");
        BundleBid bid2 = new BundleBid(BigDecimal.valueOf(10), Sets.newHashSet(new SimpleGood("item2")), "2");
        BundleBid bid3 = new BundleBid(BigDecimal.valueOf(15), Sets.newHashSet(item, new SimpleGood("item3", 2, false)), "3");

        // FIXME
        //assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> bid3.getBundle().getSingleGood());
        //assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> new Bundle(ImmutableMap.of(item, 3)).getSingleGood());

        Bids bidsOnTwoDifferentGoods = new Bids();
        bidsOnTwoDifferentGoods.setBid(bidder1, new Bid(Sets.newHashSet(bid1)));
        bidsOnTwoDifferentGoods.setBid(bidder2, new Bid(Sets.newHashSet(bid2)));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new SingleItemBids(bidsOnTwoDifferentGoods));

        Bids bidsOnComplexGood = new Bids();
        bidsOnComplexGood.setBid(bidder1, new Bid(Sets.newHashSet(bid1)));
        bidsOnComplexGood.setBid(bidder3, new Bid(Sets.newHashSet(bid3)));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new SingleItemBids(bidsOnComplexGood));
    }

    private void checkResult(Outcome outcome, Bidder expectedWinner, BundleBid expectedWinningBid, BigDecimal expectedPayment) {
        Set<Bidder> losers = Sets.newHashSet(bidder1, bidder2, bidder3);
        losers.remove(expectedWinner);
        Allocation allocation = outcome.getAllocation();
        Payment payment = outcome.getPayment();
        assertThat(allocation.getTotalAllocationValue()).isEqualTo(expectedWinningBid.getAmount());
        for (Bidder bidder : losers) {
            assertThat(allocation.allocationOf(bidder).getAcceptedBids()).isEmpty();
            assertThat(allocation.allocationOf(bidder).getValue()).isZero();
            assertThat(allocation.allocationOf(bidder).getBundle().getBundleEntries()).isEmpty();
            assertThat(payment.paymentOf(bidder).getAmount()).isZero();
        }

        assertThat(allocation.allocationOf(expectedWinner).getAcceptedBids()).hasSize(1);
        BundleBid winningBid = allocation.allocationOf(expectedWinner).getAcceptedBids().iterator().next();
        assertThat(winningBid.getId()).isEqualTo(expectedWinningBid.getId());
        assertThat(winningBid.getAmount()).isEqualTo(expectedWinningBid.getAmount());
        assertThat(winningBid.getBundle().getSingleGood()).isEqualTo(item);
        assertThat(allocation.allocationOf(expectedWinner).getBundle().getBundleEntries()).hasSize(1);
        assertThat(allocation.allocationOf(expectedWinner).getBundle().getBundleEntries().iterator().next().getGood()).isEqualTo(item);
        assertThat(allocation.allocationOf(expectedWinner).getValue()).isEqualTo(BigDecimal.TEN);

        assertThat(payment.getTotalPayments()).isEqualTo(expectedPayment);
        assertThat(payment.paymentOf(expectedWinner).getAmount()).isEqualTo(expectedPayment);

    }

}
