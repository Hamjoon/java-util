package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toCalendar_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    /**
     * Minimal {@link Converter} implementation that returns a fixed {@link ConverterOptions}.
     */
    private static class TestConverter implements Converter {

        private final ConverterOptions options;

        TestConverter(ZoneId zone) {
            this.options = new SimpleOptions(zone);
        }

        @Override
        public ConverterOptions getOptions() {
            return options;
        }

        // The remaining methods of Converter are not needed for these tests.
        // They can safely throw {@link UnsupportedOperationException}.
        @Override
        public Object convert(Object from, Class<?> toClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object convert(Object from, Class<?> toClass, Object defaultValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean canConvert(Object from, Class<?> toClass) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@link ConverterOptions} that only supplies a {@link TimeZone}.
     */
    private static class SimpleOptions implements ConverterOptions {

        private final TimeZone tz;

        SimpleOptions(ZoneId zone) {
            this.tz = (zone == null) ? null : TimeZone.getTimeZone(zone);
        }

        @Override
        public TimeZone getTimeZone() {
            return tz;
        }

        // All other option methods are irrelevant for these tests.
        @Override
        public Locale getLocale() {
            return Locale.getDefault();
        }

        @Override
        public boolean isCaseSensitive() {
            return true;
        }

        @Override
        public boolean isAllowNull() {
            return true;
        }

        @Override
        public boolean isStrict() {
            return false;
        }

        @Override
        public boolean isAllowEmptyStrings() {
            return false;
        }

        @Override
        public boolean isAllowEmptyCollections() {
            return false;
        }

        @Override
        public boolean isAllowEmptyMaps() {
            return false;
        }

        @Override
        public boolean isAllowEmptyArrays() {
            return false;
        }

        @Override
        public boolean isAllowEmptyOptionals() {
            return false;
        }

        @Override
        public boolean isAllowNaN() {
            return false;
        }

        @Override
        public boolean isAllowInfinity() {
            return false;
        }

        @Override
        public boolean isAllowNonFiniteNumbers() {
            return false;
        }

        @Override
        public boolean isAllowStringToNumber() {
            return false;
        }

        @Override
        public boolean isAllowStringToBoolean() {
            return false;
        }

        @Override
        public boolean isAllowStringToEnum() {
            return false;
        }

        @Override
        public boolean isAllowStringToDate() {
            return false;
        }

        @Override
        public boolean isAllowStringToTemporal() {
            return false;
        }

        @Override
        public boolean isAllowStringToCollection() {
            return false;
        }

        @Override
        public boolean isAllowStringToMap() {
            return false;
        }

        @Override
        public boolean isAllowStringToArray() {
            return false;
        }

        @Override
        public boolean isAllowStringToChar() {
            return false;
        }

        @Override
        public boolean isAllowStringToByte() {
            return false;
        }

        @Override
        public boolean isAllowStringToShort() {
            return false;
        }

        @Override
        public boolean isAllowStringToInt() {
            return false;
        }

        @Override
        public boolean isAllowStringToLong() {
            return false;
        }

        @Override
        public boolean isAllowStringToFloat() {
            return false;
        }

        @Override
        public boolean isAllowStringToDouble() {
            return false;
        }

        @Override
        public boolean isAllowStringToBigDecimal() {
            return false;
        }

        @Override
        public boolean isAllowStringToBigInteger() {
            return false;
        }

        @Override
        public boolean isAllowStringToAtomicLong() {
            return false;
        }

        @Override
        public boolean isAllowStringToAtomicInteger() {
            return false;
        }

        @Override
        public boolean isAllowStringToInstant() {
            return false;
        }

        @Override
        public boolean isAllowStringToDuration() {
            return false;
        }

        @Override
        public boolean isAllowStringToPeriod() {
            return false;
        }

        @Override
        public boolean isAllowStringToZoneId() {
            return false;
        }

        @Override
        public boolean isAllowStringToZoneOffset() {
            return false;
        }

        @Override
        public boolean isAllowStringToMonthDay() {
            return false;
        }

        @Override
        public boolean isAllowStringToYearMonth() {
            return false;
        }

        @Override
        public boolean isAllowStringToLocalDate() {
            return false;
        }

        @Override
        public boolean isAllowStringToLocalTime() {
            return false;
        }

        @Override
        public boolean isAllowStringToLocalDateTime() {
            return false;
        }

        @Override
        public boolean isAllowStringToOffsetDateTime() {
            return false;
        }

        @Override
        public boolean isAllowStringToZonedDateTime() {
            return false;
        }

        @Override
        public boolean isAllowStringToInstant() {
            return false;
        }

        @Override
        public boolean isAllowStringToDate() {
            return false;
        }

        @Override
        public boolean isAllowStringToTimestamp() {
            return false;
        }

        @Override
        public boolean isAllowStringToSqlDate() {
            return false;
        }

        @Override
        public boolean isAllowStringToSqlTime() {
            return false;
        }

        @Override
        public boolean isAllowStringToSqlTimestamp() {
            return false;
        }

        @Override
        public boolean isAllowStringToByteArray() {
            return false;
        }

        @Override
        public boolean isAllowStringToCharArray() {
            return false;
        }

        @Override
        public boolean isAllowStringToUUID() {
            return false;
        }

        @Override
        public boolean isAllowStringToURL() {
            return false;
        }

        @Override
        public boolean isAllowStringToURI() {
            return false;
        }

        @Override
        public boolean isAllowStringToFile() {
            return false;
        }

        @Override
        public boolean isAllowStringToPath() {
            return false;
        }

        @Override
        public boolean isAllowStringToClass() {
            return false;
        }

        @Override
        public boolean isAllowStringToEnumSet() {
            return false;
        }

        @Override
        public boolean isAllowStringToEnumMap() {
            return false;
        }

        @Override
        public boolean isAllowStringToProperties() {
            return false;
        }

        @Override
        public boolean isAllowStringToMapEntry() {
            return false;
        }

        @Override
        public boolean isAllowStringToPair() {
            return false;
        }

        @Override
        public boolean isAllowStringToTuple() {
            return false;
        }

        @Override
        public boolean isAllowStringToRecord() {
            return false;
        }
    }

    /**
     * Converter that deliberately returns {@code null} for {@link ConverterOptions}.
     */
    private static class TestConverterWithNullOptions implements Converter {

        @Override
        public ConverterOptions getOptions() {
            return null;
        }

        @Override
        public Object convert(Object from, Class<?> toClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object convert(Object from, Class<?> toClass, Object defaultValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean canConvert(Object from, Class<?> toClass) {
            throw new UnsupportedOperationException();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy: converter -> options -> time zone
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        // Default mock behaviour: converter returns options, options returns UTC.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        clearInvocations(mockConverter, mockOptions);
        clearInvocations(mockConverter, mockOptions);
    }

    /**
     * Builds the stream of test cases for successful conversions.
     */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// group: toCalendar conversion tests
        Arguments.of(new TestCase(ZonedDateTime.parse("2023-05-15T10:15:30+02:00[Europe/Paris]"), new TestConverter(ZoneId.of("UTC")), null, ZonedDateTime.parse("2023-05-15T10:15:30+02:00[Europe/Paris]").toInstant().toEpochMilli(), "UTC", "Convert a ZonedDateTime to Calendar using a converter with UTC time zone")), Arguments.of(new TestCase(OffsetDateTime.parse("2022-12-01T23:00:00-05:00"), new TestConverter(ZoneId.of("America/New_York")), null, // offset -05 => instant 2022‑12‑02T04:00:00Z
        OffsetDateTime.parse("2022-12-01T23:00:00-05:00").toInstant().toEpochMilli(), "America/New_York", "Pass an OffsetDateTime; Calendar should reflect the converter's zone")), Arguments.of(new TestCase(Date.from(Instant.parse("2021-07-20T12:00:00Z")), new TestConverter(ZoneId.of("Asia/Tokyo")), null, Instant.parse("2021-07-20T12:00:00Z").toEpochMilli(), "Asia/Tokyo", "Convert a java.util.Date (Instant) to Calendar")), Arguments.of(new TestCase(Timestamp.valueOf("2020-01-01 00:00:00"), new TestConverter(ZoneId.of("Europe/London")), null, Timestamp.valueOf("2020-01-01 00:00:00").toInstant().toEpochMilli(), "Europe/London", "Convert a java.sql.Timestamp")), Arguments.of(new TestCase("2024-03-10T14:30:00", new TestConverter(ZoneId.of("America/Los_Angeles")), null, // interpreted as local date‑time in converter zone (UTC‑8)
        LocalDateTime.parse("2024-03-10T14:30:00").atZone(ZoneId.of("America/Los_Angeles")).toInstant().toEpochMilli(), "America/Los_Angeles", "ISO‑8601 string without zone, interpreted in converter's zone")), // group: toCalendar additional conversion tests
        Arguments.of(new TestCase(LocalDateTime.parse("2022-03-01T15:45:00"), new TestConverter(ZoneId.of("Europe/Berlin")), null, LocalDateTime.parse("2022-03-01T15:45:00").atZone(ZoneId.of("Europe/Berlin")).toInstant().toEpochMilli(), "Europe/Berlin", "LocalDateTime interpreted in converter's zone")), Arguments.of(new TestCase(LocalDate.parse("2021-12-31"), new TestConverter(ZoneId.of("America/Los_Angeles")), null, LocalDate.parse("2021-12-31").atStartOfDay(ZoneId.of("America/Los_Angeles")).toInstant().toEpochMilli(), "America/Los_Angeles", "LocalDate start‑of‑day in converter's zone")), Arguments.of(new TestCase(LocalTime.parse("13:30:15"), new TestConverter(ZoneId.of("UTC")), null, LocalTime.parse("13:30:15").atDate(LocalDate.ofEpochDay(0)).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli(), "UTC", "LocalTime combined with epoch date in converter's zone")), Arguments.of(new TestCase(Instant.parse("2020-06-15T12:34:56Z"), new TestConverter(ZoneId.of("UTC")), null, Instant.parse("2020-06-15T12:34:56Z").toEpochMilli(), "UTC", "Instant conversion – zone only affects Calendar's time‑zone field")), Arguments.of(new TestCase(new AtomicLong(1620000000000L), new TestConverter(ZoneId.of("Asia/Kolkata")), null, 1620000000000L, "Asia/Kolkata", "AtomicLong interpreted as epoch milliseconds")), Arguments.of(new TestCase("2023-08-01T12:00:00-04:00", new TestConverter(ZoneId.of("UTC")), null, OffsetDateTime.parse("2023-08-01T12:00:00-04:00").toInstant().toEpochMilli(), "UTC", "String with offset – instant is shifted to UTC")), Arguments.of(new TestCase("2023-08-01T12:00:00+02:00[Europe/Paris]", new TestConverter(ZoneId.of("America/New_York")), null, ZonedDateTime.parse("2023-08-01T12:00:00+02:00[Europe/Paris]").toInstant().toEpochMilli(), "America/New_York", "String with full zone ID – instant calculated from that zone")), Arguments.of(new TestCase(java.sql.Date.valueOf("2022-05-10"), new TestConverter(ZoneId.of("UTC")), null, java.sql.Date.valueOf("2022-05-10").toInstant().toEpochMilli(), "UTC", "java.sql.Date represents midnight UTC")));
    }

    /**
     * Builds the stream of test cases that are expected to fail with an exception.
     */
    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// group: toCalendar conversion tests – null source
        Arguments.of(new TestCase(null, new TestConverter(ZoneId.of("UTC")), NullPointerException.class, null, null, "Null input should cause NPE")), // unsupported map type
        Arguments.of(new TestCase(new HashMap<>(), new TestConverter(ZoneId.of("UTC")), ConversionException.class, null, null, "Unsupported type should raise ConversionException")), // malformed string
        Arguments.of(new TestCase("not-a-date", new TestConverter(ZoneId.of("UTC")), ConversionException.class, null, null, "Malformed date‑time string")), // empty string
        Arguments.of(new TestCase("", new TestConverter(ZoneId.of("UTC")), ConversionException.class, null, null, "Empty string cannot be parsed")), // whitespace string
        Arguments.of(new TestCase("   ", new TestConverter(ZoneId.of("UTC")), ConversionException.class, null, null, "Whitespace‑only string is invalid")), // boolean value
        Arguments.of(new TestCase(Boolean.TRUE, new TestConverter(ZoneId.of("UTC")), ConversionException.class, null, null, "Boolean cannot be converted")), // arbitrary object
        Arguments.of(new TestCase(new Object(), new TestConverter(ZoneId.of("UTC")), ConversionException.class, null, null, "Arbitrary object unsupported")), // unsupported numeric types
        Arguments.of(new TestCase(Long.valueOf(1620000000000L), new TestConverter(ZoneId.of("UTC")), ConversionException.class, null, null, "Long (plain Number) not supported")), Arguments.of(new TestCase(Double.valueOf(1620000000.0), new TestConverter(ZoneId.of("UTC")), ConversionException.class, null, null, "Double not supported")), // null converter
        Arguments.of(new TestCase(Instant.now(), null, NullPointerException.class, null, null, "Null Converter should cause NPE")), // converter with null options
        Arguments.of(new TestCase(ZonedDateTime.parse("2023-01-01T12:00:00+01:00[Europe/Paris]"), new TestConverterWithNullOptions(), NullPointerException.class, null, null, "Converter returns null options")), // converter with null time‑zone (ZonedDateTime source)
        Arguments.of(new TestCase(ZonedDateTime.parse("2023-01-01T12:00:00+01:00[Europe/Paris]"), new TestConverter(null), NullPointerException.class, null, null, "Null TimeZone in options")), // converter with null time‑zone (LocalDateTime source)
        Arguments.of(new TestCase(LocalDateTime.parse("2023-01-01T12:00:00"), new TestConverter(null), NullPointerException.class, null, null, "Null TimeZone when converting LocalDateTime")), // converter with null time‑zone (LocalDate source)
        Arguments.of(new TestCase(LocalDate.parse("2023-01-01"), new TestConverter(null), NullPointerException.class, null, null, "Null TimeZone when converting LocalDate")), // converter with null time‑zone (LocalTime source)
        Arguments.of(new TestCase(LocalTime.parse("12:34:56"), new TestConverter(null), NullPointerException.class, null, null, "Null TimeZone when converting LocalTime")));
    }

    @Test
    void toCalendar_givenZonedDateTime_usesConverterTimeZone_andReturnsCorrectMillis() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 10, // 123 ms nanos
        12, // 123 ms nanos
        30, // 123 ms nanos
        45, // 123 ms nanos
        123_000_000, ZoneOffset.UTC);
        // WHEN
        Calendar result = LocalDateTimeConversions.toCalendar(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(TimeZone.getTimeZone("UTC"), result.getTimeZone(), "Calendar should be created with the Converter's time zone");
        assertEquals(source.toInstant().toEpochMilli(), result.getTimeInMillis(), "Calendar millis should match the source ZonedDateTime instant");
        // Verify interaction with the mock options
        verify(mockOptions, times(1)).getTimeZone();
        verify(mockConverter, times(1)).getOptions();
    }

    @Test
    void toCalendar_givenInstant_usesConverterTimeZone_andReturnsCorrectMillis() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.ofInstant(java.time.Instant.parse("2022-12-31T23:59:59.999Z"), ZoneOffset.UTC);
        // WHEN
        Calendar result = LocalDateTimeConversions.toCalendar(source, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(source.toInstant().toEpochMilli(), result.getTimeInMillis());
        verify(mockOptions, atLeastOnce()).getTimeZone();
    }

    @Test
    void toCalendar_givenNullSource_throwsNullPointerException() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toCalendar(nullSource, mockConverter));
    }

    @ParameterizedTest(name = "{index}: {0.description}")
    @MethodSource("successfulConversionProvider")
    void toCalendar_successfulConversions(TestCase tc) {
        Calendar result = LocalDateTimeConversions.toCalendar(tc.from, tc.converter);
        assertNotNull(result, "Resulting Calendar must not be null");
        assertEquals(TimeZone.getTimeZone(tc.expectedTimeZoneId), result.getTimeZone(), "Calendar time‑zone should match the converter's zone");
        assertEquals(tc.expectedEpochMillis.longValue(), result.getTimeInMillis(), "Calendar epoch millis should match expected value");
    }

    @ParameterizedTest(name = "{index}: {0.description}")
    @MethodSource("exceptionConversionProvider")
    void toCalendar_expectedExceptions(TestCase tc) {
        if (tc.expectedException == NullPointerException.class) {
            assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toCalendar(tc.from, tc.converter));
        } else {
            // The library throws a custom ConversionException wrapping parsing errors.
            assertThrows(tc.expectedException, () -> LocalDateTimeConversions.toCalendar(tc.from, tc.converter));
        }
    }
}
