package org.marketdesignresearch.mechlib.outcomerules.ccg.paymentrules;

import org.marketdesignresearch.mechlib.metainfo.MetaInfo;
import com.google.common.collect.ImmutableList;
import edu.harvard.econcs.jopt.solver.mip.Constraint;

import java.util.List;

public class PrimaryObjective {
    private final List<Constraint> constraints;
    private final MetaInfo metaInfo;

    public PrimaryObjective(Constraint constraint, MetaInfo metaInfo) {
        this(ImmutableList.of(constraint), metaInfo);
    }

    public PrimaryObjective(List<Constraint> constraints, MetaInfo metaInfo) {
        this.constraints = constraints;
        this.metaInfo = metaInfo;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public MetaInfo getMetaInfo() {
        return metaInfo;
    }
}
