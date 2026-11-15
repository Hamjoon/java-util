package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.util.stream.Stream;

class DateConversions_toYear_Test {

    /**
     * Default converter used for the simple test cases (system default zone).
     */
    private Converter defaultConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a Converter with default options for the majority of tests.
        // uses system default zone
        ConverterOptions defaultOptions = new ConverterOptions();
        defaultConverter = new Converter(defaultOptions);
    }

    /**
     * Creates a {@link Converter} that uses the supplied {@link ZoneId}.
     *
     * @param zone the zone the converter should operate in
     * @return a fully‑initialised {@code Converter}
     */
    private static Converter converterWithZone(ZoneId zone) {
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(zone);
        return new Converter(options);
    }

    static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// epoch 1970‑01‑01T00:00:00Z → year 1970 (UTC)
        Arguments.of("Epoch (1970‑01‑01T00:00:00Z) with UTC", new Date(0L), ZoneId.of("UTC"), Year.of(1970)), // 2023‑01‑01T01:00:00Z shifted to America/New_York (UTC‑5) → 2022‑12‑31
        Arguments.of("2023‑01‑01T01:00:00Z with America/New_York", Date.from(Instant.parse("2023-01-01T01:00:00Z")), ZoneId.of("America/New_York"), Year.of(2022)));
    }

    static Stream<Arguments> exactBoundaryProvider() {
        return Stream.of(Arguments.of("Start of year 2000 UTC", Date.from(Instant.parse("2000-01-01T00:00:00Z")), Year.of(2000)), Arguments.of("One second before epoch year (1969‑12‑31T23:59:59Z)", Date.from(Instant.parse("1969-12-31T23:59:59Z")), Year.of(1969)));
    }

    static Stream<Arguments> timeZoneOffsetProvider() {
        return Stream.of(// 2022‑12‑31T23:00Z → Europe/Paris (+01) → 2023‑01‑01
        Arguments.of("2022‑12‑31T23:00Z shifted to Europe/Paris", Date.from(Instant.parse("2022-12-31T23:00:00Z")), ZoneId.of("Europe/Paris"), Year.of(2023)), // 2023‑01‑01T01:00Z → America/New_York (UTC‑5) → 2022‑12‑31
        Arguments.of("2023‑01‑01T01:00Z shifted to America/New_York", Date.from(Instant.parse("2023-01-01T01:00:00Z")), ZoneId.of("America/New_York"), Year.of(2022)), // 2023‑07‑01T12:00Z → Asia/Tokyo (+09) stays in 2023
        Arguments.of("2023‑07‑01T12:00Z shifted to Asia/Tokyo", Date.from(Instant.parse("2023-07-01T12:00:00Z")), ZoneId.of("Asia/Tokyo"), Year.of(2023)));
    }

    static Stream<Arguments> farFuturePastProvider() {
        return Stream.of(Arguments.of("Maximum supported year 9999‑12‑31T23:59:59Z", Date.from(Instant.parse("9999-12-31T23:59:59Z")), Year.of(9999)), Arguments.of("One day before Unix epoch (1969‑12‑31)", // -86400000 ms
        new Date(-86_400_000L), Year.of(1969)));
    }

    static Stream<Arguments> invalidDateProvider() {
        return Stream.of(Arguments.of("Date with Long.MAX_VALUE milliseconds (out of range)", new Date(Long.MAX_VALUE)), Arguments.of("Date with Long.MIN_VALUE milliseconds (out of range)", new Date(Long.MIN_VALUE)));
    }

    /**
     * Verify that a normal {@link Date} is converted to the correct {@link Year}
     * using the default time‑zone of the {@link Converter}.
     */
    @Test
    void testToYearBasic() {
        // 2022‑03‑15T10:20:30Z → year 2022 regardless of zone (the instant is in 2022)
        Date date = Date.from(Instant.parse("2022-03-15T10:20:30Z"));
        Year result = DateConversions.toYear(date, defaultConverter);
        assertEquals(Year.of(2022), result);
    }

    /**
     * Verify that the zone supplied via {@link ConverterOptions} influences the
     * resulting {@link Year}. The instant 2022‑01‑01T01:00:00Z is still 2022 in UTC,
     * but in America/Los_Angeles (UTC‑08) it is 2021‑12‑31, therefore the year should be 2021.
     */
    @Test
    void testToYearWithDifferentZone() {
        // Prepare options with a specific ZoneId
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of("America/Los_Angeles"));
        Converter converter = new Converter(options);
        // 2022‑01‑01T01:00:00Z → 2021‑12‑31 17:00:00‑08:00
        Date date = Date.from(Instant.parse("2022-01-01T01:00:00Z"));
        Year result = DateConversions.toYear(date, converter);
        assertEquals(Year.of(2021), result);
    }

    /**
     * The implementation casts the source object to {@link Date}. Passing {@code null}
     * should therefore result in a {@link NullPointerException}.
     */
    @Test
    void testToYearNullSourceThrowsNPE() {
        assertThrows(NullPointerException.class, () -> DateConversions.toYear(null, defaultConverter));
    }

    /**
     * Passing an object that is not a {@link Date} should also cause a {@link ClassCastException}
     * because the method performs an unchecked cast.
     */
    @Test
    void testToYearInvalidSourceThrowsClassCastException() {
        Object notADate = "2022-01-01";
        assertThrows(ClassCastException.class, () -> DateConversions.toYear(notADate, defaultConverter));
    }

    /**
     * -------- Valid conversions ------------------------------------------------
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionsProvider")
    void testValidConversions(String description, Date from, ZoneId zone, Year expected) {
        Converter converter = converterWithZone(zone);
        Year result = DateConversions.toYear(from, converter);
        assertEquals(expected, result, description);
    }

    /**
     * -------- Null handling ---------------------------------------------------
     */
    @Test
    void testToYearNullSourceThrowsNPE_Json() {
        Converter converter = converterWithZone(ZoneId.of("UTC"));
        assertThrows(NullPointerException.class, () -> DateConversions.toYear(null, converter));
    }

    @Test
    void testToYearNullConverterThrowsNPE() {
        Date date = new Date(0L);
        assertThrows(NullPointerException.class, () -> DateConversions.toYear(date, null));
    }

    /**
     * -------- Invalid type handling -------------------------------------------
     */
    @Test
    void testToYearInvalidTypeThrowsClassCastException() {
        Object notADate = Integer.valueOf(12345);
        assertThrows(ClassCastException.class, () -> DateConversions.toYear(notADate, defaultConverter));
    }

    /**
     * -------- Exact year boundaries (UTC) -------------------------------------
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exactBoundaryProvider")
    void testExactYearBoundaries(String description, Date from, Year expected) {
        Converter converter = converterWithZone(ZoneId.of("UTC"));
        Year result = DateConversions.toYear(from, converter);
        assertEquals(expected, result, description);
    }

    /**
     * -------- Time‑zone offset effects ----------------------------------------
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("timeZoneOffsetProvider")
    void testTimeZoneOffsetEffects(String description, Date from, ZoneId zone, Year expected) {
        Converter converter = converterWithZone(zone);
        Year result = DateConversions.toYear(from, converter);
        assertEquals(expected, result, description);
    }

    /**
     * -------- Leap year handling ----------------------------------------------
     */
    @Test
    void testLeapYearHandling() {
        Date leapDay = Date.from(Instant.parse("2020-02-29T12:34:56Z"));
        Converter converter = converterWithZone(ZoneId.of("UTC"));
        Year result = DateConversions.toYear(leapDay, converter);
        assertEquals(Year.of(2020), result, "Leap day 2020‑02‑29 should yield year 2020");
    }

    /**
     * -------- Far future and far past years -----------------------------------
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("farFuturePastProvider")
    void testFarFutureAndPast(String description, Date from, Year expected) {
        Converter converter = converterWithZone(ZoneId.of("UTC"));
        Year result = DateConversions.toYear(from, converter);
        assertEquals(expected, result, description);
    }

    /**
     * -------- Invalid Date value handling (out‑of‑range instants) ------------
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidDateProvider")
    void testInvalidDateThrowsDateTimeException(String description, Date from) {
        Converter converter = converterWithZone(ZoneId.of("UTC"));
        assertThrows(DateTimeException.class, () -> DateConversions.toYear(from, converter), description);
    }
}
