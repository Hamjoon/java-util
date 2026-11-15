package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.Date;
import static com.cedarsoftware.util.UniqueIdGenerator.getDate19;
import static com.cedarsoftware.util.UniqueIdGenerator.getUniqueId19;
import static java.lang.Math.abs;
import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UniqueIdGenerator_getDate19_Test {

    /**
     * Supplies the argument stream for {@link #testGetDate19_VariousValues}.
     *
     * The data mirrors the JSON test cases.  The expected milliseconds are
     * calculated here (rather than hard‑coding the numbers) to avoid transcription
     * errors and to keep the test logic aligned with the implementation contract.
     */
    private static Stream<Arguments> provideUniqueIds() {
        return Stream.of(/* Zero and Small Positive Values */
        Arguments.of(0L, 0L / 1_000_000L, "When uniqueId is 0 the division yields 0, so the method should return a Date at the epoch."), Arguments.of(999_999L, 999_999L / 1_000_000L, "uniqueId just below one million still truncates to 0 after division, expecting the epoch Date."), Arguments.of(1_000_000L, 1_000_000L / 1_000_000L, "Exactly one million yields 1 after division, so the Date should be 1 ms after epoch."), Arguments.of(1_500_000L, 1_500_000L / 1_000_000L, "1.5 million truncates to 1, confirming integer division behavior."), /* Large Positive Values */
        Arguments.of(9_223_372_036_854_775_807L, 9_223_372_036_854_775_807L / 1_000_000L, "Maximum long value divided by 1,000,000 should not overflow and produce a valid Date with the calculated milliseconds."), Arguments.of(1_234_567_890_123L, 1_234_567_890_123L / 1_000_000L, "A typical large identifier to verify correct conversion to milliseconds."), /* Negative Values */
        Arguments.of(-1L, -1L / 1_000_000L, "Negative values whose absolute magnitude is less than 1,000,000 truncate toward zero, resulting in epoch Date."), Arguments.of(-999_999L, -999_999L / 1_000_000L, "Negative just below one million still truncates to 0."), Arguments.of(-1_000_000L, -1_000_000L / 1_000_000L, "Exactly -1,000,000 yields -1 after division, producing a Date one millisecond before the epoch."), Arguments.of(-1_500_000L, -1_500_000L / 1_000_000L, "-1.5 million truncates toward zero to -1, confirming integer division for negatives."), /* Exact Multiples of One Million */
        Arguments.of(2_000_000L, 2_000_000L / 1_000_000L, "A positive multiple of 1,000,000 should map directly to the same number of milliseconds."), Arguments.of(10_000_000L, 10_000_000L / 1_000_000L, "Larger positive multiple to verify correct scaling."), Arguments.of(-2_000_000L, -2_000_000L / 1_000_000L, "Negative multiple should produce a Date before the epoch."), /* Just Above Multiples (Truncation Check) */
        Arguments.of(2_000_001L, 2_000_001L / 1_000_000L, "Value just above a multiple must truncate toward zero, yielding the same milliseconds as the exact multiple."), Arguments.of(2_999_999L, 2_999_999L / 1_000_000L, "Maximum value that still truncates to 2 milliseconds."), Arguments.of(-2_000_001L, -2_000_001L / 1_000_000L, "Negative value just below a multiple truncates toward zero, keeping the same negative millisecond count."), /* Extreme Long Values */
        Arguments.of(9_223_372_036_854_775_807L, 9_223_372_036_854_775_807L / 1_000_000L, "Maximum long value divided by 1,000,000; verifies no overflow and correct date conversion."), Arguments.of(-9_223_372_036_854_775_808L, -9_223_372_036_854_775_808L / 1_000_000L, "Minimum long value divided by 1,000,000; checks handling of the most negative identifier."), /* Non‑Multiple Large Values */
        Arguments.of(5_555_555_555_555L, 5_555_555_555_555L / 1_000_000L, "Large positive identifier that is not a multiple; confirms integer division truncates correctly."), Arguments.of(-5_555_555_555_555L, -5_555_555_555_555L / 1_000_000L, "Large negative identifier that is not a multiple; verifies correct negative truncation."), /* Boundary Around One Million */
        Arguments.of(1_000_001L, 1_000_001L / 1_000_000L, "Just above the first million; ensures the method still returns 1 ms after truncation."), Arguments.of(-1_000_001L, -1_000_001L / 1_000_000L, "Just below the negative one‑million boundary; confirms truncation to -1 ms."));
    }

    /**
     * Verify conversion for a deterministic ID value.
     */
    @Test
    void testGetDate19_WithKnownId() {
        // Choose an arbitrary ID where the timestamp part is obvious.
        // For example, 1_600_000_000_000_000L (2020‑09‑13T12:26:40Z) * 1_000_000
        // 2020‑09‑13 12:26:40 UTC
        long knownTimestampMs = 1_600_000_000_000L;
        // sequence & serverId are zero
        long uniqueId = knownTimestampMs * 1_000_000L;
        Date date = getDate19(uniqueId);
        assertNotNull(date, "Date should not be null");
        assertEquals(knownTimestampMs, date.getTime(), "Date time should equal the timestamp encoded in the ID");
    }

    /**
     * Verify conversion for a freshly generated ID – the result should be close to now.
     */
    @Test
    void testGetDate19_WithGeneratedId() {
        // generate a fresh 19‑digit ID
        long id = getUniqueId19();
        // convert to Date
        Date date = getDate19(id);
        assertNotNull(date, "Date should not be null");
        long now = currentTimeMillis();
        // The generator uses the current millisecond timestamp, so the difference
        // should be very small (a few milliseconds at most).
        assertTrue(abs(date.getTime() - now) < 10, "Date derived from generated ID should be within a few ms of the current time");
    }

    /**
     * Edge case: ID of zero should map to the epoch (1970‑01‑01T00:00:00Z).
     */
    @Test
    void testGetDate19_WithZeroId() {
        Date date = getDate19(0L);
        assertNotNull(date, "Date should not be null");
        assertEquals(0L, date.getTime(), "Zero ID should correspond to the epoch (time = 0)");
    }

    /**
     * Parameterised test that iterates over all groups / cases supplied in the
     * JSON document.  For each {@code uniqueId} we compute the expected epoch
     * milliseconds using the same integer‑division rule that the production
     * method uses, then verify that {@link UniqueIdGenerator#getDate19(long)}
     * returns a {@link Date} with exactly that timestamp.
     *
     * @param uniqueId   the raw identifier passed to {@code getDate19}
     * @param expectedMs the expected milliseconds since epoch after division
     * @param description textual description of the scenario (used as assertion message)
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideUniqueIds")
    void testGetDate19_VariousValues(long uniqueId, long expectedMs, String description) {
        Date date = getDate19(uniqueId);
        assertNotNull(date, "Date should never be null");
        assertEquals(expectedMs, date.getTime(), () -> "Failed for case: " + description + " (uniqueId=" + uniqueId + ")");
    }
}
