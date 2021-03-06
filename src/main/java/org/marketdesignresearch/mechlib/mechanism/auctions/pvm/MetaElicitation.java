package org.marketdesignresearch.mechlib.mechanism.auctions.pvm;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.marketdesignresearch.mechlib.core.bid.Bids;
import org.marketdesignresearch.mechlib.core.bidder.Bidder;
import org.marketdesignresearch.mechlib.core.bidder.valuefunction.ValueFunction;
import org.marketdesignresearch.mechlib.mechanism.auctions.pvm.ml.MLAlgorithm;
import org.springframework.data.annotation.PersistenceConstructor;

import java.util.*;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@PersistenceConstructor}))
@ToString
@EqualsAndHashCode
public class MetaElicitation {
    private final Map<UUID, MLAlgorithm> algorithms;
    private final Set<Bidder> bidders;

    public MetaElicitation(Map<Bidder, MLAlgorithm> bidderMLAlgorithmMap) {
        this.bidders = new HashSet<>(bidderMLAlgorithmMap.keySet());
        this.algorithms = new HashMap<>();
        bidderMLAlgorithmMap.forEach((k, v) -> this.algorithms.put(k.getId(), v));
    }

    public Map<Bidder, ValueFunction> process(Bids bids) {
        Map<Bidder, ValueFunction> inferredValues = new HashMap<>();
        for (Map.Entry<UUID, MLAlgorithm> entry : algorithms.entrySet()) {
            if (bids.getBid(getBidder(entry.getKey())) != null) {
                entry.getValue().addReport(bids.getBid(getBidder(entry.getKey())));
            }
            inferredValues.put(getBidder(entry.getKey()), entry.getValue().inferValueFunction());
        }
        return inferredValues;
    }

    private Bidder getBidder(UUID id) {
        return bidders.stream().filter(b -> b.getId().equals(id)).findAny().orElseThrow(NoSuchElementException::new);
    }

}
