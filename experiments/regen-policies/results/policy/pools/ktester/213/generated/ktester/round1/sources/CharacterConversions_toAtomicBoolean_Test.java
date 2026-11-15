package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

class CharacterConversions_toAtomicBoolean_Test {

    private Converter converter;

    /**
     * A dummy class whose {@code toString()} deliberately throws a {@link RuntimeException}.
     */
    private static final class BadToString {

        @Override
        public String toString() {
            throw new RuntimeException("toString failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the ConverterOptions class provides a no‑arg constructor
        // that yields the standard conversion behaviour.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no state leaks between tests
        converter = null;
    }

    private static Stream<Arguments> standardAndNumericConversions() {
        return Stream.of(// ----- standard type conversions -----
        arguments("Boolean TRUE → true", Boolean.TRUE, null, true), arguments("Boolean FALSE → false", Boolean.FALSE, null, false), arguments("String \"true\" → true (case‑insensitive)", "true", null, true), arguments("String \"FALSE\" → false (case‑insensitive)", "FALSE", null, false), arguments("String \"yes\" → true", "yes", null, true), arguments("String \"no\" → false", "no", null, false), arguments("String \"1\" → true", "1", null, true), arguments("String \"0\" → false", "0", null, false), arguments("Integer 1 → true (non‑zero)", Integer.valueOf(1), null, true), arguments("Integer 0 → false (zero)", Integer.valueOf(0), null, false), arguments("null source → false (default fallback)", null, null, false), // ----- numeric type conversions -----
        arguments("Long 42L → true (non‑zero)", Long.valueOf(42L), null, true), arguments("Long 0L → false (zero)", Long.valueOf(0L), null, false), arguments("Short 5 → true (non‑zero)", Short.valueOf((short) 5), null, true), arguments("Byte 0 → false (zero)", Byte.valueOf((byte) 0), null, false), arguments("Double 0.0001 → true (non‑zero)", Double.valueOf(0.0001), null, true), arguments("Double 0.0 → false (zero)", Double.valueOf(0.0), null, false), arguments("Float 3.14f → true (non‑zero)", Float.valueOf(3.14f), null, true), arguments("Float 0.0f → false (zero)", Float.valueOf(0.0f), null, false), arguments("BigInteger 123456789 → true (non‑zero)", BigInteger.valueOf(123456789L), null, true), arguments("BigInteger ZERO → false (zero)", BigInteger.ZERO, null, false), arguments("BigDecimal 0.01 → true (non‑zero)", BigDecimal.valueOf(0.01), null, true), arguments("BigDecimal ZERO → false (zero)", BigDecimal.ZERO, null, false));
    }

    private static Stream<Arguments> stringEdgeCases() {
        return Stream.of(arguments("Mixed‑case \"TrUe\" → true (case‑insensitive)", "TrUe", true), arguments("String with surrounding whitespace \"  true  \" → true", "  true  ", true), arguments("Numeric string with spaces \" 0 \" → false", " 0 ", false), arguments("String \"yes \" with trailing space → true", "yes ", true), arguments("Empty string \"\" → false (default)", "", false), arguments("String containing only whitespace \"   \" → false (default)", "   ", false), arguments("Unrecognised token \"maybe\" → false (fallback)", "maybe", false));
    }

    private static Stream<Arguments> characterAndObjectCases() {
        return Stream.of(arguments("Character 'Y' → true", Character.valueOf('Y'), true), arguments("Character 'n' → false", Character.valueOf('n'), false), arguments("Unsupported Date object → false (fallback)", new java.util.Date(0), false));
    }

    @Test
    void toAtomicBoolean_fromBooleanTrue_returnsTrue() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should contain true for Boolean.TRUE input");
    }

    @Test
    void toAtomicBoolean_fromBooleanFalse_returnsFalse() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should contain false for Boolean.FALSE input");
    }

    @Test
    void toAtomicBoolean_fromStringTrue_returnsTrue() {
        // given
        String source = "true";
        // when
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should contain true for string \"true\"");
    }

    @Test
    void toAtomicBoolean_fromStringFalse_returnsFalse() {
        // given
        String source = "false";
        // when
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should contain false for string \"false\"");
    }

    @Test
    void toAtomicBoolean_fromIntegerOne_returnsTrue() {
        // given
        Integer source = 1;
        // when
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should be true for non‑zero numeric input");
    }

    @Test
    void toAtomicBoolean_fromIntegerZero_returnsFalse() {
        // given
        Integer source = 0;
        // when
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false for zero numeric input");
    }

    @Test
    void toAtomicBoolean_fromNull_returnsFalse() {
        // given
        Object source = null;
        // when
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false when source is null");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("standardAndNumericConversions")
    void toAtomicBoolean_standardAndNumeric(String description, Object source, Converter conv, boolean expected) {
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, conv);
        assertNotNull(result, "Result must never be null");
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringEdgeCases")
    void toAtomicBoolean_stringEdgeCases(String description, String source, boolean expected) {
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("characterAndObjectCases")
    void toAtomicBoolean_characterAndObject(String description, Object source, boolean expected) {
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    @Test
    void toAtomicBoolean_customAlwaysTrueConverter() {
        // a converter that always returns true regardless of input
        Converter alwaysTrue = mock(Converter.class);
        when(alwaysTrue.convert(any(), eq(Boolean.class))).thenReturn(Boolean.TRUE);
        AtomicBoolean result = CharacterConversions.toAtomicBoolean("anyString", alwaysTrue);
        assertNotNull(result);
        assertTrue(result.get(), "Custom always‑true converter must force true");
    }

    @Test
    void toAtomicBoolean_customAlwaysFalseConverter() {
        // a converter that always returns false regardless of input
        Converter alwaysFalse = mock(Converter.class);
        when(alwaysFalse.convert(any(), eq(Boolean.class))).thenReturn(Boolean.FALSE);
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(Integer.valueOf(0), alwaysFalse);
        assertNotNull(result);
        assertFalse(result.get(), "Custom always‑false converter must force false");
    }

    @Test
    void toAtomicBoolean_converterThrowsRuntimeException_string() {
        Converter throwing = mock(Converter.class);
        when(throwing.convert(any(), eq(Boolean.class))).thenThrow(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> CharacterConversions.toAtomicBoolean("anyValue", throwing));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void toAtomicBoolean_converterThrowsRuntimeException_number() {
        Converter throwing = mock(Converter.class);
        when(throwing.convert(any(), eq(Boolean.class))).thenThrow(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> CharacterConversions.toAtomicBoolean(Integer.valueOf(5), throwing));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void toAtomicBoolean_arraySourceThrowsIllegalArgumentException() {
        int[] source = new int[] { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> CharacterConversions.toAtomicBoolean(source, converter), "Array source should trigger IllegalArgumentException");
    }

    @Test
    void toAtomicBoolean_mapSourceThrowsIllegalArgumentException() {
        java.util.Map<String, String> source = new java.util.HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> CharacterConversions.toAtomicBoolean(source, converter), "Map source should trigger IllegalArgumentException");
    }

    @Test
    void toAtomicBoolean_genericObjectWithoutConverterThrowsIllegalArgumentException() {
        Object source = new Object();
        assertThrows(IllegalArgumentException.class, () -> CharacterConversions.toAtomicBoolean(source, null), "Generic Object without a custom converter should trigger IllegalArgumentException");
    }

    @Test
    void toAtomicBoolean_badToStringThrowsRuntimeException() {
        BadToString source = new BadToString();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> CharacterConversions.toAtomicBoolean(source, converter));
        assertEquals("toString failure", ex.getMessage());
    }
}
