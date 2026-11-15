package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toSqlDate_Test {

    /**
     * A (mocked) Converter is supplied because the method signature requires it,
     *  although the implementation does not use it.
     */
    @Mock
    Converter converter;

    /**
     * Convert an ISO‑8601 date (e.g. {@code "1970-01-01"}) to the epoch‑millisecond
     * value that {@link java.sql.Date} stores.  The conversion is performed in UTC
     * to avoid any influence of the host machine's default time‑zone.
     */
    private static long epochMillisFromDate(String isoDate) {
        return LocalDate.parse(isoDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    private static Stream<Arguments> provideValidDoubleConversions() {
        return Stream.of(// Zero seconds → epoch
        Arguments.of(0.0, "1970-01-01"), // Exactly one day (86 400 seconds) → next day
        Arguments.of(86_400.0, "1970-01-02"), // Negative one day → previous day
        Arguments.of(-86_400.0, "1969-12-31"), // Large positive value → 2017‑07‑14
        Arguments.of(1_500_000_000.0, "2017-07-14"));
    }

    private static Stream<Arguments> provideFractionalSecondConversions() {
        return Stream.of(// Positive fractional seconds (< 1 s) stay on epoch day
        Arguments.of(0.5, "1970-01-01"), // Negative fractional seconds → previous day
        Arguments.of(-0.5, "1969-12-31"), // Just under one second → epoch day
        Arguments.of(0.999, "1970-01-01"), // One day + half second → next day
        Arguments.of(86_400.5, "1970-01-02"));
    }

    private static Stream<Arguments> provideMillisecondBoundaryConversions() {
        return Stream.of(// 1 ms shy of 24 h → still epoch day
        Arguments.of(86_399.999, "1970-01-01"), // 1 ms over 24 h → next day
        Arguments.of(86_400.001, "1970-01-02"));
    }

    private static Stream<Arguments> provideSpecialDoubleConversions() {
        return Stream.of(// NaN → cast to long yields 0 → epoch
        Arguments.of(Double.NaN, "1970-01-01"), // Positive infinity → Long.MAX_VALUE
        Arguments.of(Double.POSITIVE_INFINITY, Long.MAX_VALUE), // Negative infinity → Long.MIN_VALUE
        Arguments.of(Double.NEGATIVE_INFINITY, Long.MIN_VALUE));
    }

    private static Stream<Arguments> provideExtremeDoubleConversions() {
        return Stream.of(// Double.MAX_VALUE → overflow to Long.MAX_VALUE
        Arguments.of(Double.MAX_VALUE, Long.MAX_VALUE), // -Double.MAX_VALUE → overflow to Long.MIN_VALUE
        Arguments.of(-Double.MAX_VALUE, Long.MIN_VALUE), // Double.MIN_VALUE (smallest positive) → 0 ms → epoch
        Arguments.of(Double.MIN_VALUE, "1970-01-01"), // Negative zero → 0 ms → epoch
        Arguments.of(-0.0, "1970-01-01"));
    }

    private static Stream<Arguments> provideInvalidInputs() {
        return Stream.of(// null input → NullPointerException when casting
        Arguments.of(null, NullPointerException.class), // Integer instead of Double → ClassCastException
        Arguments.of(123, ClassCastException.class), // String that cannot be cast to Double → ClassCastException
        Arguments.of("not a number", ClassCastException.class));
    }

    @Test
    void toSqlDate_convertsPositiveDouble() {
        // 1.5 seconds → 1500 ms
        Double input = 1.5;
        Date result = DoubleConversions.toSqlDate(input, converter);
        assertNotNull(result);
        assertEquals(1500L, result.getTime(), "Expected milliseconds to be 1500 for input 1.5");
    }

    @Test
    void toSqlDate_convertsZeroDouble() {
        // 0 seconds → 0 ms
        Double input = 0.0;
        Date result = DoubleConversions.toSqlDate(input, converter);
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "Expected milliseconds to be 0 for input 0.0");
    }

    @Test
    void toSqlDate_convertsNegativeDouble() {
        // -2.3 seconds → -2300 ms
        Double input = -2.3;
        Date result = DoubleConversions.toSqlDate(input, converter);
        assertNotNull(result);
        assertEquals(-2300L, result.getTime(), "Expected milliseconds to be -2300 for input -2.3");
    }

    @Test
    void toSqlDate_convertsLargeDoubleBeyondLongRange() {
        // Double.MAX_VALUE * 1000 is far beyond Long.MAX_VALUE.
        // Java casts to Long.MAX_VALUE in this situation.
        Double input = Double.MAX_VALUE;
        Date result = DoubleConversions.toSqlDate(input, converter);
        assertNotNull(result);
        assertEquals(Long.MAX_VALUE, result.getTime(), "When the computed milliseconds exceed Long.MAX_VALUE, the cast should yield Long.MAX_VALUE");
    }

    @Test
    void toSqlDate_throwsClassCastExceptionWhenInputIsNotDouble() {
        Object notADouble = "123.45";
        assertThrows(ClassCastException.class, () -> DoubleConversions.toSqlDate(notADouble, converter), "Expected a ClassCastException when the input object is not a Double");
    }

    @ParameterizedTest(name = "[{index}] {0} sec → {1}")
    @MethodSource("provideValidDoubleConversions")
    void toSqlDate_validConversions(Double from, String expectedDate) {
        Date result = DoubleConversions.toSqlDate(from, converter);
        assertNotNull(result);
        assertEquals(epochMillisFromDate(expectedDate), result.getTime(), () -> "Expected date " + expectedDate + " for input " + from);
    }

    @ParameterizedTest(name = "[{index}] {0} sec → {1}")
    @MethodSource("provideFractionalSecondConversions")
    void toSqlDate_fractionalSeconds(Double from, String expectedDate) {
        Date result = DoubleConversions.toSqlDate(from, converter);
        assertEquals(epochMillisFromDate(expectedDate), result.getTime(), () -> "Fractional seconds test failed for input " + from);
    }

    @ParameterizedTest(name = "[{index}] {0} sec → {1}")
    @MethodSource("provideMillisecondBoundaryConversions")
    void toSqlDate_millisecondBoundaries(Double from, String expectedDate) {
        Date result = DoubleConversions.toSqlDate(from, converter);
        assertEquals(epochMillisFromDate(expectedDate), result.getTime(), () -> "Millisecond boundary test failed for input " + from);
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("provideSpecialDoubleConversions")
    void toSqlDate_specialValues(Object from, Object expected) {
        Date result = DoubleConversions.toSqlDate(from, converter);
        assertNotNull(result);
        if (expected instanceof String) {
            assertEquals(epochMillisFromDate((String) expected), result.getTime(), () -> "Expected epoch date for special value " + from);
        } else if (expected instanceof Long) {
            assertEquals(((Long) expected).longValue(), result.getTime(), () -> "Expected millisecond value " + expected + " for special value " + from);
        }
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("provideExtremeDoubleConversions")
    void toSqlDate_extremeValues(Object from, Object expected) {
        Date result = DoubleConversions.toSqlDate(from, converter);
        assertNotNull(result);
        if (expected instanceof String) {
            assertEquals(epochMillisFromDate((String) expected), result.getTime(), () -> "Expected epoch date for extreme value " + from);
        } else if (expected instanceof Long) {
            assertEquals(((Long) expected).longValue(), result.getTime(), () -> "Expected millisecond value " + expected + " for extreme value " + from);
        }
    }

    @Test
    void toSqlDate_converterArgumentIgnored() {
        // one day
        Double input = 86_400.0;
        // Pass a non‑null, non‑mock object as the converter.
        Object dummyConverter = new Object();
        Date result = DoubleConversions.toSqlDate(input, (Converter) dummyConverter);
        assertEquals(epochMillisFromDate("1970-01-02"), result.getTime(), "The converter argument should be ignored; result must match normal conversion.");
    }

    @Test
    void toSqlDate_converterIgnoredWithExtremeValue() {
        Double input = Double.MAX_VALUE;
        Object dummyConverter = new Object();
        Date result = DoubleConversions.toSqlDate(input, (Converter) dummyConverter);
        assertEquals(Long.MAX_VALUE, result.getTime(), "Even with a non‑null converter, overflow should still produce Long.MAX_VALUE.");
    }

    @ParameterizedTest(name = "[{index}] from={0} expects {1}")
    @MethodSource("provideInvalidInputs")
    void toSqlDate_invalidInputs(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DoubleConversions.toSqlDate(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
