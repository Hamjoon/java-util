package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DateConversions_toSqlDate_Test {

    /**
     * Converter used for all test cases – created once per test method.
     */
    private Converter converter;

    /**
     * Original default time‑zone – restored after all tests.
     */
    private static TimeZone originalDefaultZone;

    @Mock
    Converter // injected by MockitoExtension
    mockConverter;

    @BeforeAll
    static void setUpAll() {
        // Preserve original zone and force UTC for deterministic results.
        originalDefaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the default options – they contain the built‑in conversion mappings.
        converter = new Converter(new ConverterOptions());
        // Default converter with built‑in mappings.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent.
        converter = null;
        converter = null;
    }

    @AfterAll
    static void tearDownAll() {
        // Restore the original default zone.
        TimeZone.setDefault(originalDefaultZone);
    }

    static Stream<Arguments> numericSourceProvider() {
        return Stream.of(// Long value (milliseconds)
        Arguments.of(1622548800000L, 1622548800000L), // Integer value (treated as milliseconds)
        Arguments.of(1622548800, 1622548800L), // Numeric string
        Arguments.of("1622548800000", 1622548800000L), // Negative numeric string
        Arguments.of("-86400000", -86400000L));
    }

    static Stream<Arguments> javaTimeProvider() {
        return Stream.of(// Instant
        Arguments.of(Instant.ofEpochMilli(1622548800000L), 1622548800000L), // LocalDate (midnight UTC)
        Arguments.of(LocalDate.of(2021, 6, 1), 1622505600000L), // LocalDateTime (UTC)
        Arguments.of(LocalDateTime.of(2021, 6, 1, 12, 30, 45), 1622555445000L), // ZonedDateTime America/New_York (UTC‑04:00)
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 12, 30, 45, 0, ZoneId.of("America/New_York")), 1622565045000L), // OffsetDateTime offset -05:00
        Arguments.of(OffsetDateTime.of(2021, 6, 1, 12, 30, 45, 0, ZoneOffset.ofHours(-5)), 1622568645000L));
    }

    static Stream<Arguments> stringDateProvider() {
        return Stream.of(// ISO‑8601 UTC
        Arguments.of("2021-06-01T00:00:00Z", 1622505600000L), // Date‑only string (interpreted as midnight UTC)
        Arguments.of("2021-06-01", 1622505600000L));
    }

    /* -------------------------------------------------------------
     *  Helper – assertThrows that accepts multiple possible exception types.
     * ------------------------------------------------------------- */
    private static void assertThrowsAnyOf(Class<? extends Throwable> first, Class<? extends Throwable> second, Executable executable) {
        try {
            executable.execute();
            fail("Expected exception of type " + first.getName() + " or " + second.getName());
        } catch (Throwable actual) {
            if (!first.isInstance(actual) && !second.isInstance(actual)) {
                fail("Unexpected exception type: " + actual.getClass().getName(), actual);
            }
            // Expected – test passes.
        }
    }

    @Test
    void toSqlDate_givenUtilDate_returnsNewDateWithSameTime() {
        // GIVEN
        // 2022‑01‑01 00:00:00 UTC
        Date source = new Date(1_640_995_200_000L);
        // WHEN
        Date result = DateConversions.toSqlDate(source, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(source.getTime(), result.getTime(), "The epoch milliseconds must be identical");
        assertNotSame(source, result, "A new Date instance should be created, not the original one");
    }

    @Test
    void toSqlDate_givenLongTimestamp_returnsDateWithThatTime() {
        // GIVEN
        // same instant as above
        Long timestamp = 1_640_995_200_000L;
        // WHEN
        Date result = DateConversions.toSqlDate(timestamp, converter);
        // THEN
        assertNotNull(result);
        assertEquals(timestamp.longValue(), result.getTime(), "Date should represent the supplied millisecond timestamp");
    }

    @Test
    void toSqlDate_givenStringNumber_returnsDateWithParsedTime() {
        // GIVEN
        // milliseconds as a string
        String numericString = "1640995200000";
        // WHEN
        Date result = DateConversions.toSqlDate(numericString, converter);
        // THEN
        assertNotNull(result);
        assertEquals(1_640_995_200_000L, result.getTime(), "String containing a numeric value should be parsed to the correct epoch time");
    }

    @Test
    void toSqlDate_givenNull_returnsDateAtEpochZero() {
        // GIVEN
        Object nullValue = null;
        // WHEN
        Date result = DateConversions.toSqlDate(nullValue, converter);
        // THEN
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "Null source should be converted to 0 (epoch start) for numeric target types");
    }

    @ParameterizedTest(name = "numeric source [{0}] → epoch {1}")
    @MethodSource("numericSourceProvider")
    void toSqlDate_givenNumericSource_returnsDateWithExpectedEpoch(Object source, long expectedEpoch) {
        Date result = DateConversions.toSqlDate(source, converter);
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedEpoch, result.getTime(), () -> "Epoch milliseconds should match for source: " + source);
    }

    @Test
    void toSqlDate_givenUtilDate_returnsEquivalentDate() {
        Date src = new Date(1622548800000L);
        Date result = DateConversions.toSqlDate(src, converter);
        assertNotNull(result);
        assertEquals(src.getTime(), result.getTime(), "Date should preserve the same epoch milliseconds");
        assertNotSame(src, result, "A new Date instance must be created");
    }

    @Test
    void toSqlDate_givenSqlTimestamp_returnsDateWithSameInstant() {
        Timestamp ts = new Timestamp(1622548800000L);
        Date result = DateConversions.toSqlDate(ts, converter);
        assertNotNull(result);
        assertEquals(ts.getTime(), result.getTime(), "Timestamp should be converted to Date with identical epoch");
    }

    @Test
    void toSqlDate_givenCalendar_returnsDateWithSameInstant() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1622548800000L);
        Date result = DateConversions.toSqlDate(cal, converter);
        assertNotNull(result);
        assertEquals(1622548800000L, result.getTime(), "Calendar instant should be reflected in the resulting Date");
    }

    @ParameterizedTest(name = "java.time source [{0}] → epoch {1}")
    @MethodSource("javaTimeProvider")
    void toSqlDate_givenJavaTimeSource_returnsDateWithExpectedEpoch(Object source, long expectedEpoch) {
        Date result = DateConversions.toSqlDate(source, converter);
        assertNotNull(result);
        assertEquals(expectedEpoch, result.getTime(), () -> "Converted epoch mismatch for source: " + source);
    }

    @ParameterizedTest(name = "String date [{0}] → epoch {1}")
    @MethodSource("stringDateProvider")
    void toSqlDate_givenStringDate_returnsDateWithExpectedEpoch(String source, long expectedEpoch) {
        Date result = DateConversions.toSqlDate(source, converter);
        assertNotNull(result);
        assertEquals(expectedEpoch, result.getTime(), "String date should be parsed to the correct epoch milliseconds");
    }

    @Test
    void toSqlDate_givenUnparsableString_throwsException() {
        String bad = "2021/06/01";
        assertThrowsAnyOf(IllegalArgumentException.class, DateTimeParseException.class, () -> DateConversions.toSqlDate(bad, converter));
    }

    @Test
    void toSqlDate_givenLongMaxValue_returnsDateWithSameEpoch() {
        long max = Long.MAX_VALUE;
        Date result = DateConversions.toSqlDate(max, converter);
        assertNotNull(result);
        assertEquals(max, result.getTime(), "Long.MAX_VALUE should be wrapped without overflow");
    }

    @Test
    void toSqlDate_givenLongMinValue_returnsDateWithSameEpoch() {
        long min = Long.MIN_VALUE;
        Date result = DateConversions.toSqlDate(min, converter);
        assertNotNull(result);
        assertEquals(min, result.getTime(), "Long.MIN_VALUE should be wrapped without overflow");
    }

    @Test
    void toSqlDate_givenNullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DateConversions.toSqlDate(null, converter), "Null source should cause NullPointerException");
    }

    @Test
    void toSqlDate_givenUnsupportedObject_throwsException() {
        Object unsupported = new Object();
        assertThrowsAnyOf(IllegalArgumentException.class, NumberFormatException.class, () -> DateConversions.toSqlDate(unsupported, converter));
    }

    @Test
    void toSqlDate_givenList_throwsIllegalArgumentException() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toSqlDate(list, converter), "A List cannot be converted to a Date");
    }

    @Test
    void toSqlDate_givenCustomConverter_returnsDateFromConverter() {
        // Mock converter to always return 123456789L irrespective of input.
        when(mockConverter.convert(eq(Long.class), any())).thenReturn(123456789L);
        Date result = DateConversions.toSqlDate("any", mockConverter);
        assertNotNull(result);
        assertEquals(123456789L, result.getTime(), "Resulting Date must reflect the value returned by the mock Converter");
        verify(mockConverter).convert(eq(Long.class), eq("any"));
    }

    @Test
    void toSqlDate_customConverterThrows_propagatesException() {
        when(mockConverter.convert(eq(Long.class), any())).thenThrow(new IllegalStateException("converter failed"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> DateConversions.toSqlDate("any", mockConverter));
        assertEquals("converter failed", ex.getMessage(), "Exception message should be propagated unchanged");
    }

    @Test
    void toSqlDate_customConverterReturnsNull_throwsNullPointerException() {
        when(mockConverter.convert(eq(Long.class), any())).thenReturn(null);
        assertThrows(NullPointerException.class, () -> DateConversions.toSqlDate("any", mockConverter), "Null return from Converter should cause NullPointerException");
    }
}
