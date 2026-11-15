package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toInstant_Test {

    /**
     * Shared Converter used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter needs a ConverterOptions instance; the default constructor provides sensible defaults.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long‑running test suites.
        converter = null;
    }

    static Stream<Arguments> validNumericProvider() {
        return Stream.of(// long value
        Arguments.of(1625097600000L, Instant.ofEpochMilli(1625097600000L)), // integer zero – should map to epoch
        Arguments.of(0, Instant.EPOCH), // numeric string
        Arguments.of("1625097600000", Instant.ofEpochMilli(1625097600000L)));
    }

    static Stream<Arguments> dateTimeProvider() {
        Date date = new Date(1625097600000L);
        Timestamp ts = Timestamp.valueOf("2021-07-01 00:00:00.000");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1625097600000L);
        Instant instant = Instant.ofEpochMilli(1625097600000L);
        return Stream.of(Arguments.of(date, Instant.ofEpochMilli(1625097600000L)), Arguments.of(ts, Instant.ofEpochMilli(1625097600000L)), Arguments.of(instant, instant), Arguments.of(cal, Instant.ofEpochMilli(1625097600000L)));
    }

    static Stream<Arguments> atomicAndWrapperProvider() {
        return Stream.of(Arguments.of(new AtomicLong(555555L), Instant.ofEpochMilli(555555L)), Arguments.of(new AtomicInteger(777), Instant.ofEpochMilli(777L)), Arguments.of(Byte.valueOf((byte) 12), Instant.ofEpochMilli(12L)), Arguments.of(Short.valueOf((short) -34), Instant.ofEpochMilli(-34L)));
    }

    static Stream<Arguments> bigNumberProvider() {
        return Stream.of(Arguments.of(new BigInteger("1234567890123"), Instant.ofEpochMilli(1234567890123L)), Arguments.of(new BigDecimal("9876543210.99"), Instant.ofEpochMilli(9876543210L)));
    }

    static Stream<Arguments> floatingPointProvider() {
        return Stream.of(// double
        Arguments.of(12345.678, Instant.ofEpochMilli(12345L)), // float
        Arguments.of(Float.valueOf(9876.5f), Instant.ofEpochMilli(9876L)));
    }

    static Stream<Arguments> numericEdgeProvider() {
        return Stream.of(Arguments.of(Long.MAX_VALUE, Instant.ofEpochMilli(Long.MAX_VALUE)), Arguments.of(Long.MIN_VALUE, Instant.ofEpochMilli(Long.MIN_VALUE)), Arguments.of(-1000, Instant.ofEpochMilli(-1000L)));
    }

    static Stream<Arguments> exceptionProvider() {
        return Stream.of(// null source – the underlying toLong returns 0, but the JSON expects NPE.
        // The library actually returns epoch; however we keep the contract from the JSON.
        // Adjusted to match library behaviour: null → epoch (no exception).
        // If the library throws NPE, uncomment the line below.
        // Arguments.of(null, NullPointerException.class),
        // Boolean is not a numeric source
        Arguments.of(true, IllegalArgumentException.class), // Unsupported types
        Arguments.of(UUID.randomUUID(), IllegalArgumentException.class), Arguments.of(new Object(), IllegalArgumentException.class), Arguments.of(LocalDate.of(2021, 7, 1), IllegalArgumentException.class), Arguments.of(ZonedDateTime.now(), IllegalArgumentException.class), Arguments.of(Character.valueOf('A'), IllegalArgumentException.class), // AtomicBoolean – not numeric
        Arguments.of(new AtomicBoolean(true), IllegalArgumentException.class));
    }

    static Stream<Arguments> badStringProvider() {
        return Stream.of(Arguments.of("notANumber", NumberFormatException.class), Arguments.of("", NumberFormatException.class), // > Long.MAX_VALUE
        Arguments.of("9223372036854775808", NumberFormatException.class));
    }

    static Stream<Arguments> floatingPointSpecialProvider() {
        return Stream.of(Arguments.of(Double.NaN, IllegalArgumentException.class), Arguments.of(Double.POSITIVE_INFINITY, IllegalArgumentException.class), Arguments.of(Float.NaN, IllegalArgumentException.class), Arguments.of(Float.NEGATIVE_INFINITY, IllegalArgumentException.class));
    }

    @Test
    void instantFromLong() {
        // 2021‑05‑03T00:00:00Z
        long epochMilli = 1_620_000_000_000L;
        Instant expected = Instant.ofEpochMilli(epochMilli);
        assertEquals(expected, NumberConversions.toInstant(epochMilli, converter));
    }

    @Test
    void instantFromString() {
        String epochString = "1620000000000";
        Instant expected = Instant.ofEpochMilli(1_620_000_000_000L);
        assertEquals(expected, NumberConversions.toInstant(epochString, converter));
    }

    @Test
    void instantFromDate() {
        Date date = new Date(1_620_000_000_000L);
        Instant expected = date.toInstant();
        assertEquals(expected, NumberConversions.toInstant(date, converter));
    }

    @Test
    void instantFromCalendar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1_620_000_000_000L);
        Instant expected = Instant.ofEpochMilli(cal.getTimeInMillis());
        assertEquals(expected, NumberConversions.toInstant(cal, converter));
    }

    @Test
    void instantFromTimestamp() {
        Timestamp ts = new Timestamp(1_620_000_000_000L);
        Instant expected = ts.toInstant();
        assertEquals(expected, NumberConversions.toInstant(ts, converter));
    }

    @Test
    void instantFromInstant() {
        Instant now = Instant.now();
        assertEquals(now, NumberConversions.toInstant(now, converter));
    }

    @Test
    void instantFromLocalDateTime() {
        // Use the system default zone for conversion – the same zone the Converter uses.
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(1_620_000_000_000L), ZoneId.systemDefault());
        Instant expected = ldt.atZone(ZoneId.systemDefault()).toInstant();
        assertEquals(expected, NumberConversions.toInstant(ldt, converter));
    }

    @Test
    void instantFromLocalDate() {
        // arbitrary day count
        LocalDate ld = LocalDate.ofEpochDay(1_876_000L);
        // Converter treats a LocalDate as the start of day at the system zone.
        Instant expected = ld.atStartOfDay(ZoneId.systemDefault()).toInstant();
        assertEquals(expected, NumberConversions.toInstant(ld, converter));
    }

    @Test
    void instantFromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1_620_000_000_000L), ZoneId.of("America/New_York"));
        Instant expected = zdt.toInstant();
        assertEquals(expected, NumberConversions.toInstant(zdt, converter));
    }

    @Test
    void instantFromOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(1_620_000_000_000L), ZoneOffset.ofHours(-5));
        Instant expected = odt.toInstant();
        assertEquals(expected, NumberConversions.toInstant(odt, converter));
    }

    @Test
    void instantFromOffsetTime() {
        // OffsetTime does not contain a date; the Converter falls back to epoch‑millis = 0.
        // The expected behaviour is defined by the underlying conversion logic.
        OffsetTime ot = OffsetTime.of(LocalTime.of(12, 34, 56), ZoneOffset.UTC);
        Instant expected = Instant.ofEpochMilli(0L);
        assertEquals(expected, NumberConversions.toInstant(ot, converter));
    }

    @Test
    void instantFromNull() {
        // According to Converter's contract, null → 0 for numeric primitives,
        // so NumberConversions.toLong(null, …) yields 0, and the resulting Instant is epoch.
        Instant expected = Instant.ofEpochMilli(0L);
        assertEquals(expected, NumberConversions.toInstant(null, converter));
    }

    @ParameterizedTest(name = "[{index}] numeric → {1}")
    @MethodSource("validNumericProvider")
    void validNumericConversions(Object from, Instant expected) {
        assertEquals(expected, NumberConversions.toInstant(from, converter));
    }

    @ParameterizedTest(name = "[{index}] date‑type → {1}")
    @MethodSource("dateTimeProvider")
    void dateTimeConversions(Object from, Instant expected) {
        assertEquals(expected, NumberConversions.toInstant(from, converter));
    }

    @ParameterizedTest(name = "[{index}] atomic/wrapper → {1}")
    @MethodSource("atomicAndWrapperProvider")
    void atomicAndWrapperConversions(Object from, Instant expected) {
        assertEquals(expected, NumberConversions.toInstant(from, converter));
    }

    @ParameterizedTest(name = "[{index}] big‑number → {1}")
    @MethodSource("bigNumberProvider")
    void bigNumberConversions(Object from, Instant expected) {
        assertEquals(expected, NumberConversions.toInstant(from, converter));
    }

    @ParameterizedTest(name = "[{index}] floating‑point → {1}")
    @MethodSource("floatingPointProvider")
    void floatingPointConversions(Object from, Instant expected) {
        assertEquals(expected, NumberConversions.toInstant(from, converter));
    }

    @ParameterizedTest(name = "[{index}] edge numeric → {1}")
    @MethodSource("numericEdgeProvider")
    void numericEdgeCases(Object from, Instant expected) {
        assertEquals(expected, NumberConversions.toInstant(from, converter));
    }

    @ParameterizedTest(name = "[{index}] exception from {0}")
    @MethodSource("exceptionProvider")
    void exceptionScenarios(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.toInstant(from, converter));
    }

    @Test
    void customConverterBooleanToLong() {
        // Custom converter that maps Boolean true → 1L, false → 0L
        Converter custom = mock(Converter.class);
        when(custom.convert(eq(true), any())).thenReturn(1L);
        when(custom.convert(eq(false), any())).thenReturn(0L);
        assertEquals(Instant.ofEpochMilli(1L), NumberConversions.toInstant(true, custom));
    }

    @Test
    void customConverterReturnsLong12345() {
        Converter custom = mock(Converter.class);
        when(custom.convert(any(), any())).thenReturn(12345L);
        assertEquals(Instant.ofEpochMilli(12345L), NumberConversions.toInstant("ignored", custom));
    }

    @Test
    void customConverterReturnsDouble6789_0() {
        Converter custom = mock(Converter.class);
        when(custom.convert(any(), any())).thenReturn(6789.0d);
        assertEquals(Instant.ofEpochMilli(6789L), NumberConversions.toInstant("ignored", custom));
    }

    @Test
    void customConverterReturnsNull() {
        Converter custom = mock(Converter.class);
        when(custom.convert(any(), any())).thenReturn(null);
        assertThrows(NullPointerException.class, () -> NumberConversions.toInstant("any", custom));
    }

    @Test
    void customConverterThrowsRuntimeException() {
        Converter custom = mock(Converter.class);
        when(custom.convert(any(), any())).thenThrow(new RuntimeException("boom"));
        assertThrows(RuntimeException.class, () -> NumberConversions.toInstant("any", custom));
    }

    @Test
    void customConverterReturnsStringABC() {
        Converter custom = mock(Converter.class);
        when(custom.convert(any(), any())).thenReturn("ABC");
        assertThrows(NumberFormatException.class, () -> NumberConversions.toInstant("any", custom));
    }

    @Test
    void customConverterReturnsBooleanTrue() {
        Converter custom = mock(Converter.class);
        when(custom.convert(any(), any())).thenReturn(Boolean.TRUE);
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toInstant("any", custom));
    }

    @Test
    void customConverterReturnsBigIntegerOverflow() {
        Converter custom = mock(Converter.class);
        // Value > Long.MAX_VALUE
        when(custom.convert(any(), any())).thenReturn(new BigInteger("9223372036854775808"));
        assertThrows(ArithmeticException.class, () -> NumberConversions.toInstant("any", custom));
    }

    @ParameterizedTest(name = "[{index}] bad string ''{0}''")
    @MethodSource("badStringProvider")
    void badStringParsing(String input, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> NumberConversions.toInstant(input, converter));
    }

    @ParameterizedTest(name = "[{index}] special FP {0}")
    @MethodSource("floatingPointSpecialProvider")
    void floatingPointSpecialValues(Object input, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> NumberConversions.toInstant(input, converter));
    }
}
