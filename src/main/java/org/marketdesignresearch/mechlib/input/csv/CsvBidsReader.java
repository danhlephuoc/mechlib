package org.marketdesignresearch.mechlib.input.csv;

import com.opencsv.bean.CsvToBeanBuilder;
import org.marketdesignresearch.mechlib.core.*;
import org.marketdesignresearch.mechlib.core.bid.bundle.BundleValueBid;
import org.marketdesignresearch.mechlib.core.bid.bundle.BundleValueBids;
import org.marketdesignresearch.mechlib.core.bid.bundle.BundleValuePair;
import org.marketdesignresearch.mechlib.core.bidder.Bidder;
import org.marketdesignresearch.mechlib.core.bidder.XORBidder;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CsvBidsReader {

    /**
     *
     * @param   path - The path to the CSV file
     * @return  The bids that are represented by this CSV file, which can be further processed
     * @throws  FileNotFoundException if there is no file at this path
     */
    public static BundleValueBids<BundleValuePair> csvToXORBids(Path path) throws FileNotFoundException {
        // Maps the CSV rows to BidBeans
        List<BidBean> bidBeans = new CsvToBeanBuilder<BidBean>(new FileReader(path.toString()))
                .withType(BidBean.class).build().parse();
        if (bidBeans.isEmpty()) return new BundleValueBids<BundleValuePair>();
        // Collect the information about the items
        List<SimpleGood> goods = bidBeans.get(0).getGoods().keySet().stream().map(SimpleGood::new).collect(Collectors.toList());
        Map<Bidder, BundleValueBid<BundleValuePair>> bidMap = new HashMap<>();
        // Iterate through all BidBeans to collect the bids
        for (BidBean bidBean : bidBeans) {
            Bidder bidder = bidMap.keySet().stream().filter(b -> b.getName().equals(bidBean.getBidder())).findAny().orElse(new XORBidder(bidBean.getBidder()));
            Set<BundleEntry> bundleEntries = new HashSet<>();
            for (Good good : goods) {
                Collection<Integer> columnValues = bidBean.getGoods().get(good.getName());
                if (columnValues.size() != 1) {
                    throw new IllegalArgumentException("Bad input: " + bidBean);
                }
                int numberOfGoods = columnValues.iterator().next();
                if (numberOfGoods > 0) {
                    bundleEntries.add(new BundleEntry(good, numberOfGoods));
                }
            }
            BundleValuePair bundleBid = new BundleValuePair(bidBean.getBid(), new Bundle(bundleEntries), UUID.randomUUID().toString());
            BundleValueBid<BundleValuePair> bid = bidMap.getOrDefault(bidder, new BundleValueBid<BundleValuePair>());
            bid.addBundleBid(bundleBid);
            bidMap.put(bidder, bid);
        }

        return new BundleValueBids<BundleValuePair>(bidMap);
    }

}
