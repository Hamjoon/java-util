package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toFloat_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    mockConverter;

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// 'A' → 65
        Arguments.of(Character.valueOf('A'), 65.0f), // 'z' → 122
        Arguments.of(Character.valueOf('z'), 122.0f));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(5)), Arguments.of("test"), Arguments.of(Boolean.TRUE), Arguments.of(Double.valueOf(3.14)));
    }

    @Test
    void toFloat_WithCharacter_ReturnsCharCodeAsFloat() {
        // GIVEN
        // Unicode code point 65
        Character source = 'A';
        // WHEN
        float result = CharacterConversions.toFloat(source, mockConverter);
        // THEN
        assertEquals(65.0f, result, 0.0f, "The float value should be the numeric code of the character");
    }

    @Test
    void toFloat_WithCharPrimitiveViaAutoboxing_ReturnsCharCodeAsFloat() {
        // GIVEN
        // Unicode code point 122
        char source = 'z';
        // WHEN
        float result = CharacterConversions.toFloat(source, mockConverter);
        // THEN
        assertEquals(122.0f, result, 0.0f);
    }

    @Test
    void toFloat_WithInvalidType_ThrowsClassCastException() {
        // GIVEN
        Integer invalidSource = 123;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toFloat(invalidSource, mockConverter), "Casting a non‑character object should result in ClassCastException");
    }

    @Test
    void toFloat_WithNull_ThrowsNullPointerException() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharacterConversions.toFloat(nullSource, mockConverter), "Passing null should result in NullPointerException");
    }

    /**
     * Normal conversions – a {@link Character} (or autoboxed {@code char}) should be
     * converted to its Unicode code point expressed as a {@code float}.
     */
    @ParameterizedTest(name = "[{index}] from=''{0}'' → expected={1}")
    @MethodSource("normalConversionProvider")
    void toFloat_NormalConversions(Object from, float expected) {
        // WHEN
        float result = CharacterConversions.toFloat(from, mockConverter);
        // THEN
        assertEquals(expected, result, 0.0f, () -> "Expected the float representation of the char code point");
    }

    /**
     * Null source – the method attempts to cast {@code null} to {@code char},
     * which must result in a {@link NullPointerException}.
     */
    @Test
    void toFloat_NullSource_ThrowsNullPointerException() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharacterConversions.toFloat(nullSource, mockConverter), "Passing null should trigger a NullPointerException");
    }

    /**
     * Invalid type conversions – any object that is not a {@link Character}
     * (or a primitive {@code char}) must cause a {@link ClassCastException}
     * when the method attempts the cast.
     */
    @ParameterizedTest(name = "[{index}] from=''{0}'' (type={0.getClass().getSimpleName()}) → ClassCastException")
    @MethodSource("invalidConversionProvider")
    void toFloat_InvalidTypes_ThrowClassCastException(Object from) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toFloat(from, mockConverter), "Casting a non‑character object should result in ClassCastException");
    }
}
