package ch.uzh.ifi.ce.mechanisms.singleitem;

import ch.uzh.ifi.ce.domain.*;
import ch.uzh.ifi.ce.mechanisms.AuctionResult;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

public class SecondPriceAuctionTest {

    private Good item;
    private Bidder bidder1;
    private Bidder bidder2;
    private Bidder bidder3;

    @Before
    public void setUp() {
        item = new SimpleGood("item");
        bidder1 = new Bidder("B" + 1);
        bidder2 = new Bidder("B" + 2);
        bidder3 = new Bidder("B" + 3);
    }

    @Test
    public void testSimpleSecondPriceAuction() {
        BundleBid bid1A = new BundleBid(BigDecimal.valueOf(2), Sets.newHashSet(item), "1A");
        BundleBid bid1B = new BundleBid(BigDecimal.valueOf(7), Sets.newHashSet(item), "1B");
        BundleBid bid2A = new BundleBid(BigDecimal.valueOf(1), Sets.newHashSet(item), "2A");
        BundleBid bid2B = new BundleBid(BigDecimal.valueOf(10), Sets.newHashSet(item), "2B");
        BundleBid bid3A = new BundleBid(BigDecimal.valueOf(3), Sets.newHashSet(item), "3A");
        BundleBid bid3B = new BundleBid(BigDecimal.valueOf(1), Sets.newHashSet(item), "3B");
        BundleBid bid3C = new BundleBid(BigDecimal.valueOf(8), Sets.newHashSet(item), "3C");
        Bids bids = new Bids();
        bids.setBid(bidder1, new Bid(Sets.newHashSet(bid1A, bid1B)));
        bids.setBid(bidder2, new Bid(Sets.newHashSet(bid2A, bid2B)));
        bids.setBid(bidder3, new Bid(Sets.newHashSet(bid3A, bid3B, bid3C)));
        SingleItemAuctionInstance auctionInstance = new SingleItemAuctionInstance(bids);
        AuctionResult auctionResult = new SecondPriceAuction(auctionInstance).getAuctionResult();
        checkResult(auctionResult, bidder2, bid2B, BigDecimal.valueOf(8));
    }

    @Test
    public void testSecondPriceAuctionTwoWinningBids() {
        BundleBid bid1A = new BundleBid(BigDecimal.valueOf(2), Sets.newHashSet(item), "1A");
        BundleBid bid1B = new BundleBid(BigDecimal.valueOf(10), Sets.newHashSet(item), "1B");
        BundleBid bid2A = new BundleBid(BigDecimal.valueOf(1), Sets.newHashSet(item), "2A");
        BundleBid bid2B = new BundleBid(BigDecimal.valueOf(10), Sets.newHashSet(item), "2B");
        BundleBid bid3A = new BundleBid(BigDecimal.valueOf(3), Sets.newHashSet(item), "3A");
        BundleBid bid3B = new BundleBid(BigDecimal.valueOf(1), Sets.newHashSet(item), "3B");
        BundleBid bid3C = new BundleBid(BigDecimal.valueOf(8), Sets.newHashSet(item), "3C");
        Bids bids = new Bids();
        bids.setBid(bidder1, new Bid(Sets.newHashSet(bid1A, bid1B)));
        bids.setBid(bidder2, new Bid(Sets.newHashSet(bid2A, bid2B)));
        bids.setBid(bidder3, new Bid(Sets.newHashSet(bid3A, bid3B, bid3C)));
        SingleItemAuctionInstance auctionInstance = new SingleItemAuctionInstance(bids);
        AuctionResult auctionResult = new SecondPriceAuction(auctionInstance).getAuctionResult();
        checkResult(auctionResult, bidder1, bid1B, BigDecimal.TEN);
    }

    @Test
    public void testSecondPriceAuctionNoBidder() {
        SingleItemAuctionInstance auctionInstance = new SingleItemAuctionInstance(new Bids());
        AuctionResult auctionResult = new SecondPriceAuction(auctionInstance).getAuctionResult();
        Allocation allocation = auctionResult.getAllocation();
        assertThat(allocation.getTotalAllocationValue()).isZero();
        Payment payment = auctionResult.getPayment();
        assertThat(payment.getTotalPayments()).isZero();
        assertThat(payment.getPaymentMap()).isEmpty();
    }

