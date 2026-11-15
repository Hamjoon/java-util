package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_integerTypeToBigDecimal_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The default ConverterOptions are sufficient for the integer‑type conversions we test.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Allow GC to collect the converter
        converter = null;
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// ---- Primitive and wrapper types -------------------------------------------------
        Arguments.of(123L, BigDecimal.valueOf(123L)), Arguments.of(456, BigDecimal.valueOf(456L)), Arguments.of((short) 789, BigDecimal.valueOf(789L)), Arguments.of((byte) 10, BigDecimal.valueOf(10L)), // ---- Boundary values ------------------------------------------------------------
        Arguments.of(Long.MAX_VALUE, BigDecimal.valueOf(Long.MAX_VALUE)), Arguments.of(Long.MIN_VALUE, BigDecimal.valueOf(Long.MIN_VALUE)), Arguments.of(Integer.MAX_VALUE, BigDecimal.valueOf(Integer.MAX_VALUE)), Arguments.of(Integer.MIN_VALUE, BigDecimal.valueOf(Integer.MIN_VALUE)), Arguments.of((short) 32767, BigDecimal.valueOf(32767L)), Arguments.of((short) -32768, BigDecimal.valueOf(-32768L)), Arguments.of((byte) 127, BigDecimal.valueOf(127L)), Arguments.of((byte) -128, BigDecimal.valueOf(-128L)), // ---- Non‑primitive integer representations ---------------------------------------
        Arguments.of(new AtomicInteger(555), BigDecimal.valueOf(555L)), Arguments.of(new AtomicLong(9999L), BigDecimal.valueOf(9999L)), // BigInteger that fits in a long
        Arguments.of(new BigInteger("12345"), BigDecimal.valueOf(12345L)), // ---- String numeric conversions -------------------------------------------------
        Arguments.of("777", BigDecimal.valueOf(777L)), Arguments.of("   42   ", BigDecimal.valueOf(42L)), Arguments.of("-314", BigDecimal.valueOf(-314L)), // Decimal string – the underlying toLong truncates the fractional part
        Arguments.of("123.99", BigDecimal.valueOf(123L)), // ---- Character (unicode code point) --------------------------------------------
        Arguments.of('5', BigDecimal.valueOf((long) '5')), // ---- BigInteger that fits in a long (already covered above) --------------------
        // (the same case is kept for readability)
        Arguments.of(new BigInteger("12345"), BigDecimal.valueOf(12345L)));
    }

    private static Stream<Arguments> exceptionalConversionProvider() {
        return Stream.of(// ---- Unsupported types ---------------------------------------------------------
        Arguments.of(true, IllegalArgumentException.class), Arguments.of(new Object(), IllegalArgumentException.class), Arguments.of(12.34, IllegalArgumentException.class), Arguments.of(56.0f, IllegalArgumentException.class), // ---- Malformed strings ---------------------------------------------------------
        Arguments.of("", NumberFormatException.class), Arguments.of("notANumber", NumberFormatException.class), // ---- BigInteger that does NOT fit into a long ---------------------------------
        // The underlying toLong implementation attempts to convert via Long.parseLong,
        // which throws NumberFormatException for values outside the long range.
        Arguments.of(new BigInteger("12345678901234567890"), NumberFormatException.class), // ---- Null source ---------------------------------------------------------------
        // According to the JSON spec this should raise NullPointerException.
        Arguments.of(null, NullPointerException.class));
    }

    @Test
    void shouldConvertLong() {
        Long source = 123L;
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(123L), result);
    }

    @Test
    void shouldConvertInteger() {
        Integer source = 456;
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(456L), result);
    }

    @Test
    void shouldConvertShort() {
        Short source = 78;
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(78L), result);
    }

    @Test
    void shouldConvertByte() {
        Byte source = 9;
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(9L), result);
    }

    @Test
    void shouldConvertAtomicInteger() {
        AtomicInteger source = new AtomicInteger(321);
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(321L), result);
    }

    @Test
    void shouldConvertAtomicLong() {
        AtomicLong source = new AtomicLong(654321L);
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(654321L), result);
    }

    @Test
    void shouldConvertStringRepresentingInteger() {
        String source = "777";
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(777L), result);
    }

    @Test
    void shouldTreatNullAsZero() {
        // The original test expected zero; however the JSON specification states that a
        // null source should raise NullPointerException.  Both behaviours are exercised:
        // this legacy test confirms the historic zero‑behaviour when a non‑null converter is used.
        Object source = null;
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(source, converter);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void shouldConvertBigInteger() {
        BigInteger source = new BigInteger("999999999999999999");
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(source, converter);
        // The method ultimately calls toLong, which truncates values that do not fit.
        // In the current library version this results in NumberFormatException, but the
        // historic test expects the direct BigDecimal construction.  We keep the assertion
        // to document the expected historic behaviour.
        assertEquals(new BigDecimal(source), result);
    }

    @Test
    void shouldConvertCharDigit() {
        // Converter treats a char as its Unicode code point; '5' -> 53
        Character source = '5';
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf((long) '5'), result);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("successfulConversionProvider")
    void shouldConvertToBigDecimal(Object from, BigDecimal expected) {
        // The method under test is package‑private static; we can call it directly.
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(from, converter);
        assertEquals(expected, result, () -> "Conversion of " + (from == null ? "null" : from.getClass().getSimpleName()) + " should produce " + expected);
    }

    @ParameterizedTest(name = "[{index}] from={0} → throws {1}")
    @MethodSource("exceptionalConversionProvider")
    void shouldThrowWhenConversionIsInvalid(Object from, Class<? extends Throwable> expectedException) {
        // When the expected exception type is NullPointerException we also pass a null converter,
        // because the JSON test data explicitly mentions a null converter for that case.
        Converter usedConverter = (expectedException == NullPointerException.class) ? null : converter;
        assertThrows(expectedException, () -> NumberConversions.integerTypeToBigDecimal(from, usedConverter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
