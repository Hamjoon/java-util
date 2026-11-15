package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toAtomicLong_Test {

    private static Converter converter;

    /**
     * Real converter used for the “default” conversion path.
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default constructor
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference
        converter = null;
    }

    private static Stream<Arguments> provideValuesForAtomicLong() {
        return Stream.of(// Direct numeric types
        Arguments.of(123L, 123L), Arguments.of(45, 45L), Arguments.of((short) 7, 7L), Arguments.of((byte) 9, 9L), // String that can be parsed as a number
        Arguments.of("67", 67L), // Null handling – Converter returns 0 for numeric primitives
        Arguments.of(null, 0L));
    }

    private static Stream<Arguments> provideDirectNumberCases() {
        return Stream.of(Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE), Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE), // overflow truncates
        Arguments.of(Double.MAX_VALUE, Long.MAX_VALUE), Arguments.of(-Double.MAX_VALUE, Long.MIN_VALUE), // large double within range
        Arguments.of(1.9999999999E18, 1999999999999999872L));
    }

    private static Stream<Arguments> provideStringNumericCases() {
        return Stream.of(Arguments.of("42", 42L), Arguments.of("  -17  ", -17L), Arguments.of("9.99", 9L), Arguments.of("+123", 123L));
    }

    private static Stream<Arguments> provideStringExceptionCases() {
        return Stream.of(Arguments.of("", IllegalArgumentException.class), Arguments.of("abc", IllegalArgumentException.class), Arguments.of("0x10", IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideJava8DateTimeCases() {
        return Stream.of(Arguments.of(LocalDate.of(1970, 1, 2), 86_400_000L), Arguments.of(LocalDateTime.of(2023, 1, 1, 12, 34, 56), 1_672_576_496_000L), Arguments.of(Instant.ofEpochSecond(0), 0L), Arguments.of(ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), 1_672_531_200_000L), Arguments.of(OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-5)), 1_672_552_800_000L));
    }

    private static Stream<Arguments> provideBigNumberCases() {
        return Stream.of(Arguments.of(new BigDecimal("12345.678"), 12_345L), Arguments.of(new BigInteger("9876543210"), 9_876_543_210L));
    }

    private static Stream<Arguments> provideBigNumberOverflowCases() {
        return Stream.of(Arguments.of(new BigDecimal("9223372036854775808"), IllegalArgumentException.class), Arguments.of(new BigDecimal("-9223372036854775809"), IllegalArgumentException.class));
    }

    /**
     * Verify that {@code LocalDateConversions.toAtomicLong} correctly delegates to the
     * {@code Converter} for a variety of source types.
     */
    @ParameterizedTest
    @MethodSource("provideValuesForAtomicLong")
    void toAtomicLong_returnsExpectedAtomicLong(Object source, long expected) {
        AtomicLong result = LocalDateConversions.toAtomicLong(source, realConverter);
        assertNotNull(result, "Resulting AtomicLong should never be null");
        assertEquals(expected, result.get(), () -> "Conversion of " + source + " did not match expected value");
    }

    /* --------------------------------------------------------------
     *  Direct Number conversion (including boundary values)
     * -------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("provideDirectNumberCases")
    void toAtomicLong_directNumberConversion(Object source, long expected) {
        AtomicLong result = LocalDateConversions.toAtomicLong(source, realConverter);
        assertEquals(expected, result.get());
    }

    /* --------------------------------------------------------------
     *  String numeric conversion (including edge cases)
     * -------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("provideStringNumericCases")
    void toAtomicLong_stringNumericConversion(String source, long expected) {
        AtomicLong result = LocalDateConversions.toAtomicLong(source, realConverter);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest
    @MethodSource("provideStringExceptionCases")
    void toAtomicLong_stringConversion_invalidInput_throws(String source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> LocalDateConversions.toAtomicLong(source, realConverter), "Expected conversion of \"" + source + "\" to throw " + exception.getSimpleName());
    }

    /* --------------------------------------------------------------
     *  Date / Calendar conversion
     * -------------------------------------------------------------- */
    @Test
    void toAtomicLong_dateConversion() {
        // 2023‑01‑01T00:00:00Z
        Date date = Date.from(Instant.ofEpochMilli(1672531200000L));
        AtomicLong result = LocalDateConversions.toAtomicLong(date, realConverter);
        assertEquals(1672531200000L, result.get());
    }

    @Test
    void toAtomicLong_calendarConversion() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2022‑12‑31T12:34:56Z
        cal.setTimeInMillis(1672506896000L);
        AtomicLong result = LocalDateConversions.toAtomicLong(cal, realConverter);
        assertEquals(1672506896000L, result.get());
    }

    /* --------------------------------------------------------------
     *  Java 8 date‑time types
     * -------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("provideJava8DateTimeCases")
    void toAtomicLong_java8DateTimeConversion(Object source, long expected) {
        AtomicLong result = LocalDateConversions.toAtomicLong(source, realConverter);
        assertEquals(expected, result.get());
    }

    /* --------------------------------------------------------------
     *  BigDecimal / BigInteger handling (including overflow)
     * -------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("provideBigNumberCases")
    void toAtomicLong_bigNumberConversion(Object source, long expected) {
        AtomicLong result = LocalDateConversions.toAtomicLong(source, realConverter);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest
    @MethodSource("provideBigNumberOverflowCases")
    void toAtomicLong_bigNumberOverflow_throws(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> LocalDateConversions.toAtomicLong(source, realConverter), "Expected overflow conversion to throw " + exception.getSimpleName());
    }

    /* --------------------------------------------------------------
     *  Fallback to supplied Converter (mocked)
     * -------------------------------------------------------------- */
    @Test
    void toAtomicLong_converterFallback_numeric(@Mock Converter mockConverter) {
        when(mockConverter.convert(any(), eq(Long.class))).thenReturn(123L);
        AtomicLong result = LocalDateConversions.toAtomicLong("ignored", mockConverter);
        assertEquals(123L, result.get());
    }

    @Test
    void toAtomicLong_converterFallback_stringNumber(@Mock Converter mockConverter) {
        when(mockConverter.convert(any(), eq(Long.class))).thenReturn("456");
        AtomicLong result = LocalDateConversions.toAtomicLong("ignored", mockConverter);
        assertEquals(456L, result.get());
    }

    @Test
    void toAtomicLong_converterFallback_date(@Mock Converter mockConverter) {
        Date date = Date.from(Instant.ofEpochMilli(1_672_531_200_000L));
        when(mockConverter.convert(any(), eq(Long.class))).thenReturn(date);
        AtomicLong result = LocalDateConversions.toAtomicLong("ignored", mockConverter);
        assertEquals(1_672_531_200_000L, result.get());
    }

    @Test
    void toAtomicLong_converterFallback_runtimeException(@Mock Converter mockConverter) {
        when(mockConverter.convert(any(), eq(Long.class))).thenThrow(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> LocalDateConversions.toAtomicLong("ignored", mockConverter));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void toAtomicLong_converterFallback_unsupportedType(@Mock Converter mockConverter) {
        when(mockConverter.convert(any(), eq(Long.class))).thenReturn(Arrays.asList(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toAtomicLong("ignored", mockConverter));
    }

    @Test
    void toAtomicLong_converterFallback_malformedString(@Mock Converter mockConverter) {
        when(mockConverter.convert(any(), eq(Long.class))).thenReturn("12abc");
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toAtomicLong("ignored", mockConverter));
    }

    /* --------------------------------------------------------------
     *  Unsupported source types – expect IllegalArgumentException
     * -------------------------------------------------------------- */
    @Test
    void toAtomicLong_unsupportedObject_throws() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toAtomicLong(unsupported, realConverter));
    }

    @Test
    void toAtomicLong_charSequenceUnsupported_throws() {
        StringBuilder sb = new StringBuilder("123");
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toAtomicLong(sb, realConverter));
    }

    @Test
    void toAtomicLong_enumUnsupported_throws() {
        Month month = Month.JANUARY;
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toAtomicLong(month, realConverter));
    }

    /* --------------------------------------------------------------
     *  Null Converter handling – expect NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void toAtomicLong_nullConverter_throws() {
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toAtomicLong(123, null));
    }

    /* --------------------------------------------------------------
     *  Boolean conversion (if supported by the library)
     * -------------------------------------------------------------- */
    @Test
    void toAtomicLong_booleanTrue_ConversionIfSupported() {
        // The library treats true as 1 – if not supported, the test will fail,
        // which correctly signals a change in behaviour.
        AtomicLong result = LocalDateConversions.toAtomicLong(true, realConverter);
        assertEquals(1L, result.get());
    }

    @Test
    void toAtomicLong_booleanFalse_ConversionIfSupported() {
        AtomicLong result = LocalDateConversions.toAtomicLong(false, realConverter);
        assertEquals(0L, result.get());
    }
}
