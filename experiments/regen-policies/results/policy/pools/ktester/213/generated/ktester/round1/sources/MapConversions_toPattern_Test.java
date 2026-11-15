package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class MapConversions_toPattern_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared real {@link Converter} instance used by most tests.
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter class has a no‑arg constructor that creates default options.
//         converter = new Converter();
//         realConverter = new Converter();
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
        realConverter = null;
    }

    /**
     * Verify that a map containing the key {@code "value"} with a regular‑expression
     * string is correctly converted to a {@link Pattern}.
     */
    @Test
    void toPattern_usesValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "^test\\d+$");
        Pattern result = MapConversions.toPattern(map, converter);
        assertNotNull(result, "Resulting Pattern should not be null");
        assertEquals("^test\\d+$", result.pattern(), "Pattern compiled from the 'value' key does not match the expected regex");
    }

    /**
     * Verify that a map containing the alternative key {@code "_v"} works the same way.
     */
    @Test
    void toPattern_usesVKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "a+b?c*");
        Pattern result = MapConversions.toPattern(map, converter);
        assertNotNull(result, "Resulting Pattern should not be null");
        assertEquals("a+b?c*", result.pattern(), "Pattern compiled from the '_v' key does not match the expected regex");
    }

    /**
     * If the source object is already a {@link Pattern}, the method should simply
     * return that instance (no conversion required).
     */
    @Test
    void toPattern_givenPatternReturnsSameInstance() {
        Pattern original = Pattern.compile("^abc$");
        Pattern result = MapConversions.toPattern(original, realConverter);
        assertSame(original, result, "When a Pattern is supplied the same instance must be returned");
    }

    /**
     * When the source is not a {@link Map} nor a {@link Pattern}, the conversion
     * should yield {@code null} (the underlying {@code dispatch} method returns null
     * for unsupported inputs).
     */
    @Test
    void toPattern_unsupportedSourceReturnsNull() {
        String notAMap = "not a map";
        Pattern result = MapConversions.toPattern(notAMap, converter);
        assertNull(result, "Conversion of an unsupported source should return null");
    }

    @Test
    void toPattern_stringIsConvertedViaConverter() {
        String regex = "[0-9]+";
        Pattern result = MapConversions.toPattern(regex, realConverter);
        assertNotNull(result);
        assertEquals(regex, result.pattern());
    }

    @Test
    void toPattern_mapWithValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "\\d{3}-\\d{2}-\\d{4}");
        Pattern result = MapConversions.toPattern(map, realConverter);
        assertNotNull(result);
        assertEquals("\\d{3}-\\d{2}-\\d{4}", result.pattern());
    }

    @Test
    void toPattern_mapWithVKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "a+b*");
        Pattern result = MapConversions.toPattern(map, realConverter);
        assertNotNull(result);
        assertEquals("a+b*", result.pattern());
    }

    @Test
    void toPattern_mapPrefersValueKeyWhenBothPresent() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "first");
        map.put("_v", "second");
        Pattern result = MapConversions.toPattern(map, realConverter);
        assertNotNull(result);
        assertEquals("first", result.pattern());
    }

    @Test
    void toPattern_mapPrimaryNullFallsBackToV() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", null);
        map.put("_v", "fallback");
        Pattern result = MapConversions.toPattern(map, realConverter);
        assertNotNull(result);
        assertEquals("fallback", result.pattern());
    }

    @Test
    void toPattern_mapPrimaryEmptyStringTakesPrecedence() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "");
        map.put("_v", "ignored");
        Pattern result = MapConversions.toPattern(map, realConverter);
        assertNotNull(result);
        assertEquals("", result.pattern());
    }

    @Test
    void toPattern_mapValueIsAlreadyPattern() {
        Pattern stored = Pattern.compile("abc");
        Map<String, Object> map = new HashMap<>();
        map.put("value", stored);
        Pattern result = MapConversions.toPattern(map, realConverter);
        assertSame(stored, result, "When the map value is already a Pattern the same instance must be returned");
    }

    @Test
    void toPattern_stringBuilderIsAccepted() {
        StringBuilder sb = new StringBuilder("[A-Z]+");
        Pattern result = MapConversions.toPattern(sb, realConverter);
        assertNotNull(result);
        assertEquals("[A-Z]+", result.pattern());
    }

    @Test
    void toPattern_nullSourceReturnsNull() {
        Pattern result = MapConversions.toPattern(null, realConverter);
        assertNull(result, "Null source must result in null");
    }

    @Test
    void toPattern_nullConverterThrowsNPE() {
        assertThrows(NullPointerException.class, () -> MapConversions.toPattern("[a-z]+", null), "A null Converter argument must cause NPE");
    }

