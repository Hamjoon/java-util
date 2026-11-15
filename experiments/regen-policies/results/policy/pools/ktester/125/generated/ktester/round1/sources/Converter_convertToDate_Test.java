package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.sql.Timestamp;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.convert.ConversionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToDate_Test {

    static Stream<Arguments> nullAndExistingDateProvider() {
        return Stream.of(Arguments.of("null input → null", null, null), Arguments.of("java.util.Date instance", new Date(1_600_000_000_000L), new Date(1_600_000_000_000L)), Arguments.of("java.sql.Date instance", new java.sql.Date(1_600_000_000_000L), new Date(1_600_000_000_000L)), Arguments.of("java.sql.Timestamp instance", Timestamp.valueOf("2020-09-13 12:34:56.789"), new Date(Timestamp.valueOf("2020-09-13 12:34:56.789").getTime())));
    }

    static Stream<Arguments> javaTimeProvider() {
        ZoneId systemZone = ZoneId.systemDefault();
        Instant instant = Instant.ofEpochMilli(1_600_000_000_000L);
        LocalDate localDate = LocalDate.of(2020, 9, 13);
        long localDateEpoch = localDate.atStartOfDay(systemZone).toInstant().toEpochMilli();
        LocalDateTime ldt = LocalDateTime.of(2020, 9, 13, 12, 34, 56, 789_000_000);
        long ldtEpoch = ldt.atZone(systemZone).toInstant().toEpochMilli();
        ZonedDateTime zdt = ZonedDateTime.parse("2020-09-13T12:34:56.789+02:00");
        long zdtEpoch = zdt.toInstant().toEpochMilli();
        return Stream.of(Arguments.of("Instant → Date", instant, instant.toEpochMilli()), Arguments.of("LocalDate → Date (start of day)", localDate, localDateEpoch), Arguments.of("LocalDateTime → Date (system zone)", ldt, ldtEpoch), Arguments.of("ZonedDateTime → Date (same instant)", zdt, zdtEpoch));
    }

    static Stream<Arguments> numericProvider() {
        return Stream.of(Arguments.of("Long epoch millis", 1_600_000_000_000L, 1_600_000_000_000L), Arguments.of("Integer epoch millis", 1_600_000_000, 1_600_000_000L), Arguments.of("Double epoch millis (fraction truncated)", 1_600_000.0d, 1_600_000L), Arguments.of("BigDecimal epoch millis", new BigDecimal("1_600_000_000_000"), 1_600_000_000_000L), Arguments.of("BigInteger epoch millis", new BigInteger("1_600_000_000_000"), 1_600_000_000_000L));
    }

    static Stream<Arguments> charSeqBufferProvider() {
        String iso = "2020-09-13T12:34:56.789Z";
        long epoch = Instant.parse(iso).toEpochMilli();
        return Stream.of(Arguments.of("ISO‑8601 String", iso, epoch), Arguments.of("CharBuffer with ISO‑8601", CharBuffer.wrap(iso), epoch), Arguments.of("ByteBuffer with decimal epoch string", ByteBuffer.wrap("1600000000000".getBytes(StandardCharsets.UTF_8)), 1_600_000_000_000L));
    }

    static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("Invalid month in ISO‑8601", "2020-13-01T00:00:00Z"), Arguments.of("Invalid day in ISO‑8601", "2020-02-30T12:00:00Z"), Arguments.of("Empty string", ""), Arguments.of("Non‑date text", "not-a-date"));
    }

    static Stream<Arguments> invalidBufferProvider() {
        return Stream.of(Arguments.of("CharBuffer with malformed date", CharBuffer.wrap("invalid‑date")), Arguments.of("ByteBuffer with non‑numeric text", ByteBuffer.wrap("not-a-number".getBytes(StandardCharsets.UTF_8))), Arguments.of("ByteBuffer with overflow value (> Long.MAX_VALUE)", // Long.MAX_VALUE + 1
        ByteBuffer.wrap("9223372036854775808".getBytes(StandardCharsets.UTF_8))));
    }

    static Stream<Arguments> invalidNumericProvider() {
        return Stream.of(Arguments.of("Double.NaN", Double.NaN), Arguments.of("Double.POSITIVE_INFINITY", Double.POSITIVE_INFINITY), Arguments.of("Float.NaN", Float.NaN), Arguments.of("BigDecimal overflow (> Long.MAX_VALUE)", new BigDecimal("9223372036854775808")), Arguments.of("BigInteger overflow (> Long.MAX_VALUE)", new BigInteger("9223372036854775808")));
    }

    static Stream<Arguments> unsupportedStructureProvider() {
        return Stream.of(Arguments.of("List of integers", Arrays.asList(1, 2, 3)), Arguments.of("Map with a single entry", Collections.singletonMap("key", "value")), Arguments.of("int array", new int[] { 1, 2, 3 }));
    }

    @Test
    void convertFromDate_returnsSameInstance() {
        Date now = new Date();
        Date result = Converter.convertToDate(now);
        // The converter should return a Date representing the same instant.
        assertNotNull(result);
        assertEquals(now.getTime(), result.getTime());
    }

    @Test
    void convertFromTimestamp_returnsDateWithSameEpochMillis() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Date result = Converter.convertToDate(ts);
        assertNotNull(result);
        assertEquals(ts.getTime(), result.getTime());
    }

    @Test
    void convertFromLongReturnsDate() {
        // 2020‑09‑13T12:26:40Z
        long epochMillis = 1_600_000_000_000L;
        Date result = Converter.convertToDate(epochMillis);
        assertNotNull(result);
        assertEquals(epochMillis, result.getTime());
    }

    @Test
    void convertFromStringReturnsDate() {
        // The built‑in converter accepts ISO‑8601 strings, e.g. "2020-01-02T03:04:05Z"
        String iso = "2020-01-02T03:04:05Z";
        Instant expected = Instant.parse(iso);
        Date result = Converter.convertToDate(iso);
        assertNotNull(result);
        assertEquals(expected.toEpochMilli(), result.getTime());
    }

    @Test
    void convertFromLocalDateReturnsStartOfDayDate() {
        LocalDate localDate = LocalDate.of(2021, Month.MARCH, 15);
        // Converter treats a LocalDate as the start of the day in the default zone.
        ZoneId zone = ZoneId.systemDefault();
        Instant expected = localDate.atStartOfDay(zone).toInstant();
        Date result = Converter.convertToDate(localDate);
        assertNotNull(result);
        assertEquals(expected.toEpochMilli(), result.getTime());
    }

    @Test
    void convertFromLocalDateTimeReturnsDate() {
        LocalDateTime ldt = LocalDateTime.of(2022, 5, 10, 14, 30, 45);
        ZoneId zone = ZoneId.systemDefault();
        Instant expected = ldt.atZone(zone).toInstant();
        Date result = Converter.convertToDate(ldt);
        assertNotNull(result);
        assertEquals(expected.toEpochMilli(), result.getTime());
    }

    @Test
    void convertFromZonedDateTimeReturnsDate() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 7, 4, 9, 15, 0, 0, ZoneId.of("UTC"));
        Instant expected = zdt.toInstant();
        Date result = Converter.convertToDate(zdt);
        assertNotNull(result);
        assertEquals(expected.toEpochMilli(), result.getTime());
    }

    @Test
    void convertFromNullReturnsNull() {
        assertNull(Converter.convertToDate(null));
    }

    @Test
    void convertFromUnsupportedTypeReturnsNull() {
        // An arbitrary object that has no conversion rule.
        Object unsupported = new Object();
        assertNull(Converter.convertToDate(unsupported));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullAndExistingDateProvider")
    void testNullAndExistingDate(String description, Object input, Object expected) {
        if (expected == null) {
            assertNull(Converter.convertToDate(input), description);
        } else {
            Date result = Converter.convertToDate(input);
            assertNotNull(result, description);
            assertEquals(((Date) expected).getTime(), result.getTime(), description);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("javaTimeProvider")
    void testJavaTimeConversions(String description, Object input, long expectedEpochMilli) {
        Date result = Converter.convertToDate(input);
        assertNotNull(result, description);
        assertEquals(expectedEpochMilli, result.getTime(), description);
    }

    @Test
    void convertFromCalendar_returnsDateWithSameInstant() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(1_600_000_000_000L);
        Date result = Converter.convertToDate(calendar);
        assertNotNull(result);
        assertEquals(1_600_000_000_000L, result.getTime());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numericProvider")
    void testNumericConversions(String description, Object input, long expectedEpochMilli) {
        Date result = Converter.convertToDate(input);
        assertNotNull(result, description);
        assertEquals(expectedEpochMilli, result.getTime(), description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("charSeqBufferProvider")
    void testCharSeqAndBufferConversions(String description, Object input, long expectedEpochMilli) {
        Date result = Converter.convertToDate(input);
        assertNotNull(result, description);
        assertEquals(expectedEpochMilli, result.getTime(), description);
    }

    @Test
    void convertNegativeEpochDate_preservesValue() {
        Date negative = new Date(-123_456_789L);
        Date result = Converter.convertToDate(negative);
        assertNotNull(result);
        assertEquals(-123_456_789L, result.getTime());
    }

    @Test
    void convertLongMaxValue_throwsConversionException() {
        assertThrows(ConversionException.class, () -> Converter.convertToDate(Long.MAX_VALUE), "Long.MAX_VALUE is out of java.util.Date range and should cause ConversionException");
    }

    @Test
    void convertUnsupportedObject_throwsConversionException() {
        Object unsupported = new Object();
        assertThrows(ConversionException.class, () -> Converter.convertToDate(unsupported), "Unsupported source type should raise ConversionException");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidStringProvider")
    void testInvalidStringFormats(String description, String input) {
        assertThrows(ConversionException.class, () -> Converter.convertToDate(input), description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidBufferProvider")
    void testInvalidBufferContents(String description, Object buffer) {
        assertThrows(ConversionException.class, () -> Converter.convertToDate(buffer), description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidNumericProvider")
    void testInvalidNumericValues(String description, Object input) {
        assertThrows(ConversionException.class, () -> Converter.convertToDate(input), description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedStructureProvider")
    void testUnsupportedStructures(String description, Object input) {
        assertThrows(ConversionException.class, () -> Converter.convertToDate(input), description);
    }
}
