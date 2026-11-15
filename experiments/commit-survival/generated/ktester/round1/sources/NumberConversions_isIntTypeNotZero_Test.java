package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class NumberConversions_isIntTypeNotZero_Test {

    private Converter converter;

    // the default, fully‑functional converter
    private Converter realConverter;

    @BeforeEach
    void setupBeforeEach() {
        // ConverterOptions has a public no‑arg constructor in the library.
        // Using the default options gives us a fully functional Converter.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /**
     * Supplies a {@link Stream} of {@link Arguments} for the parameterised test.
     * Each argument contains:
     * <ul>
     *   <li>a description (for readable test names)</li>
     *   <li>the {@code from} value</li>
     *   <li>a {@link Converter} (either {@code null} to use the real one,
     *       or a Mockito mock with bespoke behaviour)</li>
     *   <li>the expected boolean result (null when an exception is expected)</li>
     *   <li>the expected exception type (null when no exception is expected)</li>
     * </ul>
     */
    private static Stream<Arguments> provideTestCases() {
        // Helper to create a mock Converter that returns a specific value.
        java.util.function.Function<Object, Converter> mockReturning = returnValue -> {
            Converter mock = mock(Converter.class);
            when(mock.convert(any(), any())).thenReturn(returnValue);
            return mock;
        };
        // Helper to create a mock Converter that throws a specific exception.
        java.util.function.Function<Throwable, Converter> mockThrowing = throwable -> {
            Converter mock = mock(Converter.class);
            when(mock.convert(any(), any())).thenThrow(throwable);
            return mock;
        };
        return Stream.of(// 1. Non‑zero Integer (real converter)
        Arguments.of("Non‑zero Integer value", Integer.valueOf(5), null, true, null), // 2. Zero Integer (real converter)
        Arguments.of("Zero Integer value", Integer.valueOf(0), null, false, null), // 3. Zero Long (real converter)
        Arguments.of("Zero Long value", Long.valueOf(0L), null, false, null), // 4. String "123" with custom converter that parses to Long
        Arguments.of("String \"123\" converted via custom converter to long 123", "123", mockReturning.apply(Long.parseLong("123")), true, null), // 5. Null source with custom converter returning 0L
        Arguments.of("Null source with converter returning 0L", null, mockReturning.apply(0L), false, null), // 6. Object with converter that throws IllegalArgumentException
        Arguments.of("Object with converter throwing IllegalArgumentException", new Object(), mockThrowing.apply(new IllegalArgumentException("cannot convert")), null, IllegalArgumentException.class), // 7. Non‑zero Long (real converter)
        Arguments.of("Non‑zero Long value", Long.valueOf(123L), null, true, null), // 8. Zero Short (real converter)
        Arguments.of("Zero Short value", Short.valueOf((short) 0), null, false, null), // 9. Non‑zero Byte (real converter)
        Arguments.of("Non‑zero Byte value", Byte.valueOf((byte) 5), null, true, null), // 10. AtomicInteger zero (real converter)
        Arguments.of("AtomicInteger zero", new AtomicInteger(0), null, false, null), // 11. AtomicLong negative non‑zero (real converter)
        Arguments.of("AtomicLong -10 (non‑zero)", new AtomicLong(-10L), null, true, null), // 12. BigInteger zero (real converter)
        Arguments.of("BigInteger zero", BigInteger.valueOf(0), null, false, null), // 13. BigInteger positive (real converter)
        Arguments.of("BigInteger 999 (non‑zero)", BigInteger.valueOf(999), null, true, null), // 14. BigDecimal zero (real converter)
        Arguments.of("BigDecimal ZERO", BigDecimal.ZERO, null, false, null), // 15. BigDecimal negative non‑zero (real converter)
        Arguments.of("BigDecimal -0.1 (non‑zero)", BigDecimal.valueOf(-0.1), null, true, null), // 16. Integer.MAX_VALUE (real converter)
        Arguments.of("Integer.MAX_VALUE (non‑zero)", Integer.valueOf(Integer.MAX_VALUE), null, true, null), // 17. Integer.MIN_VALUE (real converter)
        Arguments.of("Integer.MIN_VALUE (non‑zero)", Integer.valueOf(Integer.MIN_VALUE), null, true, null), // 18. String "0" with custom converter parsing to long 0
        Arguments.of("String \"0\" converted via custom converter to long 0", "0", mockReturning.apply(Long.parseLong("0")), false, null), // 19. String "-42" with custom converter parsing to long -42
        Arguments.of("String \"-42\" converted via custom converter to long -42", "-42", mockReturning.apply(Long.parseLong("-42")), true, null), // 20. Object with converter returning Boolean (causes ClassCastException)
        Arguments.of("Object with converter returning Boolean (type mismatch)", new Object(), mockReturning.apply(Boolean.TRUE), null, ClassCastException.class));
    }

    @Test
    void returnsTrueForNonZeroInteger() {
        assertTrue(NumberConversions.isIntTypeNotZero(5, realConverter));
    }

    @Test
    void returnsFalseForZeroInteger() {
        assertFalse(NumberConversions.isIntTypeNotZero(0, realConverter));
    }

    @Test
    void returnsTrueForNonZeroLong() {
        assertTrue(NumberConversions.isIntTypeNotZero(123L, converter));
    }

    @Test
    void returnsFalseForZeroLong() {
        assertFalse(NumberConversions.isIntTypeNotZero(0L, converter));
    }

    @Test
    void returnsTrueForNonZeroStringNumber() {
        assertTrue(NumberConversions.isIntTypeNotZero("42", converter));
    }

    @Test
    void returnsFalseForZeroStringNumber() {
        assertFalse(NumberConversions.isIntTypeNotZero("0", converter));
    }

    @Test
    void returnsFalseForNullInput() {
        assertFalse(NumberConversions.isIntTypeNotZero(null, converter));
    }

    @Test
    void returnsTrueForNonZeroBigInteger() {
        assertTrue(NumberConversions.isIntTypeNotZero(new java.math.BigInteger("999"), converter));
    }

    @Test
    void returnsFalseForZeroBigInteger() {
        assertFalse(NumberConversions.isIntTypeNotZero(java.math.BigInteger.ZERO, converter));
    }

    /**
     * Parameterised test that iterates over all scenarios described in the
     * supplied JSON test cases.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideTestCases")
    void testIsIntTypeNotZero(String description, Object from, Converter converter, Boolean expectedResult, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            // Verify that the expected exception is thrown.
            Throwable thrown = assertThrows(expectedException, () -> NumberConversions.isIntTypeNotZero(from, converter), description);
            // Optional: assert the exception message contains something useful.
            assertNotNull(thrown.getMessage());
        } else {
            // Normal execution – verify the boolean outcome.
            boolean actual = NumberConversions.isIntTypeNotZero(from, converter);
            assertEquals(expectedResult, actual, description);
        }
    }
}