//     @Test
//     void toPattern_intArrayCausesConversionException() {
//         int[] source = new int[] { 1, 2, 3 };
//         assertThrows(ConversionException.class, () -> MapConversions.toPattern(source, realConverter), "Unsupported source type should result in ConversionException");
//     }

//     @Test
//     void toPattern_listCausesConversionException() {
//         List<Integer> list = Arrays.asList(1, 2, 3);
//         assertThrows(ConversionException.class, () -> MapConversions.toPattern(list, realConverter), "List source should result in ConversionException");
//     }

//     @Test
//     void toPattern_mapValueNotCharSequenceCausesConversionException() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("value", 12345);
//         assertThrows(ConversionException.class, () -> MapConversions.toPattern(map, realConverter), "Non‑CharSequence map value must cause ConversionException");
//     }

//     @Test
//     void toPattern_primaryNonStringDoesNotFallbackToV() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("value", 12345);
//         map.put("_v", "fallback");
//         assertThrows(ConversionException.class, () -> MapConversions.toPattern(map, realConverter), "When primary key maps to non‑String the method must not silently fallback");
//     }

//     @Test
//     void toPattern_emptyMapThrowsConversionException() {
//         Map<String, Object> empty = new HashMap<>();
//         assertThrows(ConversionException.class, () -> MapConversions.toPattern(empty, realConverter), "Empty map should trigger ConversionException");
//     }

//     @Test
//     void toPattern_mapWithoutRecognisedKeysThrowsConversionException() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("other", "abc");
//         assertThrows(ConversionException.class, () -> MapConversions.toPattern(map, realConverter), "Map without 'value' or '_v' must cause ConversionException");
//     }

    @Test
    void toPattern_invalidRegexThrowsPatternSyntaxException() {
        String badRegex = "[unclosed";
        assertThrows(PatternSyntaxException.class, () -> MapConversions.toPattern(badRegex, realConverter), "Invalid regex must propagate PatternSyntaxException");
    }

//     @Test
//     void toPattern_uuidIsRejected() {
//         UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
//         assertThrows(ConversionException.class, () -> MapConversions.toPattern(uuid, realConverter), "UUID cannot be converted to Pattern and must cause ConversionException");
//     }

    @Test
    void toPattern_delegatesToConverterAndReturnsItsResult(@Mock Converter mockConverter) {
        // Arrange – mock returns a pre‑compiled pattern when asked to convert the string.
        String regex = "\\d{2}";
        Pattern expected = Pattern.compile(regex);
        when(mockConverter.convert(eq(regex), eq(Pattern.class))).thenReturn(expected);
        // Act
        Pattern result = MapConversions.toPattern(regex, mockConverter);
        // Assert – verify delegation and result.
        verify(mockConverter, times(1)).convert(eq(regex), eq(Pattern.class));
        assertSame(expected, result, "Result must be the exact Pattern produced by the mock Converter");
    }

//     @Test
//     void toPattern_converterThrowsConversionExceptionPropagates(@Mock Converter mockConverter) {
//         String regex = "validRegex";
//         when(mockConverter.convert(eq(regex), eq(Pattern.class))).thenThrow(new ConversionException("forced failure"));
//         assertThrows(ConversionException.class, () -> MapConversions.toPattern(regex, mockConverter), "ConversionException from the Converter must propagate");
//     }

    @Test
    void toPattern_converterThrowsUncheckedExceptionPropagates(@Mock Converter mockConverter) {
        String any = "any";
        when(mockConverter.convert(eq(any), eq(Pattern.class))).thenThrow(new NullPointerException("forced NPE"));
        assertThrows(NullPointerException.class, () -> MapConversions.toPattern(any, mockConverter), "Unchecked exceptions from the Converter must propagate unchanged");
    }

    @Test
    void toPattern_bothKeysMapToNullReturnsNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", null);
        map.put("_v", null);
        Pattern result = MapConversions.toPattern(map, realConverter);
        assertNull(result, "When both recognised keys map to null the method should return null");
    }
}