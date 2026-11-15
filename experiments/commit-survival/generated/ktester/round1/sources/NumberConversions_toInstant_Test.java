package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toInstant_Test {

    /**
     * Converter used by all test cases – created once for the whole suite.
     */
    private static Converter converter;

    // -----------------------------------------------------------------
    private static class FixedLongConverter extends Converter {

        private final long value;

        FixedLongConverter(long value) {
            super(new ConverterOptions());
            this.value = value;
        }

        @Override
        public Object convert(Object src, Class<?> target) {
            // The test only needs to support conversion to Long (or long primitive)
            if (target == Long.class || target == long.class) {
                return value;
            }
            // Fallback to the default implementation for any other request
            return super.convert(src, target);
        }
    }

    // -----------------------------------------------------------------
    private static class ThrowingConverter extends Converter {

        private final RuntimeException toThrow;

        ThrowingConverter(RuntimeException toThrow) {
            super(new ConverterOptions());
            this.toThrow = toThrow;
        }

        @Override
        public Object convert(Object src, Class<?> target) {
            throw toThrow;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor
        // provides the standard configuration (zone, overrides, etc.).
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the
        // test class tidy.
        converter = null;
    }

    private static Stream<Arguments> numericValidProvider() {
        return Stream.of(// Long value (epoch‑millis)
        Arguments.of("Long value is interpreted as epoch‑millis; Instant should represent the same moment.", 1620000000000L, "2021-05-03T00:00:00Z"), // Integer value (treated as milliseconds)
        Arguments.of("Integer value is converted to long via Number.longValue(); Instant reflects the epoch‑millis.", 12345, "1970-01-01T00:00:12.345Z"), // Double value (truncated)
        Arguments.of("Floating‑point number is truncated to long (12 ms) before creating the Instant.", 12.34d, "1970-01-01T00:00:00.012Z"));
    }

    private static Stream<Arguments> dateTimeValidProvider() {
        // Prepare reusable Calendar instance in UTC
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(1620000000000L);
        return Stream.of(Arguments.of("java.util.Date#getTime() supplies epoch‑millis; Instant should match the date.", new Date(1620000000000L), "2021-05-03T00:00:00Z"), Arguments.of("Calendar#getTimeInMillis() is used; Instant must represent the same moment.", utcCal, "2021-05-03T00:00:00Z"), Arguments.of("If the source object is already an Instant, its epoch‑millis are reused.", Instant.ofEpochMilli(1620000000000L), "2021-05-03T00:00:00Z"));
    }

    private static Stream<Arguments> stringParsingValidProvider() {
        return Stream.of(Arguments.of("String containing a decimal number is parsed to long; Instant reflects the parsed epoch‑millis.", "1620000000000", "2021-05-03T00:00:00Z"), Arguments.of("ISO‑8601 instant string is parsed (via Converter fallback) and converted to epoch‑millis.", "2021-05-03T00:00:00Z", "2021-05-03T00:00:00Z"));
    }

    private static Stream<Arguments> epochBoundaryProvider() {
        return Stream.of(Arguments.of("Maximum long epoch‑millis should be converted to the far‑future Instant defined by Instant.ofEpochMilli(Long.MAX_VALUE).", Long.MAX_VALUE, "292278994-08-17T07:12:55.807Z"), Arguments.of("Minimum long epoch‑millis should be converted to the far‑past Instant defined by Instant.ofEpochMilli(Long.MIN_VALUE).", Long.MIN_VALUE, "-292278994-08-17T07:12:55.808Z"));
    }

    private static Stream<Arguments> fractionalTruncationProvider() {
        return Stream.of(Arguments.of("A double less than 1 ms is truncated to 0 ms when cast to long; Instant should represent the epoch.", 0.999d, "1970-01-01T00:00:00Z"), Arguments.of("Negative fractional values are also truncated toward zero; -0.5 ms becomes 0 ms, yielding the epoch Instant.", -0.5f, "1970-01-01T00:00:00Z"));
    }

    private static Stream<Arguments> stringParsingInvalidProvider() {
        return Stream.of(Arguments.of("Empty string cannot be parsed to a number; method should throw IllegalArgumentException.", ""), Arguments.of("String containing only whitespace is not a valid numeric representation; expect IllegalArgumentException.", "   "), Arguments.of("Non‑numeric string should cause an IllegalArgumentException during parsing.", "not-a-number"));
    }

    private static Stream<Arguments> atomicAndBigNumberProvider() {
        return Stream.of(Arguments.of("AtomicLong value is extracted via longValue() and converted to the corresponding Instant.", new AtomicLong(1620000000000L), "2021-05-03T00:00:00Z"), Arguments.of("BigDecimal representing whole milliseconds is converted correctly.", new BigDecimal("1620000000000"), "2021-05-03T00:00:00Z"), Arguments.of("BigInteger is handled like other integer types.", new BigInteger("1620000000000"), "2021-05-03T00:00:00Z"), Arguments.of("Fractional BigDecimal is truncated to whole milliseconds before Instant creation.", new BigDecimal("12345.67"), "1970-01-01T00:00:12.345Z"));
    }

    @Test
    void toInstant_fromLong_returnsCorrectInstant() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        long epochMilli = 1_640_995_200_000L;
        // WHEN
        Instant result = NumberConversions.toInstant(epochMilli, converter);
        // THEN
        assertEquals(Instant.ofEpochMilli(epochMilli), result, "Instant created from a Long epoch‑millis should match the expected value");
    }

    @Test
    void toInstant_fromInteger_isPromotedToLong() {
        // GIVEN
        // seconds since epoch (2022‑01‑01T00:00:00Z)
        int epochSecond = 1_640_995_200;
        long expectedMillis = epochSecond * 1_000L;
        // WHEN
        Instant result = NumberConversions.toInstant(epochSecond, converter);
        // THEN
        assertEquals(Instant.ofEpochMilli(expectedMillis), result, "Integer values should be treated as milliseconds and promoted to long");
    }

    @Test
    void toInstant_fromDate_usesDateTimeInMillis() {
        // GIVEN
        // 2022‑01‑01T00:00:00.123Z
        Date date = new Date(1_640_995_200_123L);
        // WHEN
        Instant result = NumberConversions.toInstant(date, converter);
        // THEN
        assertEquals(Instant.ofEpochMilli(date.getTime()), result, "Date should be converted using its internal epoch‑millis value");
    }

    @Test
    void toInstant_fromStringNumeric_ParsesAsLong() {
        // GIVEN
        // same as above, expressed as a string
        String numeric = "1640995200123";
        // WHEN
        Instant result = NumberConversions.toInstant(numeric, converter);
        // THEN
        assertEquals(Instant.ofEpochMilli(Long.parseLong(numeric)), result, "String containing a numeric value should be parsed to a long epoch‑millis");
    }

    @Test
    void toInstant_nullInputReturnsEpochZeroInstant() {
        // GIVEN
        Object nullObj = null;
        // WHEN
        Instant result = NumberConversions.toInstant(nullObj, converter);
        // THEN
        // The library treats null as zero for primitive targets.
        assertEquals(Instant.ofEpochMilli(0L), result, "Null input should be interpreted as epoch‑millis zero");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numericValidProvider")
    void toInstant_numericValidConversions(String description, Object from, String expectedIso) {
        Instant expected = Instant.parse(expectedIso);
        Instant actual = NumberConversions.toInstant(from, converter);
        assertEquals(expected, actual, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("dateTimeValidProvider")
    void toInstant_dateTimeValidConversions(String description, Object from, String expectedIso) {
        Instant expected = Instant.parse(expectedIso);
        Instant actual = NumberConversions.toInstant(from, converter);
        assertEquals(expected, actual, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringParsingValidProvider")
    void toInstant_stringParsingValidConversions(String description, String from, String expectedIso) {
        Instant expected = Instant.parse(expectedIso);
        Instant actual = NumberConversions.toInstant(from, converter);
        assertEquals(expected, actual, description);
    }

    // -----------------------------------------------------------------
    @Test
    void toInstant_customConverterSuppliesLong() {
        // Custom converter returns 500 ms for any input
        Converter custom = new FixedLongConverter(500L);
        Instant result = NumberConversions.toInstant("custom", custom);
        assertEquals(Instant.ofEpochMilli(500L), result, "A custom Converter that interprets the string \"custom\" as 500 ms; toInstant must use the Converter‑provided long.");
    }

    // -----------------------------------------------------------------
    @Test
    void toInstant_unsupportedBoolean_throwsIllegalArgumentException() {
        Boolean src = true;
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toInstant(src, converter), "Boolean cannot be converted to a long; method should throw IllegalArgumentException.");
    }

    // -----------------------------------------------------------------
    @Test
    void toInstant_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toInstant(null, null), "Passing null as the source object should result in NPE (or IllegalArgumentException depending on implementation).");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("epochBoundaryProvider")
    void toInstant_epochBoundaryValues(String description, Long from, String expectedIso) {
        Instant expected = Instant.parse(expectedIso);
        Instant actual = NumberConversions.toInstant(from, converter);
        assertEquals(expected, actual, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("fractionalTruncationProvider")
    void toInstant_fractionalTruncation(String description, Number from, String expectedIso) {
        Instant expected = Instant.parse(expectedIso);
        Instant actual = NumberConversions.toInstant(from, converter);
        assertEquals(expected, actual, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringParsingInvalidProvider")
    void toInstant_invalidStringParsingThrows(String description, String from) {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toInstant(from, converter), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("atomicAndBigNumberProvider")
    void toInstant_atomicAndBigNumberValid(String description, Object from, String expectedIso) {
        Instant expected = Instant.parse(expectedIso);
        Instant actual = NumberConversions.toInstant(from, converter);
        assertEquals(expected, actual, description);
    }

    // -----------------------------------------------------------------
    @Test
    void toInstant_customConverterHandlesUnsupportedBoolean() {
        // Converter returns 1000 ms irrespective of input
        Converter custom = new FixedLongConverter(1000L);
        Instant result = NumberConversions.toInstant(Boolean.TRUE, custom);
        assertEquals(Instant.ofEpochMilli(1000L), result, "Even though Boolean is unsupported, a custom Converter supplies the epoch‑millis (1000) which should be used.");
    }

    // -----------------------------------------------------------------
    @Test
    void toInstant_unsupportedObject_throwsIllegalArgumentException() {
        Object src = new Object();
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toInstant(src, converter), "Object type is not convertible to a long; method must throw IllegalArgumentException.");
    }

    // -----------------------------------------------------------------
    @Test
    void toInstant_converterThrowsException_propagates() {
        RuntimeException cause = new IllegalStateException("converter failure");
        Converter throwing = new ThrowingConverter(cause);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> NumberConversions.toInstant("any", throwing), "Custom Converter throws an exception during conversion; toInstant should propagate the exception.");
        assertSame(cause, thrown, "The thrown exception should be the same instance supplied by the Converter.");
    }
}
