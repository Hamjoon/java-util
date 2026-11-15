package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class Converter_convertToTimestamp_Test {

    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// already a Timestamp – should return the same instance (or equal value)
        Arguments.of("Timestamp instance should be returned unchanged", new Timestamp(1622548800000L), new Timestamp(1622548800000L)), // java.util.Date
        Arguments.of("Date → Timestamp", new Date(1622548800000L), new Timestamp(1622548800000L)), // java.time.Instant
        Arguments.of("Instant → Timestamp", Instant.ofEpochMilli(1622548800000L), new Timestamp(1622548800000L)), // java.time.LocalDateTime (system default zone)
        Arguments.of("LocalDateTime → Timestamp (system default zone)", LocalDateTime.of(2021, 6, 1, 12, 0, 0), Timestamp.from(LocalDateTime.of(2021, 6, 1, 12, 0, 0).atZone(ZoneId.systemDefault()).toInstant())), // java.time.ZonedDateTime (UTC)
        Arguments.of("ZonedDateTime UTC → Timestamp", ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC")), new Timestamp(1622548800000L)), // Long epoch millis
        Arguments.of("Long epoch millis → Timestamp", 1622548800000L, new Timestamp(1622548800000L)), // ISO‑8601 String without zone (treated as UTC)
        Arguments.of("ISO‑8601 String → Timestamp", "2021-06-01T12:00:00", new Timestamp(1622548800000L)), // ByteBuffer containing a long (epoch millis)
        Arguments.of("ByteBuffer (8‑byte long) → Timestamp", // 0x00000100_00000000 = 4294967296? actually represents 0x00000100_00000000 = 4294967296 ms ≈ 1970‑01‑50; use simpler example:
        ByteBuffer.wrap(new byte[] { 0, 0, 1, 0, 0, 0, 0, 0 }), new Timestamp(ByteBuffer.wrap(new byte[] { 0, 0, 0, 0, 0, 0, 0, 1 }).getLong())), // CharBuffer containing numeric string (epoch millis)
        Arguments.of("CharBuffer numeric string → Timestamp", CharBuffer.wrap("1622548800000"), new Timestamp(1622548800000L)));
    }

    private static Stream<Arguments> edgeCasesProvider() {
        return Stream.of(Arguments.of("null input returns null", null, null), Arguments.of("epoch 0 → Timestamp", 0L, new Timestamp(0L)), Arguments.of("Long.MIN_VALUE → Timestamp", Long.MIN_VALUE, new Timestamp(Long.MIN_VALUE)), Arguments.of("Long.MAX_VALUE → Timestamp", Long.MAX_VALUE, new Timestamp(Long.MAX_VALUE)), Arguments.of("empty string → IllegalArgumentException", "", IllegalArgumentException.class), Arguments.of("invalid month in string → IllegalArgumentException", "2021-13-01T00:00:00", IllegalArgumentException.class));
    }

    private static Stream<Arguments> additionalSupportedProvider() {
        // Calendar (UTC)
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(1622548800000L);
        // java.sql.Date (midnight UTC)
        java.sql.Date sqlDate = new java.sql.Date(1622548800000L);
        // LocalDate (start of day system default)
        LocalDate localDate = LocalDate.of(2021, 6, 1);
        Timestamp tsFromLocalDate = Timestamp.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        // LocalDateTime with nanoseconds (truncated to ms)
        LocalDateTime ldtNanos = LocalDateTime.of(2021, 6, 1, 12, 34, 56, 123_456_789);
        Timestamp tsFromLdtNanos = Timestamp.valueOf(ldtNanos);
        // ZonedDateTime New York (UTC‑4 in summer)
        ZonedDateTime zdtNY = ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("America/New_York"));
        Timestamp tsFromZdtNY = Timestamp.from(zdtNY.toInstant());
        // Instant with sub‑millisecond part (truncated)
        Instant instantWithNanos = Instant.ofEpochSecond(1622548800L, 500_000_000L);
        Timestamp tsFromInstantNanos = Timestamp.from(instantWithNanos);
        // CharBuffer with surrounding whitespace
        CharBuffer cbWhitespace = CharBuffer.wrap(" 1622548800000 ");
        // ByteBuffer full 8‑byte long (value 1)
        ByteBuffer bbOne = ByteBuffer.wrap(new byte[] { 0, 0, 0, 0, 0, 0, 0, 1 });
        return Stream.of(Arguments.of("Calendar (UTC) → Timestamp", utcCal, new Timestamp(1622548800000L)), Arguments.of("java.sql.Date → Timestamp", sqlDate, new Timestamp(1622548800000L)), Arguments.of("LocalDate → Timestamp (system default zone start‑of‑day)", localDate, tsFromLocalDate), Arguments.of("LocalDateTime with nanoseconds → Timestamp (truncated to ms)", ldtNanos, tsFromLdtNanos), Arguments.of("ZonedDateTime America/New_York → Timestamp (UTC equivalent)", zdtNY, tsFromZdtNY), Arguments.of("Instant with nanosecond part → Timestamp (truncated to ms)", instantWithNanos, tsFromInstantNanos), Arguments.of("CharBuffer with whitespace → Timestamp", cbWhitespace, new Timestamp(1622548800000L)), Arguments.of("ByteBuffer (value 1) → Timestamp", bbOne, new Timestamp(1L)));
    }

    private static Stream<Arguments> unsupportedProvider() {
        return Stream.of(Arguments.of("plain Object → IllegalArgumentException", new Object()), Arguments.of("UUID → IllegalArgumentException", UUID.randomUUID()), Arguments.of("Collection → IllegalArgumentException", Arrays.asList(1, 2, 3)));
    }

    private static Stream<Arguments> invalidFormatsProvider() {
        return Stream.of(Arguments.of("non‑date string → IllegalArgumentException", "not-a-date"), Arguments.of("CharBuffer non‑numeric → IllegalArgumentException", CharBuffer.wrap("abc123")), Arguments.of("CharBuffer blank → IllegalArgumentException", CharBuffer.wrap("   ")), Arguments.of("String with trailing garbage → IllegalArgumentException", "1622548800000abc"), Arguments.of("String exceeding Long.MAX_VALUE → IllegalArgumentException", "9223372036854775808"));
    }

    @Test
    void convertFromSqlTimestampReturnsSameInstance() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp result = Converter.convertToTimestamp(now);
        assertSame(now, result, "Converting a Timestamp should return the original instance");
    }

    @Test
    void convertFromDateToTimestamp() {
        // specific epoch millis
        Date date = new Date(1_600_000_000_000L);
        Timestamp ts = Converter.convertToTimestamp(date);
        assertNotNull(ts);
        assertEquals(date.getTime(), ts.getTime(), "Timestamp should have the same epoch millis as the Date");
    }

    @Test
    void convertFromLongToTimestamp() {
        long epochMillis = 1_600_000_000_000L;
        Timestamp ts = Converter.convertToTimestamp(epochMillis);
        assertNotNull(ts);
        assertEquals(epochMillis, ts.getTime());
    }

    @Test
    void convertFromStringToTimestamp() {
        // ISO‑8601 format is supported by the underlying converter
        String iso = "2023-01-15T12:34:56.789";
        Timestamp ts = Converter.convertToTimestamp(iso);
        assertNotNull(ts);
        // Verify that the parsed instant matches the expected epoch millis
        // assume UTC if no zone supplied
        long expected = Instant.parse(iso + "Z").toEpochMilli();
        assertEquals(expected, ts.getTime());
    }

    @Test
    void convertFromMapToTimestamp() {
        // Map with the key "time" (millis) is a supported representation
        Map<String, Object> map = Map.of("time", 1_600_000_000_000L);
        Timestamp ts = Converter.convertToTimestamp(map);
        assertNotNull(ts);
        assertEquals(1_600_000_000_000L, ts.getTime());
    }

    @Test
    void convertNullReturnsNull() {
        Timestamp ts = Converter.convertToTimestamp(null);
        assertNull(ts, "Converting null should return null for object target types");
    }

    // ---------- Valid conversions ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionsProvider")
    void validConversions(String description, Object input, Timestamp expected) {
        Timestamp actual = Converter.convertToTimestamp(input);
        assertNotNull(actual, description);
        assertEquals(expected.getTime(), actual.getTime(), description);
    }

    // ---------- Edge cases and null handling ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("edgeCasesProvider")
    void edgeCases(String description, Object input, Object expected) {
        if (expected == null) {
            Timestamp actual = Converter.convertToTimestamp(input);
            assertNull(actual, description);
        } else if (expected instanceof Timestamp) {
            Timestamp actual = Converter.convertToTimestamp(input);
            assertNotNull(actual, description);
            assertEquals(((Timestamp) expected).getTime(), actual.getTime(), description);
        } else if (expected instanceof Class && ((Class<?>) expected).equals(IllegalArgumentException.class)) {
            assertThrows(IllegalArgumentException.class, () -> Converter.convertToTimestamp(input), description);
        }
    }

    // ---------- Supported conversions not yet covered ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("additionalSupportedProvider")
    void additionalSupportedConversions(String description, Object input, Timestamp expected) {
        Timestamp actual = Converter.convertToTimestamp(input);
        assertNotNull(actual, description);
        assertEquals(expected.getTime(), actual.getTime(), description);
    }

    // ---------- Unsupported source types ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedProvider")
    void unsupportedConversions(String description, Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToTimestamp(input), description);
    }

    // ---------- Invalid string / CharBuffer formats ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidFormatsProvider")
    void invalidStringAndCharBufferFormats(String description, Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToTimestamp(input), description);
    }

    // ---------- ByteBuffer with insufficient bytes ----------
    @Test
    void byteBufferTooShortThrows() {
        // only 4 bytes
        ByteBuffer shortBuffer = ByteBuffer.wrap(new byte[] { 0, 0, 1, 0 });
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToTimestamp(shortBuffer), "ByteBuffer with fewer than 8 bytes should cause IllegalArgumentException");
    }
}
