package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.time.*;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToLocalDateTime_Test {

    /**
     * Provides a variety of supported source objects that should be converted
     * to the expected {@link LocalDateTime}. The expected value is expressed
     * as an ISO‑8601 string for readability.
     */
    private static Stream<Arguments> provideValidInputs() {
        ZoneId systemZone = ZoneId.systemDefault();
        // LocalDateTime – identity
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 15, 30, 45);
        // LocalDate – start of day
        LocalDate ld = LocalDate.of(2023, 5, 10);
        // Instant – converted using system default zone
        Instant instant = Instant.parse("2023-05-10T15:30:45Z");
        // java.util.Date – same instant as above
        Date date = Date.from(instant);
        // java.sql.Timestamp – same fields
        Timestamp ts = Timestamp.valueOf(ldt);
        // ZonedDateTime – offset removed (converted to system zone)
        ZonedDateTime zdt = ZonedDateTime.parse("2023-05-10T15:30:45+02:00[Europe/Paris]");
        // Calendar (UTC)
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.MAY, 10, 15, 30, 45);
        cal.set(Calendar.MILLISECOND, 0);
        // ISO‑8601 string
        String isoString = "2023-05-10T15:30:45";
        // String with space separator
        String spaceString = "2023-05-10 15:30:45";
        // Epoch milliseconds as Long
        Long epochMillis = instant.toEpochMilli();
        // CharBuffer containing ISO string
        CharBuffer charBuffer = CharBuffer.wrap(isoString);
        return Stream.of(// LocalDateTime
        Arguments.of(ldt, ldt.toString()), // LocalDate
        Arguments.of(ld, ld.atStartOfDay().toString()), // Instant
        Arguments.of(instant, LocalDateTime.ofInstant(instant, systemZone).toString()), // Date
        Arguments.of(date, LocalDateTime.ofInstant(date.toInstant(), systemZone).toString()), // Timestamp
        Arguments.of(ts, ts.toLocalDateTime().toString()), // ZonedDateTime (offset removed)
        Arguments.of(zdt, LocalDateTime.ofInstant(zdt.toInstant(), systemZone).toString()), // Calendar (UTC)
        Arguments.of(cal, LocalDateTime.ofInstant(cal.toInstant(), systemZone).toString()), // ISO‑8601 String
        Arguments.of(isoString, LocalDateTime.parse(isoString).toString()), // String with space separator
        Arguments.of(spaceString, LocalDateTime.parse(spaceString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString()), // Long epoch millis
        Arguments.of(epochMillis, LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), systemZone).toString()), // CharBuffer
        Arguments.of(charBuffer, LocalDateTime.parse(isoString).toString()));
    }

    private static Stream<Arguments> provideUnsupportedTypes() {
        return Stream.of(Arguments.of(Boolean.TRUE), Arguments.of(Integer.valueOf(123)), Arguments.of(Double.valueOf(1.23)), Arguments.of(UUID.randomUUID()), Arguments.of(List.of(1, 2, 3)), Arguments.of(Map.of("key", "value")));
    }

    private static Stream<Arguments> provideMalformedStrings() {
        return Stream.of(Arguments.of("2023-02-30T10:00:00"), Arguments.of("2023-05-10T25:61:61"), Arguments.of("not-a-date"), Arguments.of(""), Arguments.of("   "));
    }

    private static Stream<Arguments> provideInvalidCharBuffers() {
        return Stream.of(// invalid month
        Arguments.of(CharBuffer.wrap("2023-13-01T00:00:00")), // empty buffer
        Arguments.of(CharBuffer.wrap("")));
    }

    @Test
    void shouldReturnSameLocalDateTimeWhenInputIsLocalDateTime() {
        // given
        LocalDateTime now = LocalDateTime.now();
        // when
        LocalDateTime result = Converter.convertToLocalDateTime(now);
        // then
        assertEquals(now, result, "Converter should return the same LocalDateTime instance");
    }

    @Test
    void shouldConvertInstantToLocalDateTimeUsingSystemDefaultZone() {
        // given
        Instant instant = Instant.parse("2023-03-15T10:15:30.00Z");
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime expected = LocalDateTime.ofInstant(instant, zone);
        // when
        LocalDateTime result = Converter.convertToLocalDateTime(instant);
        // then
        assertEquals(expected, result, "Instant should be converted to LocalDateTime using the system default zone");
    }

    @Test
    void shouldConvertDateToLocalDateTimeUsingSystemDefaultZone() {
        // given
        // Date representing the same instant as above
        Date date = Date.from(Instant.parse("2023-03-15T10:15:30.00Z"));
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime expected = LocalDateTime.ofInstant(date.toInstant(), zone);
        // when
        LocalDateTime result = Converter.convertToLocalDateTime(date);
        // then
        assertEquals(expected, result, "java.util.Date should be converted to LocalDateTime using the system default zone");
    }

    @Test
    void shouldConvertTimestampToLocalDateTime() {
        // given
        Timestamp ts = Timestamp.valueOf("2023-03-15 10:15:30.123456789");
        LocalDateTime expected = ts.toLocalDateTime();
        // when
        LocalDateTime result = Converter.convertToLocalDateTime(ts);
        // then
        assertEquals(expected, result, "java.sql.Timestamp should be converted to its LocalDateTime representation");
    }

    @Test
    void shouldReturnNullWhenInputIsNull() {
        // given
        Object nullObj = null;
        // when
        LocalDateTime result = Converter.convertToLocalDateTime(nullObj);
        // then
        assertNull(result, "Converter should return null for a null source when target type is an object");
    }

    @Test
    void shouldThrowExceptionWhenSourceTypeIsUnsupported() {
        // given
        String unsupported = "not a date/time type";
        // when / then
        assertThrows(RuntimeException.class, () -> Converter.convertToLocalDateTime(unsupported), "Converter should throw an exception for unsupported source types");
    }

    @ParameterizedTest(name = "[{index}] convertToLocalDateTime({0}) → {1}")
    @MethodSource("provideValidInputs")
    void shouldConvertSupportedSourcesToLocalDateTime(Object source, String expectedIso) {
        // when
        LocalDateTime result = Converter.convertToLocalDateTime(source);
        // then
        assertNotNull(result, "Result should not be null for supported source");
        assertEquals(expectedIso, result.toString(), () -> "Converted value does not match expected for source: " + source);
    }

    @ParameterizedTest(name = "[{index}] convertToLocalDateTime({0}) throws IllegalArgumentException")
    @MethodSource("provideUnsupportedTypes")
    void shouldThrowWhenSourceTypeIsUnsupported(Object unsupported) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToLocalDateTime(unsupported), "Unsupported source type should cause IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] convertToLocalDateTime(\"{0}\") throws DateTimeParseException")
    @MethodSource("provideMalformedStrings")
    void shouldThrowWhenStringIsMalformed(String malformed) {
        assertThrows(DateTimeParseException.class, () -> Converter.convertToLocalDateTime(malformed), "Malformed date string should cause DateTimeParseException");
    }

    @ParameterizedTest(name = "[{index}] convertToLocalDateTime(CharBuffer) throws DateTimeParseException")
    @MethodSource("provideInvalidCharBuffers")
    void shouldThrowWhenCharBufferIsInvalid(CharBuffer buffer) {
        assertThrows(DateTimeParseException.class, () -> Converter.convertToLocalDateTime(buffer), "Invalid CharBuffer content should cause DateTimeParseException");
    }

    @Test
    void shouldThrowWhenSourceIsByteBuffer() {
        ByteBuffer bb = ByteBuffer.wrap("2023-05-10T15:30:45".getBytes());
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToLocalDateTime(bb), "ByteBuffer is not a supported source type for LocalDateTime conversion");
    }
}
