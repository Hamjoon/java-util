package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.Year;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.YEAR;

@ExtendWith(MockitoExtension.class)
class YearConversions_toBigDecimal_Test {

    /**
     * Converter used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter has a no‑arg constructor that
        // creates an empty ConverterOptions instance.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long‑running suites.
        converter = null;
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Valid numeric conversions
        Arguments.of(42, BigDecimal.valueOf(42)), Arguments.of(123L, BigDecimal.valueOf(123)), Arguments.of(5.9, BigDecimal.valueOf(5)), Arguments.of("77", BigDecimal.valueOf(77)), Arguments.of("  88  ", BigDecimal.valueOf(88)), Arguments.of(Year.of(2021), BigDecimal.valueOf(2021)), Arguments.of(Map.of(YEAR, 1999), BigDecimal.valueOf(1999)), // Additional numeric conversions
        Arguments.of(new BigInteger("123456"), BigDecimal.valueOf(123456)), Arguments.of(new BigDecimal("9876.543"), BigDecimal.valueOf(9876)), Arguments.of((short) 7, BigDecimal.valueOf(7)), Arguments.of((byte) -3, BigDecimal.valueOf(-3)), // Boundary and overflow cases
        Arguments.of(Integer.MAX_VALUE, BigDecimal.valueOf(2147483647)), Arguments.of(Integer.MIN_VALUE, BigDecimal.valueOf(-2147483648)), // Long overflow – casting to int yields -1
        Arguments.of(Long.MAX_VALUE, BigDecimal.valueOf((int) Long.MAX_VALUE)), // Long.MIN_VALUE cast to int yields 0
        Arguments.of(Long.MIN_VALUE, BigDecimal.valueOf((int) Long.MIN_VALUE)), Arguments.of(0, BigDecimal.valueOf(0)), Arguments.of(-42, BigDecimal.valueOf(-42)), // Map with non‑integer YEAR values
        Arguments.of(Map.of(YEAR, "2022"), BigDecimal.valueOf(2022)), Arguments.of(Map.of(YEAR, new BigDecimal("3033.99")), BigDecimal.valueOf(3033)), // Boolean and atomic conversions
        Arguments.of(true, BigDecimal.valueOf(1)), Arguments.of(false, BigDecimal.valueOf(0)), Arguments.of(new AtomicInteger(55), BigDecimal.valueOf(55)), Arguments.of(new AtomicLong(1234567890123L), BigDecimal.valueOf((int) 1234567890123L)), Arguments.of(new AtomicBoolean(true), BigDecimal.valueOf(1)), // String edge cases
        Arguments.of("+15", BigDecimal.valueOf(15)), Arguments.of("0010", BigDecimal.valueOf(10)), Arguments.of("10.0", BigDecimal.valueOf(10)));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// Invalid or exceptional conversions
        Arguments.of("abc", IllegalArgumentException.class), Arguments.of(Map.of("otherKey", 10), IllegalArgumentException.class), Arguments.of(null, IllegalArgumentException.class), Arguments.of(new Object(), IllegalArgumentException.class), // Additional exception scenarios
        Arguments.of("+", IllegalArgumentException.class), Arguments.of("-", IllegalArgumentException.class), Arguments.of("12abc", IllegalArgumentException.class), Arguments.of(Double.NaN, IllegalArgumentException.class), Arguments.of(Double.POSITIVE_INFINITY, IllegalArgumentException.class), Arguments.of(Float.NaN, IllegalArgumentException.class), Arguments.of(Float.NEGATIVE_INFINITY, IllegalArgumentException.class), Arguments.of(Map.of(YEAR, true), IllegalArgumentException.class), Arguments.of(Map.of(YEAR, java.util.List.of(2020)), IllegalArgumentException.class), Arguments.of("2147483648", IllegalArgumentException.class), Arguments.of("-2147483649", IllegalArgumentException.class), // String edge cases that are invalid
        Arguments.of("10.5", IllegalArgumentException.class), Arguments.of("   ", IllegalArgumentException.class), Arguments.of("", IllegalArgumentException.class), // Map with YEAR key but null value
        Arguments.of(Map.of(YEAR, null), IllegalArgumentException.class), // Null converter handling – the method should reject a null converter
        // will be invoked with null converter below
        Arguments.of(42, IllegalArgumentException.class));
    }

    @Test
    void toBigDecimal_fromYear_returnsYearValue() {
        // given
        Year year = Year.of(2023);
        // when
        BigDecimal result = YearConversions.toBigDecimal(year, converter);
        // then
        assertEquals(BigDecimal.valueOf(2023), result, "BigDecimal should contain the int value of the Year instance");
    }

    @Test
    void toBigDecimal_fromInteger_returnsSameValue() {
        // given
        Integer source = 1999;
        // when
        BigDecimal result = YearConversions.toBigDecimal(source, converter);
        // then
        assertEquals(BigDecimal.valueOf(1999), result);
    }

    @Test
    void toBigDecimal_fromStringNumeric_returnsParsedValue() {
        // given
        String source = "2020";
        // when
        BigDecimal result = YearConversions.toBigDecimal(source, converter);
        // then
        assertEquals(BigDecimal.valueOf(2020), result);
    }

    @Test
    void toBigDecimal_fromNull_returnsZero() {
        // given
        Object source = null;
        // when
        BigDecimal result = YearConversions.toBigDecimal(source, converter);
        // then
        assertEquals(BigDecimal.ZERO, result, "Null source should be converted to 0 according to Converter null‑handling rules");
    }

    @Test
    void toBigDecimal_fromUnsupportedType_throwsException() {
        // given
        // not convertible to int by default
        Object unsupported = new Object();
        // when / then
        assertThrows(RuntimeException.class, () -> YearConversions.toBigDecimal(unsupported, converter), "Conversion of an unsupported type should result in a runtime exception");
    }

    /* --------------------------------------------------------------
     *  Successful conversion cases – parameterized
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → {1}")
    @MethodSource("successfulConversionProvider")
    void toBigDecimal_successful(Object from, BigDecimal expected) {
        // when
        BigDecimal result = YearConversions.toBigDecimal(from, converter);
        // then
        assertEquals(expected, result, () -> "Expected BigDecimal " + expected + " for input " + from);
    }

    /* --------------------------------------------------------------
     *  Expected exception cases – parameterized
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} throws {1}")
    @MethodSource("exceptionConversionProvider")
    void toBigDecimal_throws(Object from, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> YearConversions.toBigDecimal(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input " + from);
    }

    /**
     * Special test for the *null converter* scenario because the generic
     * {@code exceptionConversionProvider} supplies a non‑null {@code from}
     * value. We invoke the method with a {@code null} converter explicitly.
     */
    @Test
    void toBigDecimal_nullConverter_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toBigDecimal(42, null), "Passing a null Converter should result in IllegalArgumentException");
    }
}
