package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toSqlDate_Test {

    private Converter converter;

    private static final ZoneId TEST_ZONE = ZoneId.of("UTC");

    /**
     * Simple test implementation of {@link Converter} that merely returns the
     * supplied {@link ConverterOptions}.  It is sufficient for the conversion
     * logic exercised by {@code toSqlDate}.
     */
    private static class TestConverter extends Converter {

        private final ConverterOptions options;

        TestConverter(ConverterOptions options) {
            super(options);
            this.options = options;
        }

        @Override
        public ConverterOptions getOptions() {
            return options;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default converter used by the majority of tests (UTC zone).
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(TEST_ZONE);
        converter = new TestConverter(options);
    }

    private static Stream<Arguments> standardConversions() {
        return Stream.of(Arguments.of("Convert a java.util.Date to java.sql.Date using UTC zone – the date part should match the instant's UTC calendar day.", java.util.Date.from(Instant.parse("2023-05-15T12:34:56Z")), java.sql.Date.valueOf(LocalDate.of(2023, 5, 15))), Arguments.of("Convert an Instant directly – the resulting java.sql.Date must represent the same UTC calendar day.", Instant.parse("2022-12-01T03:45:00Z"), java.sql.Date.valueOf(LocalDate.of(2022, 12, 1))), Arguments.of("Convert a java.sql.Timestamp – the time component is ignored and only the date part is kept.", Timestamp.valueOf("2021-08-20 23:59:59.123"), java.sql.Date.valueOf(LocalDate.of(2021, 8, 20))), Arguments.of("Convert a leap‑year LocalDateTime – the resulting java.sql.Date must represent the same calendar day.", LocalDateTime.of(2020, 2, 29, 10, 15, 30), java.sql.Date.valueOf(LocalDate.of(2020, 2, 29))));
    }

    private static Stream<Arguments> zoneHandling() {
        return Stream.of(Arguments.of("LocalDate is converted via its start‑of‑day instant; the converter’s zone (America/New_York) does not shift the calendar day, so the resulting java.sql.Date matches the original LocalDate.", LocalDate.of(2022, 3, 31), ZoneId.of("America/New_York"), java.sql.Date.valueOf(LocalDate.of(2022, 3, 31))), Arguments.of("ZonedDateTime 2022‑03‑27T03:30+01:00 converted to America/New_York yields 2022‑03‑26.", ZonedDateTime.of(2022, 3, 27, 3, 30, 0, 0, ZoneId.of("Europe/Paris")), ZoneId.of("America/New_York"), java.sql.Date.valueOf(LocalDate.of(2022, 3, 26))), Arguments.of("OffsetDateTime with +02:00 offset translates to 2022‑12‑31T21:00Z; in UTC the calendar day remains 2022‑12‑31.", OffsetDateTime.parse("2022-12-31T23:00:00+02:00"), ZoneId.of("UTC"), java.sql.Date.valueOf(LocalDate.of(2022, 12, 31))), Arguments.of("A +14:00 offset pushes the instant back to the previous UTC day; the resulting java.sql.Date reflects 2021‑12‑31.", OffsetDateTime.parse("2022-01-01T01:00:00+14:00"), ZoneId.of("UTC"), java.sql.Date.valueOf(LocalDate.of(2021, 12, 31))));
    }

    private static Stream<Arguments> nonInstantSources() {
        // Calendar set to 2022‑02‑28 23:59:59 GMT
        Calendar calGmt = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calGmt.set(2022, Calendar.FEBRUARY, 28, 23, 59, 59);
        calGmt.set(Calendar.MILLISECOND, 0);
        // Calendar set to 2022‑02‑28 23:59:59 PST (America/Los_Angeles)
        Calendar calPst = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        calPst.set(2022, Calendar.FEBRUARY, 28, 23, 59, 59);
        calPst.set(Calendar.MILLISECOND, 0);
        Calendar calPstClone = (Calendar) calPst.clone();
        return Stream.of(Arguments.of("Calendar set to 2022‑02‑28 23:59:59 GMT; the instant corresponds to the same UTC day, so the sql.Date is 2022‑02‑28.", calGmt, ZoneId.of("UTC"), java.sql.Date.valueOf(LocalDate.of(2022, 2, 28))), Arguments.of("Calendar set to 2022‑02‑28 23:59:59 PST (UTC‑8). The instant is 2022‑03‑01 07:59:59Z, therefore the sql.Date after conversion to UTC is 2022‑03‑01.", calPstClone, ZoneId.of("UTC"), java.sql.Date.valueOf(LocalDate.of(2022, 3, 1))), Arguments.of("Leap‑year LocalDateTime converted using Asia/Tokyo (UTC+9); the instant still falls on 2020‑02‑29 in Tokyo, so the sql.Date remains 2020‑02‑29.", LocalDateTime.of(2020, 2, 29, 10, 15, 30), ZoneId.of("Asia/Tokyo"), java.sql.Date.valueOf(LocalDate.of(2020, 2, 29))));
    }

    private static Stream<Arguments> boundaryDates() {
        return Stream.of(Arguments.of("Instant one second before the Unix epoch maps to 1969‑12‑31 UTC.", Instant.ofEpochSecond(-1), java.sql.Date.valueOf(LocalDate.of(1969, 12, 31))), Arguments.of("Maximum representable Instant (9999‑12‑31T23:59:59Z) should produce a sql.Date of 9999‑12‑31.", Instant.ofEpochSecond(253402300799L), java.sql.Date.valueOf(LocalDate.of(9999, 12, 31))));
    }

    private static Stream<Arguments> unsupportedSources() {
        return Stream.of(Arguments.of("The source object type is not supported by toInstant; conversion should fail with IllegalArgumentException.", new Object()), Arguments.of("A String value cannot be converted to an Instant by the internal toInstant logic, resulting in IllegalArgumentException.", "some random string"));
    }

    private static Stream<Arguments> outOfRangeDates() {
        return Stream.of(Arguments.of("Instant corresponds to year 0‑01‑01, which is outside the java.sql.Date supported range (year 1‑9999); Date.valueOf should throw IllegalArgumentException.", // 0000-01-01T00:00:00Z
        Instant.ofEpochSecond(-62167219200L)), Arguments.of("Instant is one second beyond the maximum representable date (year 10000‑01‑01); converting to java.sql.Date should raise IllegalArgumentException.", // 10000-01-01T00:00:00Z
        Instant.ofEpochSecond(253402300800L)));
    }

    @Test
    void toSqlDate_fromJavaUtilDate_returnsCorrectSqlDate() {
        // GIVEN – a java.util.Date representing 2023‑03‑15 at midnight UTC
        Instant instant = Instant.parse("2023-03-15T00:00:00Z");
        java.util.Date sourceDate = java.util.Date.from(instant);
        // WHEN
        java.util.Date result = BigDecimalConversions.toSqlDate(sourceDate, converter);
        // THEN – the result should be a java.sql.Date for the same LocalDate
        java.sql.Date expected = java.sql.Date.valueOf(LocalDate.of(2023, 3, 15));
        assertEquals(expected, result);
    }

    @Test
    void toSqlDate_fromInstant_returnsCorrectSqlDate() {
        // GIVEN – an Instant that falls on 2021‑12‑31 in UTC
        // time part is ignored for date conversion
        Instant instant = Instant.parse("2021-12-31T13:45:30Z");
        // WHEN
        java.util.Date result = BigDecimalConversions.toSqlDate(instant, converter);
        // THEN – the date portion should be 2021‑12‑31
        java.sql.Date expected = java.sql.Date.valueOf(LocalDate.of(2021, 12, 31));
        assertEquals(expected, result);
    }

    @Test
    void toSqlDate_fromStringRepresentingDate_isConvertedViaConverter() {
        // GIVEN – a string that the Converter can turn into a java.util.Date
        String dateString = "2022-07-04";
        // The Converter can parse ISO‑8601 dates into java.util.Date (via its internal logic).
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(dateString, converter);
        // THEN – the resulting sql date should correspond to 2022‑07‑04
        java.sql.Date expected = java.sql.Date.valueOf(LocalDate.of(2022, 7, 4));
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("standardConversions")
    void toSqlDate_standardConversions(String description, Object from, java.sql.Date expected) {
        java.util.Date result = BigDecimalConversions.toSqlDate(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zoneHandling")
    void toSqlDate_zoneHandling(String description, Object from, ZoneId converterZone, java.sql.Date expected) {
        // Use a converter with the supplied zone
        ConverterOptions opt = new ConverterOptions();
        opt.setZoneId(converterZone);
        Converter conv = new TestConverter(opt);
        java.util.Date result = BigDecimalConversions.toSqlDate(from, conv);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nonInstantSources")
    void toSqlDate_nonInstantSources(String description, Object from, ZoneId converterZone, java.sql.Date expected) {
        ConverterOptions opt = new ConverterOptions();
        opt.setZoneId(converterZone);
        Converter conv = new TestConverter(opt);
        java.util.Date result = BigDecimalConversions.toSqlDate(from, conv);
        assertEquals(expected, result, description);
    }

    @Test
    void toSqlDate_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toSqlDate(null, converter), "Passing a null source object should cause a NullPointerException.");
    }

    @Test
    void toSqlDate_nullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toSqlDate(Instant.now(), null), "A null Converter cannot provide a ZoneId; the method should throw NullPointerException.");
    }

    @Test
    void toSqlDate_localTimeUnsupported_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toSqlDate(LocalTime.of(15, 30), converter), "LocalTime lacks a date component; conversion should raise IllegalArgumentException.");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("boundaryDates")
    void toSqlDate_boundaryDates(String description, Instant instant, java.sql.Date expected) {
        java.util.Date result = BigDecimalConversions.toSqlDate(instant, converter);
        assertEquals(expected, result, description);
    }

    @Test
    void toSqlDate_converterWithNullZoneId_throwsNullPointerException() {
        // Converter constructed with a null ZoneId
        ConverterOptions opt = new ConverterOptions();
        opt.setZoneId(null);
        Converter badConverter = new TestConverter(opt);
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toSqlDate(Instant.parse("2023-01-01T00:00:00Z"), badConverter), "Converter with null ZoneId should cause NPE when zone is accessed.");
    }

    @Test
    void toSqlDate_converterReturningNullOptions_throwsNullPointerException() {
        // Anonymous subclass that returns null from getOptions()
        Converter nullOptionsConverter = new Converter(new ConverterOptions()) {

            @Override
            public ConverterOptions getOptions() {
                return null;
            }
        };
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toSqlDate(Instant.parse("2023-01-01T00:00:00Z"), nullOptionsConverter), "Converter returning null Options should cause NPE.");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedSources")
    void toSqlDate_unsupportedSource_throwsIllegalArgumentException(String description, Object from) {
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toSqlDate(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("outOfRangeDates")
    void toSqlDate_outOfRangeDates_throwsIllegalArgumentException(String description, Instant instant) {
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toSqlDate(instant, converter), description);
    }
}
