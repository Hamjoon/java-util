package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.format.DateTimeFormatter;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toString_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    /**
     * Provides a stream of valid {@link ZonedDateTime} instances together with the
     * expected ISO‑DATE‑TIME string produced by {@code ZonedDateTimeConversions.toString}.
     */
    private static Stream<Arguments> validZonedDateTimeProvider() {
        return Stream.of(// UTC – full nanoseconds
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 123_456_789, ZoneOffset.UTC), "2023-05-10T15:30:45.123456789Z"), // Non‑UTC offset (Europe/Paris, summer time = +02:00)
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 123_456_789, ZoneId.of("Europe/Paris")), "2023-05-10T15:30:45.123456789+02:00"), // No fractional seconds (nanosecond = 0)
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.UTC), "2023-05-10T15:30:45Z"), // Negative offset (-05:00)
        Arguments.of(ZonedDateTime.of(2023, 12, 31, 23, 59, 59, 999_999_999, ZoneOffset.ofHours(-5)), "2023-12-31T23:59:59.999999999-05:00"), // Epoch start (1970‑01‑01T00:00Z)
        Arguments.of(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), "1970-01-01T00:00:00Z"), // Maximum nanosecond value with Asia/Tokyo (+09:00)
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 999_999_999, ZoneId.of("Asia/Tokyo")), "2023-05-10T15:30:45.999999999+09:00"), // DST spring‑forward gap (America/New_York, 2023‑03‑12 02:30 does not exist)
        // Java normalises it to the later offset, which yields -04:00.
        Arguments.of(ZonedDateTime.of(2023, 3, 12, 2, 30, 0, 0, ZoneId.of("America/New_York")), "2023-03-12T02:30:00-04:00"), // Half‑hour offset (Asia/Kathmandu +05:45)
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 12, 0, 0, 0, ZoneId.of("Asia/Kathmandu")), "2023-05-10T12:00:00+05:45"), // Leap‑second simulation (nanosecond overflow rolls to next second/day)
        Arguments.of(ZonedDateTime.of(2016, 12, 31, 23, 59, 59, 1_000_000_000, ZoneOffset.UTC), "2017-01-01T00:00:00Z"));
    }

    /**
     * Provides inputs that should cause an exception when passed to {@code toString}.
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null 'from' argument
        Arguments.of(null, NullPointerException.class), // Wrong type (String instead of ZonedDateTime)
        Arguments.of("2023-05-10T15:30:45Z", ClassCastException.class));
    }

    @Test
    void toString_formatsZonedDateTimeUsingIsoDateTime() {
        // given
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 123_456_789, ZoneId.of("UTC"));
        // when
        String result = ZonedDateTimeConversions.toString(zdt, mockConverter);
        // then
        String expected = zdt.format(DateTimeFormatter.ISO_DATE_TIME);
        assertEquals(expected, result);
    }

    @Test
    void toString_formatsZonedDateTimeWithOffset() {
        // given
        ZonedDateTime zdt = ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 0, // offset -05:00 (or -04:00 depending on DST)
        ZoneId.of("America/New_York"));
        // when
        String result = ZonedDateTimeConversions.toString(zdt, mockConverter);
        // then
        String expected = zdt.format(DateTimeFormatter.ISO_DATE_TIME);
        assertEquals(expected, result);
    }

    @Test
    void toString_nullInput_throwsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toString(nullObj, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("validZonedDateTimeProvider")
    void toString_validZonedDateTime_returnsExpectedString(ZonedDateTime input, String expected) {
        // The method does not use the Converter, so we can safely pass null.
        String result = ZonedDateTimeConversions.toString(input, null);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {0} throws {1}")
    @MethodSource("exceptionProvider")
    void toString_invalidInput_throwsExpectedException(Object input, Class<? extends Throwable> exceptionClass) {
        assertThrows(exceptionClass, () -> ZonedDateTimeConversions.toString(input, null));
    }
}
