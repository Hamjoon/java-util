package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static com.cedarsoftware.util.UniqueIdGenerator.getDate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UniqueIdGenerator_getDate_Test {

    private static Stream<Arguments> provideGetDateCases() {
        return Stream.of(// Typical positive value
        Arguments.of(1234567890L, 12345L, "Typical positive value: 1 234 567 890 / 100 000 = 12 345 ms → Date representing 1970-01-01T00:00:12.345Z"), // Zero input
        Arguments.of(0L, 0L, "Zero input should yield epoch start (0 ms since 1970-01-01T00:00:00Z)"), // Negative value (truncates toward zero)
        Arguments.of(-500000L, -5L, "Negative value: -500 000 / 100 000 = -5 ms → Date before epoch (1969-12-31T23:59:59.995Z)"), // Maximum long value
        Arguments.of(Long.MAX_VALUE, 92233720368547L, "Maximum long value: division truncates to 92 233 720 368 547 ms → far future date (approx year 292278994)"), // Just below 100 000
        Arguments.of(99_999L, 0L, "Value just below 100 000 results in integer division yielding 0 ms → epoch start."), // Exact multiple of 100 000
        Arguments.of(100_000L, 1L, "Exact multiple of 100 000 gives 1 ms after epoch."));
    }

    private static Stream<Arguments> provideGetDateAdditionalCases() {
        return Stream.of(Arguments.of(1L, 0L, "Small positive value less than 100 000 yields 0 ms (epoch start)."), Arguments.of(-1L, 0L, "Small negative value greater than -100 000 truncates toward zero, resulting in 0 ms."), Arguments.of(-99_999L, 0L, "Negative value just above -100 000 also truncates to 0 ms."), Arguments.of(-100_000L, -1L, "Exact negative multiple of 100 000 yields -1 ms (one millisecond before epoch)."), Arguments.of(150_000L, 1L, "Positive value with remainder (150 000) still results in 1 ms after division."), Arguments.of(250_000L, 2L, "Positive value 250 000 yields 2 ms (250 000 / 100 000)."), Arguments.of(1_000_000L, 10L, "Larger positive value 1 000 000 produces 10 ms."), Arguments.of(Long.MIN_VALUE, -92233720368547L, "Long.MIN_VALUE division by 100 000 yields -92 233 720 368 547 ms, testing extreme negative boundary without overflow."));
    }

    @Test
    void getDate_returnsDateMatchingTimestampPortion() {
        // Arrange: create a realistic timestamp and embed it in a unique id.
        long currentMillis = System.currentTimeMillis();
        // any number < 1000
        long sequence = 123;
        // any number < 100
        long serverId = 42;
        // UniqueId format: timestampMs * 100_000 + sequence * 100 + serverId
        long uniqueId = currentMillis * 100_000L + sequence * 100L + serverId;
        // Act
        Date result = getDate(uniqueId);
        // Assert: the Date's epoch milliseconds must equal the original timestamp.
        assertEquals(currentMillis, result.getTime(), "Date returned by getDate should reflect the timestamp portion of the unique id");
    }

    @Test
    void getDate_withZeroId_returnsEpoch() {
        // Arrange
        long zeroId = 0L;
        // Act
        Date result = getDate(zeroId);
        // Assert
        assertEquals(0L, result.getTime(), "A unique id of 0 should map to the epoch (1970‑01‑01T00:00:00Z)");
    }

    @Test
    void getDate_isPureFunction_andDoesNotModifyInput() {
        // Arrange
        // arbitrary large value
        long original = 1_600_000_000_000_00L;
        // keep a copy to ensure immutability
        long copy = original;
        // Act
        Date date = getDate(original);
        // Assert
        assertNotNull(date, "Resulting Date should not be null");
        assertEquals(copy, original, "Input value must remain unchanged after calling getDate");
    }

    @Test
    void getDate_handlesMaximumLongValueWithoutOverflow() {
        // Arrange: use Long.MAX_VALUE, which is still a valid long.
        long maxId = Long.MAX_VALUE;
        // Act
        Date date = getDate(maxId);
        // Assert: the method should not throw and should produce a Date.
        // The exact epoch value is not asserted because it may be far in the future,
        // but we verify that the Date object is created.
        assertNotNull(date, "Date should be created even for the maximum long value");
    }

    /**
     * Test cases for the primary {@code getDate} group.
     *
     * Each case provides a {@code uniqueId} and the expected epoch‑millisecond
     * value after the internal division by {@code 100_000}.
     */
    @ParameterizedTest(name = "[{index}] uniqueId={0} → expectedMs={1}")
    @MethodSource("provideGetDateCases")
    void getDate_parameterized(long uniqueId, long expectedMs, String description) {
        // Act
        Date result = getDate(uniqueId);
        // Assert
        assertEquals(expectedMs, result.getTime(), () -> "Failed case: " + description);
    }

    /**
     * Additional edge‑case tests covering small positive/negative numbers,
     * exact multiples, and the {@code Long.MIN_VALUE} boundary.
     */
    @ParameterizedTest(name = "[{index}] uniqueId={0} → expectedMs={1}")
    @MethodSource("provideGetDateAdditionalCases")
    void getDate_additional_parameterized(long uniqueId, long expectedMs, String description) {
        // Act
        Date result = getDate(uniqueId);
        // Assert
        assertEquals(expectedMs, result.getTime(), () -> "Failed case: " + description);
    }
}
