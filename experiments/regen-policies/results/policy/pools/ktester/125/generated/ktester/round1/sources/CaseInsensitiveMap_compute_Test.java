package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.BiFunction;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_compute_Test {

    private CaseInsensitiveMap<String, String> map;

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure the cache is in its default state for all tests
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test
        map = new CaseInsensitiveMap<>();
        map.put("KeyOne", "ValueOne");
        map.put("KeyTwo", "ValueTwo");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination
        map.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Reset any static configuration that might have been changed
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    /**
     * Utility to set a private field via reflection.
     *
     * @param target the object whose field should be modified
     * @param fieldName the name of the private field
     * @param value the new value to assign
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void computeReplacesExistingValueCaseInsensitive() {
        // given
        BiFunction<String, String, String> remap = (k, v) -> v + "-updated";
        // when
        // different case
        String result = map.compute("keyone", remap);
        // then
        assertEquals("ValueOne-updated", result);
        assertEquals("ValueOne-updated", map.get("KEYONE"));
    }

    @Test
    void computeAddsNewEntryWhenKeyAbsent() {
        // given
        BiFunction<String, String, String> remap = (k, v) -> "new-" + k.toLowerCase();
        // when
        String result = map.compute("NewKey", remap);
        // then
        assertEquals("new-newkey", result);
        assertEquals("new-newkey", map.get("newkey"));
        assertEquals("new-newkey", map.get("NEWKEY"));
    }

    @Test
    void computePassesOriginalKeyToBiFunction() {
        // given
        @SuppressWarnings("unchecked")
        BiFunction<String, String, String> mockBiFunction = mock(BiFunction.class);
        when(mockBiFunction.apply(anyString(), any())).thenReturn("ignored");
        // when
        map.compute("KeYOnE", mockBiFunction);
        // then
        // Verify that the BiFunction received the original key (the one passed to compute)
        verify(mockBiFunction, times(1)).apply(eq("KeYOnE"), any());
    }

    @Test
    void computeHandlesNullKeyAndFunction() {
        // given
        map.put(null, "nullValue");
        BiFunction<String, String, String> remap = (k, v) -> v + "-changed";
        // when
        String result = map.compute(null, remap);
        // then
        assertEquals("nullValue-changed", result);
        assertEquals("nullValue-changed", map.get(null));
    }

    @Test
    void computeThrowsWhenRemappingFunctionIsNull() {
        // when / then
        assertThrows(NullPointerException.class, () -> map.compute("anyKey", null));
    }

    @Test
    void computeWorksWithNonStringKeyType() {
        // given
        CaseInsensitiveMap<Integer, String> intMap = new CaseInsensitiveMap<>();
        intMap.put(1, "one");
        BiFunction<Integer, String, String> remap = (k, v) -> v + "-mapped";
        // when
        String result = intMap.compute(1, remap);
        // then
        assertEquals("one-mapped", result);
        assertEquals("one-mapped", intMap.get(1));
    }

    @Test
    void computeUpdatesExistingIntegerValueCaseInsensitive() {
        // Map initially contains {"test"=1}
        CaseInsensitiveMap<String, Integer> intMap = new CaseInsensitiveMap<>();
        intMap.put("test", 1);
        // remapping function adds 10 to the current Integer value
        BiFunction<String, Integer, Integer> remap = (k, v) -> v + 10;
        // key differs in case
        Integer result = intMap.compute("TeSt", remap);
        assertEquals(11, result);
        // underlying key is stored in lower‑case form, but retrieval is case‑insensitive
        assertEquals(11, intMap.get("test"));
        assertEquals(11, intMap.get("TEST"));
    }

    @Test
    void computeAddsAbsentIntegerKeyCaseInsensitive() {
        // Empty map
        CaseInsensitiveMap<String, Integer> intMap = new CaseInsensitiveMap<>();
        // remapping function ignores the (null) current value and returns 100
        BiFunction<String, Integer, Integer> remap = (k, v) -> 100;
        Integer result = intMap.compute("NeWKeY", remap);
        assertEquals(100, result);
        // The key is stored in lower‑case form
        assertEquals(100, intMap.get("newkey"));
        assertEquals(100, intMap.get("NEWKEY"));
    }

    @Test
    void computeRemappingFunctionReturnsNullCausesRemoval() {
        // Map initially contains {"test"=5}
        CaseInsensitiveMap<String, Integer> intMap = new CaseInsensitiveMap<>();
        intMap.put("test", 5);
        // remapping function returns null, signalling removal
        BiFunction<String, Integer, Integer> remap = (k, v) -> null;
        Integer result = intMap.compute("TeSt", remap);
        assertNull(result);
        assertFalse(intMap.containsKey("test"));
        assertFalse(intMap.containsKey("TEST"));
    }

    @Test
    void computeWhenExistingKeyHasNullValue() {
        // Map initially contains {"test"=null}
        CaseInsensitiveMap<String, Integer> intMap = new CaseInsensitiveMap<>();
        intMap.put("test", null);
        // remapping function treats null as 0 and returns 1
        BiFunction<String, Integer, Integer> remap = (k, v) -> (v == null ? 0 : v) + 1;
        Integer result = intMap.compute("TeSt", remap);
        assertEquals(1, result);
        assertEquals(1, intMap.get("test"));
    }

    @Test
    void computePropagatesRuntimeExceptionFromRemappingFunction() {
        // Map contains {"example"=5}
        CaseInsensitiveMap<String, Integer> intMap = new CaseInsensitiveMap<>();
        intMap.put("example", 5);
        // remapping function deliberately throws IllegalArgumentException
        BiFunction<String, Integer, Integer> remap = (k, v) -> {
            throw new IllegalArgumentException("bad");
        };
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> intMap.compute("example", remap));
        assertEquals("bad", thrown.getMessage());
        // The original entry must remain unchanged
        assertEquals(5, intMap.get("example"));
    }

    @Test
    void computeReturnsIncompatibleValueTypeThrowsClassCastException() {
        // Map initially contains {"test"=10} (Integer values)
        CaseInsensitiveMap<String, Integer> intMap = new CaseInsensitiveMap<>();
        intMap.put("test", 10);
        // remapping function returns a String, which cannot be cast to Integer
        @SuppressWarnings("unchecked")
        BiFunction<String, Integer, ?> remap = (BiFunction<String, Integer, Object>) (k, v) -> "incompatibleString";
        assertThrows(ClassCastException.class, () -> intMap.compute("test", remap));
        // Original entry must stay intact
        assertEquals(10, intMap.get("test"));
    }

    @Test
    void computeOnUnmodifiableBackingMapThrowsUnsupportedOperationException() throws Exception {
        // Create a normal map, wrap it with Collections.unmodifiableMap, then inject it via reflection
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
        CaseInsensitiveMap<String, Integer> ciMap = new CaseInsensitiveMap<>();
        // Use reflection to replace the private backing map with the unmodifiable one
        Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(ciMap, unmodifiable);
        // remapping function that would add a new entry
        BiFunction<String, Integer, Integer> remap = (k, v) -> 42;
        assertThrows(UnsupportedOperationException.class, () -> ciMap.compute("key", remap));
    }
}