    @Test
    public void testSecondPriceAuctionSingleBidder() {
        BundleBid bid1A = new BundleBid(BigDecimal.valueOf(2), Sets.newHashSet(item), "1A");
        BundleBid bid1B = new BundleBid(BigDecimal.valueOf(10), Sets.newHashSet(item), "1B");
        BundleBid bid2A = new BundleBid(BigDecimal.valueOf(1), Sets.newHashSet(item), "2A");
        BundleBid bid2B = new BundleBid(BigDecimal.valueOf(10), Sets.newHashSet(item), "2B");
        BundleBid bid3A = new BundleBid(BigDecimal.valueOf(3), Sets.newHashSet(item), "3A");
        BundleBid bid3B = new BundleBid(BigDecimal.valueOf(1), Sets.newHashSet(item), "3B");
        BundleBid bid3C = new BundleBid(BigDecimal.valueOf(8), Sets.newHashSet(item), "3C");
        Bids bids = new Bids();
        bids.setBid(bidder1, new Bid(Sets.newHashSet(bid1A, bid1B, bid2A, bid2B, bid3A, bid3B, bid3C)));
        SingleItemAuctionInstance auctionInstance = new SingleItemAuctionInstance(bids);
        AuctionResult auctionResult = new SecondPriceAuction(auctionInstance).getAuctionResult();
        checkResult(auctionResult, bidder1, bid2B, BigDecimal.ZERO);
    }

    @Test
    public void testInvalidSingleGoodAuction() {
        BundleBid bid1 = new BundleBid(BigDecimal.valueOf(2), Sets.newHashSet(item), "1");
        BundleBid bid2 = new BundleBid(BigDecimal.valueOf(10), Sets.newHashSet(new SimpleGood(false, "item2")), "2");
        BundleBid bid3 = new BundleBid(BigDecimal.valueOf(15), Sets.newHashSet(item, new SimpleGood(false, "item3", 2)), "3");

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> bid3.getBundle().getSingleGood());
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> new Bundle(ImmutableMap.of(item, 3)).getSingleGood());

        Bids bidsOnTwoDifferentGoods = new Bids();
        bidsOnTwoDifferentGoods.setBid(bidder1, new Bid(Sets.newHashSet(bid1)));
        bidsOnTwoDifferentGoods.setBid(bidder2, new Bid(Sets.newHashSet(bid2)));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new SingleItemAuctionInstance(bidsOnTwoDifferentGoods));

        Bids bidsOnComplexGood = new Bids();
        bidsOnComplexGood.setBid(bidder1, new Bid(Sets.newHashSet(bid1)));
        bidsOnComplexGood.setBid(bidder3, new Bid(Sets.newHashSet(bid3)));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new SingleItemAuctionInstance(bidsOnComplexGood));
    }

    private void checkResult(AuctionResult auctionResult, Bidder expectedWinner, BundleBid expectedWinningBid, BigDecimal expectedPayment) {
        Set<Bidder> losers = Sets.newHashSet(bidder1, bidder2, bidder3);
        losers.remove(expectedWinner);
        Allocation allocation = auctionResult.getAllocation();
        Payment payment = auctionResult.getPayment();
        assertThat(allocation.getTotalAllocationValue()).isEqualTo(expectedWinningBid.getAmount());
        for (Bidder bidder : losers) {
            assertThat(allocation.allocationOf(bidder).getAcceptedBids()).isEmpty();
            assertThat(allocation.allocationOf(bidder).getValue()).isZero();
            assertThat(allocation.allocationOf(bidder).getGoodsMap()).isEmpty();
            assertThat(payment.paymentOf(bidder).getAmount()).isZero();
        }

        assertThat(allocation.allocationOf(expectedWinner).getAcceptedBids()).hasSize(1);
        BundleBid winningBid = allocation.allocationOf(expectedWinner).getAcceptedBids().iterator().next();
        assertThat(winningBid.getId()).isEqualTo(expectedWinningBid.getId());
        assertThat(winningBid.getAmount()).isEqualTo(expectedWinningBid.getAmount());
        assertThat(winningBid.getBundle().getSingleGood()).isEqualTo(item);
        assertThat(allocation.allocationOf(expectedWinner).getGoodsMap()).hasSize(1);
        assertThat(allocation.allocationOf(expectedWinner).getGoodsMap().keySet().iterator().next()).isEqualTo(item);
        assertThat(allocation.allocationOf(expectedWinner).getValue()).isEqualTo(BigDecimal.TEN);

        assertThat(payment.getTotalPayments()).isEqualTo(expectedPayment);
        assertThat(payment.paymentOf(expectedWinner).getAmount()).isEqualTo(expectedPayment);

    }

}
