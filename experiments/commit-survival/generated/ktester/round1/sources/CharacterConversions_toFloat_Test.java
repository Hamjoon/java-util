package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toFloat_Test {

    @Mock
    private Converter // Converter is not used by the method, but required by the signature
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise mocks (handled by @ExtendWith(MockitoExtension.class), but explicit init is safe)
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to keep each test isolated
        reset(mockConverter);
    }

    static Stream<Arguments> validCharacterConversionProvider() {
        return Stream.of(Arguments.of(Character.valueOf('A'), null, 65.0f, "Converts Character 'A' to its float representation (ASCII 65). Converter is null."), Arguments.of(Character.valueOf('z'), new Converter() {

            // Stub implementation – should never be called
            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new RuntimeException("Converter should not be invoked");
            }
        }, 122.0f, "Converts Character 'z' to float (ASCII 122) with a non‑null Converter (ignored)."));
    }

    static Stream<Arguments> nullInputHandlingProvider() {
        return Stream.of(Arguments.of(null, null, NullPointerException.class, "Null 'from' with null Converter should throw NullPointerException."), Arguments.of(null, new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new RuntimeException("Should not be called");
            }
        }, NullPointerException.class, "Null 'from' with non‑null Converter still throws NullPointerException."));
    }

    static Stream<Arguments> invalidTypeConversionProvider() {
        return Stream.of(// Simple non‑character objects
        Arguments.of(Integer.valueOf(65), null, ClassCastException.class, "Integer cannot be cast to char; expect ClassCastException."), Arguments.of(new Object(), null, ClassCastException.class, "Plain Object cannot be cast to char; expect ClassCastException."), Arguments.of("A", null, ClassCastException.class, "String is not a Character; expect ClassCastException."));
    }

    static Stream<Arguments> boundaryCharacterValuesProvider() {
        return Stream.of(Arguments.of(Character.valueOf('\u0000'), null, 0.0f, "Null character (code point 0) converts to float 0.0f."), Arguments.of(Character.valueOf('\uFFFF'), null, 65535.0f, "Maximum char value (0xFFFF) converts to its numeric float representation."), Arguments.of(Character.valueOf('0'), null, 48.0f, "Character '0' (ASCII 48) converts to float 48.0f."), Arguments.of(Character.valueOf('\n'), null, 10.0f, "Newline character '\\n' (code point 10) converts to float 10.0f."), Arguments.of(Character.valueOf(' '), null, 32.0f, "Space character (ASCII 32) converts to float 32.0f."));
    }

    static Stream<Arguments> converterIgnoredProvider() {
        return Stream.of(Arguments.of(Character.valueOf('M'), new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new RuntimeException("Should not be called");
            }
        }, 77.0f, "Even with a Converter that throws if invoked, method returns float value of 'M' (ASCII 77)."));
    }

    static Stream<Arguments> invalidWrapperTypesProvider() {
        return Stream.of(// Byte
        Arguments.of(Byte.valueOf((byte) 65), null, ClassCastException.class, "Byte cannot be cast to char; expect ClassCastException."), Arguments.of(Byte.valueOf((byte) 65), new Converter() {
        }, ClassCastException.class, "Non‑null Converter does not affect ClassCastException for Byte source."), // Short
        Arguments.of(Short.valueOf((short) 65), null, ClassCastException.class, "Short cannot be cast to char; expect ClassCastException."), Arguments.of(Short.valueOf((short) 65), new Converter() {
        }, ClassCastException.class, "Non‑null Converter does not affect ClassCastException for Short source."), // Long
        Arguments.of(Long.valueOf(65L), null, ClassCastException.class, "Long cannot be cast to char; expect ClassCastException."), Arguments.of(Long.valueOf(65L), new Converter() {
        }, ClassCastException.class, "Non‑null Converter does not affect ClassCastException for Long source."), // Float
        Arguments.of(Float.valueOf(65.0f), null, ClassCastException.class, "Float cannot be cast to char; expect ClassCastException."), Arguments.of(Float.valueOf(65.0f), new Converter() {
        }, ClassCastException.class, "Non‑null Converter does not affect ClassCastException for Float source."), // Double
        Arguments.of(Double.valueOf(65.0), null, ClassCastException.class, "Double cannot be cast to char; expect ClassCastException."), Arguments.of(Double.valueOf(65.0), new Converter() {
        }, ClassCastException.class, "Non‑null Converter does not affect ClassCastException for Double source."), // Boolean
        Arguments.of(Boolean.TRUE, null, ClassCastException.class, "Boolean cannot be cast to char; expect ClassCastException."), Arguments.of(Boolean.TRUE, new Converter() {
        }, ClassCastException.class, "Non‑null Converter does not affect ClassCastException for Boolean source."));
    }

    static Stream<Arguments> invalidReferenceTypesProvider() {
        return Stream.of(// char array
        Arguments.of(new char[] { 'A', 'B' }, null, ClassCastException.class, "char[] is not a Character; casting to char throws ClassCastException."), Arguments.of(new char[] { 'A', 'B' }, new Converter() {
        }, ClassCastException.class, "Non‑null Converter does not prevent ClassCastException for char[] source."), // StringBuilder (implements CharSequence but not Character)
        Arguments.of(new StringBuilder("A"), null, ClassCastException.class, "StringBuilder is not a Character; expect ClassCastException."), Arguments.of(new StringBuilder("A"), new Converter() {
        }, ClassCastException.class, "Converter presence does not change the ClassCastException for StringBuilder source."));
    }

    @Test
    void toFloat_WithCharacter_ReturnsCorrectFloatValue() {
        // given
        // Unicode value 65
        Character input = 'A';
        // when
        float result = CharacterConversions.toFloat(input, mockConverter);
        // then
        assertEquals((float) 'A', result, "The float value should match the char's numeric value");
    }

    @Test
    void toFloat_WithPrimitiveCharWrappedInObject_ReturnsCorrectFloatValue() {
        // given
        // Unicode value 122
        Object input = Character.valueOf('z');
        // when
        float result = CharacterConversions.toFloat(input, mockConverter);
        // then
        assertEquals((float) 'z', result);
    }

    @Test
    void toFloat_WithNull_ThrowsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toFloat(input, mockConverter));
    }

    @Test
    void toFloat_WithInvalidType_ThrowsClassCastException() {
        // given
        Object input = Integer.valueOf(10);
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toFloat(input, mockConverter));
    }

    /* --------------------------------------------------------------
     *  Valid character conversion
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validCharacterConversionProvider")
    void toFloat_ValidCharacterConversion(Object from, Converter converter, float expected, String description) {
        // when
        float result = CharacterConversions.toFloat(from, converter);
        // then
        assertEquals(expected, result, 0.00001f, description);
    }

    /* --------------------------------------------------------------
     *  Null input handling
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nullInputHandlingProvider")
    void toFloat_NullInputThrows(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> CharacterConversions.toFloat(from, converter), description);
    }

    /* --------------------------------------------------------------
     *  Invalid type conversion (non‑character objects)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidTypeConversionProvider")
    void toFloat_InvalidTypeThrows(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> CharacterConversions.toFloat(from, converter), description);
    }

    /* --------------------------------------------------------------
     *  Boundary character values
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("boundaryCharacterValuesProvider")
    void toFloat_BoundaryValues(Object from, Converter converter, float expected, String description) {
        float result = CharacterConversions.toFloat(from, converter);
        assertEquals(expected, result, 0.00001f, description);
    }

    /* --------------------------------------------------------------
     *  Converter ignored with non‑null instance
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("converterIgnoredProvider")
    void toFloat_ConverterIgnored(Object from, Converter converter, float expected, String description) {
        float result = CharacterConversions.toFloat(from, converter);
        assertEquals(expected, result, 0.00001f, description);
    }

    /* --------------------------------------------------------------
     *  Additional invalid type conversion – primitive wrappers
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidWrapperTypesProvider")
    void toFloat_InvalidWrapperThrows(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> CharacterConversions.toFloat(from, converter), description);
    }

    /* --------------------------------------------------------------
     *  Invalid non‑character reference types (arrays, CharSequence, etc.)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidReferenceTypesProvider")
    void toFloat_InvalidReferenceThrows(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> CharacterConversions.toFloat(from, converter), description);
    }
}
