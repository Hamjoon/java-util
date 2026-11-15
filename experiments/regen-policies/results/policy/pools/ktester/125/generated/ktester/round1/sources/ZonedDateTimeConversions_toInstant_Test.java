package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toInstant_Test {

    /**
     * The Converter instance required by the focal method.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter does not participate in the conversion performed by toInstant,
        // but a non‑null instance satisfies the method contract.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validInstantProvider() {
        return Stream.of(// Basic UTC conversion
        Arguments.of("Converts a UTC ZonedDateTime to the same instant; verifies basic conversion.", ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 0, ZoneId.of("UTC")), Instant.parse("2023-05-10T12:30:45Z")), // America/New_York (UTC‑4) conversion
        Arguments.of("Converts a ZonedDateTime in America/New_York (UTC‑4) to the correct UTC instant.", ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 0, ZoneId.of("America/New_York")), Instant.parse("2023-05-10T16:30:45Z")), // ZonedDateTime created from an Instant (Asia/Tokyo) – should retain the instant
        Arguments.of("Ensures that a ZonedDateTime created from an Instant retains the original instant regardless of the zone.", ZonedDateTime.ofInstant(Instant.parse("2020-01-01T00:00:00Z"), ZoneId.of("Asia/Tokyo")), Instant.parse("2020-01-01T00:00:00Z")), // Nanosecond precision preservation
        Arguments.of("Verifies that nanosecond precision is preserved during conversion.", ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 123_456_789, ZoneId.of("UTC")), Instant.ofEpochSecond(1640995199L, 123_456_789L)), // Extreme boundary: Instant.MIN
        Arguments.of("Conversion of the smallest representable Instant should succeed without overflow.", ZonedDateTime.ofInstant(Instant.MIN, ZoneId.of("UTC")), Instant.MIN), // Extreme boundary: Instant.MAX
        Arguments.of("Conversion of the largest representable Instant should succeed without overflow.", ZonedDateTime.ofInstant(Instant.MAX, ZoneId.of("UTC")), Instant.MAX), // DST transition in Europe/Paris (spring forward)
        Arguments.of("Europe/Paris switches to DST on 2022‑03‑27; verifies correct instant handling across the DST transition.", ZonedDateTime.of(2022, 3, 27, 2, 30, 0, 0, ZoneId.of("Europe/Paris")), Instant.parse("2022-03-27T00:30:00Z")), // Maximum positive offset (+14) – Pacific/Kiritimati
        Arguments.of("Zone with the maximum positive offset (+14) should convert to the correct UTC instant.", ZonedDateTime.of(2022, 12, 31, 23, 0, 0, 0, ZoneId.of("Pacific/Kiritimati")), Instant.parse("2022-12-31T09:00:00Z")), // Maximum negative offset (-12) – Etc/GMT+12 (note the sign inversion in the ID)
        Arguments.of("Zone with the maximum negative offset (-12) should convert correctly.", ZonedDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneId.of("Etc/GMT+12")), Instant.parse("2022-01-01T12:00:00Z")), // Custom offset (+05:30)
        Arguments.of("Custom ZoneOffset (+05:30) verifies conversion with a non‑standard offset.", ZonedDateTime.of(2022, 6, 15, 12, 0, 0, 0, ZoneOffset.ofHoursMinutes(5, 30)), Instant.parse("2022-06-15T06:30:00Z")));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null source object
        Arguments.of("Passing null as the source object should throw NullPointerException due to the cast to ZonedDateTime.", null, NullPointerException.class), // Wrong type: OffsetDateTime
        Arguments.of("Providing an object that is not a ZonedDateTime must result in a ClassCastException.", OffsetDateTime.now(), ClassCastException.class), // Converter argument ignored – still a valid ZonedDateTime
        Arguments.of("The Converter argument is ignored; conversion still returns the correct instant.", ZonedDateTime.ofInstant(Instant.parse("1970-01-01T00:00:00Z"), ZoneId.of("UTC")), // No exception expected – we will test the success path separately,
        // but to keep this provider focused on exceptions we skip this case.
        // (Handled in validInstantProvider.)
        null), // Overflow positive side – exceeds Instant.MAX
        Arguments.of("The ZonedDateTime represents a local date‑time that, when adjusted by the maximum positive offset (+18:00), exceeds Instant.MAX; toInstant() must throw DateTimeException.", ZonedDateTime.of(LocalDateTime.of(999_999_999, 12, 31, 23, 59, 59, 999_999_999), ZoneOffset.ofHours(18)), DateTimeException.class), // Overflow negative side – falls before Instant.MIN
        Arguments.of("The ZonedDateTime represents a local date‑time that, when adjusted by the maximum negative offset (‑18:00), falls before Instant.MIN; toInstant() must throw DateTimeException.", ZonedDateTime.of(LocalDateTime.of(-999_999_999, 1, 1, 0, 0, 0, 0), ZoneOffset.ofHours(-18)), DateTimeException.class)).filter(// Remove the placeholder case where no exception is expected
        args -> args.get()[2] != null);
    }

    @Test
    void toInstant_withZonedDateTime_returnsCorrectInstant() {
        // GIVEN a ZonedDateTime in a known zone
        ZonedDateTime zdt = ZonedDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        15, // hour, minute, second, nano
        10, // hour, minute, second, nano
        30, // hour, minute, second, nano
        45, // hour, minute, second, nano
        123_000_000, ZoneId.of("America/New_York"));
        Instant expected = zdt.toInstant();
        // WHEN the static conversion method is invoked
        Instant actual = ZonedDateTimeConversions.toInstant(zdt, converter);
        // THEN the returned Instant matches the ZonedDateTime's instant
        assertEquals(expected, actual, "The Instant produced by toInstant should match the ZonedDateTime's instant");
    }

    @Test
    void toInstant_withNull_throwsNullPointerException() {
        // GIVEN a null source object
        Object source = null;
        // WHEN & THEN a NullPointerException is thrown because the cast yields null and .toInstant() is invoked
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toInstant(source, converter), "Calling toInstant with null should throw NullPointerException");
    }

    @Test
    void toInstant_withInvalidType_throwsClassCastException() {
        // GIVEN an object that is NOT a ZonedDateTime (e.g., a LocalDateTime)
        LocalDateTime notZdt = LocalDateTime.of(2023, 5, 15, 10, 30);
        // WHEN & THEN a ClassCastException is thrown because the method casts directly to ZonedDateTime
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toInstant(notZdt, converter), "Calling toInstant with a non‑ZonedDateTime should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validInstantProvider")
    void toInstant_validInputs_returnsExpectedInstant(String description, ZonedDateTime input, Instant expected) {
        // WHEN
        Instant actual = ZonedDateTimeConversions.toInstant(input, converter);
        // THEN
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void toInstant_invalidInputs_throwsExpectedException(String description, Object input, Class<? extends Throwable> expectedException) {
        // WHEN & THEN
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toInstant(input, converter), description);
    }
}
