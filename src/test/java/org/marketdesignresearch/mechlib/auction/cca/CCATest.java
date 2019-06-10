package org.marketdesignresearch.mechlib.auction.cca;

import org.marketdesignresearch.mechlib.domain.*;
import org.marketdesignresearch.mechlib.domain.bid.Bids;
import org.marketdesignresearch.mechlib.domain.cats.CATSAdapter;
import org.marketdesignresearch.mechlib.domain.cats.CATSAuction;
import org.marketdesignresearch.mechlib.domain.cats.CATSParser;
import org.marketdesignresearch.mechlib.mechanisms.MechanismResult;
import org.marketdesignresearch.mechlib.mechanisms.MechanismType;
import org.marketdesignresearch.mechlib.auction.cca.priceupdate.PriceUpdater;
import org.marketdesignresearch.mechlib.auction.cca.priceupdate.SimpleRelativePriceUpdate;
import org.marketdesignresearch.mechlib.auction.cca.bidcollection.supplementaryround.ProfitMaximizingSupplementaryRound;
import org.marketdesignresearch.mechlib.mechanisms.vcg.VCGMechanism;
import org.marketdesignresearch.mechlib.mechanisms.vcg.XORVCGMechanism;
import org.marketdesignresearch.mechlib.winnerdetermination.XORWinnerDetermination;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.data.Offset;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Slf4j
public class CCATest {

    private static SimpleXORDomain domain;

    @BeforeClass
    public static void setUp() throws IOException {
        Path catsFile = Paths.get("src/test/resources/hard0000.txt");
        CATSParser parser = new CATSParser();
        CATSAuction catsAuction = parser.readCatsAuctionBean(catsFile);
        CATSAdapter adapter = new CATSAdapter();
        domain = adapter.adaptToDomain(catsAuction);
    }

    @Test
    public void testCCAWithCATSAuction() {
        CCAuction cca = new CCAuction(domain);
        PriceUpdater priceUpdater = new SimpleRelativePriceUpdate().withInitialUpdate(BigDecimal.TEN);
        cca.setPriceUpdater(priceUpdater);
        cca.addSupplementaryRound(new ProfitMaximizingSupplementaryRound(cca).withNumberOfSupplementaryBids(3));
        MechanismResult mechanismResult = cca.getMechanismResult();
        assertThat(mechanismResult.getAllocation().getTotalAllocationValue().doubleValue()).isEqualTo(8240.2519, Offset.offset(1e-4));
        log.info(mechanismResult.toString());
    }

    @Test
    public void testCCAWithCATSAuctionAndVCG() {
        CCAuction cca = new CCAuction(domain, MechanismType.VCG_XOR);
        PriceUpdater priceUpdater = new SimpleRelativePriceUpdate().withInitialUpdate(BigDecimal.TEN);
        cca.setPriceUpdater(priceUpdater);
        cca.addSupplementaryRound(new ProfitMaximizingSupplementaryRound(cca).withNumberOfSupplementaryBids(3));
        MechanismResult mechanismResult = cca.getMechanismResult();
        assertThat(mechanismResult.getAllocation().getTotalAllocationValue().doubleValue()).isEqualTo(8240.2519, Offset.offset(1e-4));
        log.info(mechanismResult.toString());
    }

    @Test
    public void testRoundAfterRoundCCAWithCATSAuction() {
        VCGMechanism auction = new XORVCGMechanism(Bids.fromXORBidders(domain.getBidders()));
        MechanismResult resultIncludingAllBids = auction.getMechanismResult();

        CCAuction cca = new CCAuction(domain);
        PriceUpdater priceUpdater = new SimpleRelativePriceUpdate().withInitialUpdate(BigDecimal.TEN);
        cca.setPriceUpdater(priceUpdater);
        cca.addSupplementaryRound(new ProfitMaximizingSupplementaryRound(cca).withNumberOfSupplementaryBids(2));
        cca.addSupplementaryRound(new ProfitMaximizingSupplementaryRound(cca).withNumberOfSupplementaryBids(3));
        cca.addSupplementaryRound(new ProfitMaximizingSupplementaryRound(cca).withNumberOfSupplementaryBids(4));
        Allocation previousAllocation = Allocation.EMPTY_ALLOCATION;
        while (!cca.isClockPhaseCompleted()) {
            cca.nextClockRound();
            Allocation allocation = new XORWinnerDetermination(cca.getLatestAggregatedBids()).getAllocation();
            assertThat(allocation.getTotalAllocationValue()).isLessThanOrEqualTo(resultIncludingAllBids.getAllocation().getTotalAllocationValue());
            assertThat(allocation.getTotalAllocationValue()).isGreaterThanOrEqualTo(previousAllocation.getTotalAllocationValue());
            previousAllocation = allocation;
        }
        while (cca.hasNextSupplementaryRound()) {
            cca.nextSupplementaryRound();
            Allocation allocation = new XORWinnerDetermination(cca.getLatestAggregatedBids()).getAllocation();
            assertThat(allocation.getTotalAllocationValue()).isLessThanOrEqualTo(resultIncludingAllBids.getAllocation().getTotalAllocationValue());
            assertThat(allocation.getTotalAllocationValue()).isGreaterThanOrEqualTo(previousAllocation.getTotalAllocationValue());
            previousAllocation = allocation;
        }
        MechanismResult mechanismResult = cca.getMechanismResult();
        assertThat(mechanismResult.getAllocation().getTotalAllocationValue().doubleValue()).isEqualTo(8240.2519, Offset.offset(1e-4));
        assertThat(mechanismResult.getAllocation()).isEqualTo(previousAllocation);
        log.info(mechanismResult.toString());
    }

    @Test
    public void testResettingCCAWithCATSAuction() {
        CCAuction cca = new CCAuction(domain);
        PriceUpdater priceUpdater = new SimpleRelativePriceUpdate().withInitialUpdate(BigDecimal.TEN);
        cca.setPriceUpdater(priceUpdater);
        cca.addSupplementaryRound(new ProfitMaximizingSupplementaryRound(cca).withNumberOfSupplementaryBids(2));
        cca.addSupplementaryRound(new ProfitMaximizingSupplementaryRound(cca).withNumberOfSupplementaryBids(3));
        cca.addSupplementaryRound(new ProfitMaximizingSupplementaryRound(cca).withNumberOfSupplementaryBids(4));
        cca.addSupplementaryRound(new ProfitMaximizingSupplementaryRound(cca).withNumberOfSupplementaryBids(3));
        MechanismResult first = cca.getMechanismResult();
        assertThat(cca.isClockPhaseCompleted()).isTrue();
        assertThat(cca.hasNextSupplementaryRound()).isFalse();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> cca.resetToRound(50));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> cca.resetToRound(28));
        cca.resetToRound(25);
        assertThat(cca.isClockPhaseCompleted()).isFalse();
        assertThat(cca.hasNextSupplementaryRound()).isTrue();

        VCGMechanism auction = new XORVCGMechanism(cca.getLatestAggregatedBids());
        MechanismResult intermediate = auction.getMechanismResult();
        assertThat(intermediate.getAllocation().getTotalAllocationValue())
                .isLessThan(first.getAllocation().getTotalAllocationValue());

        MechanismResult second = cca.getMechanismResult();
        assertThat(cca.isClockPhaseCompleted()).isTrue();
        assertThat(cca.hasNextSupplementaryRound()).isFalse();
        assertThat(second.getAllocation().getTotalAllocationValue())
                .isEqualTo(first.getAllocation().getTotalAllocationValue());

    }


}