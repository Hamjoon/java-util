package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.YEAR;

@ExtendWith(MockitoExtension.class)
class YearConversions_toAtomicLong_Test {

    /**
     * Converter used for all test cases.
     */
    private static Converter converter;

    /**
     * --------------------------------------------------------------------
     *  Global test lifecycle – initialise a real {@link Converter} once for the
     *  whole test suite.  The {@link Converter} does not hold external resources,
     *  so a simple instantiation with default {@link ConverterOptions} is enough.
     *  --------------------------------------------------------------------
     */
    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a no‑arg constructor that provides the default settings.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /**
     * --------------------------------------------------------------------
     *  Global cleanup – release the static {@link Converter} reference.
     *  --------------------------------------------------------------------
     */
    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    /* --------------------------------------------------------------------- *
     *  Valid conversion test data
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Year instance
        Arguments.of(Year.of(2023), 2023L), // Integer wrapper
        Arguments.of(Integer.valueOf(42), 42L), // Long that overflows int (casting semantics)
        Arguments.of(Long.valueOf(1234567890123L), 1912276171L), // String containing a valid integer
        Arguments.of("2020", 2020L), // Map with YEAR key holding a Year
        Arguments.of(Collections.singletonMap(YEAR, Year.of(1999)), 1999L), // Null source → 0
        Arguments.of(null, 0L), // Byte value
        Arguments.of(Byte.valueOf((byte) 7), 7L), // Short value
        Arguments.of(Short.valueOf((short) 123), 123L), // Float (truncated)
        Arguments.of(Float.valueOf(3.14f), 3L), // Double (truncated)
        Arguments.of(Double.valueOf(2.718), 2L), // AtomicInteger
        Arguments.of(new AtomicInteger(55), 55L), // AtomicLong (fits into int)
        Arguments.of(new AtomicLong(99L), 99L), // BigInteger larger than int range (overflow via cast)
        Arguments.of(BigInteger.valueOf(1234567890123456789L), -1912276183L), // BigDecimal exceeding int range (capped to Integer.MAX_VALUE)
        Arguments.of(new BigDecimal("9876543210"), Integer.MAX_VALUE), // Map with YEAR key holding an Integer
        Arguments.of(Map.of(YEAR, Integer.valueOf(2025)), 2025L), // Map with YEAR key holding a numeric String
        Arguments.of(Map.of(YEAR, "2026"), 2026L), // Long.MAX_VALUE (overflow)
        Arguments.of(Long.MAX_VALUE, -1L), // Integer.MAX_VALUE
        Arguments.of(Integer.MAX_VALUE, (long) Integer.MAX_VALUE), // Integer.MIN_VALUE
        Arguments.of(Integer.MIN_VALUE, (long) Integer.MIN_VALUE), // CharSequence (StringBuilder) containing a numeric string
        Arguments.of(new StringBuilder("2022"), 2022L));
    }

    /* --------------------------------------------------------------------- *
     *  Invalid conversion test data – expecting exceptions
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// Non‑numeric string
        Arguments.of("invalidNumber", NumberFormatException.class), // Boolean value
        Arguments.of(Boolean.TRUE, IllegalArgumentException.class), // Arbitrary object
        Arguments.of(new Object(), IllegalArgumentException.class), // Empty string
        Arguments.of("", NumberFormatException.class), // Whitespace‑only string
        Arguments.of("   ", NumberFormatException.class), // AtomicBoolean (unsupported)
        Arguments.of(new AtomicBoolean(true), IllegalArgumentException.class), // Map without YEAR key
        Arguments.of(Map.of("someKey", "value"), IllegalArgumentException.class), // Map with YEAR key but null value
        Arguments.of(Map.of(YEAR, null), IllegalArgumentException.class), // Array (unsupported)
        Arguments.of(new Object[] { 1, 2, 3 }, IllegalArgumentException.class), // Map with YEAR key holding Boolean
        Arguments.of(Map.of(YEAR, Boolean.TRUE), IllegalArgumentException.class), // Map with YEAR key holding arbitrary Object
        Arguments.of(Map.of(YEAR, new Object()), IllegalArgumentException.class), // Map with YEAR key holding non‑numeric String
        Arguments.of(Map.of(YEAR, "notANumber"), NumberFormatException.class), // CharSequence with non‑numeric content
        Arguments.of(new StringBuilder("abc"), NumberFormatException.class), // String with leading/trailing whitespace (not trimmed)
        Arguments.of("  123  ", NumberFormatException.class), // Special floating‑point values – NaN / Infinity
        Arguments.of(Double.NaN, IllegalArgumentException.class), Arguments.of(Double.POSITIVE_INFINITY, IllegalArgumentException.class), Arguments.of(Float.NaN, IllegalArgumentException.class), Arguments.of(Float.POSITIVE_INFINITY, IllegalArgumentException.class), // Unsupported source types
        Arguments.of(new java.util.Date(), IllegalArgumentException.class), Arguments.of(new java.sql.Timestamp(System.currentTimeMillis()), IllegalArgumentException.class));
    }

    /**
     * --------------------------------------------------------------------
     *  Test conversion from a {@link Year} instance.
     *  Expected result: the atomic long contains the numeric year value.
     *  --------------------------------------------------------------------
     */
    @Test
    void toAtomicLong_fromYear_returnsCorrectValue() {
        Year year = Year.of(2023);
        AtomicLong atomic = YearConversions.toAtomicLong(year, converter);
        assertNotNull(atomic, "Result must not be null");
        assertEquals(2023L, atomic.get(), "AtomicLong should contain the year value");
    }

