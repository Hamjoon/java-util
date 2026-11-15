package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toBigInteger_Test {

    /**
     * Shared {@link Converter} used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor provides sensible defaults.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared Converter reference.
        converter = null;
    }

    static Stream<Arguments> durationConversionProvider() {
        return Stream.of(Arguments.of(Duration.ofSeconds(5).plusNanos(0), BigInteger.valueOf(5_000_000_000L)), Arguments.of(Duration.ofSeconds(-3).plusNanos(0), BigInteger.valueOf(-3_000_000_000L)), Arguments.of(Duration.ZERO, BigInteger.ZERO));
    }

    static Stream<Arguments> dateConversionProvider() {
        return Stream.of(// 2000 ms after epoch → 2_000_000_000 ns
        Arguments.of(new Date(2000L), BigInteger.valueOf(2_000_000_000L)), // epoch → 0 ns
        Arguments.of(new Date(0L), BigInteger.ZERO));
    }

    static Stream<Arguments> timestampConversionProvider() {
        return Stream.of(// 1 second + 123,456,789 ns = 1_123_456_789 ns
        Arguments.of(Timestamp.valueOf("1970-01-01 00:00:01.123456789"), BigInteger.valueOf(1_123_456_789L)));
    }

    static Stream<Arguments> instantConversionProvider() {
        return Stream.of(// 10 seconds + 500_000_000 ns = 10_500_000_000 ns
        Arguments.of(Instant.ofEpochSecond(10, 500_000_000), BigInteger.valueOf(10_500_000_000L)));
    }

    static Stream<Arguments> localDateTimeConversionProvider() {
        return Stream.of(// 1970-01-01T00:00:03 interpreted as UTC → 3_000_000_000 ns
        Arguments.of(LocalDateTime.of(1970, 1, 1, 0, 0, 3), BigInteger.valueOf(3_000_000_000L)));
    }

    static Stream<Arguments> zonedDateTimeConversionProvider() {
        return Stream.of(// Epoch instant → 0 ns
        Arguments.of(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), BigInteger.ZERO), // 2023‑05‑15T12:30:45+02:00 → epoch‑nanoseconds calculation
        Arguments.of(ZonedDateTime.of(2023, 5, 15, 12, 30, 45, 0, ZoneOffset.ofHours(2)), // Convert to epoch seconds first, then to nanos.
        // 2023‑05‑15T10:30:45Z is the UTC instant.
        // Use Instant to compute expected value.
        BigInteger.valueOf(ZonedDateTime.of(2023, 5, 15, 12, 30, 45, 0, ZoneOffset.ofHours(2)).toInstant().toEpochMilli() * 1_000_000L)));
    }

    static Stream<Arguments> offsetDateTimeConversionProvider() {
        return Stream.of(// Epoch → 0 ns
        Arguments.of(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), BigInteger.ZERO));
    }

    static Stream<Arguments> isoStringConversionProvider() {
        return Stream.of(// PT20.345S = 20.345 seconds → 20_345_000_000 ns
        Arguments.of("PT20.345S", BigInteger.valueOf(20_345_000_000L)), // -PT5S = -5 seconds → -5_000_000_000 ns
        Arguments.of("-PT5S", BigInteger.valueOf(-5_000_000_000L)));
    }

    static Stream<Arguments> longMillisecondsConversionProvider() {
        return Stream.of(Arguments.of(12_345L, BigInteger.valueOf(12_345_000_000L)), Arguments.of(-5_000L, BigInteger.valueOf(-5_000_000_000L)));
    }

    /**
     * Verify that a {@link Duration} is correctly converted to a {@link BigInteger}
     * by delegating to {@link DurationConversions#toBigInteger(Duration, Converter)}.
     */
    @Test
    void toBigInteger_givenDuration_returnsSameAsDurationConversions() {
        // GIVEN
        // a non‑trivial duration
        Duration duration = Duration.ofSeconds(5).plusNanos(123);
        // WHEN
        BigInteger result = TimestampConversions.toBigInteger(duration, converter);
        // THEN
        // The expected value is obtained by calling the same static helper directly.
        BigInteger expected = DurationConversions.toBigInteger(duration, converter);
        assertEquals(expected, result, "TimestampConversions.toBigInteger should delegate to DurationConversions.toBigInteger");
    }

    /**
     * Verify that a {@code null} source returns {@code null}.
     */
    @Test
    void toBigInteger_givenNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        BigInteger result = TimestampConversions.toBigInteger(source, converter);
        // THEN
        assertNull(result, "Converting a null source should produce a null result");
    }

    @ParameterizedTest(name = "[{index}] Duration {0} → {1}")
    @MethodSource("durationConversionProvider")
    void toBigInteger_fromDuration(Duration input, BigInteger expected) {
        // WHEN
        BigInteger result = TimestampConversions.toBigInteger(input, converter);
        // THEN
        assertEquals(expected, result, () -> "Duration " + input + " should convert to " + expected);
    }

    @ParameterizedTest(name = "[{index}] Date {0} → {1}")
    @MethodSource("dateConversionProvider")
    void toBigInteger_fromDate(Date input, BigInteger expected) {
        BigInteger result = TimestampConversions.toBigInteger(input, converter);
        assertEquals(expected, result, () -> "Date " + input + " should convert to " + expected);
    }

    @ParameterizedTest(name = "[{index}] Timestamp {0} → {1}")
    @MethodSource("timestampConversionProvider")
    void toBigInteger_fromTimestamp(Timestamp input, BigInteger expected) {
        BigInteger result = TimestampConversions.toBigInteger(input, converter);
        assertEquals(expected, result, () -> "Timestamp " + input + " should convert to " + expected);
    }

    @ParameterizedTest(name = "[{index}] Instant {0} → {1}")
    @MethodSource("instantConversionProvider")
    void toBigInteger_fromInstant(Instant input, BigInteger expected) {
        BigInteger result = TimestampConversions.toBigInteger(input, converter);
        assertEquals(expected, result, () -> "Instant " + input + " should convert to " + expected);
    }

    @ParameterizedTest(name = "[{index}] LocalDateTime {0} → {1}")
    @MethodSource("localDateTimeConversionProvider")
    void toBigInteger_fromLocalDateTime(LocalDateTime input, BigInteger expected) {
        BigInteger result = TimestampConversions.toBigInteger(input, converter);
        assertEquals(expected, result, () -> "LocalDateTime " + input + " should convert to " + expected);
    }

    @ParameterizedTest(name = "[{index}] ZonedDateTime {0} → {1}")
    @MethodSource("zonedDateTimeConversionProvider")
    void toBigInteger_fromZonedDateTime(ZonedDateTime input, BigInteger expected) {
        BigInteger result = TimestampConversions.toBigInteger(input, converter);
        assertEquals(expected, result, () -> "ZonedDateTime " + input + " should convert to " + expected);
    }

    @ParameterizedTest(name = "[{index}] OffsetDateTime {0} → {1}")
    @MethodSource("offsetDateTimeConversionProvider")
    void toBigInteger_fromOffsetDateTime(OffsetDateTime input, BigInteger expected) {
        BigInteger result = TimestampConversions.toBigInteger(input, converter);
        assertEquals(expected, result, () -> "OffsetDateTime " + input + " should convert to " + expected);
    }

    @ParameterizedTest(name = "[{index}] ISO‑8601 String \"{0}\" → {1}")
    @MethodSource("isoStringConversionProvider")
    void toBigInteger_fromIsoString(String input, BigInteger expected) {
        BigInteger result = TimestampConversions.toBigInteger(input, converter);
        assertEquals(expected, result, () -> "ISO‑8601 string \"" + input + "\" should convert to " + expected);
    }

    @ParameterizedTest(name = "[{index}] Long milliseconds {0} → {1}")
    @MethodSource("longMillisecondsConversionProvider")
    void toBigInteger_fromLong(Long input, BigInteger expected) {
        BigInteger result = TimestampConversions.toBigInteger(input, converter);
        assertEquals(expected, result, () -> "Long " + input + " ms should convert to " + expected + " ns");
    }

    @Test
    void toBigInteger_nullSource_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toBigInteger(null, converter), "Null source should trigger IllegalArgumentException");
    }

    @Test
    void toBigInteger_nullConverter_throwsIllegalArgumentException() {
        Duration dur = Duration.ofSeconds(1);
        assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toBigInteger(dur, null), "Null converter should trigger IllegalArgumentException");
    }

    @Test
    void toBigInteger_unsupportedSource_throwsIllegalArgumentException() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toBigInteger(unsupported, converter), "Unsupported source type should trigger IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] Malformed ISO‑8601 string \"{0}\"")
    @ValueSource(strings = { "PT-XYZ", "", "12345" })
    void toBigInteger_malformedIsoString_throwsIllegalArgumentException(String malformed) {
        assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toBigInteger(malformed, converter), "Malformed ISO‑8601 string \"" + malformed + "\" should trigger IllegalArgumentException");
    }

    @Test
    void toBigInteger_invalidMap_throwsIllegalArgumentException() {
        Map<String, Object> badMap = Collections.singletonMap("seconds", 5L);
        assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toBigInteger(badMap, converter), "Map lacking proper duration fields should trigger IllegalArgumentException");
    }

    @Test
    void toBigInteger_givenDuration_delegatesToDurationConversions() {
        Duration duration = Duration.ofSeconds(5).plusNanos(123);
        BigInteger result = TimestampConversions.toBigInteger(duration, converter);
        BigInteger expected = DurationConversions.toBigInteger(duration, converter);
        assertEquals(expected, result, "TimestampConversions.toBigInteger should delegate to DurationConversions.toBigInteger");
    }
}
