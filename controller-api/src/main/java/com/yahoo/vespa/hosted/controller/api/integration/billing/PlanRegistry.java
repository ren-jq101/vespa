package com.yahoo.vespa.hosted.controller.api.integration.billing;

import java.util.Optional;

/**
 * Registry of all current plans we have support for
 *
 * @author ogronnesby
 */
public interface PlanRegistry {

    /** Get the default plan */
    Plan defaultPlan();

    /** Get a plan given a plan ID */
    Optional<Plan> plan(PlanId planId);

    /** Get a plan give a plan ID */
    default Optional<Plan> plan(String planId) {
        if (planId == null || planId.isBlank())
            return Optional.empty();
        return plan(PlanId.from(planId));
    }

}
