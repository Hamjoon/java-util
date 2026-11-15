package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toOffsetDateTime_Test {

    @Mock
    private Converter // Converter is not used by the method but required by signature
    mockConverter;

    private static Stream<Arguments> provideValidZonedDateTimeConversions() {
        return Stream.of(// UTC – offset +00:00
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 123_456_789, ZoneId.of("UTC")), OffsetDateTime.of(2023, 5, 10, 15, 30, 45, 123_456_789, ZoneOffset.UTC), "A ZonedDateTime in UTC is correctly converted to an OffsetDateTime with offset +00:00"), // Fixed non‑UTC zone (America/New_York) during standard time
        Arguments.of(ZonedDateTime.of(2023, 11, 5, 2, 30, 0, 0, ZoneId.of("America/New_York")), OffsetDateTime.of(2023, 11, 5, 2, 30, 0, 0, ZoneOffset.of("-05:00")), "A ZonedDateTime in America/New_York (standard time) retains offset -05:00"), // DST summer in New York (offset -04:00)
        Arguments.of(ZonedDateTime.of(2023, 7, 15, 12, 0, 0, 0, ZoneId.of("America/New_York")), OffsetDateTime.of(2023, 7, 15, 12, 0, 0, 0, ZoneOffset.of("-04:00")), "A ZonedDateTime during daylight saving time in New York should have offset -04:00"), // DST spring forward – 02:30 does not exist, ZonedDateTime normalises to 03:30
        Arguments.of(ZonedDateTime.of(2023, 3, 12, 2, 30, 0, 0, ZoneId.of("America/New_York")), OffsetDateTime.of(2023, 3, 12, 3, 30, 0, 0, ZoneOffset.of("-04:00")), "Spring‑forward transition in New York normalises to 03:30 with offset -04:00"), // Paris summer (CEST) – offset +02:00
        Arguments.of(ZonedDateTime.of(2023, 10, 29, 1, 30, 0, 0, ZoneId.of("Europe/Paris")), OffsetDateTime.of(2023, 10, 29, 1, 30, 0, 0, ZoneOffset.of("+02:00")), "Paris summer time (CEST) should have offset +02:00"), // Paris after DST ends – same wall‑clock time now offset +01:00
        Arguments.of(ZonedDateTime.of(2023, 10, 29, 2, 30, 0, 0, ZoneId.of("Europe/Paris")), OffsetDateTime.of(2023, 10, 29, 2, 30, 0, 0, ZoneOffset.of("+01:00")), "Paris after DST ends should have offset +01:00"), // Fixed offset zone UTC+05:30
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneId.of("UTC+05:30")), OffsetDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.of("+05:30")), "A ZonedDateTime with a fixed offset zone (UTC+05:30) retains that offset"), // Epoch instant (1970‑01‑01T00:00Z)
        Arguments.of(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC")), OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), "Conversion of the epoch instant should produce an OffsetDateTime at 1970‑01‑01T00:00Z"), // Maximum supported year (9999) in UTC
        Arguments.of(ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999, ZoneId.of("UTC")), OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999, ZoneOffset.UTC), "Maximum supported year (9999) conversion retains all fields and UTC offset"));
    }

    private static Stream<Arguments> provideInvalidInputs() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "Passing null as the source object should result in a NullPointerException"), Arguments.of(// a String, not a ZonedDateTime
        "2023-05-10T15:30:45Z", ClassCastException.class, "Providing a String instead of ZonedDateTime should cause a ClassCastException"));
    }

    @Test
    void toOffsetDateTime_withValidZonedDateTime_returnsEquivalentOffsetDateTime() {
        // given
        ZonedDateTime zdt = ZonedDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        10, // hour, minute, second, nano
        12, // hour, minute, second, nano
        30, // hour, minute, second, nano
        45, // hour, minute, second, nano
        0, // +02:00 in summer
        ZoneId.of("Europe/Paris"));
        // when
        OffsetDateTime result = ZonedDateTimeConversions.toOffsetDateTime(zdt, mockConverter);
        // then
        OffsetDateTime expected = zdt.toOffsetDateTime();
        assertEquals(expected, result, "The OffsetDateTime should match the one produced by ZonedDateTime#toOffsetDateTime()");
    }

    @Test
    void toOffsetDateTime_withNullInput_throwsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toOffsetDateTime(nullObj, mockConverter), "Calling toOffsetDateTime with null should throw NullPointerException");
    }

    @Test
    void toOffsetDateTime_withIncorrectType_throwsClassCastException() {
        // given
        Object notAZonedDateTime = "2023-05-10T12:30:45+02:00";
        // when / then
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toOffsetDateTime(notAZonedDateTime, mockConverter), "Calling toOffsetDateTime with a non‑ZonedDateTime object should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideValidZonedDateTimeConversions")
    void toOffsetDateTime_validInputs_returnsExpectedOffsetDateTime(ZonedDateTime from, OffsetDateTime expected, String description) {
        // when
        OffsetDateTime result = ZonedDateTimeConversions.toOffsetDateTime(from, null);
        // then
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideInvalidInputs")
    void toOffsetDateTime_invalidInputs_throwsExpectedException(Object from, Class<? extends Throwable> expectedException, String description) {
        // when / then
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toOffsetDateTime(from, null), description);
    }
}
