package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringConversions_toDouble_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    mockConverter;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for this test instance.
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources.
        if (mocks != null) {
            mocks.close();
        }
    }

    /**
     * Provides a stream of arguments for {@link #toDouble_ParameterizedTests}.
     *
     * Each argument consists of:
     * <ul>
     *   <li>the {@code from} value (Object)</li>
     *   <li>the {@code converter} to pass (may be {@code null} or a mock)</li>
     *   <li>the expected result:
     *       <ul>
     *         <li>a {@link Double} for successful parses,</li>
     *         <li>a {@link Class} representing an expected exception,</li>
     *         <li>or the special string {@code "NaN"} / {@code "Infinity"} which are mapped to the corresponding {@link Double} constants.</li>
     *       </ul>
     *   </li>
     * </ul>
     */
    private static Stream<Arguments> provideToDoubleCases() {
        // will be replaced by a mock where needed
        Converter dummyConverter = null;
        return Stream.of(// Empty / whitespace cases
        arguments("", null, 0.0), arguments("   ", null, 0.0), // Normal numeric strings
        arguments("123.45", null, 123.45), arguments("-0.001", null, -0.001), arguments("1e3", null, 1000.0), // Special floating‑point literals
        arguments("NaN", null, Double.NaN), arguments("Infinity", null, Double.POSITIVE_INFINITY), arguments("-Infinity", null, Double.NEGATIVE_INFINITY), // Invalid numeric string
        arguments("abc", null, IllegalArgumentException.class), // Null reference (treated as empty → 0.0 per implementation)
        arguments(null, null, 0.0), // Additional tests
        arguments("  42.0  ", null, 42.0), arguments("+5.5", null, 5.5), arguments("-0", null, -0.0), // Edge of double range
        arguments("1.7976931348623157E308", null, Double.MAX_VALUE), arguments("1.7976931348623157E309", null, Double.POSITIVE_INFINITY), arguments("-1.7976931348623157E309", null, Double.NEGATIVE_INFINITY), // Hexadecimal floating‑point literal (Java 8+ supports this)
        arguments("0x1.0p0", null, 1.0), // Underscore not allowed by Double.valueOf → IllegalArgumentException
        arguments("123_456", null, IllegalArgumentException.class), // Non‑String objects → ClassCastException
        arguments(new Integer(5), null, ClassCastException.class), arguments(new Object(), null, ClassCastException.class), // Non‑null Converter (ignored)
        arguments("123.45", mock(Converter.class), 123.45));
    }

    // Helper to create a Mockito mock without polluting the field name.
    private static Converter mock(Class<Converter> cls) {
        return org.mockito.Mockito.mock(cls);
    }

    @Test
    void toDouble_EmptyString_ReturnsZero() {
        // GIVEN
        String empty = "";
        // WHEN
        Double result = StringConversions.toDouble(empty, mockConverter);
        // THEN
        assertEquals(0.0, result);
    }

    @Test
    void toDouble_NullString_ReturnsZero() {
        // GIVEN
        String nullString = null;
        // WHEN
        Double result = StringConversions.toDouble(nullString, mockConverter);
        // THEN
        assertEquals(0.0, result);
    }

    @Test
    void toDouble_ValidDoubleString_ReturnsParsedValue() {
        // GIVEN
        String value = "1234.5678";
        // WHEN
        Double result = StringConversions.toDouble(value, mockConverter);
        // THEN
        assertEquals(1234.5678, result);
    }

    @Test
    void toDouble_InvalidString_ThrowsIllegalArgumentException() {
        // GIVEN
        String badValue = "not-a-number";
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDouble(badValue, mockConverter));
        // Verify that the message contains the original value and that a cause is present.
        assertTrue(ex.getMessage().contains(badValue));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof NumberFormatException);
    }

    @Test
    void toDouble_NonStringObject_ThrowsClassCastException() {
        // GIVEN
        // Integer, not a String
        Object notAString = 42;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> StringConversions.toDouble(notAString, mockConverter));
    }

    /**
     * Executes the parameterised test matrix defined in {@link #provideToDoubleCases()}.
     *
     * The assertion strategy depends on the expected value:
     * <ul>
     *   <li>If the expected object is a {@link Double}, the result is compared using {@link Assertions#assertEquals(double, double)}.
     *       Special handling is applied for {@code NaN} and {@code -0.0}.</li>
     *   <li>If the expected object is a {@link Class} (exception type), {@link Assertions#assertThrows} is used.</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1}, expected={2}")
    @MethodSource("provideToDoubleCases")
    void toDouble_ParameterizedTests(Object from, Converter converter, Object expected) {
        // Use the mockConverter when the test explicitly provides a mock; otherwise fall back to the field mock.
        Converter effectiveConverter = (converter != null) ? converter : mockConverter;
        if (expected instanceof Class) {
            // Expected an exception.
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            Throwable thrown = assertThrows(exClass, () -> StringConversions.toDouble(from, effectiveConverter));
            // For IllegalArgumentException we also verify that the cause is a NumberFormatException,
            // mirroring the behaviour of the original method.
            if (exClass == IllegalArgumentException.class) {
                assertNotNull(thrown.getCause(), "Cause should not be null for IllegalArgumentException");
                assertTrue(thrown.getCause() instanceof NumberFormatException, "Cause should be NumberFormatException");
            }
        } else {
            // Expected a successful Double result.
            Double result = StringConversions.toDouble(from, effectiveConverter);
            Double expectedDouble = (Double) expected;
            if (expectedDouble.isNaN()) {
                assertTrue(Double.isNaN(result), "Result should be NaN");
            } else if (expectedDouble.equals(-0.0)) {
                // Distinguish -0.0 from +0.0 using bit patterns.
                assertEquals(Double.doubleToLongBits(-0.0), Double.doubleToLongBits(result), "Result should be -0.0");
            } else {
                assertEquals(expectedDouble, result, () -> "Parsed value does not match expected for input: " + from);
            }
        }
    }
}
