package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToDouble_Test {

    /**
     * Provides a stream of arguments for {@link #shouldConvertSuccessfully}.
     * Each argument consists of the source object and the expected {@link Double}
     * value after conversion.
     */
    static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// ---- numeric primitives / wrappers ----
        Arguments.of(123, 123.0), Arguments.of(123L, 123.0), Arguments.of(123.45f, 123.45), Arguments.of(123.456d, 123.456), // ---- BigDecimal / BigInteger ----
        Arguments.of(new BigDecimal("123.456"), 123.456), Arguments.of(new BigInteger("123"), 123.0), // ---- numeric strings (trimmed) ----
        Arguments.of("456.78", 456.78), Arguments.of("  42.5  ", 42.5), Arguments.of("-3.14e2", -314.0), // ---- special string literals ----
        Arguments.of("NaN", Double.NaN), Arguments.of("Infinity", Double.POSITIVE_INFINITY), Arguments.of("-Infinity", Double.NEGATIVE_INFINITY), // ---- boolean values ----
        Arguments.of(Boolean.TRUE, 1.0), Arguments.of(Boolean.FALSE, 0.0), // ---- character literals ----
        Arguments.of('5', 5.0), // ---- date / timestamp ----
        Arguments.of(new Date(0L), 0.0), Arguments.of(new Timestamp(1622548800000L), 1622548800000.0), // ---- numeric wrapper edge values ----
        Arguments.of(Byte.MAX_VALUE, (double) Byte.MAX_VALUE), Arguments.of(Short.MIN_VALUE, (double) Short.MIN_VALUE), Arguments.of(Float.NaN, Double.NaN), Arguments.of(Float.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
    }

    /**
     * Supplies the sources that should trigger an {@link IllegalArgumentException}.
     */
    static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// ---- empty or whitespace‑only strings ----
        Arguments.of(""), Arguments.of("   "), // ---- malformed numeric strings ----
        Arguments.of("not-a-number"), Arguments.of("12abc"), Arguments.of("1e"), Arguments.of("0xFF"), Arguments.of("123,456"), // ---- non‑numeric character ----
        Arguments.of('A'), // ---- unsupported temporal types ----
        Arguments.of(LocalDate.of(2020, 1, 1)), Arguments.of(LocalDateTime.of(2020, 1, 1, 0, 0)), Arguments.of(ZonedDateTime.now()), // ---- Calendar (no direct numeric representation) ----
        Arguments.of(Calendar.getInstance()), // ---- generic Object and collections ----
        Arguments.of(new Object()), Arguments.of(Arrays.asList(1, 2, 3)), // ---- primitive array ----
        Arguments.of(new int[] { 1, 2, 3 }), // ---- StringBuilder (CharSequence not explicitly supported) ----
        Arguments.of(new StringBuilder("123")), // ---- java.sql.Date (different semantics) ----
        Arguments.of(new java.sql.Date(0L)), // ---- atomic types (not supported for Double conversion) ----
        Arguments.of(new java.util.concurrent.atomic.AtomicInteger(42)), Arguments.of(new java.util.concurrent.atomic.AtomicLong(42L)), // ---- enum value ----
        Arguments.of(DayOfWeek.MONDAY));
    }

    @Test
    void shouldConvertFromInteger() {
        Integer source = 42;
        Double result = Converter.convertToDouble(source);
        assertNotNull(result);
        assertEquals(42.0, result);
    }

    @Test
    void shouldConvertFromLong() {
        Long source = 123456789L;
        Double result = Converter.convertToDouble(source);
        assertNotNull(result);
        assertEquals(123456789.0, result);
    }

    @Test
    void shouldConvertFromFloat() {
        Float source = 3.14f;
        Double result = Converter.convertToDouble(source);
        assertNotNull(result);
        assertEquals(3.14, result, 1e-6);
    }

    @Test
    void shouldConvertFromDouble() {
        Double source = 2.71828;
        Double result = Converter.convertToDouble(source);
        // identity for same wrapper type
        assertSame(source, result);
    }

    @Test
    void shouldConvertFromStringContainingNumber() {
        String source = "99.99";
        Double result = Converter.convertToDouble(source);
        assertNotNull(result);
        assertEquals(99.99, result);
    }

    @Test
    void shouldReturnNullWhenStringIsNotNumeric() {
        String source = "not-a-number";
        Double result = Converter.convertToDouble(source);
        assertNull(result);
    }

    @Test
    void shouldConvertFromBigDecimal() {
        BigDecimal source = new BigDecimal("12345.6789");
        Double result = Converter.convertToDouble(source);
        assertNotNull(result);
        assertEquals(12345.6789, result, 1e-12);
    }

    @Test
    void shouldConvertFromBigInteger() {
        BigInteger source = new BigInteger("987654321");
        Double result = Converter.convertToDouble(source);
        assertNotNull(result);
        assertEquals(987654321.0, result);
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        Double result = Converter.convertToDouble(null);
        assertNull(result, "Conversion of null should yield null");
    }

    /**
     * Covers all cases where a conversion to {@link Double} is expected
     * to succeed (including special values such as {@code NaN} and
     * {@code Infinity}).
     */
    @ParameterizedTest(name = "{index} ⇒ convertToDouble({0}) = {1}")
    @MethodSource("successfulConversionProvider")
    void shouldConvertSuccessfully(Object source, Double expected) {
        Double result = Converter.convertToDouble(source);
        assertNotNull(result, "Result should not be null for source: " + source);
        if (expected.isNaN()) {
            assertTrue(result.isNaN(), "Result should be NaN for source: " + source);
        } else if (expected.isInfinite()) {
            assertEquals(expected, result, "Result should be " + expected + " for source: " + source);
        } else {
            assertEquals(expected, result, "Unexpected conversion result");
        }
    }

    /**
     * All inputs that are expected to cause an {@link IllegalArgumentException}
     * when passed to {@link Converter#convertToDouble(Object)}.
     */
    @ParameterizedTest(name = "{index} ⇒ convertToDouble({0}) throws IllegalArgumentException")
    @MethodSource("exceptionConversionProvider")
    void shouldThrowIllegalArgumentException(Object source) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToDouble(source), "Expected IllegalArgumentException for source: " + source);
    }
}
