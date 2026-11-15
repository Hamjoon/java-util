package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toDuration_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The method under test does not actually use the Converter,
        // but we provide a real instance to keep the contract realistic.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        converter = null;
    }

    static Stream<Arguments> validTimestampProvider() {
        // Helper to create a non‑null Converter when required.
        Converter nonNullConv = new Converter(new ConverterOptions());
        Timestamp epochTs = Timestamp.from(Instant.EPOCH);
        Timestamp ts2023 = Timestamp.valueOf("2023-01-01 12:34:56");
        Timestamp ts1960 = Timestamp.valueOf("1960-01-01 00:00:00");
        Timestamp ts1970plus1 = Timestamp.valueOf("1970-01-01 00:00:01");
        return Stream.of(// 1. Epoch → zero duration
        Arguments.of(epochTs, null, Duration.ZERO, "Timestamp exactly at the Unix epoch should produce a zero Duration."), // 2. Positive duration after epoch
        Arguments.of(ts2023, null, Duration.between(Instant.EPOCH, ts2023.toInstant()), "A Timestamp after the epoch (2023‑01‑01T12:34:56) should return the correct positive Duration."), // 3. Negative duration (before epoch)
        Arguments.of(ts1960, null, Duration.between(Instant.EPOCH, ts1960.toInstant()), "A Timestamp before the epoch (1960‑01‑01T00:00:00) should return a negative Duration."), // 4. Non‑null converter (ignored)
        Arguments.of(ts1970plus1, nonNullConv, Duration.ofSeconds(1), "Even when a non‑null Converter is supplied, the method must still return the correct Duration."));
    }

    static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// null `from` → NullPointerException when cast or toInstant()
        Arguments.of(null, null, NullPointerException.class, "Passing a null `from` argument results in a NullPointerException."), // non‑Timestamp object → ClassCastException
        Arguments.of(new java.util.Date(), null, ClassCastException.class, "Supplying an object that is not a java.sql.Timestamp triggers a ClassCastException."));
    }

    static Stream<Arguments> nanosecondPrecisionProvider() {
        return Stream.of(// 123,456,789 ns after epoch
        Arguments.of(Timestamp.from(Instant.ofEpochSecond(0, 123_456_789)), null, Duration.ofNanos(123_456_789), "Timestamp at the epoch with 123,456,789 nanoseconds should produce a Duration of exactly 123,456,789 nanoseconds."), // Maximum nanosecond fraction
        Arguments.of(Timestamp.from(Instant.ofEpochSecond(0, 999_999_999)), null, Duration.ofNanos(999_999_999), "Timestamp at the epoch with the maximum nanosecond fraction (999,999,999) should yield a Duration of that many nanoseconds."), // One second before epoch with +500,000,000 ns → -0.5 seconds
        Arguments.of(Timestamp.from(Instant.ofEpochSecond(-1, 500_000_000)), null, Duration.ofNanos(-500_000_000), "Timestamp one second before the epoch but with +500,000,000 nanoseconds results in a Duration of –0.5 seconds."), // Zero millis, then setNanos(500,000,000)
        Arguments.of(createTimestampWithNanos(0L, 500_000_000), null, Duration.ofNanos(500_000_000), "Timestamp created with zero milliseconds and then set to 500,000,000 nanoseconds should give a Duration of +0.5 seconds."));
    }

    /**
     * Helper to create a {@link Timestamp} with a specific millisecond value and then set the nanosecond part.
     */
    private static Timestamp createTimestampWithNanos(long millis, int nanos) {
        Timestamp ts = new Timestamp(millis);
        ts.setNanos(nanos);
        return ts;
    }

    static Stream<Arguments> outOfRangeProvider() {
        return Stream.of(// Milliseconds beyond Instant.MAX
        Arguments.of(new Timestamp(Long.MAX_VALUE), null, DateTimeException.class, "A Timestamp whose millisecond value exceeds the maximum Instant range should cause DateTimeException."), // Milliseconds below Instant.MIN
        Arguments.of(new Timestamp(Long.MIN_VALUE), null, DateTimeException.class, "A Timestamp whose millisecond value is below the minimum Instant range should cause DateTimeException."), // Instant far in the future (beyond supported range)
        Arguments.of(Timestamp.from(Instant.ofEpochSecond(Long.MAX_VALUE / 1000, 0)), null, DateTimeException.class, "Creating a Timestamp from an Instant that is outside the supported Instant range (far future) should result in DateTimeException."), // Instant far in the past (beyond supported range)
        Arguments.of(Timestamp.from(Instant.ofEpochSecond(Long.MIN_VALUE / 1000, 0)), null, DateTimeException.class, "Creating a Timestamp from an Instant that is outside the supported Instant range (far past) should result in DateTimeException."));
    }

    @Test
    void toDuration_givenEpochTimestamp_returnsZeroDuration() {
        // 1970‑01‑01T00:00:00Z
        Timestamp ts = new Timestamp(0L);
        Duration result = TimestampConversions.toDuration(ts, converter);
        assertEquals(Duration.ZERO, result, "Duration for epoch timestamp should be zero");
    }

    @Test
    void toDuration_givenTimestampWithMillis_returnsCorrectDuration() {
        // arbitrary moment after epoch
        long millis = 1_234_567_890L;
        Timestamp ts = new Timestamp(millis);
        Duration expected = Duration.ofMillis(millis);
        Duration result = TimestampConversions.toDuration(ts, converter);
        assertEquals(expected, result, "Duration should match the milliseconds of the timestamp");
    }

    @Test
    void toDuration_givenTimestampWithNanos_returnsCorrectDuration() {
        // Timestamp with 0 seconds and 500,000,000 nanoseconds (0.5 seconds)
        Timestamp ts = Timestamp.from(Instant.ofEpochSecond(0, 500_000_000));
        Duration expected = Duration.ofNanos(500_000_000);
        Duration result = TimestampConversions.toDuration(ts, converter);
        assertEquals(expected, result, "Duration should include the nanosecond component");
    }

    @Test
    void toDuration_givenNonTimestamp_throwsClassCastException() {
        Object notATimestamp = "2023-01-01";
        assertThrows(ClassCastException.class, () -> TimestampConversions.toDuration(notATimestamp, converter), "Passing a non‑Timestamp should result in ClassCastException");
    }

    /* --------------------------------------------------------------
     *  Parameterized tests – Valid Timestamp Conversions
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("validTimestampProvider")
    void toDuration_validTimestamps(Object from, Converter conv, Duration expected, String description) {
        Duration result = TimestampConversions.toDuration(from, conv);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Parameterized tests – Invalid Input Handling
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("invalidInputProvider")
    void toDuration_invalidInputs(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> TimestampConversions.toDuration(from, conv), description);
    }

    /* --------------------------------------------------------------
     *  Parameterized tests – Nanosecond Precision Cases
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("nanosecondPrecisionProvider")
    void toDuration_nanosecondPrecision(Object from, Converter conv, Duration expected, String description) {
        Duration result = TimestampConversions.toDuration(from, conv);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Parameterized tests – Out of Range Timestamp Handling
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("outOfRangeProvider")
    void toDuration_outOfRangeTimestamps(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> TimestampConversions.toDuration(from, conv), description);
    }
}
