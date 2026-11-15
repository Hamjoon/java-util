package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.convert.ConversionException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToAtomicBoolean_Test {

    /**
     * Provides a stream of arguments for successful conversion tests.
     *
     * @return stream of {@link Arguments} where the first element is the input object
     *         and the second element is the expected boolean value.
     */
    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// primitive‑like Booleans (autoboxed)
        Arguments.of(Boolean.TRUE, true), Arguments.of(Boolean.FALSE, false), // Boolean objects
        Arguments.of(Boolean.TRUE, true), Arguments.of(Boolean.FALSE, false), // String values (case‑insensitive, trimmed)
        Arguments.of("true", true), Arguments.of("FALSE", false), Arguments.of("  true  ", true), Arguments.of("", false), Arguments.of("   ", false), // Numeric values – any non‑zero is true
        Arguments.of(1, true), Arguments.of(0, false), Arguments.of(2, true), Arguments.of(-5, true), Arguments.of(10L, true), Arguments.of(0L, false), Arguments.of(0.5, true), Arguments.of(0.0, false), // BigInteger / BigDecimal
        Arguments.of(new BigInteger("123"), true), Arguments.of(new BigInteger("0"), false), Arguments.of(new BigDecimal("3.14"), true), Arguments.of(new BigDecimal("0.0"), false), // Character literals
        Arguments.of('1', true), Arguments.of('0', false), // StringBuilder (delegates to toString())
        Arguments.of(new StringBuilder("FALSE"), false), Arguments.of(new StringBuilder("TrUe"), true), // CharBuffer – not a supported source, but we treat it as unsupported
        // (added later in exception tests)
        // AtomicBoolean – should return a *new* instance with the same value
        Arguments.of(new AtomicBoolean(true), true), Arguments.of(new AtomicBoolean(false), false));
    }

    private static Stream<Arguments> provideNullConversions() {
        return Stream.of(Arguments.of(null));
    }

    /**
     * Supplies inputs that are not supported for conversion to {@code AtomicBoolean}.
     *
     * @return stream of arguments containing unsupported objects.
     */
    private static Stream<Arguments> provideExceptionConversions() {
        return Stream.of(// Unsupported string tokens
        Arguments.of("yes"), Arguments.of("no"), // Unsupported character literals
        Arguments.of('y'), Arguments.of('Y'), // CharBuffer (explicitly unsupported)
        Arguments.of(CharBuffer.wrap("true")), // ByteBuffer (unsupported)
        Arguments.of(ByteBuffer.allocate(4).putInt(1).flip()), // AtomicInteger / AtomicLong (unsupported)
        Arguments.of(new AtomicInteger(1)), Arguments.of(new AtomicLong(0L)), // Array of Boolean
        Arguments.of(new Boolean[] { true }), // NaN (numeric but not a valid boolean representation)
        Arguments.of(Double.NaN), // StringBuilder with unknown token
        Arguments.of(new StringBuilder("maybe")));
    }

    /**
     * Safely converts an object to a readable {@code String} for use in assertion
     * messages. Handles {@code null} and arrays gracefully.
     *
     * @param obj the object to represent
     * @return a string representation
     */
    private static String safeToString(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj.getClass().isArray()) {
            return java.util.Arrays.deepToString(new Object[] { obj });
        }
        return obj.toString();
    }

    @Test
    void convertFromBooleanTrue() {
        AtomicBoolean result = Converter.convertToAtomicBoolean(Boolean.TRUE);
        assertNotNull(result);
        assertTrue(result.get());
    }

    @Test
    void convertFromBooleanFalse() {
        AtomicBoolean result = Converter.convertToAtomicBoolean(Boolean.FALSE);
        assertNotNull(result);
        assertFalse(result.get());
    }

    @Test
    void convertFromStringTrue() {
        AtomicBoolean result = Converter.convertToAtomicBoolean("true");
        assertNotNull(result);
        assertTrue(result.get());
    }

    @Test
    void convertFromStringFalse() {
        AtomicBoolean result = Converter.convertToAtomicBoolean("false");
        assertNotNull(result);
        assertFalse(result.get());
    }

    @Test
    void convertFromIntegerOne() {
        AtomicBoolean result = Converter.convertToAtomicBoolean(1);
        assertNotNull(result);
        assertTrue(result.get());
    }

    @Test
    void convertFromIntegerZero() {
        AtomicBoolean result = Converter.convertToAtomicBoolean(0);
        assertNotNull(result);
        assertFalse(result.get());
    }

    @Test
    void convertFromAtomicBoolean() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // The converter should return a *new* AtomicBoolean with the same value.
        assertNotNull(result);
        assertNotSame(source, result);
        assertEquals(source.get(), result.get());
    }

    @Test
    void convertFromNullReturnsNull() {
        AtomicBoolean result = Converter.convertToAtomicBoolean(null);
        assertNull(result, "Converting a null source should return null for object types.");
    }

    /**
     * Parameterized test that verifies successful conversion to {@link AtomicBoolean}
     * for all supported source types.
     *
     * @param input    the source object passed to {@code convertToAtomicBoolean}
     * @param expected the expected boolean value inside the resulting {@code AtomicBoolean}
     */
    @ParameterizedTest(name = "[{index}] convert ''{0}'' → AtomicBoolean({1})")
    @MethodSource("provideSuccessfulConversions")
    void testConvertToAtomicBooleanSuccess(Object input, boolean expected) {
        AtomicBoolean result = Converter.convertToAtomicBoolean(input);
        assertNotNull(result, "Result must not be null for non‑null input");
        assertEquals(expected, result.get(), () -> "Conversion of " + safeToString(input) + " should yield " + expected);
    }

    @ParameterizedTest(name = "[{index}] convert null → null")
    @MethodSource("provideNullConversions")
    void testConvertToAtomicBooleanNull(Object input) {
        AtomicBoolean result = Converter.convertToAtomicBoolean(input);
        assertNull(result, "Converting null must return null");
    }

    /**
     * Parameterized test that verifies a {@link ConversionException} is thrown for
     * unsupported source types.
     *
     * @param input the unsupported source object
     */
    @ParameterizedTest(name = "[{index}] convert ''{0}'' → ConversionException")
    @MethodSource("provideExceptionConversions")
    void testConvertToAtomicBooleanException(Object input) {
        assertThrows(ConversionException.class, () -> Converter.convertToAtomicBoolean(input), () -> "Conversion of " + safeToString(input) + " should throw ConversionException");
    }
}
