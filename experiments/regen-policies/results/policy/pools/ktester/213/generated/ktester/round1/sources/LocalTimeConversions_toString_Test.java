package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toString_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    private static Stream<org.junit.jupiter.params.provider.Arguments> validLocalTimeProvider() {
        return Stream.of(// Standard time without nanoseconds
        org.junit.jupiter.params.provider.Arguments.of(LocalTime.of(12, 34, 56), "12:34:56"), // Time with nanosecond precision – full fractional part must be retained
        org.junit.jupiter.params.provider.Arguments.of(LocalTime.of(12, 34, 56, 123_456_789), "12:34:56.123456789"), // Midnight – seconds are omitted in ISO_LOCAL_TIME when they are zero
        org.junit.jupiter.params.provider.Arguments.of(LocalTime.of(0, 0), "00:00"), // Hour with zero minutes – minutes are shown, seconds omitted
        org.junit.jupiter.params.provider.Arguments.of(LocalTime.of(13, 0), "13:00"), // Maximum representable LocalTime – includes full nanosecond fraction
        org.junit.jupiter.params.provider.Arguments.of(LocalTime.of(23, 59, 59, 999_999_999), "23:59:59.999999999"));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidLocalTimeProvider() {
        return Stream.of(// Null source object – should raise NPE because of the cast
        org.junit.jupiter.params.provider.Arguments.of(null, NullPointerException.class), // Wrong type (String) – should raise ClassCastException during cast
        org.junit.jupiter.params.provider.Arguments.of("12:34:56", ClassCastException.class));
    }

    @Test
    void toString_formatsStandardLocalTime() {
        // given
        // 13:45:30
        LocalTime time = LocalTime.of(13, 45, 30);
        // when
        String result = LocalTimeConversions.toString(time, mockConverter);
        // then
        assertEquals("13:45:30", result);
    }

    @Test
    void toString_formatsLocalTimeWithNanos() {
        // given
        // 09:05:07.123
        LocalTime time = LocalTime.of(9, 5, 7, 123_000_000);
        // when
        String result = LocalTimeConversions.toString(time, mockConverter);
        // then
        assertEquals("09:05:07.123", result);
    }

    @Test
    void toString_formatsMidnight() {
        // given
        // 00:00
        LocalTime midnight = LocalTime.MIDNIGHT;
        // when
        String result = LocalTimeConversions.toString(midnight, mockConverter);
        // then
        assertEquals("00:00", result);
    }

    @Test
    void toString_nullInput_throwsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toString(nullObj, mockConverter));
    }

    @Test
    void toString_nonLocalTimeInput_throwsClassCastException() {
        // given
        Object notALocalTime = "13:45:30";
        // when / then
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toString(notALocalTime, mockConverter));
    }

    /* -----------------------------------------------------------
     *  Valid LocalTime conversions – parameterized tests
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0} → \"{1}\"")
    @MethodSource("validLocalTimeProvider")
    void toString_validLocalTimes(LocalTime from, String expected) {
        // when
        String result = LocalTimeConversions.toString(from, mockConverter);
        // then
        assertEquals(expected, result);
    }

    /* -----------------------------------------------------------
     *  Invalid LocalTime conversions – parameterized tests
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → throws {1}")
    @MethodSource("invalidLocalTimeProvider")
    void toString_invalidLocalTimes(Object from, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> LocalTimeConversions.toString(from, mockConverter));
    }
}
