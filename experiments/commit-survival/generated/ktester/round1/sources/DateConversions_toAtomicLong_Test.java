package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DateConversions_toAtomicLong_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @Mock
    Converter // Mockito will inject a mock instance
    mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default constructor.
        // If a different constructor is needed, adjust accordingly.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
        converter = null;
    }

    @Test
    void shouldConvertLong() {
        Long source = 42L;
        AtomicLong result = DateConversions.toAtomicLong(source, converter);
        assertEquals(42L, result.get(), "AtomicLong value should match the Long source");
    }

    @Test
    void shouldConvertInteger() {
        Integer source = 7;
        AtomicLong result = DateConversions.toAtomicLong(source, converter);
        assertEquals(7L, result.get(), "AtomicLong value should match the Integer source");
    }

    @Test
    void shouldConvertStringNumber() {
        String source = "12345";
        AtomicLong result = DateConversions.toAtomicLong(source, converter);
        assertEquals(12345L, result.get(), "AtomicLong value should match the numeric String source");
    }

    @Test
    void shouldConvertDate() {
        // Date representing 2023‑01‑01T00:00:00Z
        Date source = Date.from(Instant.parse("2023-01-01T00:00:00Z"));
        AtomicLong result = DateConversions.toAtomicLong(source, converter);
        assertEquals(source.getTime(), result.get(), "AtomicLong should contain the epoch millisecond value of the Date");
    }

    @Test
    void shouldConvertCalendar() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(Instant.parse("2022-12-31T12:34:56Z").toEpochMilli());
        AtomicLong result = DateConversions.toAtomicLong(cal, converter);
        assertEquals(cal.getTimeInMillis(), result.get(), "AtomicLong should contain the epoch millisecond value of the Calendar");
    }

    @Test
    void shouldConvertInstant() {
        Instant source = Instant.parse("2021-06-15T10:20:30.123Z");
        AtomicLong result = DateConversions.toAtomicLong(source, converter);
        assertEquals(source.toEpochMilli(), result.get(), "AtomicLong should contain the epoch millisecond value of the Instant");
    }

    @Test
    void shouldConvertLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2020, Month.MARCH, 5, 14, 30, 15);
        // Assume system default zone for conversion to epoch millis
        long expected = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        AtomicLong result = DateConversions.toAtomicLong(ldt, converter);
        assertEquals(expected, result.get(), "AtomicLong should contain the epoch millisecond value of the LocalDateTime");
    }

    @Test
    void shouldConvertZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2019, 11, 20, 9, 45, 0, 0, ZoneId.of("America/New_York"));
        long expected = zdt.toInstant().toEpochMilli();
        AtomicLong result = DateConversions.toAtomicLong(zdt, converter);
        assertEquals(expected, result.get(), "AtomicLong should contain the epoch millisecond value of the ZonedDateTime");
    }

    @Test
    void shouldConvertOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2018, 5, 10, 8, 15, 30, 0, ZoneOffset.ofHours(-4));
        long expected = odt.toInstant().toEpochMilli();
        AtomicLong result = DateConversions.toAtomicLong(odt, converter);
        assertEquals(expected, result.get(), "AtomicLong should contain the epoch millisecond value of the OffsetDateTime");
    }

    @Test
    void shouldConvertLocalDate() {
        LocalDate ld = LocalDate.of(2017, Month.JULY, 4);
        // LocalDate is interpreted as start of day at the system default zone
        long expected = ld.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        AtomicLong result = DateConversions.toAtomicLong(ld, converter);
        assertEquals(expected, result.get(), "AtomicLong should contain the epoch millisecond value of the LocalDate (start of day)");
    }

    @Test
    void shouldReturnZeroAtomicLongWhenSourceIsNull() {
        AtomicLong result = DateConversions.toAtomicLong(null, converter);
        assertEquals(0L, result.get(), "Null source should be converted to AtomicLong with value 0");
    }

    @Test
    void shouldConvertBooleanTrueToOne() {
        Boolean source = Boolean.TRUE;
        AtomicLong result = DateConversions.toAtomicLong(source, converter);
        assertEquals(1L, result.get(), "Boolean true should be converted to 1");
    }

    @Test
    void shouldConvertBooleanFalseToZero() {
        Boolean source = Boolean.FALSE;
        AtomicLong result = DateConversions.toAtomicLong(source, converter);
        assertEquals(0L, result.get(), "Boolean false should be converted to 0");
    }

    /**
     * Null source *and* null converter – the implementation is expected to NPE.
     */
    @Test
    void shouldThrowNPEWhenBothSourceAndConverterAreNull() {
        assertThrows(NullPointerException.class, () -> DateConversions.toAtomicLong(null, null), "Both parameters null should result in NullPointerException");
    }

    /**
     * Null converter but a valid numeric string source.
     */
    @Test
    void shouldParseStringZeroWhenConverterIsNull() {
        AtomicLong result = DateConversions.toAtomicLong("0", null);
        assertEquals(0L, result.get(), "String \"0\" with null converter should yield 0");
    }

    /**
     * String literal "null" cannot be parsed to a number.
     */
    @Test
    void shouldThrowNumberFormatExceptionForStringLiteralNull() {
        assertThrows(NumberFormatException.class, () -> DateConversions.toAtomicLong("null", null), "String \"null\" should trigger NumberFormatException");
    }

    @Test
    void shouldConvertBigInteger() {
        BigInteger source = new BigInteger("9876543210");
        AtomicLong result = DateConversions.toAtomicLong(source, converter);
        assertEquals(9876543210L, result.get(), "BigInteger should be converted to its long value");
    }

    @Test
    void shouldConvertBigDecimalTruncatingFraction() {
        BigDecimal source = new BigDecimal("12345.678");
        AtomicLong result = DateConversions.toAtomicLong(source, converter);
        assertEquals(12345L, result.get(), "BigDecimal should be truncated (floor) to long");
    }

    @Test
    void shouldConvertDoubleTruncatingFraction() {
        Double source = 99.99;
        AtomicLong result = DateConversions.toAtomicLong(source, converter);
        assertEquals(99L, result.get(), "Double should be truncated to long");
    }

    @ParameterizedTest(name = "[{index}] parse \"{0}\" → {1}")
    @CsvSource({ "'  555  ',555", "'-2147483648',-2147483648", "'9223372036854775807',9223372036854775807" })
    void shouldParseNumericStrings(String input, long expected) {
        AtomicLong result = DateConversions.toAtomicLong(input, null);
        assertEquals(expected, result.get(), () -> "String \"" + input + "\" should be parsed to " + expected);
    }

    @Test
    void shouldThrowNumberFormatExceptionForNonNumericString() {
        assertThrows(NumberFormatException.class, () -> DateConversions.toAtomicLong("notANumber", null), "Non‑numeric string should cause NumberFormatException");
    }

    @Test
    void shouldThrowNumberFormatExceptionForOverflowString() {
        assertThrows(NumberFormatException.class, () -> DateConversions.toAtomicLong("-9223372036854775809", null), "String below Long.MIN_VALUE should cause NumberFormatException");
    }

    @Test
    void shouldConvertSqlTimestamp() {
        Timestamp ts = new Timestamp(1609459200123L);
        AtomicLong result = DateConversions.toAtomicLong(ts, converter);
        assertEquals(1609459200123L, result.get(), "Timestamp should be converted to its epoch millisecond value");
    }

    @Test
    void shouldConvertLocalDateTimeAssumingUTC() {
        LocalDateTime ldt = LocalDateTime.of(2021, 1, 1, 0, 0, 0);
        long expected = ldt.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();
        AtomicLong result = DateConversions.toAtomicLong(ldt, converter);
        assertEquals(expected, result.get(), "LocalDateTime without zone is interpreted as UTC");
    }

    @Test
    void shouldConvertZonedDateTimeWithSpecificZone() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 1, 1, 12, 30, 0, 0, // Paris is UTC+1 in winter
        ZoneId.of("Europe/Paris"));
        long expected = zdt.toInstant().toEpochMilli();
        AtomicLong result = DateConversions.toAtomicLong(zdt, converter);
        assertEquals(expected, result.get(), "ZonedDateTime should be converted to the correct epoch millis");
    }

    @Test
    void shouldConvertCalendarInUTC() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1609459200000L);
        AtomicLong result = DateConversions.toAtomicLong(cal, converter);
        assertEquals(1609459200000L, result.get(), "Calendar set to UTC epoch millis should be converted accurately");
    }

    @Test
    void shouldUseCustomConverterResult() {
        // Arrange: mock returns a Long when asked to convert the string "custom"
        when(mockConverter.convert(eq("custom"), eq(Long.class))).thenReturn(777L);
        AtomicLong result = DateConversions.toAtomicLong("custom", mockConverter);
        assertEquals(777L, result.get(), "Custom converter's returned value should be used for AtomicLong");
        verify(mockConverter).convert("custom", Long.class);
    }

    @Test
    void shouldPropagateExceptionFromCustomConverter() {
        RuntimeException ex = new RuntimeException("converter failure");
        when(mockConverter.convert(any(), eq(Long.class))).thenThrow(ex);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> DateConversions.toAtomicLong(new Object(), mockConverter), "Exception from converter should be propagated");
        assertSame(ex, thrown, "Propagated exception should be the original one");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForArray() {
        int[] array = { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toAtomicLong(array, converter), "Array types are not supported for numeric conversion");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForMap() {
        Map<String, Object> map = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toAtomicLong(map, converter), "Map objects are not convertible to a long");
    }

    @Test
    void shouldConvertLongMaxValue() {
        AtomicLong result = DateConversions.toAtomicLong(Long.MAX_VALUE, converter);
        assertEquals(Long.MAX_VALUE, result.get(), "Maximum long value should be wrapped unchanged");
    }

    @Test
    void shouldConvertLongMinValue() {
        AtomicLong result = DateConversions.toAtomicLong(Long.MIN_VALUE, converter);
        assertEquals(Long.MIN_VALUE, result.get(), "Minimum long value should be wrapped unchanged");
    }
}
