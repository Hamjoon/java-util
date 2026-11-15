package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toBigDecimal_Test {

    @Mock
    Converter // not used by the method but required by the signature
    converter;

    @Mock
    Converter // not used by the implementation but required by the signature
    mockConverter;

    private static Stream<Arguments> validLocalTimeConversions() {
        return Stream.of(// Midnight
        Arguments.of(LocalTime.of(0, 0, 0, 0), "0.000000000"), // Typical time with nanosecond component
        Arguments.of(LocalTime.of(12, 34, 56, 789_123_456), "45296.789123456"), // One nanosecond before next day (maximum LocalTime)
        Arguments.of(LocalTime.of(23, 59, 59, 999_999_999), "86399.999999999"));
    }

    private static Stream<Arguments> additionalValidLocalTimeConversions() {
        return Stream.of(// Earliest non‑zero nanosecond
        Arguments.of(LocalTime.of(0, 0, 0, 1), "0.000000001"), // Half‑second value
        Arguments.of(LocalTime.of(0, 0, 0, 500_000_000), "0.500000000"), // Exactly noon, no nanoseconds
        Arguments.of(LocalTime.of(12, 0, 0, 0), "43200.000000000"));
    }

    private static Stream<Arguments> invalidLocalTimeConversions() {
        return Stream.of(// Null source
        Arguments.of(null, NullPointerException.class), // Wrong type (Integer)
        Arguments.of(Integer.valueOf(123), ClassCastException.class));
    }

    @Test
    @DisplayName("Typical LocalTime conversion to BigDecimal")
    void testToBigDecimalTypical() {
        // 12:34:56.123456789
        LocalTime lt = LocalTime.of(12, 34, 56, 123_456_789);
        // Expected value: nanos of day divided by 1_000_000_000 with scale 9, HALF_UP
        BigDecimal expected = new BigDecimal(lt.toNanoOfDay()).divide(BigDecimal.valueOf(1_000_000_000L), 9, RoundingMode.HALF_UP);
        BigDecimal actual = LocalTimeConversions.toBigDecimal(lt, converter);
        assertEquals(expected, actual, "BigDecimal conversion did not match expected value");
    }

    @Test
    @DisplayName("Midnight (00:00) conversion to zero BigDecimal")
    void testToBigDecimalMidnight() {
        LocalTime lt = LocalTime.MIDNIGHT;
        // 0.000000000
        BigDecimal expected = BigDecimal.ZERO.setScale(9);
        BigDecimal actual = LocalTimeConversions.toBigDecimal(lt, converter);
        assertEquals(expected, actual, "Midnight should convert to zero with scale 9");
    }

    @Test
    @DisplayName("Maximum time (23:59:59.999999999) conversion")
    void testToBigDecimalMaximum() {
        LocalTime lt = LocalTime.of(23, 59, 59, 999_999_999);
        BigDecimal expected = new BigDecimal(lt.toNanoOfDay()).divide(BigDecimal.valueOf(1_000_000_000L), 9, RoundingMode.HALF_UP);
        BigDecimal actual = LocalTimeConversions.toBigDecimal(lt, converter);
        assertEquals(expected, actual, "Maximum time conversion is incorrect");
    }

    @Test
    @DisplayName("Null source should throw NullPointerException")
    void testToBigDecimalNullSource() {
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toBigDecimal(null, converter), "Passing null should result in NPE");
    }

    @Test
    @DisplayName("Invalid source type should throw ClassCastException")
    void testToBigDecimalInvalidSource() {
        Object notATime = "2023-01-01";
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toBigDecimal(notATime, converter), "Passing a non‑LocalTime object should result in CCE");
    }

    /* -------------------------------------------------------------
     *  Valid LocalTime conversions – first group from the JSON spec
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("validLocalTimeConversions")
    @DisplayName("Valid LocalTime conversions (group: Valid LocalTime conversions)")
    void testValidLocalTimeConversions(LocalTime from, String expectedString) {
        // Expected value is the number of seconds (including fractional part) with scale 9
        BigDecimal expected = new BigDecimal(expectedString).setScale(9, RoundingMode.UNNECESSARY);
        BigDecimal actual = LocalTimeConversions.toBigDecimal(from, null);
        assertEquals(expected, actual, () -> "Conversion of " + from + " did not match expected BigDecimal");
    }

    /* -------------------------------------------------------------
     *  Additional valid LocalTime conversions – second group from JSON
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("additionalValidLocalTimeConversions")
    @DisplayName("Additional valid LocalTime conversions")
    void testAdditionalValidLocalTimeConversions(LocalTime from, String expectedString) {
        BigDecimal expected = new BigDecimal(expectedString).setScale(9, RoundingMode.UNNECESSARY);
        BigDecimal actual = LocalTimeConversions.toBigDecimal(from, null);
        assertEquals(expected, actual, () -> "Conversion of " + from + " did not match expected BigDecimal");
    }

    /* -------------------------------------------------------------
     *  Converter parameter handling – ensure the converter is ignored
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("Non‑null Converter instance is ignored")
    void testConverterParameterHandling() {
        LocalTime from = LocalTime.of(1, 2, 3, 4);
        // Create a concrete (anonymous) Converter instance – the implementation does not use it
        Converter realConverter = new Converter() {
        };
        BigDecimal expected = new BigDecimal("3723.000000004").setScale(9, RoundingMode.UNNECESSARY);
        BigDecimal actual = LocalTimeConversions.toBigDecimal(from, realConverter);
        assertEquals(expected, actual, "Result should be identical to conversion without a Converter");
    }

    /* -------------------------------------------------------------
     *  Invalid LocalTime conversions – exception cases
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} expects {1}")
    @MethodSource("invalidLocalTimeConversions")
    @DisplayName("Invalid LocalTime conversions (exception cases)")
    void testInvalidLocalTimeConversions(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalTimeConversions.toBigDecimal(from, null), () -> "Expected " + expectedException.getSimpleName() + " when converting " + from);
    }
}
