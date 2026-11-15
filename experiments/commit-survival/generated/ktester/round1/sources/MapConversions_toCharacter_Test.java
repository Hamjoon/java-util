package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

class MapConversions_toCharacter_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter needs a ConverterOptions instance – the default constructor provides
        // an empty set of overrides which is sufficient for these simple map‑to‑character tests.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Direct Character instance
        Arguments.of(new Character('a'), 'a'), // Single‑character String
        Arguments.of("Z", 'Z'), // Integer ASCII code
        Arguments.of(65, 'A'), // Map with primary key "value"
        Arguments.of(Map.of("value", "b"), 'b'), // Map with alternate key "_v"
        Arguments.of(Map.of("_v", "c"), 'c'), // Map with numeric code point
        Arguments.of(Map.of("value", 68), 'D'), // Additional numeric wrappers
        // wrapper Integer
        Arguments.of(Integer.valueOf(97), 'a'), // wrapper Long
        Arguments.of(Long.valueOf(66L), 'B'), // wrapper Short
        Arguments.of(Short.valueOf((short) 67), 'C'), // wrapper Byte
        Arguments.of(Byte.valueOf((byte) 68), 'D'), // BigInteger
        Arguments.of(new BigInteger("69"), 'E'), // BigDecimal
        Arguments.of(new BigDecimal("70"), 'F'), // CharSequence implementations
        Arguments.of(new StringBuilder("g"), 'g'), Arguments.of(new StringBuffer("h"), 'h'), // Map containing a Character object
        Arguments.of(Map.of("value", new Character('i')), 'i'), // Map with both recognised keys – primary wins
        Arguments.of(Map.of("value", "j", "_v", "k"), 'j'), // Space character
        Arguments.of(Character.valueOf(' '), ' '), // Extreme int values – low 16 bits are taken
        Arguments.of(Integer.MAX_VALUE, (char) Integer.MAX_VALUE), Arguments.of(Integer.MIN_VALUE, (char) Integer.MIN_VALUE));
    }

    private static Stream<Arguments> illegalArgumentProvider() {
        return Stream.of(// Empty String
        Arguments.of(""), // String longer than one character
        Arguments.of("TooLong"), // Map entry with multi‑character String
        Arguments.of(Map.of("value", "ab")), // Map without recognised keys
        Arguments.of(Map.of("other", "x")), // Map entry "value" with null
        Arguments.of(Map.of("value", null)), // Empty StringBuilder
        Arguments.of(new StringBuilder("")), // StringBuilder with >1 char
        Arguments.of(new StringBuilder("ab")), // CharBuffer longer than one character
        Arguments.of(java.nio.CharBuffer.wrap("xy")), // Map where primary key is invalid even though secondary is ok
        Arguments.of(Map.of("value", "ab", "_v", "c")));
    }

    private static Stream<Arguments> classCastProvider() {
        return Stream.of(// Boolean value
        Arguments.of(true), // Map entry with Boolean
        Arguments.of(Map.of("value", true)), // Double (non‑integral numeric)
        Arguments.of(new Double(71.5)), // Arbitrary Object
        Arguments.of(new Object()), // Float with non‑integral value
        Arguments.of(new Float(72.5f)), // Primitive char array
        Arguments.of(new char[] { 'z' }), // Empty primitive char array
        Arguments.of(new char[] {}), // AtomicInteger (unsupported)
        Arguments.of(java.util.concurrent.atomic.AtomicInteger.valueOf(73)));
    }

    @Test
    void toCharacter_fromMapWithStringValue_returnsCharacter() {
        Map<String, Object> source = new HashMap<>();
        // a single‑character string
        source.put("value", "Z");
        Character result = MapConversions.toCharacter(source, converter);
        assertNotNull(result);
        assertEquals('Z', result.charValue());
    }

    @Test
    void toCharacter_fromMapWithNumberValue_returnsCharacter() {
        Map<String, Object> source = new HashMap<>();
        // ASCII code for 'A'
        source.put("value", 65);
        Character result = MapConversions.toCharacter(source, converter);
        assertNotNull(result);
        assertEquals('A', result.charValue());
    }

    @Test
    void toCharacter_fromMapWithCharacterValue_returnsSameCharacter() {
        Map<String, Object> source = new HashMap<>();
        // a Character instance
        source.put("value", 'B');
        Character result = MapConversions.toCharacter(source, converter);
        assertNotNull(result);
        assertEquals('B', result.charValue());
    }

    @Test
    void toCharacter_nullInput_returnsNull() {
        Character result = MapConversions.toCharacter(null, converter);
        assertNull(result);
    }

    @ParameterizedTest(name = "[{index}] Valid conversion: {1}")
    @MethodSource("validConversionProvider")
    void toCharacter_validConversions(Object from, Character expected) {
        Character actual = MapConversions.toCharacter(from, converter);
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] Invalid conversion (IllegalArgumentException): {0}")
    @MethodSource("illegalArgumentProvider")
    void toCharacter_illegalArgument(Object from) {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toCharacter(from, converter));
    }

    @ParameterizedTest(name = "[{index}] Invalid conversion (ClassCastException): {0}")
    @MethodSource("classCastProvider")
    void toCharacter_classCast(Object from) {
        assertThrows(ClassCastException.class, () -> MapConversions.toCharacter(from, converter));
    }

    @Test
    void toCharacter_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toCharacter(null, converter));
    }

    @Test
    void toCharacter_nullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toCharacter("x", null));
    }
}
