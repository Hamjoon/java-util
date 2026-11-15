package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.RoundingMode;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toBigDecimal_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance; use the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static references.
        converter = null;
    }

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// Midnight – should be zero with scale 9.
        Arguments.of(LocalTime.of(0, 0, 0, 0), "0.000000000"), // Typical time with nanoseconds.
        Arguments.of(LocalTime.of(12, 34, 56, 789_123_456), "45296.789123456"), // Maximum representable time of the day.
        Arguments.of(LocalTime.of(23, 59, 59, 999_999_999), "86399.999999999"));
    }

    /**
     * Verify that a typical LocalTime is converted to a BigDecimal representing
     * the number of seconds with nanosecond precision (scale 9).
     */
    @Test
    void toBigDecimal_typicalTime() {
        // given
        LocalTime time = LocalTime.of(12, 34, 56, 123_456_789);
        // expected value: nanoseconds of day divided by 1_000_000_000 with scale 9, HALF_UP
        BigDecimal expected = new BigDecimal(time.toNanoOfDay()).divide(LocalTimeConversions.BILLION, 9, java.math.RoundingMode.HALF_UP);
        // when
        BigDecimal actual = LocalTimeConversions.toBigDecimal(time, converter);
        // then
        assertEquals(0, expected.compareTo(actual), () -> "Expected " + expected + " but got " + actual);
    }

    /**
     * Verify conversion of midnight (00:00) which should yield zero.
     */
    @Test
    void toBigDecimal_midnight() {
        // given
        LocalTime midnight = LocalTime.MIDNIGHT;
        // 0.000000000
        BigDecimal expected = BigDecimal.ZERO.setScale(9);
        // when
        BigDecimal actual = LocalTimeConversions.toBigDecimal(midnight, converter);
        // then
        assertEquals(0, expected.compareTo(actual), () -> "Midnight should convert to " + expected + " but got " + actual);
    }

    /**
     * Verify conversion of the maximum representable LocalTime (23:59:59.999999999).
     */
    @Test
    void toBigDecimal_maxTime() {
        // given
        LocalTime max = LocalTime.of(23, 59, 59, 999_999_999);
        BigDecimal expected = new BigDecimal(max.toNanoOfDay()).divide(LocalTimeConversions.BILLION, 9, java.math.RoundingMode.HALF_UP);
        // when
        BigDecimal actual = LocalTimeConversions.toBigDecimal(max, converter);
        // then
        assertEquals(0, expected.compareTo(actual), () -> "Max time should convert to " + expected + " but got " + actual);
    }

    /**
     * Ensure that passing a non‑LocalTime object results in a ClassCastException.
     */
    @Test
    void toBigDecimal_invalidSourceThrowsClassCastException() {
        // given
        Object notATime = "not a LocalTime";
        // when / then
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toBigDecimal(notATime, null), "A non‑LocalTime source must cause a ClassCastException");
    }

    /**
     * Verify conversion of typical {@link LocalTime} values to {@link BigDecimal}
     * with a scale of 9. The expected decimal string is taken from the JSON test
     * case definition.
     */
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("normalConversionProvider")
    void toBigDecimal_normalConversion(LocalTime from, String expectedString) {
        // Expected value constructed from the provided decimal string.
        BigDecimal expected = new BigDecimal(expectedString).setScale(9, RoundingMode.HALF_UP);
        // The method does not use the converter, so we can pass {@code null}.
        BigDecimal actual = LocalTimeConversions.toBigDecimal(from, null);
        // Compare values ignoring possible representation differences.
        assertEquals(0, expected.compareTo(actual), () -> "Expected " + expected + " but got " + actual);
        // Verify that the scale is exactly 9.
        assertEquals(9, actual.scale(), () -> "Scale should be 9 for value " + actual);
    }

    @Test
    void toBigDecimal_scaleVerification() {
        // given
        // 4 nanoseconds
        LocalTime time = LocalTime.of(1, 2, 3, 4);
        BigDecimal expected = new BigDecimal("3723.000000004").setScale(9, RoundingMode.HALF_UP);
        // when
        BigDecimal actual = LocalTimeConversions.toBigDecimal(time, null);
        // then
        assertEquals(0, expected.compareTo(actual), () -> "Expected " + expected + " but got " + actual);
        assertEquals(9, actual.scale(), () -> "Returned BigDecimal must have a scale of 9");
    }

    @Test
    void toBigDecimal_smallestPositiveNanosecond() {
        // given
        LocalTime time = LocalTime.of(0, 0, 0, 1);
        BigDecimal expected = new BigDecimal("0.000000001").setScale(9, RoundingMode.HALF_UP);
        // when
        BigDecimal actual = LocalTimeConversions.toBigDecimal(time, null);
        // then
        assertEquals(0, expected.compareTo(actual), () -> "Expected " + expected + " but got " + actual);
        assertEquals(9, actual.scale(), () -> "Scale must be 9 for the smallest nanosecond");
    }

    @Test
    void toBigDecimal_nullFromThrowsNPE() {
        // when / then
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toBigDecimal(null, null), "Passing null as the source should raise NullPointerException");
    }

    @Test
    void toBigDecimal_converterArgumentIgnored() {
        // given: a Converter that throws if its convert method is ever called.
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        LocalTime time = LocalTime.of(12, 0, 0, 0);
        BigDecimal expected = new BigDecimal("43200.000000000").setScale(9, RoundingMode.HALF_UP);
        // when
        BigDecimal actual = LocalTimeConversions.toBigDecimal(time, throwingConverter);
        // then
        assertEquals(0, expected.compareTo(actual), () -> "Expected " + expected + " but got " + actual);
        assertEquals(9, actual.scale(), () -> "Scale must be 9 even when a custom Converter is supplied");
    }
}
