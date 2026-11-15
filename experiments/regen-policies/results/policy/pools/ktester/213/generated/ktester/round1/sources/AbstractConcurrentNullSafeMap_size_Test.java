package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_size_Test {

    @Mock
    private ConcurrentMap<Object, Object> mockInternalMap;

    /**
     * Concrete subclass used only for testing the abstract class.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
    }

    @Test
    void size_delegatesToInternalMap() {
        // Arrange
        when(mockInternalMap.size()).thenReturn(5);
        AbstractConcurrentNullSafeMap<String, String> map = new TestMap<>(mockInternalMap);
        // Act
        int result = map.size();
        // Assert
        assertEquals(5, result, "size() should return the value reported by the internal map");
        verify(mockInternalMap, times(1)).size();
    }

    @Test
    void size_returnsZeroWhenMapIsEmpty() {
        // Arrange – use a real ConcurrentHashMap to avoid stubbing
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        AbstractConcurrentNullSafeMap<String, String> map = new TestMap<>(realMap);
        // Act & Assert
        assertEquals(0, map.size(), "A newly created map must report size 0");
    }

    @Test
    void size_reflectsActualNumberOfEntries() {
        // Arrange
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        AbstractConcurrentNullSafeMap<String, String> map = new TestMap<>(realMap);
        // Act & Assert – add entries and verify size changes accordingly
        assertEquals(0, map.size());
        map.put("one", "1");
        assertEquals(1, map.size());
        map.put("two", "2");
        assertEquals(2, map.size());
        map.put(null, "nullKey");
        assertEquals(3, map.size());
        map.remove("one");
        assertEquals(2, map.size());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void size_emptyMap() {
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        AbstractConcurrentNullSafeMap<String, String> map = new TestMap<>(realMap);
        assertEquals(0, map.size(), "A newly created AbstractConcurrentNullSafeMap with no entries should report size 0.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void size_afterSinglePut() {
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        AbstractConcurrentNullSafeMap<String, String> map = new TestMap<>(realMap);
        map.put("key", "value");
        assertEquals(1, map.size(), "After inserting one non‑null key/value pair, size() should return 1.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void size_afterMultiplePuts() {
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        AbstractConcurrentNullSafeMap<String, String> map = new TestMap<>(realMap);
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        assertEquals(3, map.size(), "After inserting three distinct key/value pairs, size() should return 3.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void size_afterPutWithNullKey() {
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        AbstractConcurrentNullSafeMap<String, String> map = new TestMap<>(realMap);
        map.put(null, "nullKeyValue");
        assertEquals(1, map.size(), "The map masks a null key internally; after putting (null, \"value\"), size() should count the entry as 1.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void size_afterPutWithNullValue() {
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        AbstractConcurrentNullSafeMap<String, String> map = new TestMap<>(realMap);
        map.put("keyWithNullValue", null);
        assertEquals(1, map.size(), "The map masks a null value internally; after putting (\"key\", null), size() should count the entry as 1.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void size_afterDuplicateKeyReplacement() {
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        AbstractConcurrentNullSafeMap<String, String> map = new TestMap<>(realMap);
        map.put("dupKey", "first");
        // replaces the previous value
        map.put("dupKey", "second");
        assertEquals(1, map.size(), "Putting the same key twice replaces the value; size() should remain 1, not 2.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void size_afterRemoval() {
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        AbstractConcurrentNullSafeMap<String, String> map = new TestMap<>(realMap);
        map.put("toRemove", "value");
        assertEquals(1, map.size(), "Pre‑condition check: map should contain 1 entry before removal.");
        map.remove("toRemove");
        assertEquals(0, map.size(), "After inserting an entry and then removing it, size() should return 0.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void size_afterClear() {
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        AbstractConcurrentNullSafeMap<String, String> map = new TestMap<>(realMap);
        map.put("a", "1");
        map.put("b", "2");
        map.put(null, "nullKey");
        map.put("c", null);
        assertEquals(4, map.size(), "Pre‑condition check: map should contain 4 entries before clear.");
        map.clear();
        assertEquals(0, map.size(), "After populating the map with several entries and calling clear(), size() should be 0.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void size_afterPutAll() {
        ConcurrentMap<Object, Object> realMap = new ConcurrentHashMap<>();
        AbstractConcurrentNullSafeMap<String, String> map = new TestMap<>(realMap);
        Map<String, String> source = new HashMap<>();
        source.put("k1", "v1");
        source.put("k2", "v2");
        source.put("k3", "v3");
        source.put("k4", "v4");
        map.putAll(source);
        assertEquals(4, map.size(), "After invoking putAll() with a map containing four distinct entries, size() should reflect 4.");
    }
}
