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
import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_computeIfAbsent_Test {

    private TestMap map;

    /**
     * Concrete implementation used only for testing.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a real ConcurrentHashMap as the backing store.
        ConcurrentMap<Object, Object> backing = new ConcurrentHashMap<>();
        map = new TestMap(backing);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the map is cleared between tests.
        map.clear();
    }

    @Test
    void computeIfAbsent_KeyAbsent_InvokesMappingFunctionAndStoresResult(@Mock Function<? super String, ? extends String> mappingFunction) {
        // GIVEN
        when(mappingFunction.apply("key1")).thenReturn("value1");
        // WHEN
        String result = (String) map.computeIfAbsent("key1", mappingFunction);
        // THEN
        assertEquals("value1", result);
        assertEquals("value1", map.get("key1"));
        verify(mappingFunction, times(1)).apply("key1");
    }

    @Test
    void computeIfAbsent_KeyPresent_DoesNotInvokeMappingFunctionAndReturnsExisting(@Mock Function<? super String, ? extends String> mappingFunction) {
        // GIVEN
        map.put("key2", "existing");
        // mappingFunction should not be called, but we still set a stub to avoid NPE if it is.
        when(mappingFunction.apply(anyString())).thenReturn("newValue");
        // WHEN
        String result = (String) map.computeIfAbsent("key2", mappingFunction);
        // THEN
        assertEquals("existing", result);
        assertEquals("existing", map.get("key2"));
        verifyNoInteractions(mappingFunction);
    }

    @Test
    void computeIfAbsent_NullKey_InvokesMappingFunctionAndStoresResult(@Mock Function<? super String, ? extends String> mappingFunction) {
        // GIVEN
        when(mappingFunction.apply(null)).thenReturn("nullKeyValue");
        // WHEN
        String result = (String) map.computeIfAbsent(null, mappingFunction);
        // THEN
        assertEquals("nullKeyValue", result);
        assertEquals("nullKeyValue", map.get(null));
        verify(mappingFunction, times(1)).apply(null);
    }

    @Test
    void computeIfAbsent_MappingFunctionReturnsNull_DoesNotStoreEntry(@Mock Function<? super String, ? extends String> mappingFunction) {
        // GIVEN
        when(mappingFunction.apply("key3")).thenReturn(null);
        // WHEN
        String result = (String) map.computeIfAbsent("key3", mappingFunction);
        // THEN
        assertNull(result);
        assertFalse(map.containsKey("key3"));
        assertNull(map.get("key3"));
        verify(mappingFunction, times(1)).apply("key3");
    }

    @Test
    void computeIfAbsent_NullKey_WithNullMappingResult_DoesNotStoreEntry(@Mock Function<? super String, ? extends String> mappingFunction) {
        // GIVEN
        when(mappingFunction.apply(null)).thenReturn(null);
        // WHEN
        String result = (String) map.computeIfAbsent(null, mappingFunction);
        // THEN
        assertNull(result);
        assertFalse(map.containsKey(null));
        assertEquals(0, map.size());
        verify(mappingFunction, times(1)).apply(null);
    }

    @Test
    void computeIfAbsent_NullMappingFunction_ThrowsNullPointerException() {
        // GIVEN
        Function<String, String> nullFunction = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.computeIfAbsent("absentKey", nullFunction));
        // Map must stay unchanged.
        assertFalse(map.containsKey("absentKey"));
        assertEquals(0, map.size());
    }

    @Test
    void computeIfAbsent_KeyAbsent_MappingReturnsNonNull_StoresAndReturnsValue(@Mock Function<? super String, ? extends String> mappingFunction) {
        // GIVEN
        when(mappingFunction.apply("absentKey")).thenReturn("value");
        // WHEN
        String result = (String) map.computeIfAbsent("absentKey", mappingFunction);
        // THEN
        assertEquals("value", result);
        assertEquals("value", map.get("absentKey"));
        assertEquals(1, map.size());
        verify(mappingFunction, times(1)).apply("absentKey");
    }

    @Test
    void computeIfAbsent_KeyAlreadyMappedToMaskedNull_ReturnsNullWithoutCallingMappingFunction(@Mock Function<? super String, ? extends String> mappingFunction) {
        // GIVEN: put a null value – it will be stored as a masked sentinel.
        map.put("nullValueKey", null);
        // mappingFunction should not be invoked.
        when(mappingFunction.apply(anyString())).thenReturn("shouldNotBeUsed");
        // WHEN
        Object result = map.computeIfAbsent("nullValueKey", mappingFunction);
        // THEN
        assertNull(result);
        // The entry is still present (masked null), size remains 1.
        assertTrue(map.containsKey("nullValueKey"));
        assertEquals(1, map.size());
        verifyNoInteractions(mappingFunction);
    }

    @Test
    void computeIfAbsent_MappingFunctionThrowsRuntimeException_PropagatesAndLeavesMapUnchanged() {
        // GIVEN
        Function<String, String> explodingFunction = k -> {
            throw new IllegalStateException("boom");
        };
        int sizeBefore = map.size();
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> map.computeIfAbsent("absentKey", explodingFunction));
        assertEquals("boom", ex.getMessage());
        // Map must be unchanged.
        assertEquals(sizeBefore, map.size());
        assertFalse(map.containsKey("absentKey"));
    }

    @Test
    void computeIfAbsent_AbsentKey_ReturnsCustomObjectAndStoresIt() {
        // GIVEN
        Function<String, BigDecimal> mappingFunction = k -> new BigDecimal("123.45");
        // WHEN
        BigDecimal result = (BigDecimal) map.computeIfAbsent("customKey", mappingFunction);
        // THEN
        assertEquals(new BigDecimal("123.45"), result);
        assertEquals(new BigDecimal("123.45"), map.get("customKey"));
        assertEquals(1, map.size());
    }

    @Test
    void computeIfAbsent_InvocationCount_ForSameKey(@Mock Function<? super String, ? extends String> mappingFunction) {
        // GIVEN
        when(mappingFunction.apply("absentKeyCount")).thenReturn("first");
        // First call – key absent.
        String firstResult = (String) map.computeIfAbsent("absentKeyCount", mappingFunction);
        assertEquals("first", firstResult);
        verify(mappingFunction, times(1)).apply("absentKeyCount");
        assertEquals(1, map.size());
        // Reset mock to verify no further interactions.
        reset(mappingFunction);
        when(mappingFunction.apply(anyString())).thenReturn("second");
        // Second call – key now present.
        String secondResult = (String) map.computeIfAbsent("absentKeyCount", mappingFunction);
        assertEquals("first", secondResult);
        // Mapping function must not be called the second time.
        verifyNoInteractions(mappingFunction);
        assertEquals(1, map.size());
    }
}
