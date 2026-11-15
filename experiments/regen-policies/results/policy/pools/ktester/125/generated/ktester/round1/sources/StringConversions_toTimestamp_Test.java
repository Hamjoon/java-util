package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import java.util.Date;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StringConversions_toTimestamp_Test {

    /**
     * The Converter used for all test cases.
     */
    private Converter converter;

    // Dummy class used only for the custom‑converter test.
    static class CustomTemporal {

        final String value;

        CustomTemporal(String value) {
            this.value = value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the default options – no overrides are required for the conversions we test.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
        converter = null;
    }

    /**
     * Parses a timestamp string of the form {@code "yyyy-MM-dd HH:mm:ss.S"} into a {@link Timestamp}.
     */
    private static Timestamp ts(String s) {
        return Timestamp.valueOf(s);
    }

    /**
     * Returns a {@link Calendar} set to the given epoch milli in UTC.
     */
    private static Calendar utcCalendar(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    private static Stream<Arguments> dateAndTimeProvider() {
        return Stream.of(// java.util.Date epoch (0L)
        Arguments.of(new Date(0L), "1970-01-01 00:00:00.0"), // java.sql.Timestamp with known epoch millis
        Arguments.of(new Timestamp(1622548800000L), "2021-06-01 12:00:00.0"), // java.time.Instant
        Arguments.of(Instant.parse("2022-03-15T10:15:30Z"), "2022-03-15 10:15:30.0"), // ZonedDateTime in UTC
        Arguments.of(ZonedDateTime.of(2022, 3, 15, 10, 15, 30, 0, ZoneOffset.UTC), "2022-03-15 10:15:30.0"), // OffsetDateTime with -05:00 offset (adds 5h to become UTC)
        Arguments.of(OffsetDateTime.of(2022, 3, 15, 10, 15, 30, 0, ZoneOffset.ofHours(-5)), "2022-03-15 15:15:30.0"), // Calendar set to known epoch millis in UTC
        Arguments.of(utcCalendar(1622548800000L), "2021-06-01 12:00:00.0"));
    }

    private static Stream<Arguments> stringFormatsProvider() {
        return Stream.of(Arguments.of("2022-03-15T10:15:30Z", "2022-03-15 10:15:30.0"), Arguments.of("2022-03-15 10:15:30", "2022-03-15 10:15:30.0"), Arguments.of("2022-03-15", "2022-03-15 00:00:00.0"), Arguments.of("1622548800000", "2021-06-01 12:00:00.0"));
    }

    private static Stream<Arguments> numericEpochProvider() {
        return Stream.of(Arguments.of(0L, "1970-01-01 00:00:00.0"), Arguments.of(1622548800000L, "2021-06-01 12:00:00.0"), Arguments.of(-86400000L, "1969-12-31 00:00:00.0"));
    }

    private static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("2022-13-40T25:61:61Z"), Arguments.of("2022/03/15 10:15:30"));
    }

    @Test
    void toTimestamp_fromInstant_returnsEquivalentTimestamp() {
        Instant now = Instant.now();
        Timestamp ts = StringConversions.toTimestamp(now, converter);
        assertNotNull(ts, "Timestamp should not be null for a non‑null Instant");
        assertEquals(now.toEpochMilli(), ts.getTime(), "Timestamp epoch milli must match the source Instant");
    }

    @Test
    void toTimestamp_fromDate_returnsEquivalentTimestamp() {
        Date now = new Date();
        Timestamp ts = StringConversions.toTimestamp(now, converter);
        assertNotNull(ts, "Timestamp should not be null for a non‑null Date");
        assertEquals(now.getTime(), ts.getTime(), "Timestamp epoch milli must match the source Date");
    }

    @Test
    void toTimestamp_fromStringIso8601_returnsEquivalentTimestamp() {
        String iso = "2023-08-15T12:34:56.123Z";
        Instant expected = Instant.parse(iso);
        Timestamp ts = StringConversions.toTimestamp(iso, converter);
        assertNotNull(ts, "Timestamp should not be null for a valid ISO‑8601 string");
        assertEquals(expected.toEpochMilli(), ts.getTime(), "Timestamp epoch milli must match the parsed Instant");
    }

    @Test
    void toTimestamp_fromLong_returnsEquivalentTimestamp() {
        long epochMilli = System.currentTimeMillis();
        Timestamp ts = StringConversions.toTimestamp(epochMilli, converter);
        assertNotNull(ts, "Timestamp should not be null for a non‑null Long");
        assertEquals(epochMilli, ts.getTime(), "Timestamp epoch milli must match the source long value");
    }

    @Test
    void toTimestamp_nullInput_returnsNull() {
        Timestamp ts = StringConversions.toTimestamp(null, converter);
        assertNull(ts, "Timestamp should be null when the source object is null");
    }

    @Test
    void toTimestamp_nullSource_returnsNull() {
        Timestamp ts = StringConversions.toTimestamp(null, null);
        assertNull(ts, "When source is null, result must be null");
    }

    @Test
    void toTimestamp_unsupportedObject_returnsNull() {
        Object unsupported = new Object();
        Timestamp ts = StringConversions.toTimestamp(unsupported, null);
        assertNull(ts, "Unsupported source type should yield null");
    }

    @ParameterizedTest(name = "{index}: from={0} => expected={1}")
    @MethodSource("dateAndTimeProvider")
    void toTimestamp_dateAndTimeTypes(Object from, String expectedTimestampString) {
        Timestamp expected = ts(expectedTimestampString);
        Timestamp actual = StringConversions.toTimestamp(from, converter);
        assertNotNull(actual, "Result must not be null");
        assertEquals(expected.getTime(), actual.getTime(), "Epoch millis must match for source: " + from.getClass().getSimpleName());
    }

    @ParameterizedTest(name = "{index}: string={0} => expected={1}")
    @MethodSource("stringFormatsProvider")
    void toTimestamp_stringFormats(String source, String expectedTimestampString) {
        Timestamp expected = ts(expectedTimestampString);
        Timestamp actual = StringConversions.toTimestamp(source, converter);
        assertNotNull(actual);
        assertEquals(expected.getTime(), actual.getTime(), "String '" + source + "' should be parsed to the expected instant");
    }

    @ParameterizedTest(name = "{index}: long={0} => expected={1}")
    @MethodSource("numericEpochProvider")
    void toTimestamp_numericEpochs(Long epochMilli, String expectedTimestampString) {
        Timestamp expected = ts(expectedTimestampString);
        Timestamp actual = StringConversions.toTimestamp(epochMilli, converter);
        assertNotNull(actual);
        assertEquals(expected.getTime(), actual.getTime(), "Long epoch milli should map directly to Timestamp");
    }

    @Test
    void toTimestamp_maxDate() {
        Date maxDate = new Date(Long.MAX_VALUE);
        Timestamp ts = StringConversions.toTimestamp(maxDate, converter);
        assertNotNull(ts);
        assertEquals(Long.MAX_VALUE, ts.getTime(), "Timestamp should preserve Long.MAX_VALUE epoch millis");
    }

    @Test
    void toTimestamp_minInstant() {
        Instant minInstant = Instant.ofEpochMilli(Long.MIN_VALUE);
        Timestamp ts = StringConversions.toTimestamp(minInstant, converter);
        assertNotNull(ts);
        assertEquals(Long.MIN_VALUE, ts.getTime(), "Timestamp should preserve Long.MIN_VALUE epoch millis");
    }

    @Test
    void toTimestamp_customConverterHandlesCustomTemporal() {
        // Source object that the default conversion cannot handle.
        CustomTemporal src = new CustomTemporal("2022-01-01T00:00:00Z");
        // Anonymous Converter that knows how to turn CustomTemporal into an Instant.
        Converter customConverter = new Converter() {

            @Override
            public Object convert(Object srcObj, Class<?> target) {
                if (srcObj instanceof CustomTemporal && target == Instant.class) {
                    return Instant.parse(((CustomTemporal) srcObj).value);
                }
                return null;
            }
        };
        Timestamp ts = StringConversions.toTimestamp(src, customConverter);
        assertNotNull(ts);
        Timestamp expected = ts("2022-01-01 00:00:00.0");
        assertEquals(expected.getTime(), ts.getTime(), "Custom converter should produce the correct Timestamp");
    }

    @ParameterizedTest(name = "{index}: invalidString={0}")
    @MethodSource("invalidStringProvider")
    void toTimestamp_invalidStringThrows(String badString) {
        assertThrows(DateTimeParseException.class, () -> StringConversions.toTimestamp(badString, converter), "Parsing an invalid date string must throw DateTimeParseException");
    }
}
