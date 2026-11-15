package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToCalendar_Test {

    private static Stream<Arguments> successfulConversionProvider() {
        Instant fixedInstant = Instant.parse("2023-04-01T12:34:56Z");
        long fixedEpoch = fixedInstant.toEpochMilli();
        ???;
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(Arguments.of("UUID not supported", UUID.fromString("123e4567-e89b-12d3-a456-426614174000")), Arguments.of("ByteBuffer not supported", ByteBuffer.wrap(new byte[] { 0, 0, 0, 1 })), Arguments.of("Boolean not supported", Boolean.TRUE), Arguments.of("Byte not supported", Byte.valueOf((byte) 5)), Arguments.of("Short not supported", Short.valueOf((short) 10)), Arguments.of("Float not supported", Float.valueOf(3.14f)), Arguments.of("java.sql.Date not supported", java.sql.Date.valueOf(LocalDate.of(2023, 4, 1))), Arguments.of("LocalTime not supported", LocalTime.of(12, 34, 56)), Arguments.of("OffsetDateTime not supported", OffsetDateTime.of(2023, 4, 1, 12, 34, 56, 0, ZoneOffset.ofHours(2))), Arguments.of("OffsetTime not supported", OffsetTime.of(12, 34, 56, 0, ZoneOffset.ofHours(2))), Arguments.of("List not supported", Arrays.asList(1, 2, 3)), Arguments.of("Map not supported", Map.of("a", 1)), Arguments.of("int[] not supported", new int[] { 1, 2, 3 }), Arguments.of("CharBuffer not supported", CharBuffer.wrap("2023-04-01T12:34:56Z")), Arguments.of("AtomicInteger not supported", new java.util.concurrent.atomic.AtomicInteger(5)), Arguments.of("Empty string", ""), Arguments.of("Whitespace string", "   "), Arguments.of("Invalid date string", "not-a-date"), Arguments.of("Invalid calendar date", "2023-02-30"));
    }

    @Test
    void convertFromCalendar_shouldReturnEqualCalendar() {
        Calendar original = Calendar.getInstance();
        original.set(2023, Calendar.MARCH, 15, 10, 30, 45);
        original.set(Calendar.MILLISECOND, 123);
        Calendar converted = Converter.convertToCalendar(original);
        assertNotNull(converted, "Converted calendar must not be null");
        assertEquals(original.getTimeInMillis(), converted.getTimeInMillis(), "The time in millis should be identical to the source calendar");
    }

    @Test
    void convertFromDate_shouldReturnCalendarWithSameInstant() {
        // current instant
        Date date = new Date();
        Calendar converted = Converter.convertToCalendar(date);
        assertNotNull(converted, "Converted calendar must not be null");
        assertEquals(date.getTime(), converted.getTimeInMillis(), "Calendar should represent the same instant as the source Date");
    }

    @Test
    void convertFromLong_shouldInterpretAsEpochMillis() {
        long epochMillis = System.currentTimeMillis();
        Calendar converted = Converter.convertToCalendar(Long.valueOf(epochMillis));
        assertNotNull(converted, "Converted calendar must not be null");
        assertEquals(epochMillis, converted.getTimeInMillis(), "Calendar should represent the epoch millis supplied as Long");
    }

    @Test
    void convertFromLocalDateTime_shouldUseSystemDefaultZone() {
        LocalDateTime ldt = LocalDateTime.of(2022, Month.DECEMBER, 31, 23, 59, 59, 123_000_000);
        Calendar converted = Converter.convertToCalendar(ldt);
        assertNotNull(converted, "Converted calendar must not be null");
        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
        assertEquals(zdt.toInstant().toEpochMilli(), converted.getTimeInMillis(), "Calendar should represent the same instant as the supplied LocalDateTime");
    }

    @Test
    void convertFromZonedDateTime_shouldPreserveInstant() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 5, 20, 14, 45, 30, 0, ZoneId.of("UTC"));
        Calendar converted = Converter.convertToCalendar(zdt);
        assertNotNull(converted, "Converted calendar must not be null");
        assertEquals(zdt.toInstant().toEpochMilli(), converted.getTimeInMillis(), "Calendar should represent the same instant as the supplied ZonedDateTime");
    }

    @Test
    void convertFromNull_shouldReturnNull() {
        Calendar result = Converter.convertToCalendar(null);
        assertNull(result, "Converting a null source must return null");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulConversionProvider")
    void convertToCalendar_successful(String description, Object source, long expectedEpochMillis) {
        Calendar result = Converter.convertToCalendar(source);
        assertNotNull(result, description + " – result must not be null");
        assertEquals(expectedEpochMillis, result.getTimeInMillis(), description + " – epoch millis should match");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionConversionProvider")
    void convertToCalendar_throws(String description, Object source) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToCalendar(source), description + " – should throw IllegalArgumentException");
    }
}
