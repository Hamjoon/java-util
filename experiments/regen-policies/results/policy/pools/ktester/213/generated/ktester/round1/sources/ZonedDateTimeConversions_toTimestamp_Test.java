package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toTimestamp_Test {

    /**
     * A shared {@link Converter} instance – the method under test does not use it,
     *  but the signature requires it.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter needs a ConverterOptions instance; the default constructor is sufficient
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference
        converter = null;
    }

    private static Stream<Arguments> provideValidZonedDateTimes() {
        // a dummy non‑null converter – the method ignores it
        Converter dummy = new Converter(new ConverterOptions());
        // a mock converter that would throw if used (should never be called)
        Converter throwing = mock(Converter.class, invocation -> {
            throw new IllegalStateException("Converter should not be invoked");
        });
        return Stream.of(// simple UTC case
        Arguments.of("Valid ZonedDateTime in UTC", ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 123_456_789, ZoneId.of("UTC")), Timestamp.from(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 123_456_789, ZoneId.of("UTC")).toInstant()), null), // non‑UTC zone (America/New_York)
        Arguments.of("ZonedDateTime with a non‑UTC zone", ZonedDateTime.of(2023, 12, 31, 23, 0, 0, 0, ZoneId.of("America/New_York")), Timestamp.from(ZonedDateTime.of(2023, 12, 31, 23, 0, 0, 0, ZoneId.of("America/New_York")).toInstant()), null), // dummy non‑null converter
        Arguments.of("Valid ZonedDateTime with a dummy Converter", ZonedDateTime.of(2022, 2, 28, 12, 15, 30, 0, ZoneId.of("Europe/Paris")), Timestamp.from(ZonedDateTime.of(2022, 2, 28, 12, 15, 30, 0, ZoneId.of("Europe/Paris")).toInstant()), dummy), // epoch instant
        Arguments.of("Epoch instant (1970‑01‑01T00:00:00Z)", ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), Timestamp.from(Instant.EPOCH), null), // maximum nanosecond precision
        Arguments.of("Maximum nanosecond precision", ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 999_999_999, ZoneOffset.UTC), Timestamp.from(ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 999_999_999, ZoneOffset.UTC).toInstant()), null), // DST ambiguous time – earlier offset (UTC‑04:00)
        Arguments.of("DST ambiguous time (earlier offset) in New York", ZonedDateTime.of(2021, 11, 7, 1, 30, 0, 0, ZoneId.of("America/New_York")), Timestamp.from(ZonedDateTime.of(2021, 11, 7, 1, 30, 0, 0, ZoneId.of("America/New_York")).toInstant()), null), // custom fixed offset +05:30
        Arguments.of("Custom ZoneOffset (+05:30)", ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneId.ofOffset("UTC", ZoneOffset.ofHoursMinutes(5, 30))), Timestamp.from(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneId.ofOffset("UTC", ZoneOffset.ofHoursMinutes(5, 30))).toInstant()), null), // throwing converter – should not affect conversion
        Arguments.of("Non‑null Converter that throws if used", ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.UTC), Timestamp.from(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.UTC).toInstant()), throwing));
    }

    @Test
    void toTimestamp_convertsZonedDateTimeCorrectly() {
        // given
        ZonedDateTime zdt = ZonedDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        10, // hour, minute, second, nano
        12, // hour, minute, second, nano
        30, // hour, minute, second, nano
        45, // hour, minute, second, nano
        123_000_000, // zone
        ZoneId.of("UTC"));
        Timestamp expected = Timestamp.from(zdt.toInstant());
        // when
        Timestamp actual = ZonedDateTimeConversions.toTimestamp(zdt, converter);
        // then
        assertEquals(expected, actual, "Timestamp produced from ZonedDateTime should match expected value");
    }

    @Test
    void toTimestamp_throwsClassCastExceptionWhenSourceIsNotZonedDateTime() {
        // given
        Object notZdt = "2023-05-10T12:30:45Z";
        // when / then
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toTimestamp(notZdt, converter), "Method should throw ClassCastException when the source object is not a ZonedDateTime");
    }

    @Test
    void toTimestamp_throwsNullPointerExceptionWhenSourceIsNull() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toTimestamp(nullSource, converter), "Method should throw NullPointerException when the source is null");
    }

    @Test
    void toTimestamp_throwsIllegalArgumentExceptionWhenInstantOutOfRange() {
        // given – a far‑future date beyond java.sql.Timestamp range
        ZonedDateTime farFuture = ZonedDateTime.of(292_278_995, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        // when / then
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toTimestamp(farFuture, converter), "Conversion of an instant outside Timestamp range must throw IllegalArgumentException");
    }
}
