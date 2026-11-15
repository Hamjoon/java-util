package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toBoolean_Test {

    /**
     * The Converter is a dependency of the static method but is not used in the current
     *  implementation.  We provide a mock to satisfy the method signature without
     *  pulling in any heavy real‑world configuration.
     */
    @Mock
    private Converter mockConverter;

    /* --------------------------------------------------------------
     *  Helper dummy Converter implementation for tests that need a
     *  non‑null Converter instance.
     * -------------------------------------------------------------- */
    private static class DummyConverter implements Converter {
        // No methods are required for the current tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock instance automatically because of the @Mock annotation.
        // Ensure the mock is reset before each test in case a future test adds interactions.
        reset(mockConverter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help garbage collection; not strictly required.
        mockConverter = null;
    }

    /**
     * Characters that the implementation treats as true.
     */
    private static Stream<Arguments> trueValuesProvider() {
        return Stream.of(// Primitive char literals
        Arguments.of((char) 1, null), Arguments.of('t', null), Arguments.of('T', null), Arguments.of('1', null), Arguments.of('y', null), Arguments.of('Y', null), // Character wrapper objects
        Arguments.of(Character.valueOf('t'), null), Arguments.of(Character.valueOf('Y'), null), // Non‑null Converter should be ignored
        Arguments.of('Y', new DummyConverter()), Arguments.of('t', new DummyConverter()));
    }

    /**
     * Characters that should be interpreted as false.
     */
    private static Stream<Arguments> falseValuesProvider() {
        return Stream.of(// Primitive chars
        Arguments.of('0', null), Arguments.of('f', null), Arguments.of('F', null), Arguments.of('n', null), Arguments.of('N', null), Arguments.of('z', null), Arguments.of(' ', null), Arguments.of('\n', null), Arguments.of('\u0000', null), Arguments.of('\u0002', null), Arguments.of('\u00A0', null), Arguments.of('\uFFFF', null), // Character wrappers
        Arguments.of(Character.valueOf('f'), null), Arguments.of(Character.valueOf('0'), null), // Non‑null Converter ignored
        Arguments.of('0', new DummyConverter()));
    }

    /**
     * Inputs that should cause an exception (null or non‑character types).
     */
    private static Stream<Arguments> exceptionValuesProvider() {
        return Stream.of(// Null source -> NullPointerException
        Arguments.of(null, null, NullPointerException.class), // Various non‑character objects -> ClassCastException
        Arguments.of(Integer.valueOf(123), null, ClassCastException.class), Arguments.of("string", null, ClassCastException.class), Arguments.of(Boolean.TRUE, null, ClassCastException.class), Arguments.of(Float.valueOf(1.0f), null, ClassCastException.class), Arguments.of(Double.valueOf(1.0), null, ClassCastException.class), Arguments.of(Long.valueOf(1L), null, ClassCastException.class), Arguments.of(Short.valueOf((short) 1), null, ClassCastException.class), Arguments.of(Byte.valueOf((byte) 1), null, ClassCastException.class), Arguments.of(new char[] { 't' }, null, ClassCastException.class), Arguments.of(new java.util.Date(), null, ClassCastException.class));
    }

    /* --------------------------------------------------------------
     *  Utility method for readable assertion messages.
     * -------------------------------------------------------------- */
    private static String displayObject(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof Character) {
            return "'" + obj + "' (char code " + (int) ((Character) obj) + ")";
        }
        if (obj instanceof char[]) {
            char[] arr = (char[]) obj;
            return "char[]{" + new String(arr) + "}";
        }
        return obj.toString() + " (" + obj.getClass().getSimpleName() + ")";
    }

    @Test
    void toBoolean_returnsTrueForRecognisedTrueCharacters() {
        // Use the provided converter (may be null or a DummyConverter)
        boolean result = CharacterConversions.toBoolean(from, converter);
        assertTrue(result, () -> "Expected true for input: " + displayObject(from));
        // Verify that the converter, if a mock, was not interacted with.
        if (converter instanceof Converter && !(converter instanceof DummyConverter)) {
            verifyNoInteractions(converter);
        }
    }

    @Test
    void toBoolean_returnsFalseForOtherCharacters() {
        // A selection of characters that should be interpreted as false.
        char[] falseChars = { 'f', 'F', '0', 'n', 'N', 'a', 'Z', ' ', '\u0000' };
        for (char c : falseChars) {
            assertFalse(CharacterConversions.toBoolean(c, mockConverter), () -> "Expected false for character: " + (int) c);
        }
    }

    @Test
    void toBoolean_ignoresConverterParameter() {
        // Verify that the converter is not interacted with – the method does not use it.
        char c = 't';
        boolean result = CharacterConversions.toBoolean(c, mockConverter);
        assertTrue(result);
        // No interactions should have occurred.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toBoolean_throwsClassCastExceptionWhenObjectIsNotCharacter() {
        // Passing a non‑character object should cause a ClassCastException because of the
        // internal cast to primitive char.
        Object notAChar = "not a char";
        assertThrows(ClassCastException.class, () -> CharacterConversions.toBoolean(notAChar, mockConverter));
    }

    @ParameterizedTest(name = "toBoolean throws {2} for input {0}")
    @MethodSource("exceptionValuesProvider")
    void toBoolean_throwsExpectedException(Object from, Converter converter, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> CharacterConversions.toBoolean(from, converter), () -> "Expected " + expected.getSimpleName() + " for input: " + displayObject(from));
    }

    @Test
    void toBoolean_ignoresConverterParameter_whenMocked() {
        // Verify that a Mockito mock is never touched.
        char c = 't';
        boolean result = CharacterConversions.toBoolean(c, mockConverter);
        assertTrue(result);
        verifyNoInteractions(mockConverter);
    }
}
