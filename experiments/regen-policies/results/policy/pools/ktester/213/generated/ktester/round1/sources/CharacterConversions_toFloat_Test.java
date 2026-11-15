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
class CharacterConversions_toFloat_Test {

    @Mock
    private Converter // not used by the method under test
    mockConverter;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for the @Mock fields
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the Mockito session
        if (mocks != null) {
            mocks.close();
        }
    }

    private static Stream<Arguments> validConversions() {
        return Stream.of(// Simple ASCII character
        arguments('A', 65.0f), // Null character (code point 0)
        arguments('\u0000', 0.0f), // Greek capital omega (U+03A9, code point 937)
        arguments('\u03A9', 937.0f), // Character wrapper obtained via valueOf
        arguments(Character.valueOf('Z'), 90.0f), // High‑surrogate code unit (U+D834)
        arguments('\uD834', 55348.0f), // Maximum char value (U+FFFF)
        arguments('\uFFFF', 65535.0f));
    }

    private static Stream<Arguments> invalidConversions() {
        return Stream.of(// Primitive numeric types (autoboxed)
        arguments(5, ClassCastException.class), arguments(5.5, ClassCastException.class), // String object
        arguments("string", ClassCastException.class), // Boolean primitive (autoboxed)
        arguments(true, ClassCastException.class), // Byte, Short, Long wrappers
        arguments(Byte.valueOf((byte) 1), ClassCastException.class), arguments(Short.valueOf((short) 2), ClassCastException.class), arguments(Long.valueOf(3L), ClassCastException.class), // Generic Object instance
        arguments(new Object(), ClassCastException.class), // Char array (cannot be cast to a single char)
        arguments(new char[] { 'a' }, ClassCastException.class), // Null source – must raise NullPointerException
        arguments(null, NullPointerException.class));
    }

    @Test
    void toFloat_WithCharacter_ReturnsCorrectFloat() {
        // given
        // char value 90
        Character source = 'Z';
        // when
        float result = CharacterConversions.toFloat(source, mockConverter);
        // then
        assertEquals((float) source.charValue(), result, "The float value should match the numeric value of the character");
    }

    @Test
    void toFloat_WithString_ThrowsClassCastException() {
        // given
        String source = "A";
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toFloat(source, mockConverter), "Casting a non‑character object should result in ClassCastException");
    }

    @Test
    void toFloat_WithNull_ThrowsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toFloat(source, mockConverter), "Passing null should cause a NullPointerException when casting to char");
    }

    /**
     * Valid conversions – each entry supplies a source object that can be
     * cast to {@code char} and the expected {@code float} result.
     */
    @ParameterizedTest(name = "[{index}] toFloat({0}) = {1}")
    @MethodSource("validConversions")
    void toFloat_ValidConversions(Object from, float expected) {
        // The converter argument is irrelevant – we pass {@code null}
        float result = CharacterConversions.toFloat(from, null);
        assertEquals(expected, result, () -> "Expected float value " + expected + " for source " + from);
    }

    /**
     * Invalid conversions – each entry supplies a source object that cannot be
     * cast to {@code char} (or is {@code null}) and the expected exception type.
     */
    @ParameterizedTest(name = "[{index}] toFloat({0}) throws {1}")
    @MethodSource("invalidConversions")
    void toFloat_InvalidConversions(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> CharacterConversions.toFloat(from, null), () -> "Expected " + expectedException.getSimpleName() + " for source " + from);
    }
}
