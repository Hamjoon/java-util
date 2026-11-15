package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toFloat_Test {

    @Mock
    private Converter // mock – not used by the method under test
    converter;

    private static Stream<Arguments> validNumberProvider() {
        return Stream.of(// Integer
        Arguments.of(42, 42.0f, "Integer implements Number; should be converted to float 42.0"), // Double
        Arguments.of(3.14159d, 3.14159f, "Double value should be correctly cast to float (possible rounding)"), // BigDecimal – note loss of precision is expected
        Arguments.of(new BigDecimal("12345.6789"), 12345.678f, "BigDecimal implements Number; its floatValue() should be returned (loss of precision)"), // BigInteger – may be represented in scientific notation
        Arguments.of(new BigInteger("9876543210"), 9.8765432E9f, "BigInteger implements Number; conversion to float may use scientific notation"), // AtomicInteger
        Arguments.of(new AtomicInteger(7), 7.0f, "AtomicInteger implements Number; should return its int value as float"), // AtomicLong
        Arguments.of(new AtomicLong(123456789L), 1.2345679E8f, "AtomicLong implements Number; conversion to float may be in scientific notation"));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "Passing null as the source object should cause a NullPointerException when casting to Number"), Arguments.of("not a number", ClassCastException.class, "A non‑Number object (String) should trigger a ClassCastException during the cast"), Arguments.of(new Object(), ClassCastException.class, "Any object that does not implement Number should cause a ClassCastException"));
    }

    @Test
    void toFloat_withInteger_returnsCorrectFloat() {
        Integer source = 42;
        float result = NumberConversions.toFloat(source, converter);
        assertEquals(42.0f, result, "Integer should be converted to its float representation");
    }

    @Test
    void toFloat_withDouble_returnsCorrectFloat() {
        Double source = 3.14159;
        float result = NumberConversions.toFloat(source, converter);
        assertEquals(3.14159f, result, 0.00001f, "Double should be converted to float with acceptable delta");
    }

    @Test
    void toFloat_withBigDecimal_returnsCorrectFloat() {
        BigDecimal source = new BigDecimal("123.456");
        float result = NumberConversions.toFloat(source, converter);
        assertEquals(123.456f, result, 0.0001f, "BigDecimal should be converted to float");
    }

    @Test
    void toFloat_withNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toFloat(null, converter), "Passing null should result in NPE because the casted Number is null");
    }

    @Test
    void toFloat_withNonNumber_throwsClassCastException() {
        String source = "not-a-number";
        assertThrows(ClassCastException.class, () -> NumberConversions.toFloat(source, converter), "Passing a non‑Number should cause a ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Valid numeric inputs – parameterized test
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validNumberProvider")
    void toFloat_withValidNumbers_returnsExpectedFloat(Object from, float expected, String description) {
        // The JSON test data uses a null converter, so we pass null here.
        float result = NumberConversions.toFloat(from, null);
        // Use a small delta for floating‑point comparison to avoid flaky failures.
        assertEquals(expected, result, 0.000001f, () -> "Failed for input: " + from + " – " + description);
    }

    /* -------------------------------------------------------------
     *  Invalid inputs – each should throw the expected exception
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidInputProvider")
    void toFloat_withInvalidInputs_throwsException(Object from, Class<? extends Throwable> expectedException, String description) {
        // The converter argument is irrelevant; we pass null.
        assertThrows(expectedException, () -> NumberConversions.toFloat(from, null), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from + " – " + description);
    }

    /* -------------------------------------------------------------
     *  Converter argument is non‑null – the method should ignore it
     * ------------------------------------------------------------- */
    @Test
    void toFloat_withNonNullConverter_ignoresConverter() {
        // Using the @Mock converter to prove that the value is ignored.
        Integer source = 100;
        float result = NumberConversions.toFloat(source, converter);
        assertEquals(100.0f, result, "Converter argument is ignored; conversion should still succeed");
    }
}
