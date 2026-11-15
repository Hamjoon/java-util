package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toBigInteger_Test {

    /**
     * Converter used by all test cases.
     */
    private Converter converter;

    /**
     * Creates a fresh {@link Converter} before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Converter requires a ConverterOptions instance; the default constructor yields an empty set of overrides.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /**
     * No special teardown required for the current tests.
     */
    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the reference – helps the GC in long test suites.
        converter = null;
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        // Helper objects used across several test cases
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2021‑01‑01T00:00:00Z
        utcCalendar.setTimeInMillis(1609459200000L);
        utcCalendar.set(Calendar.MILLISECOND, 0);
        Timestamp tsZeroNanos = new Timestamp(1609459200000L);
        Timestamp tsWithNanos = new Timestamp(1609459200000L);
        tsWithNanos.setNanos(123456789);
        // Map with null value cannot be created via Map.of; use mutable map instead
        Map<String, Object> mapValueNull = new HashMap<>();
        mapValueNull.put("value", null);
        // Map with non‑numeric leastSigBits
        Map<String, Object> mapBadLeastSig = new HashMap<>();
        mapBadLeastSig.put("mostSigBits", 1L);
        mapBadLeastSig.put("leastSigBits", "bad");
        // Map with non‑numeric epochMillis
        Map<String, Object> mapBadEpoch = new HashMap<>();
        mapBadEpoch.put("epochMillis", "notANumber");
        return Stream.of(// ----- original group -----
        Arguments.of(null, null), Arguments.of(new BigInteger("12345678901234567890"), new BigInteger("12345678901234567890")), Arguments.of("9876543210", new BigInteger("9876543210")), Arguments.of(12345L, new BigInteger("12345")), Arguments.of(new BigDecimal("12345.67"), new BigInteger("12345")), Arguments.of(new Date(1609459200000L), new BigInteger("1609459200000")), Arguments.of(Instant.ofEpochSecond(123456789L, 123456789), new BigInteger("123456789123456789")), Arguments.of(LocalDate.of(2020, 1, 2), new BigInteger("18262")), Arguments.of(LocalDateTime.of(2020, 1, 2, 3, 4, 5, 6_000_000), new BigInteger("1577933045600000000")), Arguments.of(Map.of("_v", "42"), new BigInteger("42")), Arguments.of(Map.of("value", "-99"), new BigInteger("-99")), Arguments.of(Map.of("epochMillis", 1620000000000L), new BigInteger("1620000000000")), // The expected value for this map is defined in the JSON as 72057594037927938
        Arguments.of(Map.of("mostSigBits", 1L, "leastSigBits", 2L), new BigInteger("72057594037927938")), Arguments.of(new UUID(0L, 5L), new BigInteger("5")), // ----- additional group -----
        Arguments.of(Integer.valueOf(42), new BigInteger("42")), Arguments.of(Short.valueOf((short) 7), new BigInteger("7")), Arguments.of(Byte.valueOf((byte) 5), new BigInteger("5")), Arguments.of(Double.valueOf(123.456), new BigInteger("123")), Arguments.of(Float.valueOf(12.9f), new BigInteger("12")), Arguments.of(new AtomicInteger(99), new BigInteger("99")), Arguments.of(new AtomicLong(1234567890123L), new BigInteger("1234567890123")), Arguments.of(utcCalendar, new BigInteger("1609459200000")), Arguments.of(tsZeroNanos, new BigInteger("1609459200000000000")), Arguments.of(tsWithNanos, new BigInteger("1609459200000123456789")), Arguments.of(ZonedDateTime.of(2020, 1, 2, 3, 4, 5, 6_000_000, ZoneOffset.UTC), new BigInteger("1577933045600000000")), Arguments.of(OffsetDateTime.of(2020, 1, 2, 3, 4, 5, 6_000_000, ZoneOffset.ofHours(2)), new BigInteger("1577846405006000000")), Arguments.of(LocalTime.of(12, 34, 56, 789_000_000), new BigInteger("45296789000000")), Arguments.of(Map.of("value", new BigInteger("555")), new BigInteger("555")), Arguments.of(Map.of("date", new Date(1620000000000L)), new BigInteger("1620000000000")), Arguments.of(Map.of("time", LocalTime.of(1, 2, 3, 4_000_000)), new BigInteger("3723004000000")), // empty string – will be handled by exception test, not here
        Arguments.of("", null), Arguments.of("  42  ", new BigInteger("42")), // hex not supported – exception test
        Arguments.of("0xFF", null), Arguments.of(new BigDecimal("-123.99"), new BigInteger("-123")), Arguments.of(new UUID(1L, 2L), new BigInteger("18446744073709551618")));
    }

    private static Stream<Arguments> provideExceptionConversions() {
        // Map with a null value for key "value" – must cause NPE during parsing
        Map<String, Object> mapValueNull = new HashMap<>();
        mapValueNull.put("value", null);
        // Map with non‑numeric "value"
        Map<String, Object> mapValueNonNumeric = new HashMap<>();
        mapValueNonNumeric.put("value", "abc");
        // Map with non‑numeric "epochMillis"
        Map<String, Object> mapBadEpoch = new HashMap<>();
        mapBadEpoch.put("epochMillis", "notANumber");
        // Map with non‑numeric "leastSigBits"
        Map<String, Object> mapBadLeastSig = new HashMap<>();
        mapBadLeastSig.put("mostSigBits", 1L);
        mapBadLeastSig.put("leastSigBits", "bad");
        return Stream.of(// ----- original exception group -----
        Arguments.of(mapValueNonNumeric, NumberFormatException.class), Arguments.of(mapValueNull, NullPointerException.class), Arguments.of(new AtomicBoolean(true), ClassCastException.class), Arguments.of(Year.of(2020), ClassCastException.class), Arguments.of(MonthDay.of(2, 29), ClassCastException.class), Arguments.of(Period.ofDays(5), ClassCastException.class), Arguments.of(Duration.ofHours(2), ClassCastException.class), Arguments.of(mapBadEpoch, NumberFormatException.class), Arguments.of(mapBadLeastSig, NumberFormatException.class), Arguments.of(Double.NaN, NumberFormatException.class), Arguments.of(Float.NaN, NumberFormatException.class), // ----- additional exception cases (derived from JSON) -----
        Arguments.of("", NumberFormatException.class), Arguments.of("0xFF", NumberFormatException.class));
    }

    @Test
    void toBigInteger_fromMapWithStringValue_returnsBigInteger() {
        // GIVEN
        Map<String, Object> source = new HashMap<>();
        // a large number as a String
        source.put("value", "9876543210123456789");
        // WHEN
        BigInteger result = MapConversions.toBigInteger(source, converter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(new BigInteger("9876543210123456789"), result, "The string value should be correctly converted to a BigInteger");
    }

    @Test
    void toBigInteger_fromMapWithBigIntegerValue_returnsSameInstance() {
        // GIVEN
        BigInteger expected = new BigInteger("123456789");
        Map<String, Object> source = new HashMap<>();
        // already a BigInteger
        source.put("value", expected);
        // WHEN
        BigInteger result = MapConversions.toBigInteger(source, converter);
        // THEN
        assertSame(expected, result, "When the map already contains a BigInteger, the same instance should be returned");
    }

    @Test
    void toBigInteger_whenSourceIsNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        BigInteger result = MapConversions.toBigInteger(source, converter);
        // THEN
        assertNull(result, "Converting a null source should yield a null result");
    }

    @ParameterizedTest(name = "{index}: from={0} ⇒ expected={1}")
    @MethodSource("provideSuccessfulConversions")
    void testToBigInteger_success(Object from, BigInteger expected) {
        // WHEN
        BigInteger result = MapConversions.toBigInteger(from, converter);
        // THEN
        if (expected == null) {
            assertNull(result, "Result should be null for null input");
        } else {
            assertNotNull(result, "Result should not be null");
            assertEquals(expected, result, () -> "Conversion of [" + from + "] did not produce the expected BigInteger");
        }
    }

    @ParameterizedTest(name = "{index}: from={0} ⇒ throws {1}")
    @MethodSource("provideExceptionConversions")
    void testToBigInteger_exceptions(Object from, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> MapConversions.toBigInteger(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
