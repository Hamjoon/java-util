package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_isBigIntegerNotZero_Test {

    /**
     * Mocked Converter – not used by the method under test.
     */
    @Mock
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock before each test to avoid cross‑test interaction verification.
        reset(mockConverter);
    }

    /**
     * Supplies the arguments required by {@link #testIsBigIntegerNotZeroVariousCases}.
     */
    private static Stream<Arguments> provideIsBigIntegerNotZeroCases() {
        return Stream.of(// Positive non‑zero BigInteger (large)
        Arguments.of(new BigInteger("12345678901234567890"), false, true, "A positive non‑zero BigInteger should be reported as not zero."), // Negative non‑zero BigInteger (large)
        Arguments.of(new BigInteger("-9876543210987654321"), false, true, "A negative non‑zero BigInteger should be reported as not zero."), // Zero
        Arguments.of(BigInteger.ZERO, false, false, "BigInteger.ZERO must be reported as zero."), // Very large positive value (greater than Long.MAX_VALUE)
        Arguments.of(new BigInteger("9223372036854775807"), false, true, "A very large positive BigInteger (greater than Long.MAX_VALUE) should be reported as not zero."), // Null 'from' argument – expect NPE from compareTo()
        Arguments.of(null, false, NullPointerException.class, "Passing a null 'from' argument results in a NullPointerException when compareTo is invoked."), // Integer (not a BigInteger) – expect ClassCastException
        Arguments.of(Integer.valueOf(42), false, ClassCastException.class, "Providing an object that is not a BigInteger causes a ClassCastException on the cast."), // Zero with a non‑null Converter (dummy)
        Arguments.of(BigInteger.ZERO, true, false, "Even when a non‑null Converter is supplied, a zero BigInteger must still be reported as zero."));
    }

    @Test
    void zeroBigIntegerReturnsFalse() {
        // GIVEN
        var zero = BigInteger.ZERO;
        // WHEN
        boolean result = NumberConversions.isBigIntegerNotZero(zero, mockConverter);
        // THEN
        assertFalse(result, "Zero BigInteger should be considered 'zero'");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void positiveBigIntegerReturnsTrue() {
        // GIVEN
        // 1
        var positive = BigInteger.ONE;
        // WHEN
        boolean result = NumberConversions.isBigIntegerNotZero(positive, mockConverter);
        // THEN
        assertTrue(result, "Positive BigInteger should not be considered zero");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void negativeBigIntegerReturnsTrue() {
        // GIVEN
        var negative = BigInteger.valueOf(-12345L);
        // WHEN
        boolean result = NumberConversions.isBigIntegerNotZero(negative, mockConverter);
        // THEN
        assertTrue(result, "Negative BigInteger should not be considered zero");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void nonBigIntegerThrowsClassCastException() {
        // GIVEN
        var notABigInteger = "not a big integer";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> NumberConversions.isBigIntegerNotZero(notABigInteger, mockConverter), "Passing a non‑BigInteger should result in ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    /**
     * Parameterised test that iterates over all scenarios described in the
     * external JSON test definition.  Each {@link Arguments} entry contains:
     *
     * <ul>
     *   <li>the {@code from} argument (as a {@link Object})</li>
     *   <li>a flag indicating whether a (mocked) {@link Converter} should be supplied</li>
     *   <li>the expected outcome – either {@link Boolean} for a normal result
     *       or {@link Class}&lt;? extends {@link Throwable}&gt; for an exception</li>
     *   <li>a textual description used in assertion messages</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideIsBigIntegerNotZeroCases")
    void testIsBigIntegerNotZeroVariousCases(Object from, boolean provideConverter, Object expected, String description) {
        // Reset mock to guarantee a clean state for each invocation.
        reset(mockConverter);
        Converter converterToPass = provideConverter ? mockConverter : null;
        if (expected instanceof Boolean) {
            // Normal boolean result expected.
            boolean actual = NumberConversions.isBigIntegerNotZero(from, converterToPass);
            assertEquals(expected, actual, description);
            // Verify that the method never touches the Converter.
            verifyNoInteractions(mockConverter);
        } else if (expected instanceof Class) {
            // An exception is expected.
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> NumberConversions.isBigIntegerNotZero(from, converterToPass), description);
            // Even when an exception occurs, the Converter must remain untouched.
            verifyNoInteractions(mockConverter);
        } else {
            fail("Test data error: expected value must be Boolean or Exception class");
        }
    }
}
