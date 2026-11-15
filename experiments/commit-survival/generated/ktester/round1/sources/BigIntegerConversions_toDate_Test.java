package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toDate_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter has a no‑arg constructor that uses default options.
        // If the library requires explicit options, we create a default one.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for each test.
        converter = null;
    }

    static Stream<Arguments> dateSource() {
        // 2023‑01‑01T00:00:00Z
        Date src = new Date(1672531200000L);
        return Stream.of(Arguments.of("When the source object is a java.util.Date, toDate should return an equivalent Date (same instant).", src, src));
    }

    static Stream<Arguments> instantSource() {
        Instant instant = Instant.ofEpochMilli(1672531200000L);
        return Stream.of(Arguments.of("When the source object is a java.time.Instant, toDate should convert it to a Date with the same instant.", instant, Date.from(instant)));
    }

    static Stream<Arguments> localDateTimeSource() {
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = ldt.atZone(zone).toInstant();
        return Stream.of(Arguments.of("When the source object is a LocalDateTime, toDate should treat it as a date‑time in the JVM default zone and convert it to the corresponding instant.", ldt, Date.from(instant)));
    }

    static Stream<Arguments> longSource() {
        Long epochMillis = 1672531200000L;
        return Stream.of(Arguments.of("When the source object is a Long containing epoch milliseconds, toDate should interpret it as an instant and return the matching Date.", epochMillis, Date.from(Instant.ofEpochMilli(epochMillis))));
    }

    static Stream<Arguments> bigIntegerSecondsSource() {
        // epoch seconds for 2023‑01‑01T00:00:00Z
        BigInteger seconds = new BigInteger("1672531200");
        Instant instant = Instant.ofEpochSecond(seconds.longValue());
        return Stream.of(Arguments.of("When the source object is a BigInteger representing epoch seconds, toDate should convert it to an Instant via Instant.ofEpochSecond and then to a Date.", seconds, Date.from(instant)));
    }

    static Stream<Arguments> unsupportedSource() {
        return Stream.of(Arguments.of("When the source object is of an unsupported type (e.g., String), toDate should throw an IllegalArgumentException.", "2023-01-01"));
    }

    static Stream<Arguments> localDateSource() {
        LocalDate ld = LocalDate.of(2023, 1, 1);
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = ld.atStartOfDay(zone).toInstant();
        return Stream.of(Arguments.of("LocalDate should be interpreted as midnight at the start of the day in the default zone and converted to the corresponding instant.", ld, Date.from(instant)));
    }

    static Stream<Arguments> localTimeSource() {
        LocalTime lt = LocalTime.of(12, 34, 56, 0);
        // combine with epoch date 1970‑01‑01 in the system default zone
        LocalDate epochDay = LocalDate.ofEpochDay(0);
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = lt.atDate(epochDay).atZone(zone).toInstant();
        return Stream.of(Arguments.of("LocalTime is combined with the epoch date (1970‑01‑01) in the default zone to produce an instant.", lt, Date.from(instant)));
    }

    @Test
    void toDate_convertsBigIntegerMillisecondsToDate() {
        // GIVEN: a BigInteger representing epoch milliseconds (2021‑01‑01T00:00:00Z)
        BigInteger epochMillis = BigInteger.valueOf(1_609_459_200_000L);
        Instant expectedInstant = Instant.ofEpochMilli(epochMillis.longValue());
        Date expectedDate = Date.from(expectedInstant);
        // WHEN: converting the BigInteger to a java.util.Date
        Date actualDate = BigIntegerConversions.toDate(epochMillis, converter);
        // THEN: the resulting Date matches the expected instant
        assertEquals(expectedDate, actualDate, "The Date produced from a BigInteger of epoch milliseconds should match the expected value.");
    }

    @Test
    void toDate_convertsBigIntegerSecondsToDate() {
        // GIVEN: a BigInteger representing epoch seconds (2021‑01‑01T00:00:00Z)
        BigInteger epochSeconds = BigInteger.valueOf(1_609_459_200L);
        // The conversion logic in the library treats a BigInteger as milliseconds,
        // so we multiply by 1000 to obtain the expected instant.
        Instant expectedInstant = Instant.ofEpochSecond(epochSeconds.longValue());
        Date expectedDate = Date.from(expectedInstant);
        // WHEN: converting the BigInteger to a java.util.Date
        Date actualDate = BigIntegerConversions.toDate(epochSeconds, converter);
        // THEN: the resulting Date matches the expected instant
        assertEquals(expectedDate, actualDate, "The Date produced from a BigInteger of epoch seconds should match the expected value.");
    }

    @Test
    void toDate_nullInputReturnsNull() {
        // GIVEN: a null source object
        Object source = null;
        // WHEN: converting null to Date
        Date result = BigIntegerConversions.toDate(source, converter);
        // THEN: the result should be null (Converter follows this contract)
        assertNull(result, "Converting a null source should return null.");
    }

    // ---------- Conversion from java.util.Date ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("dateSource")
    void toDate_fromDate(String description, Date source, Date expected) {
        Date actual = BigIntegerConversions.toDate(source, null);
        assertEquals(expected, actual, description);
    }

    // ---------- Conversion from java.time.Instant ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("instantSource")
    void toDate_fromInstant(String description, Instant source, Date expected) {
        Date actual = BigIntegerConversions.toDate(source, null);
        assertEquals(expected, actual, description);
    }

    // ---------- Conversion from java.time.LocalDateTime ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("localDateTimeSource")
    void toDate_fromLocalDateTime(String description, LocalDateTime source, Date expected) {
        Date actual = BigIntegerConversions.toDate(source, null);
        assertEquals(expected, actual, description);
    }

    // ---------- Conversion from java.lang.Long ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("longSource")
    void toDate_fromLong(String description, Long source, Date expected) {
        Date actual = BigIntegerConversions.toDate(source, null);
        assertEquals(expected, actual, description);
    }

    // ---------- Conversion from java.math.BigInteger (epoch seconds) ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("bigIntegerSecondsSource")
    void toDate_fromBigIntegerSeconds(String description, BigInteger source, Date expected) {
        Date actual = BigIntegerConversions.toDate(source, null);
        assertEquals(expected, actual, description);
    }

    // ---------- Null source object ----------
    @Test
    void toDate_nullSourceThrowsNPE() {
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toDate(null, null), "Passing a null source should result in a NullPointerException because toInstant cannot handle null.");
    }

    // ---------- Unsupported source type ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedSource")
    void toDate_unsupportedSourceThrows(String description, Object source) {
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toDate(source, null), description);
    }

    // ---------- Conversion from java.util.Calendar ----------
    @Test
    void toDate_fromCalendar() {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1672531200000L);
        Date expected = new Date(1672531200000L);
        Date actual = BigIntegerConversions.toDate(cal, null);
        assertEquals(expected, actual, "A Calendar instance set to the same instant as the reference date should be converted to an equivalent Date.");
    }

    // ---------- Conversion from java.time.LocalDate ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("localDateSource")
    void toDate_fromLocalDate(String description, LocalDate source, Date expected) {
        Date actual = BigIntegerConversions.toDate(source, null);
        assertEquals(expected, actual, description);
    }

    // ---------- Conversion from java.time.LocalTime ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("localTimeSource")
    void toDate_fromLocalTime(String description, LocalTime source, Date expected) {
        Date actual = BigIntegerConversions.toDate(source, null);
        assertEquals(expected, actual, description);
    }

    // ---------- Conversion from java.time.OffsetDateTime ----------
    @Test
    void toDate_fromOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-5));
        // Normalise to UTC
        // 2023‑01‑01T05:00:00Z
        Instant expectedInstant = odt.toInstant();
        Date expected = Date.from(expectedInstant);
        Date actual = BigIntegerConversions.toDate(odt, null);
        assertEquals(expected, actual, "OffsetDateTime with a –05:00 offset should be normalized to UTC before conversion.");
    }

    // ---------- Conversion from java.time.ZonedDateTime ----------
    @Test
    void toDate_fromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneId.of("Asia/Tokyo"));
        // 2022‑12‑31T15:00:00Z
        Instant expectedInstant = zdt.toInstant();
        Date expected = Date.from(expectedInstant);
        Date actual = BigIntegerConversions.toDate(zdt, null);
        assertEquals(expected, actual, "ZonedDateTime in Asia/Tokyo (UTC+9) should be converted to the equivalent UTC instant.");
    }

    // ---------- Conversion from java.sql.Timestamp ----------
    @Test
    void toDate_fromTimestampWithNanos() {
        // 2023‑01‑01T00:00:00.123456789Z
        Instant instant = Instant.ofEpochSecond(1672531200L, 123_456_789L);
        Timestamp ts = Timestamp.from(instant);
        // Date truncates to milliseconds -> 123 ms
        Date expected = Date.from(Instant.ofEpochMilli(1672531200123L));
        Date actual = BigIntegerConversions.toDate(ts, null);
        assertEquals(expected, actual, "Timestamp carries nanosecond precision; the conversion should round/truncate to millisecond precision as Date does.");
    }

    // ---------- BigInteger representing a large epoch second ----------
    @Test
    void toDate_bigIntegerLargeEpochSecond() {
        // 9999‑12‑31T23:59:59Z epoch seconds
        BigInteger big = new BigInteger("253402300799");
        Instant expectedInstant = Instant.ofEpochSecond(big.longValue());
        Date expected = Date.from(expectedInstant);
        Date actual = BigIntegerConversions.toDate(big, null);
        assertEquals(expected, actual, "BigInteger with a very large epoch‑second value should still be converted correctly.");
    }

    // ---------- Long.MAX_VALUE (epoch milliseconds) ----------
    @Test
    void toDate_longMaxValue() {
        Long max = Long.MAX_VALUE;
        // This is a far‑future instant; Date can hold it because it stores a long.
        Date expected = Date.from(Instant.ofEpochMilli(max));
        Date actual = BigIntegerConversions.toDate(max, null);
        assertEquals(expected, actual, "Testing the upper bound of epoch‑millisecond values to ensure no overflow occurs during conversion.");
    }

    // ---------- Conversion with a non‑null custom Converter ----------
    @Test
    void toDate_withCustomConverterIgnored() {
        // The custom converter does nothing; the source type (Instant) is handled directly.
        Converter custom = new Converter() {

            @Override
            @SuppressWarnings("unchecked")
            public <T> T convert(Object src, Class<T> target) {
                // deliberately return null to prove it is not used
                return null;
            }
        };
        Instant instant = Instant.ofEpochMilli(1672531200000L);
        Date expected = Date.from(instant);
        Date actual = BigIntegerConversions.toDate(instant, custom);
        assertEquals(expected, actual, "When a non‑null Converter is supplied but not needed for the source type, toDate should ignore it and still produce the correct result.");
    }

    // ---------- Unsupported source type with non‑null Converter ----------
    @Test
    void toDate_unsupportedWithCustomConverterThrows() {
        Converter custom = new Converter() {

            @Override
            @SuppressWarnings("unchecked")
            public <T> T convert(Object src, Class<T> target) {
                return null;
            }
        };
        Object src = new Object();
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toDate(src, custom), "Even when a Converter is provided, an object of an unsupported type should still cause an IllegalArgumentException.");
    }
}
