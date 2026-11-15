package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.time.LocalDate;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toStringBuffer_Test {

    /**
     * The Converter used for all test cases.
     */
    private Converter converter;

    /**
     * A mock {@link Converter} used for tests that need custom conversion behaviour.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple POJO used for bean‑to‑map conversion tests.
     */
    public static class TestBean {

        private final String name;

        private final int age;

        public TestBean(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }

    /**
     * Bean whose getter deliberately throws an exception.
     */
    public static class FaultyBean {

        public String getProblem() {
            throw new RuntimeException("getter failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The real Converter needs a non‑null options instance; the default options are fine for our tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the GC – not strictly required but keeps the pattern consistent.
        converter = null;
        mockConverter = null;
    }

    private static Stream<Arguments> nullAndSimpleProvider() {
        return Stream.of(// both arguments null → literal "null"
        Arguments.of(null, null, "null"), // plain string with null converter → unchanged string
        Arguments.of("plain string", null, "plain string"));
    }

    /**
     * Verify that a map containing the special key {@code "_v"} (the constant {@code V} in {@code MapConversions})
     * is converted to a {@link StringBuffer} containing the associated value.
     */
    @Test
    void toStringBuffer_FromMapWithVKey() {
        // given
        Map<String, Object> source = new CompactMap<>();
        source.put(MapConversions.V, "hello world");
        // when
        StringBuffer result = MapConversions.toStringBuffer(source, converter);
        // then
        assertNotNull(result, "Resulting StringBuffer should not be null");
        assertEquals("hello world", result.toString(), "StringBuffer should contain the value associated with the \"_v\" key");
    }

    /**
     * Verify that a map containing the generic {@code "value"} key is also converted correctly.
     * This mirrors the behaviour of {@code MapConversions} for types that fall back to the {@code "value"} key.
     */
    @Test
    void toStringBuffer_FromMapWithValueKey() {
        // given
        Map<String, Object> source = new CompactMap<>();
        source.put(MapConversions.VALUE, "value‑only");
        // when
        StringBuffer result = MapConversions.toStringBuffer(source, converter);
        // then
        assertNotNull(result);
        assertEquals("value‑only", result.toString());
    }

    /**
     * When the source object is already a {@link CharSequence} (e.g., a {@link String}),
     * the conversion should produce a {@link StringBuffer} with the same characters.
     */
    @Test
    void toStringBuffer_FromString() {
        // given
        String source = "just a string";
        // when
        StringBuffer result = MapConversions.toStringBuffer(source, converter);
        // then
        assertNotNull(result);
        assertEquals(source, result.toString());
    }

    /**
     * Passing {@code null} as the source should result in {@code null} – the method does not create an empty buffer.
     */
    @Test
    void toStringBuffer_NullSourceReturnsNull() {
        // when
        StringBuffer result = MapConversions.toStringBuffer(null, converter);
        // then
        assertNull(result, "Converting a null source should return null");
    }

    @ParameterizedTest(name = "{index} => from={0}, converter={1}")
    @MethodSource("nullAndSimpleProvider")
    void toStringBuffer_NullAndSimpleTypes(Object from, Converter conv, String expected) {
        StringBuffer result = MapConversions.toStringBuffer(from, conv);
        assertNotNull(result, "Result should never be null for these cases");
        assertEquals(expected, result.toString());
    }

    @Test
    void toStringBuffer_LinkedHashMapPreservesOrder() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("alpha", 10);
        source.put("beta", "text");
        StringBuffer result = MapConversions.toStringBuffer(source, converter);
        assertNotNull(result);
        assertEquals("{alpha=10, beta=text}", result.toString());
    }

    @Test
    void toStringBuffer_HashMapWithNullAndBooleanValues() {
        Map<String, Object> source = new HashMap<>();
        source.put("x", null);
        source.put("y", true);
        // HashMap does not guarantee order; we accept either ordering.
        StringBuffer result = MapConversions.toStringBuffer(source, converter);
        assertNotNull(result);
        String actual = result.toString();
        assertTrue(actual.equals("{x=null, y=true}") || actual.equals("{y=true, x=null}"), "Result should contain both entries with correct literal representations");
    }

    @Test
    void toStringBuffer_EmptyMap() {
        Map<String, Object> source = new HashMap<>();
        StringBuffer result = MapConversions.toStringBuffer(source, converter);
        assertNotNull(result);
        assertEquals("{}", result.toString());
    }

    @Test
    void toStringBuffer_NestedCollectionsAndMaps() {
        Map<String, Object> innerMap = new HashMap<>();
        innerMap.put("a", "A");
        Map<String, Object> source = new HashMap<>();
        source.put("list", List.of(1, 2, 3));
        source.put("innerMap", innerMap);
        StringBuffer result = MapConversions.toStringBuffer(source, converter);
        assertNotNull(result);
        // The exact spacing follows the default toString of collections/maps.
        assertEquals("{list=[1, 2, 3], innerMap={a=A}}", result.toString());
    }

    @Test
    void toStringBuffer_SimpleBeanConversion() {
        TestBean bean = new TestBean("John", 42);
        StringBuffer result = MapConversions.toStringBuffer(bean, converter);
        assertNotNull(result);
        // Bean properties are rendered as a map literal.
        assertEquals("{name=John, age=42}", result.toString());
    }

    @Test
    void toStringBuffer_LocalDateConversion() {
        LocalDate date = LocalDate.of(2023, 5, 17);
        StringBuffer result = MapConversions.toStringBuffer(date, converter);
        assertNotNull(result);
        assertEquals("2023-05-17", result.toString());
    }

    @Test
    void toStringBuffer_UtilDateConversion() {
        Date epoch = new Date(0L);
        // default Date.toString() for epoch 0
        String expected = epoch.toString();
        StringBuffer result = MapConversions.toStringBuffer(epoch, converter);
        assertNotNull(result);
        assertEquals(expected, result.toString());
    }

    @Test
    void toStringBuffer_CustomConverterOverridesConversion() {
        // Mock converter: whenever a conversion to Map is requested, return a predefined map.
        Map<String, Object> customMap = new CompactMap<>();
        customMap.put("customKey", "customValue");
        // The real Converter API expects a call like convert(Object, Class<T>).
        // We stub that method for any source object and a target of Map.class.
        when(mockConverter.convert(any(), eq(Map.class))).thenReturn(customMap);
        StringBuffer result = MapConversions.toStringBuffer("ignored", mockConverter);
        assertNotNull(result);
        assertEquals("{customKey=customValue}", result.toString());
    }

    @Test
    void toStringBuffer_ConverterThrowsExceptionIsPropagated() {
        RuntimeException failure = new RuntimeException("converter failure");
        when(mockConverter.convert(any(), eq(Map.class))).thenThrow(failure);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> MapConversions.toStringBuffer("trigger", mockConverter), "Exception from the converter should be propagated unchanged");
        assertSame(failure, thrown);
    }

    @Test
    void toStringBuffer_ArrayInputThrowsIllegalArgumentException() {
        int[] array = { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toStringBuffer(array, null), "Array inputs are not supported and should cause IllegalArgumentException");
    }

    @Test
    void toStringBuffer_OptionalInputThrowsIllegalArgumentException() {
        Optional<String> optional = Optional.of("value");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toStringBuffer(optional, null), "Optional inputs are not supported and should cause IllegalArgumentException");
    }

    @Test
    void toStringBuffer_NonStringMapKeyThrowsClassCastException() {
        Map<Object, String> source = new HashMap<>();
        source.put(123, "numericKey");
        assertThrows(ClassCastException.class, () -> MapConversions.toStringBuffer(source, null), "Map with non‑String keys should cause ClassCastException");
    }

    @Test
    void toStringBuffer_NullMapKeyThrowsNullPointerException() {
        Map<String, String> source = new HashMap<>();
        source.put(null, "nullKey");
        assertThrows(NullPointerException.class, () -> MapConversions.toStringBuffer(source, null), "Map containing a null key should trigger NullPointerException");
    }

    @Test
    void toStringBuffer_BeanGetterExceptionIsPropagated() {
        FaultyBean bean = new FaultyBean();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> MapConversions.toStringBuffer(bean, null), "Exception thrown by a bean getter should be propagated");
        assertEquals("getter failure", thrown.getMessage());
    }

    @Test
    void toStringBuffer_ConverterReturnsNullMapCausesNullPointerException() {
        // Simulate a converter that returns null instead of a map.
        when(mockConverter.convert(any(), eq(Map.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> MapConversions.toStringBuffer("any", mockConverter), "A null map returned by the converter should cause NullPointerException");
    }
}
