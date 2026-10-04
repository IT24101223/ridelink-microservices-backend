package com.ridelink.ride.domain;

import com.ridelink.ride.exception.InvalidStatusTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Enforces the ride state machine in a single place (Single Responsibility).
 *
 * <p>Allowed transitions:
 * <pre>
 *   REQUESTED   → ASSIGNED | CANCELLED
 *   ASSIGNED    → ACCEPTED | CANCELLED | REQUESTED   (driver decline)
 *   ACCEPTED    → IN_PROGRESS | CANCELLED
 *   IN_PROGRESS → COMPLETED
 *   COMPLETED   → (terminal)
 *   CANCELLED   → (terminal)
 * </pre>
 *
 * <p>Any other transition raises {@link InvalidStatusTransitionException},
 * which maps to HTTP 409 Conflict in the global exception handler.
 */
public class RideStateMachine {

    /** Immutable map of allowed next states for each current state. */
    private static final Map<RideStatus, Set<RideStatus>> TRANSITIONS =
            new EnumMap<>(RideStatus.class);

    static {
        TRANSITIONS.put(RideStatus.REQUESTED,
                EnumSet.of(RideStatus.ASSIGNED, RideStatus.CANCELLED));

        TRANSITIONS.put(RideStatus.ASSIGNED,
                EnumSet.of(RideStatus.ACCEPTED, RideStatus.CANCELLED, RideStatus.REQUESTED));

        TRANSITIONS.put(RideStatus.ACCEPTED,
                EnumSet.of(RideStatus.IN_PROGRESS, RideStatus.CANCELLED));

        TRANSITIONS.put(RideStatus.IN_PROGRESS,
                EnumSet.of(RideStatus.COMPLETED));

        // Terminal states have no allowed transitions.
        TRANSITIONS.put(RideStatus.COMPLETED, EnumSet.noneOf(RideStatus.class));
        TRANSITIONS.put(RideStatus.CANCELLED,  EnumSet.noneOf(RideStatus.class));
    }

    private RideStateMachine() { /* utility – do not instantiate */ }

    /**
     * Validates and performs a transition.
     *
     * @param current the current {@link RideStatus}
     * @param target  the desired {@link RideStatus}
     * @throws InvalidStatusTransitionException if the transition is not allowed
     */
    public static void transition(RideStatus current, RideStatus target) {
        Set<RideStatus> allowed = TRANSITIONS.getOrDefault(current, EnumSet.noneOf(RideStatus.class));
        if (!allowed.contains(target)) {
            throw new InvalidStatusTransitionException(current, target);
        }
    }

    /**
     * Returns {@code true} if the given state is terminal (COMPLETED or CANCELLED).
     */
    public static boolean isTerminal(RideStatus status) {
        return status == RideStatus.COMPLETED || status == RideStatus.CANCELLED;
    }
}
