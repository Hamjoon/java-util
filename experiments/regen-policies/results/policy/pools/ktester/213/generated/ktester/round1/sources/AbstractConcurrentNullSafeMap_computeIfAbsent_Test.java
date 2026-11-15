package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_computeIfAbsent_Test {

    private TestMap<String, String> map;

    @Mock
    private Function<? super String, ? extends String> mockMappingFunction;

    /**
     * Concrete subclass used for testing – simply forwards to the protected constructor.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a fresh ConcurrentHashMap for each test to guarantee isolation.
        ConcurrentMap<Object, Object> backing = new ConcurrentHashMap<>();
        map = new TestMap<>(backing);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to aid GC and avoid cross‑test contamination.
        map.clear();
    }

    @Test
    void computeIfAbsent_InsertsWhenKeyAbsent() {
        // Given
        when(mockMappingFunction.apply("key1")).thenReturn("value1");
        // When
        String result = map.computeIfAbsent("key1", mockMappingFunction);
        // Then
        assertEquals("value1", result, "Returned value should be the one produced by the mapping function");
        assertEquals("value1", map.get("key1"), "Map should contain the newly computed entry");
        verify(mockMappingFunction, times(1)).apply("key1");
    }

    @Test
    void computeIfAbsent_DoesNotInvokeFunctionWhenKeyPresent() {
        // Given
        map.put("existing", "oldValue");
        // Ensure the mock would throw if called – we verify later it is not.
        when(mockMappingFunction.apply(any())).thenReturn("newValue");
        // When
        String result = map.computeIfAbsent("existing", mockMappingFunction);
        // Then
        assertEquals("oldValue", result, "Should return the existing value");
        assertEquals("oldValue", map.get("existing"), "Existing entry must remain unchanged");
        verify(mockMappingFunction, never()).apply(any());
    }

    @Test
    void computeIfAbsent_AllowsNullKey() {
        // Given
        when(mockMappingFunction.apply(null)).thenReturn("nullKeyValue");
        // When
        String result = map.computeIfAbsent(null, mockMappingFunction);
        // Then
        assertEquals("nullKeyValue", result, "Result for null key should be the computed value");
        assertEquals("nullKeyValue", map.get(null), "Map must store the value under a null key");
        verify(mockMappingFunction, times(1)).apply(null);
    }

    @Test
    void computeIfAbsent_AllowsMappingFunctionReturningNull() {
        // Given
        when(mockMappingFunction.apply("k")).thenReturn(null);
        // When
        String result = map.computeIfAbsent("k", mockMappingFunction);
        // Then
        assertNull(result, "Result should be null when mapping function returns null");
        assertTrue(map.containsKey("k"), "Key should be present even if the associated value is null");
        assertNull(map.get("k"), "Stored value must be null");
        verify(mockMappingFunction, times(1)).apply("k");
    }

    @Test
    void computeIfAbsent_ExistingKeyWithNullValueDoesNotInvokeFunction() {
        // Given: store a null value (internally masked)
        map.put("nullValKey", null);
        when(mockMappingFunction.apply(any())).thenReturn("shouldNotBeUsed");
        // When
        String result = map.computeIfAbsent("nullValKey", mockMappingFunction);
        // Then
        assertNull(result, "Existing null‑valued key should return null");
        assertTrue(map.containsKey("nullValKey"), "Key must still be present");
        assertNull(map.get("nullValKey"), "Stored value must remain null");
        verify(mockMappingFunction, never()).apply(any());
    }

    @Test
    void computeIfAbsent_NullMappingFunctionThrowsNPE() {
        // When / Then
        assertThrows(NullPointerException.class, () -> map.computeIfAbsent("anyKey", null), "A null mappingFunction must cause NullPointerException");
    }

    @Test
    void computeIfAbsent_NullKeyMappingReturnsNullCreatesNoEntry() {
        // Given
        when(mockMappingFunction.apply(null)).thenReturn(null);
        // When
        String result = map.computeIfAbsent(null, mockMappingFunction);
        // Then
        assertNull(result, "Result should be null when mapping returns null for a null key");
        assertFalse(map.containsKey(null), "No entry should be created for the null key");
        verify(mockMappingFunction, times(1)).apply(null);
    }

    @Test
    void computeIfAbsent_MappingFunctionThrowsExceptionPropagatesAndNoEntryCreated() {
        // Given
        when(mockMappingFunction.apply("ex")).thenThrow(new IllegalStateException("boom"));
        // When / Then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> map.computeIfAbsent("ex", mockMappingFunction), "Exception from mapping function must propagate");
        assertEquals("boom", ex.getMessage());
        // Ensure the map was not modified
        assertFalse(map.containsKey("ex"), "No entry should be added when mapping throws");
        verify(mockMappingFunction, times(1)).apply("ex");
    }

    @Test
    void computeIfAbsent_NullKeyMappingReturnsNonNullCreatesEntry() {
        // Given
        when(mockMappingFunction.apply(null)).thenReturn("valueForNull");
        // When
        String result = map.computeIfAbsent(null, mockMappingFunction);
        // Then
        assertEquals("valueForNull", result, "Value returned for null key must match mapping result");
        assertEquals("valueForNull", map.get(null), "Map must store the value for a null key");
        verify(mockMappingFunction, times(1)).apply(null);
    }

    @Test
    void computeIfAbsent_AbsentKeyNormalCase() {
        // Given
        when(mockMappingFunction.apply("foo")).thenReturn("bar");
        // When
        String result = map.computeIfAbsent("foo", mockMappingFunction);
        // Then
        assertEquals("bar", result, "Returned value should be the one produced by the mapping function");
        assertEquals("bar", map.get("foo"), "Map should contain the newly computed entry");
        verify(mockMappingFunction, times(1)).apply("foo");
    }

    @Test
    void computeIfAbsent_AbsentKeyMappingReturnsNullCreatesNoEntry() {
        // Given
        when(mockMappingFunction.apply("baz")).thenReturn(null);
        // When
        String result = map.computeIfAbsent("baz", mockMappingFunction);
        // Then
        assertNull(result, "Result should be null when mapping function returns null");
        assertFalse(map.containsKey("baz"), "No entry should be created for the key");
        verify(mockMappingFunction, times(1)).apply("baz");
    }

    @Test
    void computeIfAbsent_ExistingKeyValueRetained() {
        // Given
        map.put("existing", "oldValue");
        // must not be called
        when(mockMappingFunction.apply("existing")).thenReturn("newValue");
        // When
        String result = map.computeIfAbsent("existing", mockMappingFunction);
        // Then
        assertEquals("oldValue", result, "Existing value must be returned");
        assertEquals("oldValue", map.get("existing"), "Existing entry must remain unchanged");
        verify(mockMappingFunction, never()).apply(any());
    }
}
