package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DateTimeException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toDuration_Test {

    @Mock
    private ConverterOptions // dependency of Converter
    mockOptions;

    // instance used in tests
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a real Converter instance using the mocked options.
        // The Converter constructor only stores the options and registers overrides,
        // which are not required for the toDuration method.
        converter = new Converter(mockOptions);
    }

    /**
     * Provides a stream of arguments for normal‑behaviour tests.
     *
     * Each argument consists of:
     * <ul>
     *   <li>the {@link Timestamp} to convert</li>
     *   <li>the {@link Converter} to pass (null or a real instance)</li>
     *   <li>the expected {@link Duration} expressed as an ISO‑8601 string</li>
     *   <li>a short description (used only for readability of the test name)</li>
     * </ul>
     */
    private static Stream<Arguments> provideNormalBehaviorCases() {
        return Stream.of(// 2023‑01‑01 00:00:00 (UTC) → 1704067200 seconds after epoch
        Arguments.of(Timestamp.valueOf("2023-01-01 00:00:00"), null, "PT1704067200S", "Timestamp after epoch, converter null"), // Exactly the epoch → zero duration
        Arguments.of(Timestamp.valueOf("1970-01-01 00:00:00"), null, "PT0S", "Timestamp at epoch, converter null"), // Before epoch → negative duration
        Arguments.of(Timestamp.valueOf("1960-01-01 00:00:00"), null, "PT-315619200S", "Timestamp before epoch, converter null"), // Millisecond precision with a non‑null converter (ignored by the method)
        Arguments.of(Timestamp.valueOf("1970-01-01 00:00:00.123"), // a dummy, real Converter instance
        new Converter(), "PT0.123S", "Timestamp with milliseconds, converter non‑null"));
    }

    @Test
    void toDuration_convertsTimestampToCorrectDuration() {
        // GIVEN: a known Timestamp instance
        Timestamp ts = Timestamp.valueOf("2023-03-15 12:34:56.789");
        // Expected Duration is the elapsed time from the epoch to the timestamp's instant
        Duration expected = Duration.between(Instant.EPOCH, ts.toInstant());
        // WHEN: the conversion method is invoked
        Duration actual = TimestampConversions.toDuration(ts, converter);
        // THEN: the resulting Duration matches the expected value
        assertEquals(expected, actual, "Duration should represent the exact instant of the Timestamp");
    }

    @Test
    void toDuration_withEpochTimestampReturnsZeroDuration() {
        // GIVEN: a Timestamp representing the epoch (1970‑01‑01T00:00:00Z)
        Timestamp epochTs = new Timestamp(0L);
        // WHEN
        Duration result = TimestampConversions.toDuration(epochTs, converter);
        // THEN
        assertEquals(Duration.ZERO, result, "Epoch timestamp must produce a zero Duration");
    }

    @Test
    void toDuration_withNonTimestampThrowsClassCastException() {
        // GIVEN: an object that is NOT a java.sql.Timestamp
        Object notATimestamp = "2023-01-01";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> TimestampConversions.toDuration(notATimestamp, converter), "Passing a non‑Timestamp should result in a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideNormalBehaviorCases")
    void toDuration_normalBehavior(Timestamp from, Converter conv, String expectedIso, String _description) {
        // GIVEN: the parameters are supplied by the method source
        // WHEN: invoke the method under test
        Duration actual = TimestampConversions.toDuration(from, conv);
        // THEN: compare against the expected Duration parsed from ISO‑8601 string
        Duration expected = Duration.parse(expectedIso);
        assertEquals(expected, actual, () -> String.format("Duration mismatch for input %s (converter %s)", from, conv));
    }

    @Test
    void toDuration_nullFromThrowsNullPointerException() {
        // GIVEN: a null 'from' argument
        Object from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimestampConversions.toDuration(from, null), "Passing null as 'from' should result in NullPointerException");
    }

    @Test
    void toDuration_nonTimestampFromThrowsClassCastException() {
        // GIVEN: a java.util.Date (not a java.sql.Timestamp)
        Object from = new java.util.Date();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> TimestampConversions.toDuration(from, null), "Passing a non‑Timestamp (java.util.Date) should trigger ClassCastException");
    }

    @Test
    void toDuration_timestampBeyondInstantRangeThrowsDateTimeException() {
        // GIVEN: a Timestamp whose millisecond value exceeds Instant.MAX
        // Using Long.MAX_VALUE milliseconds is far beyond the supported range.
        Timestamp from = new Timestamp(Long.MAX_VALUE);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> TimestampConversions.toDuration(from, null), "Timestamp beyond Instant range should throw DateTimeException");
    }
}
