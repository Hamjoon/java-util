package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toStringBuffer_Test {

    private Converter converter;

    /**
     * Real converter used for the majority of tests that rely on the default conversion logic.
     */
    private Converter realConverter;

    /**
     * Mocked converter used for tests that need to control the conversion outcome or exception.
     */
    @Mock
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter class provides a no‑arg constructor that uses default options.
        // This avoids the need to instantiate the abstract ConverterOptions directly.
//         converter = new Converter();
//         realConverter = new Converter();
    }

    @AfterEach
    void teardownAfterEach() {
        converter = null;
        realConverter = null;
    }

    @Test
    void toStringBuffer_fromMapWithValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "hello");
        StringBuffer result = MapConversions.toStringBuffer(map, converter);
        assertNotNull(result, "Result should not be null");
        assertEquals("hello", result.toString(), "StringBuffer should contain the mapped value");
    }

    @Test
    void toStringBuffer_fromMapWithVKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "world");
        StringBuffer result = MapConversions.toStringBuffer(map, converter);
        assertNotNull(result, "Result should not be null");
        assertEquals("world", result.toString(), "StringBuffer should contain the mapped value");
    }

    @Test
    void toStringBuffer_fromPlainString() {
        String source = "plainString";
        StringBuffer result = MapConversions.toStringBuffer(source, converter);
        assertNotNull(result, "Result should not be null");
        assertEquals("plainString", result.toString(), "StringBuffer should contain the original string");
    }

    @Test
    void toStringBuffer_fromNullReturnsNull() {
        StringBuffer result = MapConversions.toStringBuffer(null, converter);
        assertNull(result, "Result should be null when source is null");
    }

    @Test
    void toStringBuffer_nullSource_returnsEmptyStringBuffer() {
        // When the source is null we expect an *empty* StringBuffer (length 0), not a null reference.
        StringBuffer result = MapConversions.toStringBuffer(null, realConverter);
        assertNotNull(result, "Result should never be null");
        assertEquals(0, result.length(), "Empty StringBuffer expected for null source");
    }

    @Test
    void toStringBuffer_nullConverter_throwsNullPointerException() {
        // A null Converter is not permitted – the method should fail fast with NPE.
        assertThrows(NullPointerException.class, () -> MapConversions.toStringBuffer("test", null), "NullPointerException expected when converter is null");
    }

    @Test
    void toStringBuffer_mapWithValueKey_returnsMappedValue() {
        Map<String, Object> map = Map.of("value", "hello", "other", "ignore");
        // Mock converter to simply wrap the incoming value into a StringBuffer.
        when(mockConverter.convert(any(), eq(StringBuffer.class))).thenAnswer(invocation -> new StringBuffer(String.valueOf(invocation.getArgument(0))));
        StringBuffer result = MapConversions.toStringBuffer(map, mockConverter);
        assertNotNull(result);
        assertEquals("hello", result.toString());
        verify(mockConverter, times(1)).convert("hello", StringBuffer.class);
    }

    @Test
    void toStringBuffer_mapWithValueAndVKey_valueTakesPrecedence() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("value", 123);
        map.put("_v", "fallback");
        when(mockConverter.convert(any(), eq(StringBuffer.class))).thenAnswer(invocation -> new StringBuffer(String.valueOf(invocation.getArgument(0))));
        StringBuffer result = MapConversions.toStringBuffer(map, mockConverter);
        assertNotNull(result);
        assertEquals("123", result.toString());
        // Ensure the converter was invoked with the value for the "value" key, not the "_v" key.
        verify(mockConverter, times(1)).convert(123, StringBuffer.class);
    }

    @Test
    void toStringBuffer_mapWithVKeyOnly_returnsVValue() {
        Map<String, Object> map = Map.of("_v", "shortKey", "other", "ignore");
        when(mockConverter.convert(any(), eq(StringBuffer.class))).thenAnswer(invocation -> new StringBuffer(String.valueOf(invocation.getArgument(0))));
        StringBuffer result = MapConversions.toStringBuffer(map, mockConverter);
        assertNotNull(result);
        assertEquals("shortKey", result.toString());
        verify(mockConverter, times(1)).convert("shortKey", StringBuffer.class);
    }

    @Test
    void toStringBuffer_mapWithVKeyMappedToNull_returnsEmptyStringBuffer() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("_v", null);
        // Converter should never be called because the selected value is null.
        StringBuffer result = MapConversions.toStringBuffer(map, mockConverter);
        assertNotNull(result);
        assertEquals(0, result.length(), "Empty StringBuffer expected when selected map entry is null");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuffer_mapWithoutValueOrVKeys_returnsEmptyStringBuffer() {
        Map<String, Object> map = Map.of("foo", "bar");
        // No conversion should be attempted.
        StringBuffer result = MapConversions.toStringBuffer(map, mockConverter);
        assertNotNull(result);
        assertEquals(0, result.length());
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuffer_plainString_isConvertedDirectly() {
        String source = "plain string";
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        assertNotNull(result);
        assertEquals("plain string", result.toString());
    }

    @Test
    void toStringBuffer_integer_isConvertedToStringRepresentation() {
        Integer source = 42;
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        assertNotNull(result);
        assertEquals("42", result.toString());
    }

    @Test
    void toStringBuffer_localDate_isConvertedUsingToString() {
        LocalDate source = LocalDate.of(2023, 5, 17);
        StringBuffer result = MapConversions.toStringBuffer(source, realConverter);
        assertNotNull(result);
        assertEquals("2023-05-17", result.toString());
    }

    @Test
    void toStringBuffer_converterThrowsRuntimeException_propagates() {
        when(mockConverter.convert(any(), eq(StringBuffer.class))).thenThrow(new RuntimeException("Conversion failed"));
        assertThrows(RuntimeException.class, () -> MapConversions.toStringBuffer("bad", mockConverter), "RuntimeException from converter should be propagated");
    }

    @Test
    void toStringBuffer_converterReturnsInteger_causesClassCastException() {
        // The map contains a "value" key; the converter will return an Integer instead of StringBuffer.
        Map<String, Object> map = Map.of("value", "test");
        // Return an incompatible type via a cast so the compiler accepts it.
        when(mockConverter.convert(any(), eq(StringBuffer.class))).thenAnswer(invocation -> (StringBuffer) (Object) 123);
        assertThrows(ClassCastException.class, () -> MapConversions.toStringBuffer(map, mockConverter), "ClassCastException expected when converter returns wrong type");
    }

    @Test
    void toStringBuffer_converterReturnsBooleanForVKey_causesClassCastException() {
        Map<String, Object> map = Map.of("_v", new Object());
        when(mockConverter.convert(any(), eq(StringBuffer.class))).thenAnswer(invocation -> (StringBuffer) (Object) Boolean.TRUE);
        assertThrows(ClassCastException.class, () -> MapConversions.toStringBuffer(map, mockConverter), "ClassCastException expected when converter returns Boolean");
    }

    @Test
    void toStringBuffer_arraySource_throwsIllegalArgumentException() {
        int[] source = new int[] { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toStringBuffer(source, realConverter), "Array source should trigger IllegalArgumentException");
    }

    @Test
    void toStringBuffer_atomicReferenceSource_throwsIllegalArgumentException() {
        java.util.concurrent.atomic.AtomicReference<String> source = new java.util.concurrent.atomic.AtomicReference<>("atomic");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toStringBuffer(source, realConverter), "AtomicReference source should trigger IllegalArgumentException");
    }

    @Test
    void toStringBuffer_mapWithNonStringKey_causesClassCastException() {
        // Hashtable with an Integer key and a proper "value" entry.
        Hashtable<Object, Object> map = new Hashtable<>();
        map.put(123, "numericKey");
        map.put("value", "ok");
        // The implementation expects the map's key set to be Strings when looking up VALUE_KEYS.
        // This mismatch should surface as a ClassCastException.
        assertThrows(ClassCastException.class, () -> MapConversions.toStringBuffer(map, realConverter), "Non‑String map keys should cause ClassCastException");
    }
}