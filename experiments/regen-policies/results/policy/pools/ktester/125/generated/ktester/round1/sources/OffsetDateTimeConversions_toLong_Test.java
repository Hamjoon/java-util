package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
class OffsetDateTimeConversions_toLong_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} instance that performs the library's default conversion logic.
     */
    private static Converter identityConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter has a no‑arg constructor via ConverterOptions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        identityConverter = null;
    }

    private static Stream<Arguments> temporalProvider() {
        return Stream.of(// OffsetDateTime (UTC midnight 2023‑01‑01)
        Arguments.of(OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), 1672531200000L, "OffsetDateTime at UTC midnight"), // ZonedDateTime (America/New_York) – 12:30:15 local time = 17:30:15 UTC
        Arguments.of(ZonedDateTime.of(2023, 1, 1, 12, 30, 15, 0, ZoneId.of("America/New_York")), 1672571415000L, "ZonedDateTime America/New_York"), // java.util.Date
        Arguments.of(new Date(1672531200000L), 1672531200000L, "java.util.Date instance"), // java.sql.Timestamp
        Arguments.of(Timestamp.from(Instant.ofEpochMilli(1672531200000L)), 1672531200000L, "java.sql.Timestamp instance"));
    }

    private static Stream<Arguments> numericProvider() {
        return Stream.of(Arguments.of(1672531200000L, 1672531200000L, "Long value"), Arguments.of(new BigInteger("1672531200000"), 1672531200000L, "BigInteger value"), Arguments.of(new BigDecimal("1672531200000"), 1672531200000L, "BigDecimal value"), Arguments.of(1672531200000.0d, 1672531200000L, "Double value (truncated)"), Arguments.of(new AtomicLong(1672531200000L), 1672531200000L, "AtomicLong value"));
    }

    private static Stream<Arguments> stringProvider() {
        return Stream.of(// ISO‑8601 instant
        Arguments.of("2023-01-01T00:00:00Z", 1672531200000L, "ISO‑8601 instant string"), // Numeric epoch millisecond string
        Arguments.of("1672531200000", 1672531200000L, "Numeric string"));
    }

    @Test
    void toLong_givenOffsetDateTime_returnsEpochMilli() {
        // Arrange
        OffsetDateTime odt = OffsetDateTime.of(2023, 3, 15, 12, 30, 45, 123_000_000, ZoneOffset.ofHours(-5));
        long expected = odt.toInstant().toEpochMilli();
        // Act
        long actual = OffsetDateTimeConversions.toLong(odt, converter);
        // Assert
        assertEquals(expected, actual, "Conversion from OffsetDateTime should match its instant epoch milli");
    }

    @Test
    void toLong_givenJavaUtilDate_returnsEpochMilli() {
        // Arrange
        // Arbitrary epoch milli
        Date date = new Date(1_640_995_000_000L);
        long expected = date.getTime();
        // Act
        long actual = OffsetDateTimeConversions.toLong(date, converter);
        // Assert
        assertEquals(expected, actual, "Conversion from java.util.Date should return its getTime() value");
    }

    @Test
    void toLong_givenSqlTimestamp_returnsEpochMilli() {
        // Arrange
        java.sql.Timestamp ts = new java.sql.Timestamp(1_640_995_000_123L);
        long expected = ts.getTime();
        // Act
        long actual = OffsetDateTimeConversions.toLong(ts, converter);
        // Assert
        assertEquals(expected, actual, "Conversion from java.sql.Timestamp should return its getTime() value");
    }

    @Test
    void toLong_givenMapWithTimeKey_returnsEpochMilli() {
        // Arrange – a map representation that the Converter recognises for temporal types
        Map<String, Object> map = new HashMap<>();
        map.put("time", 1_640_995_000_456L);
        long expected = 1_640_995_000_456L;
        // Act
        long actual = OffsetDateTimeConversions.toLong(map, converter);
        // Assert
        assertEquals(expected, actual, "Conversion from a Map containing a \"time\" key should yield that epoch value");
    }

    @Test
    void toLong_givenStringRepresentingNumber_returnsParsedEpochMilli() {
        // Arrange
        String string = "1640995000789";
        long expected = 1_640_995_000_789L;
        // Act
        long actual = OffsetDateTimeConversions.toLong(string, converter);
        // Assert
        assertEquals(expected, actual, "Conversion from a numeric String should be parsed to the corresponding long value");
    }

    @Test
    void toLong_givenNull_returnsZero() {
        // Act
        long actual = OffsetDateTimeConversions.toLong(null, converter);
        // Assert
        assertEquals(0L, actual, "Null input should be converted to 0 according to Converter's null‑handling policy");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("temporalProvider")
    void toLong_validTemporalObjects(Object from, long expected, String _description) {
        long actual = OffsetDateTimeConversions.toLong(from, identityConverter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("numericProvider")
    void toLong_validNumericTypes(Object from, long expected, String _description) {
        long actual = OffsetDateTimeConversions.toLong(from, identityConverter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("stringProvider")
    void toLong_validStringRepresentations(String from, long expected, String _description) {
        long actual = OffsetDateTimeConversions.toLong(from, identityConverter);
        assertEquals(expected, actual);
    }

    @Test
    void toLong_customBooleanToLongConverter() {
        // Arrange – a mock Converter that converts Boolean true → Instant.ofEpochMilli(1)
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(true), eq(Instant.class))).thenReturn(Instant.ofEpochMilli(1L));
        // Any other conversion should fall back to the default behaviour (not used here)
        when(mockConverter.convert(any(), eq(Instant.class))).thenReturn(null);
        // Act
        long actual = OffsetDateTimeConversions.toLong(true, mockConverter);
        // Assert
        assertEquals(1L, actual, "Custom converter should map Boolean true → 1L");
    }

    @Test
    void toLong_customStringToOffsetDateTimeConverter() {
        // Arrange – a mock Converter that turns the supplied ISO‑8601 string with offset
        // into the corresponding Instant (UTC‑5 offset → epoch millis 1672552800000)
        Converter mockConverter = mock(Converter.class);
        String source = "2023-01-01T00:00:00-05:00";
        Instant expectedInstant = Instant.ofEpochMilli(1672552800000L);
        when(mockConverter.convert(eq(source), eq(Instant.class))).thenReturn(expectedInstant);
        when(mockConverter.convert(any(), eq(Instant.class))).thenReturn(null);
        // Act
        long actual = OffsetDateTimeConversions.toLong(source, mockConverter);
        // Assert
        assertEquals(1672552800000L, actual, "Custom converter should parse string with offset to the correct epoch milli");
    }

    @Test
    void toLong_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toLong(null, identityConverter), "Null input should trigger NullPointerException");
    }

    @Test
    void toLong_nullConverter_throwsNullPointerException() {
        OffsetDateTime sample = OffsetDateTime.now();
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toLong(sample, null), "Null Converter should trigger NullPointerException");
    }

    @Test
    void toLong_unsupportedBoolean_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toLong(false, identityConverter), "Unsupported Boolean should cause IllegalArgumentException");
    }

    @Test
    void toLong_unsupportedCollection_throwsIllegalArgumentException() {
        List<String> list = Collections.emptyList();
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toLong(list, identityConverter), "Unsupported collection type should cause IllegalArgumentException");
    }

    @Test
    void toLong_malformedString_throwsDateTimeParseException() {
        String bad = "not-a-date";
        assertThrows(DateTimeParseException.class, () -> OffsetDateTimeConversions.toLong(bad, identityConverter), "Non‑parseable string should trigger DateTimeParseException");
    }

    @Test
    void toLong_numericOverflowString_throwsNumberFormatException() {
        // Long.MAX_VALUE + 1
        String overflow = "9223372036854775808";
        assertThrows(NumberFormatException.class, () -> OffsetDateTimeConversions.toLong(overflow, identityConverter), "Overflow numeric string should cause NumberFormatException");
    }

    @Test
    void toLong_overflowBigInteger_throwsArithmeticException() {
        // Long.MAX_VALUE + 1
        BigInteger tooBig = new BigInteger("9223372036854775808");
        assertThrows(ArithmeticException.class, () -> OffsetDateTimeConversions.toLong(tooBig, identityConverter), "BigInteger outside long range should cause ArithmeticException");
    }

    @Test
    void toLong_mapWithTimeKey_returnsEpochMilli() {
        Map<String, Object> map = new HashMap<>();
        map.put("time", 1_640_995_000_456L);
        long actual = OffsetDateTimeConversions.toLong(map, identityConverter);
        assertEquals(1_640_995_000_456L, actual, "Map containing a \"time\" entry should be interpreted as epoch milliseconds");
    }
}
