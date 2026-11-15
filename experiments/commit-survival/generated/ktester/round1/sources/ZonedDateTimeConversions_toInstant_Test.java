package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toInstant_Test {

    // not used by the method but required by signature
    private static Converter converter;

    /**
     * A default {@link Converter} instance – the implementation does not use it.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default constructor for options.
        // The actual conversion logic inside toInstant does not use the Converter, so a simple
        // instance is sufficient.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none in this case)
        converter = null;
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// 1. Standard UTC conversion
        Arguments.of(ZonedDateTime.of(2023, 3, 25, 14, 30, 0, 0, ZoneId.of("UTC")), Instant.parse("2023-03-25T14:30:00Z"), defaultConverter, "Standard conversion: UTC ZonedDateTime to Instant"), // 2. New York (UTC‑4 on 2023‑03‑25)
        Arguments.of(ZonedDateTime.of(2023, 3, 25, 14, 30, 0, 0, ZoneId.of("America/New_York")), Instant.parse("2023-03-25T18:30:00Z"), defaultConverter, "Time‑zone offset handling: New York (UTC‑4) to UTC instant"), // 3. Tokyo (+09:00) – edge of year
        Arguments.of(ZonedDateTime.of(LocalDateTime.of(2023, 12, 31, 23, 59, 59), ZoneId.of("Asia/Tokyo")), Instant.parse("2023-12-31T14:59:59Z"), defaultConverter, "Edge of year conversion with positive offset (Tokyo UTC+9)"), // 4. Leap‑day handling
        Arguments.of(ZonedDateTime.of(LocalDateTime.of(2020, 2, 29, 12, 0, 0), ZoneId.of("UTC")), Instant.parse("2020-02-29T12:00:00Z"), defaultConverter, "Leap‑day handling – February 29th"), // 5. Ambiguous DST fall‑back time with explicit -04:00 offset
        Arguments.of(ZonedDateTime.of(LocalDateTime.of(2021, 11, 7, 1, 30, 0), ZoneId.of("America/New_York"), ZoneOffset.ofHours(-4)), Instant.parse("2021-11-07T05:30:00Z"), defaultConverter, "Ambiguous DST fall‑back time resolved with explicit -04:00 offset"), // 6. Maximum positive offset (+14) – Pacific/Kiritimati
        Arguments.of(ZonedDateTime.of(LocalDateTime.of(2023, 1, 1, 0, 0, 0), ZoneId.of("Pacific/Kiritimati")), Instant.parse("2022-12-31T10:00:00Z"), defaultConverter, "Maximum positive offset (+14) conversion"), // 7. Maximum negative offset (-12) – Etc/GMT+12
        Arguments.of(ZonedDateTime.of(LocalDateTime.of(2023, 1, 1, 0, 0, 0), ZoneId.of("Etc/GMT+12")), Instant.parse("2023-01-01T12:00:00Z"), defaultConverter, "Maximum negative offset (-12) conversion"), // 8. Nanosecond precision preservation
        Arguments.of(ZonedDateTime.of(LocalDateTime.of(2023, 3, 25, 14, 30, 0, 123_456_789), ZoneId.of("UTC")), Instant.ofEpochSecond(Instant.parse("2023-03-25T14:30:00Z").getEpochSecond(), 123_456_789L), defaultConverter, "Nanosecond precision test"), // 9. Pre‑epoch date (negative epoch seconds)
        Arguments.of(ZonedDateTime.of(LocalDateTime.of(1965, 7, 20, 20, 17, 40), ZoneId.of("UTC")), Instant.parse("1965-07-20T20:17:40Z"), defaultConverter, "Pre‑epoch date conversion"), // 10. Direct ZoneOffset (+05:30)
        Arguments.of(ZonedDateTime.of(LocalDateTime.of(2023, 3, 25, 12, 0, 0), ZoneOffset.ofHoursMinutes(5, 30)), Instant.parse("2023-03-25T06:30:00Z"), defaultConverter, "ZoneOffset directly as ZoneId (UTC+05:30)"), // 11. Non‑null Converter argument (ignored by implementation)
        Arguments.of(ZonedDateTime.of(2023, 3, 25, 14, 30, 0, 0, ZoneId.of("UTC")), Instant.parse("2023-03-25T14:30:00Z"), // Mockito mock – any non‑null instance
        mock(Converter.class), "Non‑null Converter argument is ignored"));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null source object – cast succeeds (null) but .toInstant() triggers NPE
        Arguments.of(null, NullPointerException.class, "Null source object should cause NullPointerException"), // Wrong type (String) – cast fails with ClassCastException
        Arguments.of("2023-03-25T14:30:00Z", ClassCastException.class, "Invalid type (String) should result in ClassCastException"));
    }

    @Test
    void toInstant_convertsZonedDateTimeToInstant_correctly() {
        // GIVEN: a ZonedDateTime representing a known instant
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 15, 10, 30, 45, 123_456_789, ZoneId.of("UTC"));
        // WHEN: the static toInstant method is invoked
        Instant result = ZonedDateTimeConversions.toInstant(zdt, converter);
        // THEN: the result matches the expected Instant
        Instant expected = zdt.toInstant();
        assertEquals(expected, result, "The Instant returned by toInstant should match the ZonedDateTime's instant");
    }

    @Test
    void toInstant_handlesDifferentTimeZones_correctly() {
        // GIVEN: a ZonedDateTime in a non‑UTC zone
        ZonedDateTime zdt = ZonedDateTime.of(2023, 12, 31, 23, 59, 59, 0, ZoneId.of("America/New_York"));
        // WHEN: converting to Instant
        Instant result = ZonedDateTimeConversions.toInstant(zdt, converter);
        // THEN: the instant reflects the same point on the timeline
        Instant expected = zdt.toInstant();
        assertEquals(expected, result);
    }

    @Test
    void toInstant_nullInput_throwsClassCastException() {
        // GIVEN: a null reference (the method casts the object to ZonedDateTime)
        Object nullObj = null;
        // WHEN / THEN: a ClassCastException is expected because the cast fails
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toInstant(nullObj, converter));
    }

    /* -------------------------------------------------------------
     *  Successful conversion test cases (no exception expected)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("successfulConversionProvider")
    void toInstant_convertsCorrectly(Object from, Instant expected, Converter converter, String description) {
        // WHEN
        Instant result = ZonedDateTimeConversions.toInstant(from, converter);
        // THEN
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Exception test cases (null or wrong type)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionProvider")
    void toInstant_throwsExpectedException(Object from, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toInstant(from, null), description);
    }
}
