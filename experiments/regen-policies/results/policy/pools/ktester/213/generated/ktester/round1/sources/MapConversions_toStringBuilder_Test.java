package com.cedarsoftware.util.convert;

import org.mockito.Mock;
import java.util.*;
import static org.mockito.Mockito.*;
// import org.junit.*;
// import static org.junit.Assert.*;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class MapConversions_toStringBuilder_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared real Converter used for the majority of tests.
     */
    private static Converter realConverter;

    @Mock
    Converter // injected by MockitoExtension
    mockConverter;

//     @BeforeAll
    static void setupBeforeAll() {
        // The Converter class now requires a ConverterOptions instance.
        // Passing a default‑constructed options object satisfies the constructor.
//         converter = new Converter(new ConverterOptions());
//         realConverter = new Converter(new ConverterOptions());
    }

//     @AfterAll
    static void teardownAfterAll() {
        converter = null;
        realConverter = null;
    }

//     @Test
//     void toStringBuilderFromMapUsingValueKey() {
//         Map<String, Object> map = new LinkedHashMap<>();
//         map.put("value", "hello world");
//         StringBuilder sb = MapConversions.toStringBuilder(map, converter);
//         assertNotNull("Resulting StringBuilder should not be null", sb);
//         assertEquals("hello world", sb.toString(), "StringBuilder should contain the string representation of the 'value' entry");
//     }

//     @Test
//     void toStringBuilderFromMapUsingVKey() {
//         Map<String, Object> map = new LinkedHashMap<>();
//         map.put("_v", 12345);
//         StringBuilder sb = MapConversions.toStringBuilder(map, converter);
//         assertNotNull("Resulting StringBuilder should not be null", sb);
//         assertEquals("12345", sb.toString(), "StringBuilder should contain the string representation of the '_v' entry");
//     }

//     @Test
//     void toStringBuilderFromNonMapObject() {
//         Integer source = 42;
//         StringBuilder sb = MapConversions.toStringBuilder(source, converter);
//         assertNotNull("Resulting StringBuilder should not be null", sb);
//         assertEquals("42", sb.toString(), "StringBuilder should contain the string representation of the source object");
//     }

//     @Test
//     void toStringBuilderFromMapWithoutValueKeysFallsBackToToString() {
//         Map<String, Object> map = new LinkedHashMap<>();
//         map.put("otherKey", "ignored");
//         StringBuilder sb = MapConversions.toStringBuilder(map, converter);
//         assertNotNull("Resulting StringBuilder should not be null", sb);
//         assertEquals(map.toString(), sb.toString(), "When no value keys are present, the map's toString() should be used");
//     }

//     @Test
//     void toStringBuilder_NullSourceAndNullConverter_ReturnsEmptyBuilder() {
//         StringBuilder sb = MapConversions.toStringBuilder(null, null);
//         assertNotNull("Result must never be null", sb);
//         assertEquals(0, sb.length(), "Empty StringBuilder expected for null source");
//     }

//     @ParameterizedTest(name = "Scalar {0} should become \"{1}\"")
//     @CsvSource({ "42, 42", "true, true" })
//     void toStringBuilder_ScalarTypes(Object source, String expected) {
//         // WHEN
//         StringBuilder sb = MapConversions.toStringBuilder(source, realConverter);
//         // THEN
//         assertNotNull(sb);
//         assertEquals(expected, sb.toString());
//     }

//     @Test
//     void toStringBuilder_MapValueKeyNull_ReturnsLiteralNull() {
//         Map<String, Object> map = new LinkedHashMap<>();
//         map.put("value", null);
//         map.put("other", "ignore");
//         StringBuilder sb = MapConversions.toStringBuilder(map, realConverter);
//         assertEquals("null", sb.toString(), "When the 'value' entry is null the literal \"null\" must be emitted");
//     }

//     @Test
//     void toStringBuilder_MapVKeyNull_ReturnsLiteralNull() {
//         Map<String, Object> map = new LinkedHashMap<>();
//         map.put("_v", null);
//         StringBuilder sb = MapConversions.toStringBuilder(map, realConverter);
//         assertEquals("null", sb.toString(), "When the '_v' entry is null the literal \"null\" must be emitted");
//     }

//     @Test
//     void toStringBuilder_MapBothKeys_ValueNullTakesPrecedence() {
//         Map<String, Object> map = new LinkedHashMap<>();
//         map.put("value", null);
//         map.put("_v", "fallback");
//         StringBuilder sb = MapConversions.toStringBuilder(map, realConverter);
//         assertEquals("null", sb.toString(), "Null from 'value' must win over non‑null '_v'");
//     }