    /**
     * --------------------------------------------------------------------
     *  Test conversion from a {@link String} that represents a year.
     *  Expected result: the string is parsed to an int and wrapped in an
     *  {@link AtomicLong}.
     *  --------------------------------------------------------------------
     */
    @Test
    void toAtomicLong_fromString_returnsParsedValue() {
        String yearString = "1999";
        AtomicLong atomic = YearConversions.toAtomicLong(yearString, converter);
        assertNotNull(atomic);
        assertEquals(1999L, atomic.get());
    }

    /**
     * --------------------------------------------------------------------
     *  Test conversion from a numeric wrapper (e.g., {@link Integer}).
     *  Expected result: the numeric value is transferred unchanged.
     *  --------------------------------------------------------------------
     */
    @Test
    void toAtomicLong_fromInteger_returnsSameValue() {
        Integer intValue = 42;
        AtomicLong atomic = YearConversions.toAtomicLong(intValue, converter);
        assertNotNull(atomic);
        assertEquals(42L, atomic.get());
    }

    /**
     * --------------------------------------------------------------------
     *  Test conversion when the source object is {@code null}.
     *  According to the {@link Converter} contract, a null source yields
     *  {@code 0} for numeric primitives.  The method should therefore return
     *  an {@link AtomicLong} with value {@code 0}.
     *  --------------------------------------------------------------------
     */
    @Test
    void toAtomicLong_fromNull_returnsZero() {
        AtomicLong atomic = YearConversions.toAtomicLong(null, converter);
        assertNotNull(atomic);
        assertEquals(0L, atomic.get());
    }

    /**
     * --------------------------------------------------------------------
     *  Test conversion from a {@link java.util.Map} that contains the special
     *  {@code YEAR} key used by {@code MapConversions}.  The map entry should
     *  be interpreted as a year value.
     *  --------------------------------------------------------------------
     */
    @Test
    void toAtomicLong_fromMapWithYearKey_returnsCorrectValue() {
        // The MapConversions.YEAR constant is the key that the conversion logic
        // looks for when a map is supplied.
        java.util.Map<String, Object> map = java.util.Collections.singletonMap(YEAR, 2015);
        AtomicLong atomic = YearConversions.toAtomicLong(map, converter);
        assertNotNull(atomic);
        assertEquals(2015L, atomic.get());
    }

    @ParameterizedTest(name = "[{index}] Convert {0} → AtomicLong({1})")
    @MethodSource("validConversionProvider")
    void toAtomicLong_validConversions(Object from, long expected) {
        AtomicLong result = YearConversions.toAtomicLong(from, converter);
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), () -> "Conversion of " + (from == null ? "null" : from.getClass().getSimpleName()) + " should yield " + expected);
    }

    @ParameterizedTest(name = "[{index}] Convert {0} → throws {1}")
    @MethodSource("invalidConversionProvider")
    void toAtomicLong_invalidConversions(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> YearConversions.toAtomicLong(from, converter), () -> "Conversion of " + (from == null ? "null" : from.getClass().getSimpleName()) + " should throw " + expectedException.getSimpleName());
    }

    /* --------------------------------------------------------------------- *
     *  Null Converter handling
     * --------------------------------------------------------------------- */
    @Test
    void toAtomicLong_nullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> YearConversions.toAtomicLong(Year.of(2022), null), "Passing a null Converter should result in NullPointerException");
    }
}
