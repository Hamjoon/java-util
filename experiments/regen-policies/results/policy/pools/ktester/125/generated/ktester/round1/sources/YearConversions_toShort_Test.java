package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Year;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class YearConversions_toShort_Test {

    /**
     * Shared {@link Converter} instance used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no overrides are required for these simple conversions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Explicitly release the static reference; helps GC in large test suites.
        converter = null;
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// Simple numeric literals
        // int within range
        Arguments.of(12345, (short) 12345), // int > Short.MAX_VALUE (wrap)
        Arguments.of(40000, (short) 40000), // int within range
        Arguments.of(200, (short) 200), // negative int
        Arguments.of(-10, (short) -10), // Short primitive (autoboxed)
        Arguments.of((short) 123, (short) 123), // Byte primitive (autoboxed)
        Arguments.of((byte) 7, (short) 7), // String representations
        Arguments.of("12345", (short) 12345), Arguments.of("  32767  ", (short) 32767), // Year instance
        Arguments.of(Year.of(2023), (short) 2023), // BigDecimal & BigInteger
        Arguments.of(new BigDecimal("12345"), (short) 12345), Arguments.of(new BigInteger("50000"), (short) 50000), // Floating‑point numbers (truncated)
        Arguments.of(12345.67d, (short) 12345), Arguments.of(12345.67f, (short) 12345), // Long literals (including overflow)
        Arguments.of(40000L, (short) 40000), // Atomic numeric wrappers
        Arguments.of(new AtomicInteger(200), (short) 200), Arguments.of(new AtomicLong(40000L), (short) 40000), // Edge values
        // Short.MAX_VALUE
        Arguments.of(32767, (short) 32767), // Short.MIN_VALUE
        Arguments.of(-32768, (short) -32768), // wraps to Short.MIN_VALUE
        Arguments.of(32768, (short) 32768), // wraps to Short.MAX_VALUE
        Arguments.of(-32769, (short) -32769), // Null source – per Converter contract, null yields 0 for primitive targets
        Arguments.of(null, (short) 0));
    }

    private static Stream<Arguments> provideExceptionScenarios() {
        return Stream.of(// Non‑numeric types
        Arguments.of(true, IllegalArgumentException.class), Arguments.of('A', IllegalArgumentException.class), Arguments.of(new Object(), IllegalArgumentException.class), // Null converter reference
        Arguments.of(123, NullPointerException.class), // Strings that cannot be parsed to an int
        Arguments.of("abc", NumberFormatException.class), // > Integer.MAX_VALUE
        Arguments.of("2147483648", NumberFormatException.class), // < Integer.MIN_VALUE
        Arguments.of("-2147483649", NumberFormatException.class), // Unsupported array type
        Arguments.of(new int[] { 1, 2, 3 }, IllegalArgumentException.class), // Unsupported temporal type
        Arguments.of(java.time.LocalDate.now(), IllegalArgumentException.class));
    }

    @Test
    void toShort_givenYearWithinShortRange_returnsExactValue() {
        // GIVEN
        // 2023 fits comfortably into a short
        Year year = Year.of(2023);
        // WHEN
        short result = YearConversions.toShort(year, converter);
        // THEN
        assertEquals((short) 2023, result, "Year 2023 should be converted to short 2023");
    }

    @Test
    void toShort_givenYearExceedingShortRange_behavesLikeJavaNarrowingConversion() {
        // GIVEN
        // 32777, exceeds short range
        int largeYear = Short.MAX_VALUE + 10;
        Year year = Year.of(largeYear);
        // WHEN
        short result = YearConversions.toShort(year, converter);
        // THEN
        // Java narrows by discarding high-order bits; the expected value is:
        short expected = (short) largeYear;
        assertEquals(expected, result, "Year beyond Short.MAX_VALUE should be narrowed according to Java casting rules");
    }

    @Test
    void toShort_givenNullSource_returnsZero() {
        // GIVEN
        Object nullSource = null;
        // WHEN
        short result = YearConversions.toShort(nullSource, converter);
        // THEN
        assertEquals((short) 0, result, "Null source must be converted to 0 for primitive numeric targets");
    }

    @Test
    void toShort_givenIntegerNumber_convertsViaConverter() {
        // GIVEN
        // Integer is a Number; Converter can handle it directly
        Integer number = 1999;
        // WHEN
        short result = YearConversions.toShort(number, converter);
        // THEN
        assertEquals((short) 1999, result, "Integer 1999 should be converted to short 1999");
    }

    /* -------------------------------------------------------------
     *  Successful conversion test cases (numeric result expected)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: toShort({0}) => {1}")
    @MethodSource("provideSuccessfulConversions")
    void toShort_successfulConversions(Object from, short expected) {
        // WHEN
        short result = YearConversions.toShort(from, converter);
        // THEN
        assertEquals(expected, result, () -> "Conversion of " + (from == null ? "null" : from.getClass().getSimpleName()) + " should yield short value " + expected);
    }

    /* -------------------------------------------------------------
     *  Exception‑producing test cases
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: toShort({0}) throws {1}")
    @MethodSource("provideExceptionScenarios")
    void toShort_exceptions(Object from, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> YearConversions.toShort(from, converter), () -> "Conversion of " + (from == null ? "null" : from.getClass().getSimpleName()) + " should throw " + expectedException.getSimpleName());
    }
}