//     @Test
//     void toStringBuilder_CustomConverter_DoublesInteger() {
//         when(mockConverter.convert(eq(5), eq(StringBuilder.class))).thenReturn(new StringBuilder("10"));
//         StringBuilder sb = MapConversions.toStringBuilder(5, mockConverter);
//         assertEquals("10", sb.toString());
//         verify(mockConverter).convert(eq(5), eq(StringBuilder.class));
//     }

//     @Test
//     void toStringBuilder_CustomConverter_OnMapValue() {
//         Map<String, Object> map = new LinkedHashMap<>();
//         map.put("value", 7);
//         map.put("extra", "x");
//         when(mockConverter.convert(eq(7), eq(StringBuilder.class))).thenReturn(new StringBuilder("num-7"));
//         StringBuilder sb = MapConversions.toStringBuilder(map, mockConverter);
//         assertEquals("num-7", sb.toString());
//         verify(mockConverter).convert(eq(7), eq(StringBuilder.class));
//     }

//     @Test
//     void toStringBuilder_CustomConverter_OnMapVKey() {
//         Map<String, Object> map = new LinkedHashMap<>();
//         map.put("_v", 3);
//         when(mockConverter.convert(eq(3), eq(StringBuilder.class))).thenReturn(// 3 * 5
//         new StringBuilder("15"));
//         StringBuilder sb = MapConversions.toStringBuilder(map, mockConverter);
//         assertEquals("15", sb.toString());
//         verify(mockConverter).convert(eq(3), eq(StringBuilder.class));
//     }

//     @Test
//     void toStringBuilder_CustomConverter_NonMapHexConversion() {
//         when(mockConverter.convert(eq(12345), eq(StringBuilder.class))).thenReturn(new StringBuilder("0x3039"));
//         StringBuilder sb = MapConversions.toStringBuilder(12345, mockConverter);
//         assertEquals("0x3039", sb.toString());
//         verify(mockConverter).convert(eq(12345), eq(StringBuilder.class));
//     }

//     @Test
//     void toStringBuilder_ArrayConvertedViaDefaultConverter() {
//         int[] source = { 1, 2, 3 };
//         StringBuilder sb = MapConversions.toStringBuilder(source, realConverter);
//         assertEquals("[1, 2, 3]", sb.toString(), "Arrays should be rendered using Arrays.toString()");
//     }

//     @Test
//     void toStringBuilder_ListConvertedViaDefaultConverter() {
//         List<String> source = Arrays.asList("a", "b");
//         StringBuilder sb = MapConversions.toStringBuilder(source, realConverter);
//         assertEquals("[a, b]", sb.toString(), "Collections should use their toString() representation");
//     }

//     @Test
//     void toStringBuilder_ConverterThrowsException_PropagatesIt() {
//         when(mockConverter.convert(eq("trigger"), eq(StringBuilder.class))).thenThrow(new IllegalArgumentException("boom"));
//         try {
//             MapConversions.toStringBuilder("trigger", mockConverter);
//             fail("Expected IllegalArgumentException to be thrown");
//         } catch (IllegalArgumentException ex) {
//             assertEquals("boom", ex.getMessage());
//         }
//     }

//     @Test
//     void toStringBuilder_AlreadyStringBuilder_ReturnsSameInstance() {
//         StringBuilder original = new StringBuilder("sb");
//         StringBuilder result = MapConversions.toStringBuilder(original, realConverter);
//         assertSame(original, result);
//     }

//     @Test
//     @SuppressWarnings("unchecked")
//     void toStringBuilder_MapWithNonStringKey_ThrowsClassCastException() {
//         // Raw map with an Integer key; we deliberately avoid generic typing
//         Map rawMap = new LinkedHashMap();
//         rawMap.put(1, "numericKey");
//         rawMap.put("value", "shouldBeIgnored");
//         try {
//             MapConversions.toStringBuilder(rawMap, realConverter);
//             fail("Expected ClassCastException");
//         } catch (ClassCastException ignored) {
//             // expected
//         }
//     }

//     @Test
//     public void toStringBuilder_ScalarInteger() {
//         StringBuilder sb = MapConversions.toStringBuilder(42, realConverter);
//         assertNotNull(sb);
//         assertEquals("42", sb.toString());
//     }

//     @Test
//     public void toStringBuilder_ScalarBoolean() {
//         StringBuilder sb = MapConversions.toStringBuilder(true, realConverter);
//         assertNotNull(sb);
//         assertEquals("true", sb.toString());
//     }
}