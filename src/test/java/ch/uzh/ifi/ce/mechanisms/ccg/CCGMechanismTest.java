package ch.uzh.ifi.ce.mechanisms.ccg;

import ch.uzh.ifi.ce.domain.*;
import ch.uzh.ifi.ce.domain.cats.Domain;
import ch.uzh.ifi.ce.mechanisms.ccg.blockingallocation.*;
import ch.uzh.ifi.ce.mechanisms.ccg.constraintgeneration.ConstraintGenerationAlgorithm;
import ch.uzh.ifi.ce.mechanisms.ccg.paymentrules.*;
import ch.uzh.ifi.ce.domain.cats.CATSAdapter;
import ch.uzh.ifi.ce.domain.cats.CATSAuction;
import ch.uzh.ifi.ce.domain.cats.CATSParser;
import ch.uzh.ifi.ce.mechanisms.AuctionMechanism;
import ch.uzh.ifi.ce.mechanisms.MechanismResult;
import ch.uzh.ifi.ce.mechanisms.ccg.referencepoint.VCGReferencePointFactory;
import ch.uzh.ifi.ce.utils.CPLEXUtils;
import ch.uzh.ifi.ce.utils.PrecisionUtils;
import com.google.common.collect.ImmutableList;
import org.assertj.core.data.Offset;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
@Ignore // Takes quite long and is not very relevant at currently
public class CCGMechanismTest {
    private final MechanismFactory factory;

