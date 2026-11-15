package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class DateConversions_toAtomicLong_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor gives a sensible default.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC when many test classes run together.
        converter = null;
    }

    private static Stream<Arguments> primitiveNumberProvider() {
        return Stream.of(// Long
        Arguments.of(123L, 123L), // Integer
        Arguments.of(42, 42L), // Float – truncates
        Arguments.of(3.14f, 3L), // Double – truncates
        Arguments.of(2.71828d, 2L));
    }

    private static Stream<Arguments> stringNumberProvider() {
        return Stream.of(Arguments.of("12345", 12345L), Arguments.of("   -987  ", -987L), Arguments.of("0", 0L));
    }

    @Test
    void toAtomicLong_fromLong() {
        Long src = 42L;
        AtomicLong result = DateConversions.toAtomicLong(src, converter);
        assertNotNull(result);
        assertEquals(42L, result.get());
    }

    @Test
    void toAtomicLong_fromInteger() {
        Integer src = 7;
        AtomicLong result = DateConversions.toAtomicLong(src, converter);
        assertNotNull(result);
        assertEquals(7L, result.get());
    }

    @Test
    void toAtomicLong_fromStringNumber() {
        String src = "12345";
        AtomicLong result = DateConversions.toAtomicLong(src, converter);
        assertNotNull(result);
        assertEquals(12345L, result.get());
    }

    @Test
    void toAtomicLong_fromDate() {
        // 2021‑01‑01T00:00:00Z in epoch millis
        Date src = new Date(1609459200000L);
        AtomicLong result = DateConversions.toAtomicLong(src, converter);
        assertNotNull(result);
        assertEquals(1609459200000L, result.get());
    }

    @Test
    void toAtomicLong_fromCalendar() {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(1_600_000_123_456L);
        AtomicLong result = DateConversions.toAtomicLong(cal, converter);
        assertNotNull(result);
        assertEquals(cal.getTimeInMillis(), result.get());
    }

    @Test
    void toAtomicLong_fromNull_returnsZero() {
        AtomicLong result = DateConversions.toAtomicLong(null, converter);
        assertNotNull(result);
        assertEquals(0L, result.get(), "Null source should be converted to the numeric default 0");
    }

    @Test
    void toAtomicLong_fromBoolean_true_isOne() {
        // Boolean true converts to 1 (numeric primitive default for true)
        Boolean src = Boolean.TRUE;
        AtomicLong result = DateConversions.toAtomicLong(src, converter);
        assertNotNull(result);
        assertEquals(1L, result.get());
    }

    @Test
    void toAtomicLong_fromBoolean_false_isZero() {
        Boolean src = Boolean.FALSE;
        AtomicLong result = DateConversions.toAtomicLong(src, converter);
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    @ParameterizedTest(name = "{index}: from={0} → expected={1}")
    @MethodSource("primitiveNumberProvider")
    void toAtomicLong_primitiveNumbers(Object from, long expected) {
        AtomicLong result = DateConversions.toAtomicLong(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), () -> "Conversion of " + from + " should yield " + expected);
    }

    @ParameterizedTest(name = "{index}: \"{0}\" → {1}")
    @MethodSource("stringNumberProvider")
    void toAtomicLong_stringNumbers(String src, long expected) {
        AtomicLong result = DateConversions.toAtomicLong(src, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), () -> "String '" + src + "' should be parsed to " + expected);
    }

    @Test
    void toAtomicLong_fromCalendar_Utc() {
        Calendar cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1609459200000L);
        AtomicLong result = DateConversions.toAtomicLong(cal, converter);
        assertNotNull(result);
        assertEquals(1609459200000L, result.get());
    }

    @Test
    void toAtomicLong_fromInstant() {
        Instant instant = Instant.ofEpochMilli(1609459200000L);
        AtomicLong result = DateConversions.toAtomicLong(instant, converter);
        assertNotNull(result);
        assertEquals(1609459200000L, result.get());
    }

    @Test
    void toAtomicLong_fromBigDecimal() {
        BigDecimal src = new BigDecimal("1234567890123456789");
        AtomicLong result = DateConversions.toAtomicLong(src, converter);
        assertNotNull(result);
        assertEquals(1234567890123456789L, result.get());
    }

    @Test
    void toAtomicLong_fromBigInteger() {
        // Long.MIN_VALUE
        BigInteger src = new BigInteger("-9223372036854775808");
        AtomicLong result = DateConversions.toAtomicLong(src, converter);
        assertNotNull(result);
        assertEquals(Long.MIN_VALUE, result.get());
    }

    @Test
    void toAtomicLong_fromEmptyString_returnsZero() {
        AtomicLong result = DateConversions.toAtomicLong("", converter);
        assertNotNull(result);
        assertEquals(0L, result.get(), "Empty string should be interpreted as 0");
    }

    @Test
    void toAtomicLong_customConverterOverridesLogic() {
        // Create a mock Converter that maps the literal "magic" → 777L
        Converter mockConverter = Mockito.mock(Converter.class);
        Mockito.when(mockConverter.convert(eq("magic"), eq(Long.class))).thenReturn(777L);
        // For any other value we delegate to the real default converter to avoid NPEs
        Mockito.when(mockConverter.convert(any(), eq(Long.class))).thenAnswer(invocation -> {
            Object src = invocation.getArgument(0);
            return new Converter(new ConverterOptions()).convert(src, Long.class);
        });
        AtomicLong result = DateConversions.toAtomicLong("magic", mockConverter);
        assertNotNull(result);
        assertEquals(777L, result.get(), "Custom converter should override default conversion for \"magic\"");
    }

    @Test
    void toAtomicLong_unsupportedType_throwsIllegalArgumentException() {
        Object unsupported = new Object();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DateConversions.toAtomicLong(unsupported, converter), "Unsupported source type should raise IllegalArgumentException");
        assertTrue(ex.getMessage().toLowerCase().contains("cannot"), "Exception message should indicate conversion failure");
    }
}
