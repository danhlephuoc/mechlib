package org.marketdesignresearch.mechlib.mechanisms.ccg.constraintgeneration;

import org.marketdesignresearch.mechlib.domain.Allocation;
import org.marketdesignresearch.mechlib.mechanisms.MechanismResult;

public interface ConstraintGenerator {
    /**
     * 
     * @param blockingCoalition
     * @return the ids of the added constraints
     */
    void addConstraint(Allocation blockingCoalition, MechanismResult priorResult);
}