    public CCGMechanismTest(MechanismFactory factory) {
        this.factory = factory;
        CPLEXUtils.SOLVER.initializeSolveParams();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> mechanisms() {
        MechanismFactory zeroMRCNorm = new VariableNormCCGFactory(new VCGReferencePointFactory(), new NormFactory(Norm.MANHATTAN, new EqualWeightsFactory(), Payment.ZERO),
                NormFactory.withEqualWeights(Norm.EUCLIDEAN));
      //  MechanismFactory equalNorm = new VariableNormCCGFactory(new BidsReferencePointFactory(), new NormFactory(Norm.MANHATTAN, new EqualWeightsFactory(), Payment.ZERO),
       //         NormFactory.withEqualWeights(Norm.EUCLIDEAN));

        MechanismFactory multipleSolutions = new VariableAlgorithmCCGFactory(new IntermediateSolutionsAllocationFinderFactory(MultiBlockingAllocationsDetermination.Mode.POOl_10),
                ConstraintGenerationAlgorithm.VALUE_SEPARABILITY);
        MechanismFactory orSeperability = new VariableAlgorithmCCGFactory(new OrStarBlockingCoalitionFinderFactory(), ConstraintGenerationAlgorithm.SEPARABILITY);
        MechanismFactory xorL2 = new VariableAlgorithmCCGFactory(new XORBlockingCoalitionFinderFactory(), ConstraintGenerationAlgorithm.STANDARD_CCG);
        MechanismFactory orL2 = new VariableAlgorithmCCGFactory(new OrStarBlockingCoalitionFinderFactory(), ConstraintGenerationAlgorithm.STANDARD_CCG);
        MechanismFactory orLinf = new VariableNormCCGFactory(new VCGReferencePointFactory(), Norm.MANHATTAN, Norm.MAXIMUM);
        MechanismFactory orLinfIterative = new VariableNormCCGFactory(new VCGReferencePointFactory(), Norm.MANHATTAN, Norm.ITERATIVE_MAXIMUM);
        MechanismFactory valueSeperation = new VariableAlgorithmCCGFactory(new OrStarBlockingCoalitionFinderFactory(), ConstraintGenerationAlgorithm.VALUE_SEPARABILITY);
        MechanismFactory xorLinf = new ConfigurableCCGFactory(new XORBlockingCoalitionFinderFactory(), new VCGReferencePointFactory(), ImmutableList.of(
                NormFactory.withEqualWeights(Norm.MANHATTAN), NormFactory.withEqualWeights(Norm.MAXIMUM)), ConstraintGenerationAlgorithm.SEPARABILITY);
        MechanismFactory xorSeperability = new VariableAlgorithmCCGFactory(new XORBlockingCoalitionFinderFactory(), ConstraintGenerationAlgorithm.SEPARABILITY);
        MechanismFactory orMaxTraitor = new VariableAlgorithmCCGFactory(new MaxTraitorOrBlockingCoalitionFinderFactory(), ConstraintGenerationAlgorithm.STANDARD_CCG);
        MechanismFactory xorMaxTraitor = new VariableAlgorithmCCGFactory(new MaxTraitorXORBlockingCoalitionFinderFactory(), ConstraintGenerationAlgorithm.STANDARD_CCG);
        MechanismFactory partialSeperability = new VariableAlgorithmCCGFactory(new OrStarBlockingCoalitionFinderFactory(), ConstraintGenerationAlgorithm.SEPARABILITY,
                ConstraintGenerationAlgorithm.PARTIAL_SEPARABILITY);
        MechanismFactory fullSeperability = new VariableAlgorithmCCGFactory(new OrStarBlockingCoalitionFinderFactory(), ConstraintGenerationAlgorithm.FULL);

        Object[][] data = new Object[][] { { zeroMRCNorm }, { orSeperability }, { multipleSolutions }, { xorL2 }, { orL2 }, { orLinf }, { orLinfIterative },
                { valueSeperation }, { xorLinf }, { xorSeperability }, { orMaxTraitor }, { xorMaxTraitor }, { partialSeperability }, { fullSeperability } };
        return Arrays.asList(data);
    }

    @Test
    public void testExample2007Paper() throws IOException {
        Path catsFileStream = Paths.get("src/test/resources/day2007example.txt");
        CATSParser parser = new CATSParser();
        CATSAuction catsAuction = parser.readCatsAuctionBean(catsFileStream);
        CATSAdapter adapter = new CATSAdapter();
        Domain domain = adapter.adaptToDomain(catsAuction);
        AuctionMechanism wd = factory.getMechanism(domain.toAuction());
        Payment result = wd.getPayment();
        // Compare to direct CPLEX result
        Bidder bidder0 = new Bidder("SB" + 0);
        Bidder bidder1 = new Bidder("SB" + 1);
        Bidder bidder2 = new Bidder("SB" + 2);
        Bidder bidder3 = new Bidder("SB" + 3);
        Offset<Double> offset = Offset.offset(0.00001);

        assertThat(result.getTotalPayments().doubleValue()).isEqualTo(38, offset);
        assertThat(result.paymentOf(bidder0).getAmount().doubleValue()).isEqualTo(16, offset);
        assertThat(result.paymentOf(bidder1).getAmount().doubleValue()).isEqualTo(12, offset);
        assertThat(result.paymentOf(bidder2).getAmount().doubleValue()).isEqualTo(10, offset);
        assertThat(result.paymentOf(bidder3).getAmount().doubleValue()).isZero();

        System.out.println(result.getMetaInfo());
    }

    @Test
    public void testExampleWurman2004Paper() throws IOException {
        Path catsFileStream = Paths.get("src/test/resources/wurman2004example.txt");
        CATSParser parser = new CATSParser();
        CATSAuction catsAuction = parser.readCatsAuctionBean(catsFileStream);
        CATSAdapter adapter = new CATSAdapter();
        Domain domain = adapter.adaptToDomain(catsAuction);
        AuctionMechanism wd = factory.getMechanism(domain.toAuction());
        Payment result = wd.getPayment();
        // Compare to direct CPLEX result
        Bidder bidder1 = new Bidder("DB" + 3);
        Bidder bidder2 = new Bidder("DB" + 4);
        Bidder bidder3 = new Bidder("DB" + 5);
        Bidder bidder4 = new Bidder("DB" + 6);
        System.out.println(wd.getAllocation());
        System.out.println(wd.getPayment());
        Offset<Double> offset = Offset.offset(PrecisionUtils.EPSILON.doubleValue());
        assertThat(result.getTotalPayments().doubleValue()).isEqualTo(25, offset);
        assertThat(result.paymentOf(bidder1).getAmount().doubleValue()).isEqualTo(7.5, offset);
        assertThat(result.paymentOf(bidder2).getAmount().add(result.paymentOf(bidder3).getAmount()).doubleValue()).isEqualTo(17.5, offset);
        assertThat(result.paymentOf(bidder4).getAmount().doubleValue()).isZero();
        System.out.println(result.getMetaInfo());
    }

    @Test
    public void testHardRealisticExample() throws IOException {
        Path catsFileStream = Paths.get("src/test/resources/hard0000.txt");
        CATSParser parser = new CATSParser();
        CATSAuction catsAuction = parser.readCatsAuctionBean(catsFileStream);
        CATSAdapter adapter = new CATSAdapter();
        Domain domain = adapter.adaptToDomain(catsAuction);
        AuctionMechanism wd = factory.getMechanism(domain.toAuction());
        Payment payment = wd.getPayment();
        Offset<Double> offset = Offset.offset(0.00001);
        assertThat(payment.getTotalPayments().doubleValue()).isEqualTo(7751.4898, offset);
        System.out.println(payment.getMetaInfo());
    }

    public void testHardUnrealisticExample() throws IOException {
        Path catsFileStream = Paths.get("src/test/resources/hardnodummys0000.txt");
        CATSParser parser = new CATSParser();
        CATSAuction catsAuction = parser.readCatsAuctionBean(catsFileStream);
        CATSAdapter adapter = new CATSAdapter();
        Domain domain = adapter.adaptToDomain(catsAuction);
        AuctionMechanism wd = factory.getMechanism(domain.toAuction());
        MechanismResult result = wd.getPayment();
        System.out.println(result.getMetaInfo());

    }

    @Test
    public void testQuadraticVsLinearExample() throws IOException {
        Path catsFile = Paths.get("src/test/resources/linearvsquadraticexample.txt");
        CATSParser parser = new CATSParser();
        CATSAuction catsAuction = parser.readCatsAuctionBean(catsFile);
        CATSAdapter adapter = new CATSAdapter();
        Domain domain = adapter.adaptToDomain(catsAuction);
        AuctionMechanism wd = factory.getMechanism(domain.toAuction());
        Payment payment = wd.getPayment();
        Offset<Double> offset = Offset.offset(PrecisionUtils.EPSILON.doubleValue());
        assertThat(payment.getTotalPayments().doubleValue()).isEqualTo(26, offset);
        assertThat(payment.paymentOf(new Bidder("SB" + 0)).getAmount().doubleValue()).isEqualTo(10, offset);
        assertThat(payment.paymentOf(new Bidder("SB" + 1)).getAmount().doubleValue()).isEqualTo(8, offset);
        assertThat(payment.paymentOf(new Bidder("SB" + 2)).getAmount().doubleValue() + payment.paymentOf(new Bidder("SB" + 3)).getAmount().doubleValue()).isEqualTo(8, offset);
        System.out.println(payment.getMetaInfo());

    }

    @Test
    public void fasterCCG() throws IOException {
        Path catsFileStream = Paths.get("src/test/resources/fasterccg.txt");
        CATSParser parser = new CATSParser();
        CATSAuction catsAuction = parser.readCatsAuctionBean(catsFileStream);
        CATSAdapter adapter = new CATSAdapter();
        Domain domain = adapter.adaptToDomain(catsAuction);
        AuctionMechanism wd = factory.getMechanism(domain.toAuction());
        Payment payment = wd.getPayment();
        Offset<Double> offset = Offset.offset(PrecisionUtils.EPSILON.doubleValue());
        assertThat(payment.getTotalPayments().doubleValue()).isEqualTo(10, offset);
        System.out.println(payment.getMetaInfo());
    }

    @Test
    public void fasterCCG2() throws IOException {
        Path catsFileStream = Paths.get("src/test/resources/ccgimprovement.txt");
        CATSParser parser = new CATSParser();
        CATSAuction catsAuction = parser.readCatsAuctionBean(catsFileStream);
        CATSAdapter adapter = new CATSAdapter();
        Domain domain = adapter.adaptToDomain(catsAuction);
        AuctionMechanism wd = factory.getMechanism(domain.toAuction());
        Payment payment = wd.getPayment();
        Offset<Double> offset = Offset.offset(PrecisionUtils.EPSILON.doubleValue());

        assertThat(payment.getTotalPayments().doubleValue()).isEqualTo(30, offset);
        System.out.println(payment.getMetaInfo());
    }

    @Test
    public void xorExample() throws IOException {
        Path catsFileStream = Paths.get("src/test/resources/xorbidder.txt");
        CATSParser parser = new CATSParser();
        CATSAuction catsAuction = parser.readCatsAuctionBean(catsFileStream);
        CATSAdapter adapter = new CATSAdapter();
        Domain domain = adapter.adaptToDomain(catsAuction);
        AuctionMechanism wd = factory.getMechanism(domain.toAuction());
        Payment payment = wd.getPayment();
        Offset<Double> offset = Offset.offset(PrecisionUtils.EPSILON.doubleValue());
        assertThat(payment.getTotalPayments().doubleValue()).isEqualTo(20, offset);
        System.out.println(payment.getMetaInfo());
    }

    @Test
    public void smallMultiConstraint() throws IOException {
        Path catsFileStream = Paths.get("src/test/resources/smallmulticonstraint.txt");
        CATSParser parser = new CATSParser();
        CATSAuction catsAuction = parser.readCatsAuctionBean(catsFileStream);
        CATSAdapter adapter = new CATSAdapter();
        Domain domain = adapter.adaptToDomain(catsAuction);
        AuctionMechanism wd = factory.getMechanism(domain.toAuction());
        Payment payment = wd.getPayment();
        Offset<Double> offset = Offset.offset(1e-6);
        assertThat(payment.getTotalPayments().doubleValue()).isEqualTo(3.3853760, offset);
        System.out.println(payment.getMetaInfo());
    }

    @Test
    public void testMultiConstraint() throws IOException {
        Path catsFileStream = Paths.get("src/test/resources/multiconstraint.txt");
        CATSParser parser = new CATSParser();
        CATSAuction catsAuction = parser.readCatsAuctionBean(catsFileStream);
        CATSAdapter adapter = new CATSAdapter();
        Domain domain = adapter.adaptToDomain(catsAuction);
        AuctionMechanism wd = factory.getMechanism(domain.toAuction());
        Payment result = wd.getPayment();
        Bidder bidder0 = new Bidder("DB" + 4);
        Bidder bidder1 = new Bidder("DB" + 5);
        Bidder bidder2 = new Bidder("SB" + 2);
        Bidder bidder3 = new Bidder("SB" + 1);

        assertThat(result.getTotalPayments().doubleValue()).isEqualTo(25);
        assertThat(result.paymentOf(bidder0).getAmount().doubleValue()).isEqualTo(5);
        assertThat(result.paymentOf(bidder1).getAmount().doubleValue()).isEqualTo(5);
        assertThat(result.paymentOf(bidder2).getAmount().doubleValue()).isEqualTo(15);
        assertThat(result.paymentOf(bidder3).getAmount().doubleValue()).isZero();
        System.out.println(result.getMetaInfo());
    }

    @Test
    public void testSuperSimple() throws IOException {
        Path catsFileStream = Paths.get("src/test/resources/lllg.txt");
        CATSParser parser = new CATSParser();
        CATSAuction catsAuction = parser.readCatsAuctionBean(catsFileStream);
        CATSAdapter adapter = new CATSAdapter();
        Domain domain = adapter.adaptToDomain(catsAuction);
        AuctionMechanism wd = factory.getMechanism(domain.toAuction());
        Payment result = wd.getPayment();
        Bidder bidder0 = new Bidder("SB" + 0);
        Bidder bidder1 = new Bidder("SB" + 1);
        assertThat(result.getTotalPayments().doubleValue()).isEqualTo(100);
        assertThat(result.paymentOf(bidder0).getAmount().doubleValue()).isEqualTo(60);
        assertThat(result.paymentOf(bidder1).getAmount().doubleValue()).isEqualTo(40);
        System.out.println(result.getMetaInfo());
    }
}
