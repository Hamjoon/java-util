package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DateConversions_toMap_Test {

    /**
     * Converter used for all tests – created once for the whole test class.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter has a no‑arg constructor that takes a ConverterOptions instance.
        // Using the default options is sufficient for the built‑in date conversions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC when many test classes run.
        converter = null;
    }

    /**
     * Supplies all temporal objects required by the JSON test suite together
     * with their expected ISO‑8601 string representations.
     */
    private static Stream<Arguments> temporalProvider() {
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.setTimeInMillis(0L);
        return Stream.of(// java.time.LocalDate
        Arguments.of(LocalDate.of(2022, 2, 28), "2022-02-28"), // java.time.LocalTime (millisecond precision)
        Arguments.of(LocalTime.of(23, 59, 59, 123_000_000), "23:59:59.123"), // java.time.ZonedDateTime in UTC
        Arguments.of(ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 123_000_000, ZoneOffset.UTC), "2021-12-31T23:59:59.123Z"), // java.time.YearMonth
        Arguments.of(YearMonth.of(2020, 1), "2020-01"), // java.time.Year
        Arguments.of(Year.of(1999), "1999"), // java.time.MonthDay
        Arguments.of(MonthDay.of(12, 25), "--12-25"), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2023-05-17 14:30:45.123"), "2023-05-17T14:30:45.123"), // java.util.Calendar set to epoch UTC
        Arguments.of(utcCalendar, "1970-01-01T00:00:00Z"), // java.util.concurrent.atomic.AtomicLong representing epoch millis
        Arguments.of(new AtomicLong(0L), "1970-01-01T00:00:00Z"));
    }

    /**
     * Verify that a regular {@link java.util.Date} is converted to a {@link String}
     * and stored under the {@code MapConversions.DATE} key.
     */
    @Test
    void toMap_withDate_returnsMapContainingStringRepresentation() {
        // GIVEN
        // current instant
        Date now = new Date();
        // Expected string representation as performed by DateConversions.toString(...)
        String expected = converter.convert(now, String.class);
        // WHEN
        Map<String, Object> result = DateConversions.toMap(now, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertTrue(result.containsKey(MapConversions.DATE), "Map must contain the key defined by MapConversions.DATE");
        assertEquals(expected, result.get(MapConversions.DATE), "The value stored under the DATE key must be the String representation of the Date");
    }

    /**
     * Verify that a {@code null} source yields a map with a {@code null} value
     * for the date key – the conversion logic returns {@code null} for object types.
     */
    @Test
    void toMap_withNull_returnsMapContainingNullValue() {
        // GIVEN
        Object source = null;
        // Expected value after conversion – should be null for object target types.
        String expected = null;
        // WHEN
        Map<String, Object> result = DateConversions.toMap(source, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null even when source is null");
        assertTrue(result.containsKey(MapConversions.DATE), "Map must still contain the DATE key when source is null");
        assertEquals(expected, result.get(MapConversions.DATE), "The value for the DATE key must be null when the source is null");
    }

    /**
     * Verify that a {@link java.sql.Timestamp} is also handled correctly.
     * The conversion path is the same – it ends up as a formatted String.
     */
    @Test
    void toMap_withTimestamp_returnsMapContainingStringRepresentation() {
        // GIVEN
        java.sql.Timestamp ts = new java.sql.Timestamp(System.currentTimeMillis());
        String expected = converter.convert(ts, String.class);
        // WHEN
        Map<String, Object> result = DateConversions.toMap(ts, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result.get(MapConversions.DATE));
    }

    /**
     * Both arguments are {@code null}. The internal {@code toString} implementation
     * returns the literal string "null" for a null source, even when the converter
     * itself is null.
     */
    @Test
    void toMap_bothArgumentsNull_returnsMapWithLiteralNullString() {
        // WHEN
        Map<String, Object> result = DateConversions.toMap(null, null);
        // THEN
        assertNotNull(result);
        assertEquals("null", result.get(MapConversions.DATE), "When both arguments are null the map should contain the literal string \"null\"");
    }

    /**
     * An unsupported source type without a custom converter should cause
     * {@code toString} to throw {@link IllegalArgumentException}, which propagates.
     */
    @Test
    void toMap_unsupportedObject_throwsIllegalArgumentException() {
        // GIVEN
        Object unsupported = new Object();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toMap(unsupported, converter), "Unsupported object types must trigger IllegalArgumentException");
    }

    /**
     * The same unsupported object, but a custom {@link Converter} that knows how
     * to handle it prevents the exception and supplies its own string.
     */
    @Test
    void toMap_unsupportedObjectHandledByCustomConverter_returnsCustomString() {
        // GIVEN
        Object unsupported = new Object();
        Converter custom = mock(Converter.class);
        when(custom.convert(eq(unsupported), eq(String.class))).thenReturn("object");
        // WHEN
        Map<String, Object> result = DateConversions.toMap(unsupported, custom);
        // THEN
        assertNotNull(result);
        assertEquals("object", result.get(MapConversions.DATE));
    }

    /**
     * Custom converter that returns {@code null} for any conversion.
     * The map should contain a {@code null} value for the DATE key.
     */
    @Test
    void toMap_customConverterReturnsNull_valueIsNullInMap() {
        // GIVEN
        Date date = new Date(0L);
        Converter nullReturning = mock(Converter.class);
        when(nullReturning.convert(any(), eq(String.class))).thenReturn(null);
        // WHEN
        Map<String, Object> result = DateConversions.toMap(date, nullReturning);
        // THEN
        assertNotNull(result);
        assertNull(result.get(MapConversions.DATE), "Map value should be null when converter returns null");
    }

    /**
     * Custom converter that throws an unchecked exception.
     * The exception must propagate out of {@code toMap}.
     */
    @Test
    void toMap_customConverterThrowsException_propagatesException() {
        // GIVEN
        Instant now = Instant.now();
        Converter exploding = mock(Converter.class);
        when(exploding.convert(any(), eq(String.class))).thenThrow(new IllegalStateException("boom"));
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> DateConversions.toMap(now, exploding));
        assertEquals("boom", ex.getMessage());
    }

    /**
     * Custom converter that formats a {@link java.util.Date} as "2021/04/01".
     */
    @Test
    void toMap_customDateFormatter_returnsFormattedString() {
        // GIVEN
        // epoch, but format is controlled by the converter
        Date date = new Date(0L);
        Converter slashFormatter = mock(Converter.class);
        when(slashFormatter.convert(eq(date), eq(String.class))).thenReturn("2021/04/01");
        // WHEN
        Map<String, Object> result = DateConversions.toMap(date, slashFormatter);
        // THEN
        assertNotNull(result);
        assertEquals("2021/04/01", result.get(MapConversions.DATE));
    }

    /**
     * Custom converter that formats a {@link java.time.LocalDate} as "01-04-2021".
     */
    @Test
    void toMap_customLocalDateFormatter_returnsDdMMyyyy() {
        // GIVEN
        LocalDate ld = LocalDate.of(2021, 4, 1);
        Converter slashFormatter = mock(Converter.class);
        when(slashFormatter.convert(eq(ld), eq(String.class))).thenReturn("01-04-2021");
        // WHEN
        Map<String, Object> result = DateConversions.toMap(ld, slashFormatter);
        // THEN
        assertNotNull(result);
        assertEquals("01-04-2021", result.get(MapConversions.DATE));
    }

    /**
     * Custom converter that formats a {@code null} source as "custom-null".
     */
    @Test
    void toMap_nullSourceWithCustomConverter_returnsCustomNullString() {
        // GIVEN
        Converter nullValueFormatter = mock(Converter.class);
        when(nullValueFormatter.convert(isNull(), eq(String.class))).thenReturn("custom-null");
        // WHEN
        Map<String, Object> result = DateConversions.toMap(null, nullValueFormatter);
        // THEN
        assertNotNull(result);
        assertEquals("custom-null", result.get(MapConversions.DATE));
    }

    @ParameterizedTest(name = "{index}: from={0} => expected={1}")
    @MethodSource("temporalProvider")
    void toMap_temporalTypes_withoutCustomConverter(Object from, String expected) {
        // WHEN
        Map<String, Object> result = DateConversions.toMap(from, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result.get(MapConversions.DATE));
    }
}
