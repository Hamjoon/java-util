package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toInt_Test {

    /**
     * A mock {@link Converter} – not used by the method but required for the signature.
     */
    @Mock
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock before each test so previous interactions do not affect verification.
        reset(mockConverter);
    }

    private static Stream<Arguments> basicCastingScenarios() {
        // Helper to create a throwing converter that should never be invoked
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Should not be called");
            }
        };
        return Stream.of(// Simple ASCII character
        arguments(Character.valueOf('A'), null, 65, "Convert a simple ASCII character 'A' to its int code point."), // Euro sign – non‑ASCII Unicode
        arguments(Character.valueOf('\u20AC'), null, 8364, "Convert a non‑ASCII Unicode character (Euro sign) to its int value."), // Integer cannot be cast to char
        arguments(Integer.valueOf(65), null, ClassCastException.class, "Passing an Integer cannot be cast to char; expect ClassCastException."), // String cannot be cast to char
        arguments("A", null, ClassCastException.class, "A String is not a Character; casting to char should fail with ClassCastException."), // Null input
        arguments(null, null, NullPointerException.class, "Null input should cause a NullPointerException when the cast is attempted."), // Non‑null converter should be ignored
        arguments(Character.valueOf('z'), new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Should not be called");
            }
        }, 122, "Even when a non‑null Converter instance is supplied, the method ignores it and returns the int value of the character."));
    }

    private static Stream<Arguments> edgeCharacterScenarios() {
        // A converter that throws if ever called – used to prove the method never touches it
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Should not be called");
            }
        };
        return Stream.of(arguments(Character.valueOf('\u0000'), null, 0, "The null character (code point 0) should be cast to int 0."), arguments(Character.valueOf('\uFFFF'), null, 65535, "The maximum char value (0xFFFF) should be cast to its unsigned int representation 65535."), arguments(Character.valueOf('\uD834'), null, 55348, "High‑surrogate code unit (U+D834) should be returned as its int value 55348."), arguments(Character.valueOf('\uDD1E'), null, 56606, "Low‑surrogate code unit (U+DD1E) should be returned as its int value 56606."), arguments(Character.valueOf(' '), null, 32, "Space character should be converted to its ASCII code point 32."), arguments(Character.valueOf('\n'), null, 10, "Line‑feed character should be converted to its code point 10."), arguments(Character.valueOf('\u20AC'), throwingConverter, 8364, "Even when a Converter that throws on any call is supplied, the method never uses it and correctly returns the Euro sign code point."));
    }

    private static Stream<Arguments> nonCharacterScenarios() {
        return Stream.of(arguments(Byte.valueOf((byte) 1), null, ClassCastException.class, "A Byte cannot be cast to char; the method must throw ClassCastException."), arguments(Short.valueOf((short) 2), null, ClassCastException.class, "A Short cannot be cast to char; expect ClassCastException."), arguments(Long.valueOf(3L), null, ClassCastException.class, "A Long cannot be cast to char; expect ClassCastException."), arguments(Double.valueOf(4.0), null, ClassCastException.class, "A Double cannot be cast to char; expect ClassCastException."), arguments(Boolean.TRUE, null, ClassCastException.class, "A Boolean cannot be cast to char; expect ClassCastException."), arguments(new java.math.BigInteger("123"), null, ClassCastException.class, "A BigInteger cannot be cast to char; expect ClassCastException."), arguments(new Object(), null, ClassCastException.class, "An arbitrary Object that is not a Character cannot be cast; expect ClassCastException."));
    }

    @Test
    void toInt_givenCharacter_returnsCorrectIntValue() {
        // GIVEN
        // Unicode 65
        Character source = 'A';
        // WHEN
        int result = CharacterConversions.toInt(source, mockConverter);
        // THEN
        assertEquals(65, result, "The int value of character 'A' should be 65");
        // Verify that the converter was never touched.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toInt_givenPrimitiveCharWrappedInObject_returnsCorrectIntValue() {
        // GIVEN
        // Unicode 122
        Object source = (Object) 'z';
        // WHEN
        int result = CharacterConversions.toInt(source, mockConverter);
        // THEN
        assertEquals(122, result);
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toInt_givenNonCharObject_throwsClassCastException() {
        // GIVEN
        Object source = Integer.valueOf(10);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toInt(source, mockConverter), "Casting a non‑char object should result in ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toInt_givenNull_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharacterConversions.toInt(source, mockConverter), "Passing null should result in NullPointerException");
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Parameterised tests – basic casting scenarios
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[basic] {index}: {3}")
    @MethodSource("basicCastingScenarios")
    void toInt_basicCasting(Object from, Converter converter, Object expected, String description) {
        // WHEN / THEN
        if (expected instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> CharacterConversions.toInt(from, converter), description);
        } else {
            // Expected a numeric result
            int result = CharacterConversions.toInt(from, converter);
            assertEquals(((Number) expected).intValue(), result, description);
        }
        // The method must never interact with the converter (even if it is a mock)
        if (converter == mockConverter) {
            verifyNoInteractions(mockConverter);
        }
    }

    /* -------------------------------------------------------------
     *  Parameterised tests – edge character values
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[edge] {index}: {3}")
    @MethodSource("edgeCharacterScenarios")
    void toInt_edgeCharacters(Object from, Converter converter, Object expected, String description) {
        // WHEN / THEN
        if (expected instanceof Class) {
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> CharacterConversions.toInt(from, converter), description);
        } else {
            int result = CharacterConversions.toInt(from, converter);
            assertEquals(((Number) expected).intValue(), result, description);
        }
        // Verify that the converter (if the mock) was not used
        if (converter == mockConverter) {
            verifyNoInteractions(mockConverter);
        }
    }

    /* -------------------------------------------------------------
     *  Parameterised tests – non‑character types
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[non-char] {index}: {3}")
    @MethodSource("nonCharacterScenarios")
    void toInt_nonCharacterTypes(Object from, Converter converter, Object expected, String description) {
        // WHEN / THEN
        assertThrows((Class<? extends Throwable>) expected, () -> CharacterConversions.toInt(from, converter), description);
        // Ensure the mock is not touched (if it was supplied)
        if (converter == mockConverter) {
            verifyNoInteractions(mockConverter);
        }
    }

    /* -------------------------------------------------------------
     *  Additional explicit tests for mock verification
     * ------------------------------------------------------------- */
    @Test
    void toInt_givenMockConverter_isNeverUsed() {
        // GIVEN
        Character source = 'M';
        // WHEN
        int result = CharacterConversions.toInt(source, mockConverter);
        // THEN
        assertEquals('M', result);
        verifyNoInteractions(mockConverter);
    }
}
