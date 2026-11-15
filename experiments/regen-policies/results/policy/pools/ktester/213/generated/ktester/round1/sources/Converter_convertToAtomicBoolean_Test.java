package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToAtomicBoolean_Test {

    /**
     * Supplies a {@link Stream} of arguments for {@link #shouldConvertVariousInputsToAtomicBoolean}.
     *
     * <p>Each {@link Arguments} consists of:
     * <ol>
     *   <li>the source object passed to {@code convertToAtomicBoolean}</li>
     *   <li>the expected {@link AtomicBoolean} result (or {@code null})</li>
     *   <li>the expected exception class (or {@code null} if no exception is expected)</li>
     * </ol>
     */
    private static Stream<Arguments> conversionProvider() {
        return Stream.of(// Boolean literals
        Arguments.of(Boolean.TRUE, new AtomicBoolean(true), null), Arguments.of(Boolean.FALSE, new AtomicBoolean(false), null), // String literals (case‑insensitive, trimmed)
        Arguments.of("true", new AtomicBoolean(true), null), Arguments.of("false", new AtomicBoolean(false), null), Arguments.of("TRUE", new AtomicBoolean(true), null), Arguments.of("TrUe", new AtomicBoolean(true), null), Arguments.of("  true  ", new AtomicBoolean(true), null), Arguments.of("t", new AtomicBoolean(true), null), Arguments.of("f", new AtomicBoolean(false), null), // Numeric values
        Arguments.of(1, new AtomicBoolean(true), null), Arguments.of(0, new AtomicBoolean(false), null), Arguments.of(42, new AtomicBoolean(true), null), Arguments.of(-5, new AtomicBoolean(true), null), Arguments.of(0.1, new AtomicBoolean(true), null), // Wrapper numeric types
        Arguments.of(Byte.valueOf((byte) 1), new AtomicBoolean(true), null), Arguments.of(Byte.valueOf((byte) 0), new AtomicBoolean(false), null), // BigInteger / BigDecimal
        Arguments.of(new BigInteger("1"), new AtomicBoolean(true), null), Arguments.of(new BigDecimal("0"), new AtomicBoolean(false), null), // CharBuffer containing true / whitespace
        Arguments.of(CharBuffer.wrap("true"), new AtomicBoolean(true), null), Arguments.of(CharBuffer.wrap("   true   "), new AtomicBoolean(true), null), Arguments.of(CharBuffer.wrap("   "), null, IllegalArgumentException.class), // Existing AtomicBoolean – should preserve the logical value
        Arguments.of(new AtomicBoolean(true), new AtomicBoolean(true), null), Arguments.of(new AtomicBoolean(false), new AtomicBoolean(false), null), // Null input – should return null
        Arguments.of(null, null, null), // Empty or whitespace‑only strings – illegal
        Arguments.of("", null, IllegalArgumentException.class), Arguments.of("   ", null, IllegalArgumentException.class), // Unrecognised textual values
        Arguments.of("yes", null, IllegalArgumentException.class), Arguments.of("maybe", null, IllegalArgumentException.class), Arguments.of("x", null, IllegalArgumentException.class), // Numeric‑looking but unparsable string
        Arguments.of("2abc", null, IllegalArgumentException.class), // Unsupported collection / map / enum / timestamp types
        Arguments.of(java.util.Collections.singletonList(1), null, IllegalArgumentException.class), Arguments.of(java.util.Collections.singletonMap("key", "value"), null, IllegalArgumentException.class), Arguments.of(java.time.Instant.now(), null, IllegalArgumentException.class), Arguments.of(java.util.ArrayDeque::new, null, IllegalArgumentException.class), // Generic Object (no conversion path)
        Arguments.of(new Object(), null, IllegalArgumentException.class));
    }

    @Test
    void shouldConvertTrueStringToAtomicBoolean() {
        // given
        String source = "true";
        // when
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // then
        assertNotNull(result);
        assertTrue(result.get());
    }

    @Test
    void shouldConvertFalseStringToAtomicBoolean() {
        // given
        String source = "false";
        // when
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // then
        assertNotNull(result);
        assertFalse(result.get());
    }

    @Test
    void shouldConvertBooleanTrueToAtomicBoolean() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // then
        assertNotNull(result);
        assertTrue(result.get());
    }

    @Test
    void shouldConvertBooleanFalseToAtomicBoolean() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // then
        assertNotNull(result);
        assertFalse(result.get());
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // given
        Object source = null;
        // when
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // then
        assertNull(result);
    }

    @Test
    void shouldConvertNumericZeroToAtomicBooleanFalse() {
        // given
        Integer source = 0;
        // when
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // then
        assertNotNull(result);
        assertFalse(result.get());
    }

    @Test
    void shouldConvertNumericNonZeroToAtomicBooleanTrue() {
        // given
        Integer source = 42;
        // when
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // then
        assertNotNull(result);
        assertTrue(result.get());
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] convertToAtomicBoolean({0}) => {1}")
    @MethodSource("conversionProvider")
    void shouldConvertVariousInputsToAtomicBoolean(Object source, AtomicBoolean expectedResult, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            // Verify that the conversion throws the expected exception type.
            Throwable thrown = assertThrows(expectedException, () -> Converter.convertToAtomicBoolean(source));
            // Optional: ensure the message is not empty – helps catch silent failures.
            assertNotNull(thrown.getMessage());
        } else {
            // Normal conversion – either a non‑null AtomicBoolean or null.
            AtomicBoolean result = Converter.convertToAtomicBoolean(source);
            assertEquals(expectedResult, result, () -> "Conversion of " + source + " produced unexpected result.");
        }
    }
}
