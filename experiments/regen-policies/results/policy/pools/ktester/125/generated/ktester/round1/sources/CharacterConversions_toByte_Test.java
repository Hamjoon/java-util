package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class CharacterConversions_toByte_Test {

    @Mock
    private Converter // not used by the method under test
    mockConverter;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for this test instance.
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the Mockito session.
        mocks.close();
    }

    private static Stream<Arguments> validCharToByteProvider() {
        return Stream.of(// Valid character to byte conversions
        arguments('A', (byte) 65, "Convert a simple Character 'A' (Unicode 65) to a byte; result should be 65."), arguments((int) 200, (byte) -56, "Convert a Character with code point 200; byte cast truncates to -56 (200‑256)."), arguments(0xFFFF, (byte) -1, "Convert the maximum char value (0xFFFF). After casting to byte the result is -1 (255‑256)."), arguments(0, (byte) 0, "Convert the minimum char value (0) to byte; result should be 0."), // Byte overflow edge cases
        arguments(256, (byte) 0, "Char value 256 wraps to 0 when cast to byte (256‑256)."), arguments(128, (byte) -128, "Char value 128 becomes -128 after byte cast (128‑256)."), arguments(257, (byte) 1, "Char value 257 wraps to 1 when cast to byte (257‑256)."), arguments(300, (byte) 44, "Char value 300 wraps to 44 after byte cast (300‑256)."), // Converter parameter ignored (non‑null converter)
        arguments('B', (byte) 66, "The method does not use the Converter; a non‑null converter should not affect the result."));
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(arguments(65, "Passing an Integer (not a Character) should trigger a ClassCastException."), arguments("A", "Passing a String instead of a Character should trigger a ClassCastException."), arguments(true, "Passing a Boolean (true) should trigger a ClassCastException."), arguments(3.14, "Passing a Double should trigger a ClassCastException."), arguments(new Object(), "Passing an instance of a user‑defined class should trigger a ClassCastException."));
    }

    @Test
    void toByte_WithCharacterWithinByteRange_returnsCorrectByte() {
        // given
        // Unicode code point 65
        Character input = 'A';
        // when
        byte result = CharacterConversions.toByte(input, mockConverter);
        // then
        assertEquals((byte) 65, result, "Byte value should match the character's code point");
    }

    @Test
    void toByte_WithCharacterBeyondByteRange_wrapsAccordingToJavaCasting() {
        // given
        // Character with code point 0x0100 (256) – exceeds signed byte range.
        Character input = '\u0100';
        // when
        byte result = CharacterConversions.toByte(input, mockConverter);
        // then
        // 256 as a byte is 0 (because 256 & 0xFF == 0, then interpreted as signed byte)
        assertEquals((byte) 0, result, "Casting should wrap around according to Java's byte casting rules");
    }

    @Test
    void toByte_WithNull_throwsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toByte(input, mockConverter), "Passing null should result in NPE due to unboxing of a Character");
    }

    @Test
    void toByte_WithNonCharacterObject_throwsClassCastException() {
        // given
        // String is not a Character
        Object input = "A";
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toByte(input, mockConverter), "Casting a non‑Character object to char should throw ClassCastException");
    }

    @Test
    void toByte_WithNumberObject_throwsClassCastException() {
        // given
        // Number, not a Character
        Object input = Integer.valueOf(65);
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toByte(input, mockConverter), "Casting a Number to char should throw ClassCastException");
    }

    /**
     * Tests a wide range of valid {@code char} values (including overflow behaviour)
     * and verifies that the resulting {@code byte} matches Java's casting rules.
     *
     * The {@code from} values are supplied as {@link Number}s because the JSON test
     * data represents char code points. They are converted to {@link Character}
     * before being passed to the method under test.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validCharToByteProvider")
    void toByte_ValidCharConversions(Object from, byte expected, String description) {
        // given
        Object input = (from instanceof Number) ? Character.valueOf((char) ((Number) from).intValue()) : // for the single string case in the "Converter ignored" group
        from;
        // when
        byte result = CharacterConversions.toByte(input, mockConverter);
        // then
        assertEquals(expected, result, description);
    }

    /**
     * Parameterized test for inputs that are not {@link Character} instances.
     * The method is expected to throw {@link ClassCastException}.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("invalidTypeProvider")
    void toByte_InvalidTypes_throwClassCastException(Object from, String description) {
        // given
        Object input = from;
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toByte(input, mockConverter), description);
    }
}
