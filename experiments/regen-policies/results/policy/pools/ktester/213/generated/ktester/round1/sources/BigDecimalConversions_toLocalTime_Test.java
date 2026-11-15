package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toLocalTime_Test {

    @Mock
    private Converter // not used by the method but required for the signature
    mockConverter;

    private static Stream<Arguments> validNormalRangeProvider() {
        return Stream.of(Arguments.of(BigDecimal.ZERO, "00:00", "Zero seconds should be converted to midnight (00:00)."), Arguments.of(new BigDecimal("43200"), "12:00", "Exactly half a day (43200 seconds) should become 12:00."), Arguments.of(new BigDecimal("86399.999999999"), "23:59:59.999999999", "Maximum allowed value – one nanosecond before next day – should produce 23:59:59.999999999."));
    }

    private static Stream<Arguments> validFractionalProvider() {
        return Stream.of(// 0.0000000015 s → 1 ns (truncated)
        Arguments.of(new BigDecimal("0.0000000015"), "00:00:00.000000001", "Fractional seconds that produce a non‑integer nanosecond count are truncated (floor) when converted to a long; 0.0000000015 s → 1 ns."), // 0.5 s → 500 000 000 ns
        Arguments.of(new BigDecimal("0.5"), "00:00:00.500000000", "Half‑second value should be converted to 500 000 000 ns, yielding 00:00:00.500000000."), // 123.4500 s → 2 min 3 s 450 ms
        Arguments.of(new BigDecimal("123.4500"), "00:02:03.450000000", "Value with trailing zeros is handled correctly; 123.4500 s → 2 min 3 s 450 ms."));
    }

    private static Stream<Arguments> invalidOutOfRangeProvider() {
        return Stream.of(Arguments.of(new BigDecimal("-0.1"), "Negative seconds must trigger IllegalArgumentException with the range‑check message."), Arguments.of(new BigDecimal("86400"), "Exactly 24 hours (86400 seconds) exceeds the allowed maximum and should raise IllegalArgumentException."), Arguments.of(new BigDecimal("86399.9999999995"), "Value exceeds the allowed maximum by a fraction of a nanosecond; should trigger the range‑check exception."), Arguments.of(new BigDecimal("10000000000"), "A very large value causes nanos.longValue() to overflow, leading to a DateTimeException that is wrapped as IllegalArgumentException."));
    }

    @Test
    void toLocalTime_zeroSeconds_returnsMidnight() {
        // given
        BigDecimal seconds = BigDecimal.ZERO;
        // when
        LocalTime result = BigDecimalConversions.toLocalTime(seconds, mockConverter);
        // then
        assertEquals(LocalTime.MIDNIGHT, result);
    }

    @Test
    void toLocalTime_middaySeconds_returnsNoon() {
        // given
        // 12 * 60 * 60
        BigDecimal seconds = new BigDecimal("43200");
        // when
        LocalTime result = BigDecimalConversions.toLocalTime(seconds, mockConverter);
        // then
        assertEquals(LocalTime.NOON, result);
    }

    @Test
    void toLocalTime_maximumValidSeconds_returnsLastNanosecondOfDay() {
        // given
        // 86 399.999 999 999 seconds = one nanosecond before the next day
        BigDecimal seconds = new BigDecimal("86399.999999999");
        // when
        LocalTime result = BigDecimalConversions.toLocalTime(seconds, mockConverter);
        // then
        // The expected time is 23:59:59.999999999
        LocalTime expected = LocalTime.of(23, 59, 59, 999_999_999);
        assertEquals(expected, result);
    }

    @Test
    void toLocalTime_negativeSeconds_throwsIllegalArgumentException() {
        // given
        BigDecimal seconds = new BigDecimal("-1");
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toLocalTime(seconds, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"));
    }

    @Test
    void toLocalTime_secondsBeyondDay_throwsIllegalArgumentException() {
        // given
        // exactly one day, which is out of range
        BigDecimal seconds = new BigDecimal("86400");
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toLocalTime(seconds, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validNormalRangeProvider")
    void toLocalTime_validNormalRange(BigDecimal from, String expectedString, String description) {
        // when
        LocalTime result = BigDecimalConversions.toLocalTime(from, mockConverter);
        // then
        LocalTime expected = LocalTime.parse(expectedString);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validFractionalProvider")
    void toLocalTime_validFractionalSeconds(BigDecimal from, String expectedString, String description) {
        LocalTime result = BigDecimalConversions.toLocalTime(from, mockConverter);
        LocalTime expected = LocalTime.parse(expectedString);
        assertEquals(expected, result, description);
    }

    @Test
    void toLocalTime_tinySubNanosecond_returnsMidnight() {
        // given
        // less than 1 ns
        BigDecimal from = new BigDecimal("0.0000000005");
        // when
        LocalTime result = BigDecimalConversions.toLocalTime(from, mockConverter);
        // then
        assertEquals(LocalTime.MIDNIGHT, result, "A value smaller than one nanosecond is truncated to zero nanoseconds, resulting in midnight.");
    }

    @Test
    void toLocalTime_negativeZero_isTreatedAsZero() {
        // given
        // mathematically zero
        BigDecimal from = new BigDecimal("-0");
        // when
        LocalTime result = BigDecimalConversions.toLocalTime(from, mockConverter);
        // then
        assertEquals(LocalTime.MIDNIGHT, result, "BigDecimal representing -0 is mathematically zero; conversion should succeed and return midnight.");
    }

    @Test
    void toLocalTime_nonNullConverter_isIgnored() {
        // given
        // 1 hour
        BigDecimal from = new BigDecimal("3600");
        // when
        LocalTime result = BigDecimalConversions.toLocalTime(from, mockConverter);
        // then
        assertEquals(LocalTime.of(1, 0), result, "The method does not use the Converter argument; providing a non‑null placeholder must not affect the result.");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidOutOfRangeProvider")
    void toLocalTime_invalidOutOfRange(BigDecimal from, String description) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toLocalTime(from, mockConverter), description);
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"), "Exception message must contain the range‑check hint.");
    }

    @Test
    void toLocalTime_nonBigDecimal_input_throwsClassCastException() {
        // given
        Object notABigDecimal = "notABigDecimal";
        // when / then
        assertThrows(ClassCastException.class, () -> BigDecimalConversions.toLocalTime(notABigDecimal, mockConverter), "If the supplied object is not a BigDecimal, the method throws ClassCastException before entering the try‑catch block.");
    }

    @Test
    void toLocalTime_nullFrom_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toLocalTime(null, mockConverter), "Passing null for the 'from' argument causes a NPE when the method attempts to call multiply on a null reference.");
    }
}
