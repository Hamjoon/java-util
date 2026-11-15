package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

class CharacterConversions_toShort_Test {

    private static Converter converter;

    /**
     * Simple dummy converter – does nothing special.
     */
    private static class DummyConverter extends Converter {

        DummyConverter() {
            super(new ConverterOptions());
        }
    }

    /**
     * Converter that would throw if any of its methods were invoked.
     */
    private static class ThrowingConverter extends Converter {

        ThrowingConverter() {
            super(new ConverterOptions());
        }

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            throw new RuntimeException("This converter should never be called");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter is not used by the method under test, but we create a real instance
        // to satisfy the method signature and to ensure future changes that might use it
        // do not cause NullPointerExceptions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none in this case).
        converter = null;
    }

    private static Stream<Arguments> validCharacterProvider() {
        return Stream.of(Arguments.of("Convert a normal ASCII character 'A' (code 65) to short", 'A', (short) 65), Arguments.of("Convert the null character (code 0) to short", '\u0000', (short) 0), Arguments.of("Convert the highest positive short value (char 0x7FFF = 32767) to short", '\u7FFF', (short) 32767), Arguments.of("Convert char 0x8000 (32768) which overflows signed short; wraps to -32768", '\u8000', (short) -32768), Arguments.of("Convert char 0xFFFF (65535) to short; wraps to -1", '\uFFFF', (short) -1));
    }

    private static Stream<Arguments> characterObjectProvider() {
        return Stream.of(Arguments.of("Character.valueOf('\\u0001') → 1", Character.valueOf('\u0001'), (short) 1), Arguments.of("new Character('C') → 67", new Character('C'), (short) 67), Arguments.of("Character.valueOf('\\u8001') → -32767 (overflow)", Character.valueOf('\u8001'), (short) -32767), Arguments.of("Character.valueOf('\\uFFFF') → -1 (overflow)", Character.valueOf('\uFFFF'), (short) -1));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(Arguments.of("Null source object → NullPointerException", null, NullPointerException.class), Arguments.of("Integer source → ClassCastException", 123, ClassCastException.class), Arguments.of("String source → ClassCastException", "Z", ClassCastException.class), Arguments.of("Short wrapper → ClassCastException", Short.valueOf((short) 123), ClassCastException.class), Arguments.of("Byte wrapper → ClassCastException", Byte.valueOf((byte) 5), ClassCastException.class), Arguments.of("Long wrapper → ClassCastException", Long.valueOf(100L), ClassCastException.class), Arguments.of("Double wrapper → ClassCastException", Double.valueOf(10.0), ClassCastException.class), Arguments.of("Boolean.TRUE → ClassCastException", Boolean.TRUE, ClassCastException.class), Arguments.of("Boolean.FALSE → ClassCastException", Boolean.FALSE, ClassCastException.class), Arguments.of("Float wrapper → ClassCastException", Float.valueOf(3.14f), ClassCastException.class), Arguments.of("Arbitrary Object → ClassCastException", new Object(), ClassCastException.class), Arguments.of("char[] array → ClassCastException", new char[] { 'x' }, ClassCastException.class), Arguments.of("StringBuilder (CharSequence) → ClassCastException", new StringBuilder("Z"), ClassCastException.class), Arguments.of("Enum constant → ClassCastException", TestEnum.VALUE, ClassCastException.class));
    }

    @Test
    void toShort_WithCharacter_returnsCorrectShort() {
        // given
        // Unicode code point 65
        Character input = 'A';
        // when
        short result = CharacterConversions.toShort(input, converter);
        // then
        assertEquals((short) 'A', result, "The short value should equal the char code point");
    }

    @Test
    void toShort_WithPrimitiveChar_returnsCorrectShort() {
        // given
        // Unicode code point 90
        char input = 'Z';
        // when
        short result = CharacterConversions.toShort(input, converter);
        // then
        assertEquals((short) 'Z', result);
    }

    @Test
    void toShort_WithNull_throwsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toShort(input, converter));
    }

    @Test
    void toShort_WithNonCharObject_throwsClassCastException() {
        // given
        Integer input = 12345;
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toShort(input, converter));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validCharacterProvider")
    void toShort_ValidCharacters(String description, Object from, short expected) {
        short actual = CharacterConversions.toShort(from, null);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("characterObjectProvider")
    void toShort_CharacterObjects(String description, Object from, short expected) {
        short actual = CharacterConversions.toShort(from, null);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void toShort_ExceptionCases(String description, Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> CharacterConversions.toShort(from, null), description);
    }

    @Test
    void toShort_ConverterIgnored_NullConverter() {
        short result = CharacterConversions.toShort('B', null);
        assertEquals((short) 'B', result, "Converter null should be ignored");
    }

    @Test
    void toShort_ConverterIgnored_DummyConverter() {
        Converter dummy = new DummyConverter();
        short result = CharacterConversions.toShort('B', dummy);
        assertEquals((short) 'B', result, "Converter instance should be ignored");
    }

    @Test
    void toShort_ConverterIgnored_ThrowingConverter() {
        Converter throwing = new ThrowingConverter();
        short result = CharacterConversions.toShort('D', throwing);
        assertEquals((short) 'D', result, "Even a throwing converter should be ignored");
    }

    @Test
    void toShort_PrimitiveCharAutoboxed() {
        // The literal is a primitive char; it will be autoboxed to Character when passed as Object.
        // char literal
        Object input = ('E');
        short result = CharacterConversions.toShort(input, null);
        assertEquals((short) 'E', result);
    }
}
