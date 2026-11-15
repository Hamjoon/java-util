package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toInt_Test {

    /**
     * Converter instance required by the method signature (not used internally).
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a default Converter – it does not affect the behaviour of toInt,
        // but we need a non‑null instance to satisfy the method signature.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the reference; helps the GC and mirrors typical teardown practice.
        converter = null;
    }

    /**
     * Provides a stream of arguments for {@link #toInt_validCharacters_returnsExpectedInt}.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>the {@code from} object (usually a {@link Character}),</li>
     *   <li>a {@link Converter} (may be {@code null}),</li>
     *   <li>the expected {@code int} result, and</li>
     *   <li>a short description for the test name.</li>
     * </ul>
     */
    private static Stream<Arguments> validCharacterProvider() {
        return Stream.of(// Simple characters
        Arguments.of(Character.valueOf('A'), null, 65, "Convert Character('A') → 65"), Arguments.of(Character.valueOf('\u0000'), null, 0, "Convert null character '\\u0000' → 0"), Arguments.of(Character.valueOf('\uFFFF'), null, 65535, "Convert max char '\\uFFFF' → 65535"), Arguments.of(Character.valueOf('9'), new Converter(new ConverterOptions()), 57, "Convert Character('9') with non‑null Converter → 57"), // Edge / surrogate values
        Arguments.of(Character.valueOf('\u8000'), null, 32768, "Convert Character('\\u8000') → 32768"), Arguments.of(Character.valueOf('\uD800'), null, 55296, "Convert high‑surrogate '\\uD800' → 55296"), Arguments.of(Character.valueOf('\uDC00'), null, 56320, "Convert low‑surrogate '\\uDC00' → 56320"), // Autoboxing of a primitive char literal
        Arguments.of('\u1234', null, 4660, "Autoboxed primitive char '\\u1234' → 4660"), // Converter side‑effects ignored – custom converter that would throw if used
        Arguments.of(Character.valueOf('X'), new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Should not be called");
            }
        }, 88, "Custom throwing Converter ignored for 'X' → 88"), // Converter side‑effects ignored – empty implementation
        Arguments.of(Character.valueOf('z'), new Converter(new ConverterOptions()) {
        }, 122, "Custom empty Converter ignored for 'z' → 122"));
    }

    /**
     * Provides a stream of arguments for {@link #toInt_invalidInputs_throwsExpectedException}.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>the {@code from} object (invalid type or {@code null}),</li>
     *   <li>a {@link Converter} (always {@code null} for these cases),</li>
     *   <li>the expected exception class, and</li>
     *   <li>a description.</li>
     * </ul>
     */
    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Original invalid‑input group
        Arguments.of(Integer.valueOf(5), null, ClassCastException.class, "Integer → ClassCastException"), Arguments.of("A", null, ClassCastException.class, "String → ClassCastException"), Arguments.of(null, null, NullPointerException.class, "null reference → NullPointerException"), Arguments.of(new Object(), null, ClassCastException.class, "generic Object → ClassCastException"), // Additional primitive wrapper cases
        Arguments.of(Byte.valueOf((byte) 5), null, ClassCastException.class, "Byte → ClassCastException"), Arguments.of(Short.valueOf((short) 10), null, ClassCastException.class, "Short → ClassCastException"), Arguments.of(Long.valueOf(100L), null, ClassCastException.class, "Long → ClassCastException"), Arguments.of(Float.valueOf(1.23f), null, ClassCastException.class, "Float → ClassCastException"), Arguments.of(Double.valueOf(4.56), null, ClassCastException.class, "Double → ClassCastException"), Arguments.of(Boolean.TRUE, null, ClassCastException.class, "Boolean → ClassCastException"), Arguments.of(java.math.BigInteger.valueOf(12345), null, ClassCastException.class, "BigInteger → ClassCastException"), Arguments.of(java.math.BigDecimal.valueOf(12.34), null, ClassCastException.class, "BigDecimal → ClassCastException"), Arguments.of(new java.util.concurrent.atomic.AtomicInteger(7), null, ClassCastException.class, "AtomicInteger → ClassCastException"), Arguments.of(new Character[] { 'A', 'B' }, null, ClassCastException.class, "Character[] array → ClassCastException"));
    }

    @Test
    void toInt_withCharacter_returnsCorrectInt() {
        // GIVEN
        // Unicode value 65
        Character input = 'A';
        // WHEN
        int result = CharacterConversions.toInt(input, converter);
        // THEN
        assertEquals(65, result, "The int value of character 'A' should be 65");
    }

    @Test
    void toInt_withCharPrimitive_autoboxed_returnsCorrectInt() {
        // GIVEN
        // Unicode value 122 (autoboxed to Character)
        char input = 'z';
        // WHEN
        int result = CharacterConversions.toInt(input, converter);
        // THEN
        assertEquals(122, result, "The int value of character 'z' should be 122");
    }

    @Test
    void toInt_withNonCharacter_throwsClassCastException() {
        // GIVEN
        String notAChar = "not a char";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toInt(notAChar, converter), "Passing a non‑Character object should result in ClassCastException");
    }

    @Test
    void toInt_withNull_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharacterConversions.toInt(nullObj, converter), "Passing null should result in NullPointerException due to unboxing");
    }

    /**
     * Valid {@link Character} inputs together with the expected integer value.
     * The {@code Converter} argument is either {@code null} or a dummy instance
     * (the method must ignore it).
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validCharacterProvider")
    void toInt_validCharacters_returnsExpectedInt(Object from, Converter conv, int expected, String description) {
        // WHEN
        int result = CharacterConversions.toInt(from, conv);
        // THEN
        assertEquals(expected, result, description);
    }

    /**
     * Invalid inputs that must result in either {@link ClassCastException}
     * or {@link NullPointerException}.
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidInputProvider")
    void toInt_invalidInputs_throwsExpectedException(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> CharacterConversions.toInt(from, conv), description);
    }
}
