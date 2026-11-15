package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toCalendar_Test {

    /**
     * Shared converter used for the tests.
     */
    private Converter converter;

    /**
     * Fixed time‑zone used for deterministic Calendar creation.
     */
    private static final TimeZone TEST_TIME_ZONE = TimeZone.getTimeZone("UTC");

    /**
     * Default UTC zone used by most tests.
     */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @BeforeEach
    void setupBeforeEach() {
        // Build a real Converter with a deterministic time‑zone.
        ConverterOptions options = new ConverterOptions();
        options.setTimeZone(TEST_TIME_ZONE);
        converter = new Converter(options);
    }

    /**
     * Helper to build a mocked Converter that returns the supplied time‑zone.
     */
    private static Converter mockConverter(TimeZone zone) {
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        when(options.getTimeZone()).thenReturn(zone);
        return converter;
    }

    private static Stream<Arguments> numericSourceProvider() {
        return Stream.of(Arguments.of("Long value should be converted directly to milliseconds", 123456789L, 123456789L), Arguments.of("BigInteger is accepted by toLong", new BigInteger("9876543210"), 9876543210L), Arguments.of("String containing a numeric epoch millis is parsed to long", "1610000000000", 1610000000000L), Arguments.of("BigDecimal is converted via its longValue(); fractional part discarded", new BigDecimal("12345.678"), 12345L), Arguments.of("AtomicLong provides its current value via get()", new AtomicLong(987654321L), 987654321L));
    }

    private static Stream<Arguments> temporalSourceProvider() {
        return Stream.of(Arguments.of("java.util.Date is converted via its getTime() method", new Date(1623456789012L), 1623456789012L), Arguments.of("Instant is converted to epoch millis", Instant.ofEpochMilli(1632100000000L), 1632100000000L), Arguments.of("OffsetDateTime is normalized to UTC epoch millis", OffsetDateTime.parse("2023-04-01T12:34:56+02:00"), 1680344096000L), Arguments.of("Timestamp.getTime() returns the same millisecond value", new Timestamp(123456789L), 123456789L), Arguments.of("ZonedDateTime created from a known instant retains the same epoch", ZonedDateTime.ofInstant(Instant.ofEpochMilli(1609459200000L), ZoneId.of("Europe/Paris")), 1609459200000L), Arguments.of("YearMonth is interpreted as first day of month 00:00 UTC", YearMonth.of(2022, 3), 1646092800000L), Arguments.of("Year is interpreted as Jan 1st 00:00 UTC", Year.of(1999), 915148800000L));
    }

    private static Stream<Arguments> boundaryEpochProvider() {
        return Stream.of(Arguments.of("Epoch start (1970‑01‑01T00:00:00Z) with UTC", new Date(0L), UTC, 0L), Arguments.of("Negative epoch (one day before 1970) with UTC", new Date(-86_400_000L), UTC, -86_400_000L), Arguments.of("Epoch start with America/New_York zone", new Date(0L), TimeZone.getTimeZone("America/New_York"), 0L));
    }

    @Test
    @DisplayName("toCalendar converts a Long millisecond value correctly")
    void testToCalendarFromLong() {
        // GIVEN
        // 2021‑06‑01T12:00:00Z
        Long epochMillis = 1_622_548_800_000L;
        // WHEN
        Calendar calendar = OffsetDateTimeConversions.toCalendar(epochMillis, converter);
        // THEN
        assertNotNull(calendar, "Calendar should not be null");
        assertEquals(TEST_TIME_ZONE, calendar.getTimeZone(), "Calendar must use the Converter's time‑zone");
        assertEquals(epochMillis.longValue(), calendar.getTimeInMillis(), "Calendar time must match the supplied millisecond value");
    }

    @Test
    @DisplayName("toCalendar converts a java.util.Date correctly")
    void testToCalendarFromDate() {
        // GIVEN
        // same instant as above
        java.util.Date date = new java.util.Date(1_622_548_800_000L);
        // WHEN
        Calendar calendar = OffsetDateTimeConversions.toCalendar(date, converter);
        // THEN
        assertNotNull(calendar);
        assertEquals(TEST_TIME_ZONE, calendar.getTimeZone());
        assertEquals(date.getTime(), calendar.getTimeInMillis(), "Calendar time must match the Date's epoch milliseconds");
    }

    @Test
    @DisplayName("toCalendar respects a non‑UTC time‑zone")
    void testToCalendarWithCustomTimeZone() {
        // GIVEN
        TimeZone customZone = TimeZone.getTimeZone("America/New_York");
        ConverterOptions customOptions = new ConverterOptions();
        customOptions.setTimeZone(customZone);
        Converter customConverter = new Converter(customOptions);
        // 2021‑06‑01T12:00:00Z
        Long epochMillis = 1_622_548_800_000L;
        // WHEN
        Calendar calendar = OffsetDateTimeConversions.toCalendar(epochMillis, customConverter);
        // THEN
        assertNotNull(calendar);
        assertEquals(customZone, calendar.getTimeZone(), "Calendar must be created with the time‑zone supplied by the Converter");
        assertEquals(epochMillis.longValue(), calendar.getTimeInMillis(), "Calendar time must still reflect the supplied epoch milliseconds");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numericSourceProvider")
    @DisplayName("Valid conversions – numeric types")
    void testNumericConversions(String description, Object from, long expectedMillis) {
        // GIVEN
        Converter converter = mockConverter(UTC);
        // WHEN
        Calendar cal = OffsetDateTimeConversions.toCalendar(from, converter);
        // THEN
        assertNotNull(cal, "Calendar must not be null");
        assertEquals(UTC, cal.getTimeZone(), "Calendar must use UTC");
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Calendar time must match the expected epoch millis");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("temporalSourceProvider")
    @DisplayName("Valid conversions – date/time objects")
    void testTemporalConversions(String description, Object from, long expectedMillis) {
        // GIVEN
        Converter converter = mockConverter(UTC);
        // WHEN
        Calendar cal = OffsetDateTimeConversions.toCalendar(from, converter);
        // THEN
        assertNotNull(cal);
        assertEquals(UTC, cal.getTimeZone());
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Calendar time must match the expected epoch millis");
    }

    @Test
    @DisplayName("Timezone handling – custom zone is propagated")
    void testCustomTimeZonePropagation() {
        // GIVEN
        TimeZone customZone = TimeZone.getTimeZone("America/New_York");
        Converter converter = mockConverter(customZone);
        // 2021‑01‑01T00:00:00Z
        Date date = new Date(1609459200000L);
        // WHEN
        Calendar cal = OffsetDateTimeConversions.toCalendar(date, converter);
        // THEN
        assertNotNull(cal);
        assertEquals(customZone, cal.getTimeZone(), "Calendar must be created with the Converter's custom time‑zone");
        assertEquals(date.getTime(), cal.getTimeInMillis(), "Instant must remain unchanged regardless of the zone");
    }

    @Test
    @DisplayName("Timezone handling – another custom zone")
    void testAnotherCustomTimeZone() {
        // GIVEN
        TimeZone tokyo = TimeZone.getTimeZone("Asia/Tokyo");
        Converter converter = mockConverter(tokyo);
        Date date = new Date(1609459200000L);
        // WHEN
        Calendar cal = OffsetDateTimeConversions.toCalendar(date, converter);
        // THEN
        assertEquals(tokyo, cal.getTimeZone());
        assertEquals(date.getTime(), cal.getTimeInMillis());
    }

    @Test
    @DisplayName("Null source object causes NullPointerException")
    void testNullSource() {
        Converter converter = mockConverter(UTC);
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toCalendar(null, converter), "Passing null as the source should trigger NPE from toLong");
    }

    @Test
    @DisplayName("Unsupported Boolean source causes IllegalArgumentException")
    void testUnsupportedBoolean() {
        Converter converter = mockConverter(UTC);
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toCalendar(true, converter), "Boolean is not a supported source type");
    }

    @Test
    @DisplayName("Non‑numeric String causes NumberFormatException")
    void testNonNumericString() {
        Converter converter = mockConverter(UTC);
        assertThrows(NumberFormatException.class, () -> OffsetDateTimeConversions.toCalendar("not-a-number", converter), "Parsing a non‑numeric String should fail");
    }

    @Test
    @DisplayName("Unsupported object type (ArrayList) causes IllegalArgumentException")
    void testUnsupportedObject() {
        Converter converter = mockConverter(UTC);
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toCalendar(new ArrayList<>(), converter));
    }

    @Test
    @DisplayName("Unsupported numeric wrapper (Double) causes IllegalArgumentException")
    void testUnsupportedDouble() {
        Converter converter = mockConverter(UTC);
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toCalendar(12345.67d, converter));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("boundaryEpochProvider")
    @DisplayName("Boundary epoch values")
    void testBoundaryEpochs(String description, Date date, TimeZone zone, long expectedMillis) {
        Converter converter = mockConverter(zone);
        Calendar cal = OffsetDateTimeConversions.toCalendar(date, converter);
        assertEquals(expectedMillis, cal.getTimeInMillis());
        assertEquals(zone, cal.getTimeZone());
    }

    @Test
    @DisplayName("Converter with null time‑zone falls back to JVM default")
    void testNullTimeZoneFallsBackToSystemDefault() {
        // GIVEN a converter whose options return null for getTimeZone()
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        // explicit null
        when(options.getTimeZone()).thenReturn(null);
        Date date = new Date(1623456789012L);
        TimeZone systemDefault = TimeZone.getDefault();
        // WHEN
        Calendar cal = OffsetDateTimeConversions.toCalendar(date, converter);
        // THEN
        assertEquals(systemDefault, cal.getTimeZone(), "When time‑zone is null the Calendar should use the JVM default");
        assertEquals(date.getTime(), cal.getTimeInMillis());
    }

    @Test
    @DisplayName("Null converter causes NullPointerException")
    void testNullConverter() {
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toCalendar(new Date(1623456789012L), null), "Passing a null Converter must result in NPE");
    }

    @Test
    @DisplayName("Converter with null options causes NullPointerException")
    void testConverterWithNullOptions() {
        Converter converter = mock(Converter.class);
        // options are null
        when(converter.getOptions()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toCalendar(new Date(1623456789012L), converter), "Null options should trigger NPE when getTimeZone() is called");
    }
}
