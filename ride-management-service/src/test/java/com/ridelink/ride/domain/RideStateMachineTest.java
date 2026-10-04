package com.ridelink.ride.domain;

import com.ridelink.ride.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link RideStateMachine}.
 *
 * <p>Tests every documented valid transition AND a representative set of
 * invalid transitions to ensure the 409-producing guard works correctly.
 */
@DisplayName("RideStateMachine")
class RideStateMachineTest {

    // ── Valid transitions ─────────────────────────────────────────────

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "REQUESTED,   ASSIGNED",
            "REQUESTED,   CANCELLED",
            "ASSIGNED,    ACCEPTED",
            "ASSIGNED,    CANCELLED",
            "ASSIGNED,    REQUESTED",
            "ACCEPTED,    IN_PROGRESS",
            "ACCEPTED,    CANCELLED",
            "IN_PROGRESS, COMPLETED"
    })
    @DisplayName("Valid transitions should not throw")
    void validTransitions(String from, String to) {
        assertThatCode(() ->
                RideStateMachine.transition(RideStatus.valueOf(from), RideStatus.valueOf(to))
        ).doesNotThrowAnyException();
    }

    // ── Invalid transitions ───────────────────────────────────────────

    @ParameterizedTest(name = "{0} → {1} should be rejected")
    @CsvSource({
            "REQUESTED,   IN_PROGRESS",
            "REQUESTED,   COMPLETED",
            "REQUESTED,   ACCEPTED",
            "ASSIGNED,    COMPLETED",
            "ASSIGNED,    IN_PROGRESS",
            "ACCEPTED,    REQUESTED",
            "ACCEPTED,    ASSIGNED",
            "IN_PROGRESS, REQUESTED",
            "IN_PROGRESS, ASSIGNED",
            "IN_PROGRESS, ACCEPTED",
            "IN_PROGRESS, CANCELLED",
            "COMPLETED,   REQUESTED",
            "COMPLETED,   CANCELLED",
            "CANCELLED,   REQUESTED",
            "CANCELLED,   COMPLETED"
    })
    @DisplayName("Invalid transitions should throw InvalidStatusTransitionException")
    void invalidTransitions(String from, String to) {
        assertThatThrownBy(() ->
                RideStateMachine.transition(RideStatus.valueOf(from), RideStatus.valueOf(to))
        )
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining(from)
                .hasMessageContaining(to);
    }

    // ── Terminal state check ───────────────────────────────────────────

    @Test
    @DisplayName("COMPLETED is terminal")
    void completedIsTerminal() {
        assertThat(RideStateMachine.isTerminal(RideStatus.COMPLETED)).isTrue();
    }

    @Test
    @DisplayName("CANCELLED is terminal")
    void cancelledIsTerminal() {
        assertThat(RideStateMachine.isTerminal(RideStatus.CANCELLED)).isTrue();
    }

    @Test
    @DisplayName("Non-terminal states are not terminal")
    void nonTerminalStates() {
        for (RideStatus s : new RideStatus[]{
                RideStatus.REQUESTED, RideStatus.ASSIGNED, RideStatus.ACCEPTED, RideStatus.IN_PROGRESS
        }) {
            assertThat(RideStateMachine.isTerminal(s))
                    .as("Expected %s to be non-terminal", s).isFalse();
        }
    }
}
