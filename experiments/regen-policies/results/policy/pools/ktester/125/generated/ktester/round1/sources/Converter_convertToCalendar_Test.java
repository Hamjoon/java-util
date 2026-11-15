package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToCalendar_Test {

    private static Stream<Arguments> unsupportedTypeProvider() {
        return Stream.of(// Boolean
        Arguments.of(true), // Integer
        Arguments.of(12345));
    }

    private static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("not-a-date"), // impossible date
        Arguments.of("2023-02-30T12:00:00"));
    }

    @Test
    @DisplayName("convert null returns null")
    void testConvertNull() {
        Calendar result = Converter.convertToCalendar(null);
        assertNull(result, "Converting null should return null");
    }

    @Test
    @DisplayName("convert Calendar returns equivalent Calendar")
    void testConvertFromCalendar() {
        Calendar original = new GregorianCalendar();
        // fixed instant
        original.setTimeInMillis(1_600_000_000_000L);
        Calendar converted = Converter.convertToCalendar(original);
        assertNotNull(converted);
        assertEquals(original.getTimeInMillis(), converted.getTimeInMillis(), "Converted Calendar should have the same instant as the original");
    }

    @Test
    @DisplayName("convert Date to Calendar")
    void testConvertFromDate() {
        Date date = new Date(1_600_000_000_123L);
        Calendar cal = Converter.convertToCalendar(date);
        assertNotNull(cal);
        assertEquals(date.getTime(), cal.getTimeInMillis());
    }

    @Test
    @DisplayName("convert Long (epoch millis) to Calendar")
    void testConvertFromLong() {
        Long epochMillis = 1_600_000_000_456L;
        Calendar cal = Converter.convertToCalendar(epochMillis);
        assertNotNull(cal);
        assertEquals(epochMillis.longValue(), cal.getTimeInMillis());
    }

    @Test
    @DisplayName("convert Instant to Calendar")
    void testConvertFromInstant() {
        Instant instant = Instant.ofEpochMilli(1_600_000_000_789L);
        Calendar cal = Converter.convertToCalendar(instant);
        assertNotNull(cal);
        assertEquals(instant.toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    @DisplayName("convert LocalDateTime to Calendar (system default zone)")
    void testConvertFromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2020, 9, 13, 12, 34, 56, 123_000_000);
        Calendar cal = Converter.convertToCalendar(ldt);
        assertNotNull(cal);
        long expected = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals(expected, cal.getTimeInMillis());
    }

    @Test
    @DisplayName("convert ZonedDateTime to Calendar")
    void testConvertFromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2020, 9, 13, 12, 34, 56, 0, ZoneId.of("UTC"));
        Calendar cal = Converter.convertToCalendar(zdt);
        assertNotNull(cal);
        assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    @DisplayName("convert Timestamp to Calendar")
    void testConvertFromTimestamp() {
        Timestamp ts = new Timestamp(1_600_000_001_234L);
        // add sub‑millisecond part
        ts.setNanos(567_000_000);
        Calendar cal = Converter.convertToCalendar(ts);
        assertNotNull(cal);
        // Timestamp conversion should preserve the millisecond part; nanos are ignored for Calendar
        assertEquals(ts.getTime(), cal.getTimeInMillis());
    }

    @Test
    @DisplayName("convert Map with 'time' key to Calendar")
    void testConvertFromMap() {
        Map<String, Object> map = Map.of("time", 1_600_000_002_345L);
        Calendar cal = Converter.convertToCalendar(map);
        assertNotNull(cal);
        assertEquals(1_600_000_002_345L, cal.getTimeInMillis());
    }

    @Test
    @DisplayName("convert null returns null")
    void testConvertNullReturnsNull() {
        // According to the original API documentation a null source returns null.
        // The JSON‑driven test suite expects a NullPointerException, so we keep
        // both behaviours separate: this test asserts the documented behaviour.
        Calendar result = Converter.convertToCalendar(null);
        assertNull(result, "Converting null should return null according to API contract");
    }

    @Test
    @DisplayName("convert null throws NullPointerException (JSON‑driven expectation)")
    void testConvertNullThrowsNPE() {
        assertThrows(NullPointerException.class, () -> Converter.convertToCalendar(null), "Passing null should trigger NullPointerException as per JSON test definition");
    }

    @ParameterizedTest(name = "{index}: convert {0} should throw IllegalArgumentException")
    @MethodSource("unsupportedTypeProvider")
    @DisplayName("convert unsupported types throws IllegalArgumentException")
    void testConvertUnsupportedTypes(Object unsupported) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToCalendar(unsupported), "Unsupported source type should raise IllegalArgumentException");
    }

    @ParameterizedTest(name = "{index}: convert string \"{0}\" throws IllegalArgumentException")
    @MethodSource("invalidStringProvider")
    @DisplayName("convert invalid date‑time strings throws IllegalArgumentException")
    void testConvertInvalidString(String badString) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToCalendar(badString), "Invalid date‑time string should raise IllegalArgumentException");
    }
}
