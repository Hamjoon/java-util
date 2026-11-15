package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toStringBuffer_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} instance used by most tests.
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default options is sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared converter reference.
        converter = null;
    }

    private static Stream<Arguments> nonMapObjectProvider() {
        return Stream.of(Arguments.of("plainString", "plainString"), Arguments.of(12345, "12345"));
    }

    @Test
    void toStringBuffer_givenNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        // THEN
        assertNull(result, "Converting a null source should produce a null StringBuffer");
    }

    @Test
    void toStringBuffer_givenString_returnsStringBufferWithSameContent() {
        // GIVEN
        String source = "Hello, World!";
        // WHEN
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        // THEN
        assertNotNull(result, "Result should not be null for a non‑null source");
        assertEquals(source, result.toString(), "StringBuffer content should match the original String");
    }

    @Test
    void toStringBuffer_givenStringBuilder_returnsStringBufferWithSameContent() {
        // GIVEN
        StringBuilder source = new StringBuilder("Builder content");
        // WHEN
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        // THEN
        assertNotNull(result);
        assertEquals(source.toString(), result.toString(), "StringBuffer should contain the same characters as the StringBuilder source");
    }

    @Test
    void toStringBuffer_givenStringBuffer_returnsSameInstance() {
        // GIVEN
        StringBuffer source = new StringBuffer("Existing buffer");
        // WHEN
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        // THEN
        // The conversion framework returns the same instance when the target type matches the source type.
        assertSame(source, result, "When the source is already a StringBuffer, the same instance should be returned");
    }

    @ParameterizedTest(name = "{index}: from={0} => expected={1}")
    @MethodSource("nonMapObjectProvider")
    void toStringBuffer_givenNonMapObject_returnsStringBufferWithToString(Object source, String expectedContent) {
        // WHEN
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        // THEN
        assertNotNull(result);
        assertEquals(expectedContent, result.toString(), "StringBuffer should contain the source object's toString representation");
    }

    @Test
    void toStringBuffer_givenEmptyMap_returnsEmptyMapRepresentation() {
        // GIVEN
        Map<String, Object> source = Collections.emptyMap();
        // WHEN
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        // THEN
        assertNotNull(result);
        assertEquals("{}", result.toString(), "An empty map should be rendered as {}");
    }

    @Test
    void toStringBuffer_givenSimpleMap_returnsMapStringRepresentation() {
        // GIVEN
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("key1", "value1");
        source.put("key2", 2);
        // WHEN
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        // THEN
        assertEquals("{key1=value1, key2=2}", result.toString(), "Simple map should be rendered using its standard toString format");
    }

    @Test
    void toStringBuffer_givenNestedMap_returnsNestedRepresentation() {
        // GIVEN
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("innerKey", "innerValue");
        Map<String, Object> outer = new LinkedHashMap<>();
        outer.put("outer", inner);
        // WHEN
        StringBuffer result = MapConversions.toStringBuffer(outer, realConverter);
        // THEN
        assertEquals("{outer={innerKey=innerValue}}", result.toString(), "Nested maps should be rendered recursively");
    }

    @Test
    void toStringBuffer_givenMapWithSpecialKeys_returnsStandardRepresentation() {
        // GIVEN
        Map<String, Object> source = new LinkedHashMap<>();
        source.put(MapConversions.VALUE, "val");
        source.put(MapConversions.ID, "123");
        // WHEN
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        // THEN
        assertEquals("{value=val, id=123}", result.toString(), "Constant‑named keys should appear like any other key");
    }

    @Test
    void toStringBuffer_givenMapWithNullValues_representsNullAsString() {
        // GIVEN
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("present", "yes");
        source.put("absent", null);
        // WHEN
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        // THEN
        assertEquals("{present=yes, absent=null}", result.toString(), "Null values should be rendered as the literal string \"null\"");
    }

    @Test
    void toStringBuffer_givenMapWithVariousPrimitiveTypes_rendersEachValue() {
        // GIVEN
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("bool", true);
        source.put("dbl", 3.14);
        source.put("lng", 1234567890123L);
        source.put("chr", 'c');
        // WHEN
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        // THEN
        assertEquals("{bool=true, dbl=3.14, lng=1234567890123, chr=c}", result.toString(), "Primitive wrapper values should use their standard toString output");
    }

    @Test
    void toStringBuffer_givenMapWithSpecialObjectKeys_rendersObjectsViaToString() {
        // GIVEN
        Map<String, Object> source = new LinkedHashMap<>();
        source.put(MapConversions.CLASS, Date.class);
        source.put(MapConversions.MESSAGE, "error occurred");
        source.put(MapConversions.CAUSE, new RuntimeException("boom"));
        // WHEN
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        // THEN
        // The Class object's toString is "class java.util.Date"
        // The exception's toString includes its class name and message.
        assertEquals("{class=class java.util.Date, message=error occurred, cause=java.lang.RuntimeException: boom}", result.toString(), "Object values should be rendered using their default toString implementations");
    }

    @Test
    void toStringBuffer_givenMapWithCollectionValues_rendersCollections() {
        // GIVEN
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("list", List.of(1, 2, 3));
        source.put("set", Set.of("a", "b"));
        // WHEN
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        // THEN
        // List's toString → [1, 2, 3]; Set's toString → [a, b] (order is deterministic for Set.of)
        assertEquals("{list=[1, 2, 3], set=[a, b]}", result.toString(), "Collection values should be rendered via their own toString output");
    }

    @Test
    void toStringBuffer_givenMapWithNonStringKeys_rendersKeyToString() {
        // GIVEN
        Map<Object, Object> source = new LinkedHashMap<>();
        source.put(5, "five");
        source.put(true, "bool");
        // WHEN
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        // THEN
        // Insertion order is preserved.
        assertEquals("{5=five, true=bool}", result.toString(), "Non‑String keys should be converted using their toString values");
    }

    @Test
    void toStringBuffer_givenMapAndConverterTransformsValues_usesConvertedValue(@Mock Converter mockConverter) {
        // GIVEN
        LocalDate date = LocalDate.of(2023, 1, 15);
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("date", date);
        // Mock the converter to return a formatted date string when asked to convert a LocalDate to String.
        when(mockConverter.convert(eq(date), eq(String.class))).thenReturn("2023-01-15");
        // WHEN
        StringBuffer result = MapConversions.toStringBuffer(source, mockConverter);
        // THEN
        assertEquals("{date=2023-01-15}", result.toString(), "The mock converter's transformed value should appear in the output");
        // Verify that the converter was indeed invoked for the LocalDate value.
        verify(mockConverter, times(1)).convert(eq(date), eq(String.class));
    }

    @Test
    void toStringBuffer_givenNullConverter_throwsNullPointerException() {
        // GIVEN
        Map<String, Object> source = Map.of("key", "value");
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toStringBuffer(source, nullConverter), "Supplying a null Converter should result in a NullPointerException");
    }

    @Test
    void toStringBuffer_givenStringBuilderInput_returnsStringBufferWithSameContent() {
        // GIVEN
        StringBuilder source = new StringBuilder("builder content");
        // WHEN
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        // THEN
        assertNotNull(result);
        assertEquals("builder content", result.toString(), "StringBuilder input should be converted to a StringBuffer containing the same characters");
    }
}
