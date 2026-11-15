package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class NumberConversions_floatToString_Test {

    /**
     * The {@link Converter} dependency required by the method under test.
     */
    @Mock
    private Converter converter;

    /**
     * Opens a new Mockito session before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Closes the Mockito session after each test.
     */
    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the mock – not strictly required but keeps the pattern clear.
        converter = null;
    }

    /**
     * Supplies all normal (non‑exception) test cases extracted from the JSON description.
     */
    private static Stream<Arguments> normalStringCases() {
        return Stream.of(// Zero handling
        arguments(Float.valueOf(0.0f), "0"), arguments(Float.valueOf(-0.0f), "0"), // Non‑zero typical values
        arguments(Float.valueOf(1.23f), "1.23"), arguments(Float.valueOf(-5.0f), "-5.0"), // Additional non‑zero values
        // "1.4E-45"
        arguments(Float.valueOf(Float.MIN_VALUE), Float.valueOf(Float.MIN_VALUE).toString()), // "-3.4028235E38"
        arguments(Float.valueOf(-Float.MAX_VALUE), Float.valueOf(-Float.MAX_VALUE).toString()), // Special float values
        arguments(Float.valueOf(Float.NaN), "NaN"), arguments(Float.valueOf(Float.POSITIVE_INFINITY), "Infinity"), arguments(Float.valueOf(Float.NEGATIVE_INFINITY), "-Infinity"), // "3.4028235E38"
        arguments(Float.valueOf(Float.MAX_VALUE), Float.valueOf(Float.MAX_VALUE).toString()), // "1.17549435E-38"
        arguments(Float.valueOf(Float.MIN_NORMAL), Float.valueOf(Float.MIN_NORMAL).toString()));
    }

    /**
     * Supplies all exception‑related test cases extracted from the JSON description.
     */
    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// Null input – unboxing a null reference causes NPE
        arguments(null, NullPointerException.class), // Incompatible numeric types
        arguments(Double.valueOf(1.0), ClassCastException.class), arguments(Integer.valueOf(5), ClassCastException.class), arguments(Long.valueOf(10L), ClassCastException.class), arguments(Short.valueOf((short) 2), ClassCastException.class), arguments(Byte.valueOf((byte) 3), ClassCastException.class), // Non‑numeric types
        arguments("not a float", ClassCastException.class), arguments(BigDecimal.valueOf(1.23), ClassCastException.class), arguments(BigInteger.valueOf(42), ClassCastException.class), arguments(new Object(), ClassCastException.class));
    }

    /**
     * Verify that a Float value equal to 0.0F returns the literal string "0".
     */
    @Test
    @DisplayName("floatToString returns \"0\" for zero float")
    void zeroFloatReturnsZeroString() {
        Float zero = 0.0F;
        String result = NumberConversions.floatToString(zero, converter);
        assertEquals("0", result, "Zero float should be converted to the string \"0\"");
    }

    /**
     * Verify that a non‑zero Float value is converted using its {@code toString()} implementation.
     */
    @Test
    @DisplayName("floatToString returns toString() for non‑zero float")
    void nonZeroFloatReturnsToString() {
        Float value = 12.34F;
        // "12.34"
        String expected = value.toString();
        String result = NumberConversions.floatToString(value, converter);
        assertEquals(expected, result, "Non‑zero float should be converted via toString()");
    }

    /**
     * Verify that passing a non‑Float object results in a {@link ClassCastException}.
     */
    @Test
    @DisplayName("floatToString throws ClassCastException for incompatible type")
    void incompatibleTypeThrowsException() {
        // Double cannot be cast to float directly
        Double notAFloat = 1.23;
        assertThrows(ClassCastException.class, () -> NumberConversions.floatToString(notAFloat, converter), "A non‑Float object should cause a ClassCastException");
    }

    /**
     * Parameterized test covering all cases that should return a string representation.
     *
     * @param from     the input object passed to {@code floatToString}
     * @param expected the expected string result
     */
    @ParameterizedTest(name = "[{index}] from={0} → expected=\"{1}\"")
    @MethodSource("normalStringCases")
    void returnsStringForValidFloat(Object from, String expected) {
        // The converter is irrelevant for the current implementation; we pass null.
        String result = NumberConversions.floatToString(from, null);
        assertEquals(expected, result, () -> "Expected \"" + expected + "\" for input " + from);
    }

    /**
     * Parameterized test that verifies the method throws the expected exception
     * for invalid inputs.
     *
     * @param from            the input object passed to {@code floatToString}
     * @param expectedException the class of the exception that should be thrown
     */
    @ParameterizedTest(name = "[{index}] from={0} → throws {1}")
    @MethodSource("exceptionCases")
    void throwsExpectedExceptionForInvalidInput(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.floatToString(from, null), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
