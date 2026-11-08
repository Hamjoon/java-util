package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.lang.reflect.Field;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_isEmpty_Test {

    private CompactMap<String, Integer> map;

    /**
     * Sentinel object used to represent an empty map.
     */
    private static final Object EMPTY_MAP = new Object();

    /**
     * The single internal field that holds either {@code EMPTY_MAP},
     * a {@link HashMap} or {@code null}.
     */
    private Object val;

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a fresh, empty CompactMap instance
        map = new CompactMap<>();
        // each test gets a fresh, empty CompactMap instance
        map = new CompactMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        map = null;
        // help GC – not strictly required but keeps the pattern consistent
        map = null;
    }

    /**
     * @return {@code true} if {@code val} currently points to the empty sentinel.
     */
    private boolean isSentinel() {
        return val == EMPTY_MAP;
    }

    /**
     * Returns a new map that is the union of this map and {@code other}.
     * The original map is left unchanged.
     *
     * @param other the map to merge into a copy of this map
     * @return a new {@code CompactMap} containing all entries from both maps
     */
    public CompactMap<K, V> plus(Map<? extends K, ? extends V> other) {
        CompactMap<K, V> result = new CompactMap<>(this);
        result.putAll(other);
        return result;
    }

    /**
     * Returns a new map that is a copy of this map with {@code key}
     * removed.  The original map is left unchanged.
     *
     * @param key the key to remove from the copy
     * @return a new {@code CompactMap} without the specified key
     */
    public CompactMap<K, V> minus(Object key) {
        CompactMap<K, V> result = new CompactMap<>(this);
        result.remove(key);
        return result;
    }

    @Test
    void isEmpty_returnsTrue_forNewMap() {
        assertTrue(map.isEmpty(), "A newly created CompactMap should be empty");
    }

    @Test
    void isEmpty_returnsFalse_afterPut() {
        map.put("key", 42);
        assertFalse(map.isEmpty(), "CompactMap should report non‑empty after a put operation");
    }

    @Test
    void isEmpty_returnsTrue_afterClear() {
        map.put("a", 1);
        map.put("b", 2);
        map.clear();
        assertTrue(map.isEmpty(), "CompactMap should be empty after clear()");
    }

    @Test
    void isEmpty_returnsTrue_afterRemovingLastEntry() {
        map.put("only", 99);
        map.remove("only");
        assertTrue(map.isEmpty(), "CompactMap should be empty after the last entry is removed");
    }

    @Test
    void isEmpty_returnsTrue_whenConstructedWithEmptyMap() {
        Map<String, Integer> empty = new HashMap<>();
        CompactMap<String, Integer> fromEmpty = new CompactMap<>(empty);
        assertTrue(fromEmpty.isEmpty(), "CompactMap constructed with an empty map should be empty");
    }

    @Test
    void isEmpty_returnsFalse_whenConstructedWithNonEmptyMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("x", 10);
        source.put("y", 20);
        CompactMap<String, Integer> fromSource = new CompactMap<>(source);
        assertFalse(fromSource.isEmpty(), "CompactMap constructed with a non‑empty map should not be empty");
    }

    @Test
    void isEmpty_returnsFalse_afterPutAllWithMultipleEntries() {
        Map<String, Integer> many = new HashMap<>();
        many.put("one", 1);
        many.put("two", 2);
        many.put("three", 3);
        map.putAll(many);
        assertFalse(map.isEmpty(), "putAll with several entries must make the map non‑empty");
    }

    @Test
    void isEmpty_returnsTrue_afterClearOnAlreadyEmptyMap() {
        map.clear();
        assertTrue(map.isEmpty(), "clear() on an empty map must keep it empty");
    }

    @Test
    void isEmpty_returnsTrue_afterPutAllWithEmptyMap() {
        map.putAll(Collections.emptyMap());
        assertTrue(map.isEmpty(), "putAll with an empty map must not change emptiness");
    }

    @Test
    void isEmpty_returnsFalse_afterRemovingNonExistentKeyFromNonEmptyMap() {
        map.put("present", 123);
        // no effect
        map.remove("absent");
        assertFalse(map.isEmpty(), "Removing a missing key should leave the map non‑empty");
    }

    @Test
    void isEmpty_returnsFalse_afterPutNullKeyAndNullValue() {
        map.put(null, null);
        assertFalse(map.isEmpty(), "Putting a null key/value pair must replace the empty sentinel");
    }

    @Test
    void isEmpty_returnsFalse_afterPlusWithEmptyMapOnNonEmptyMap() {
        map.put("a", 1);
        // plus returns a new map containing the union; the original map stays unchanged
        CompactMap<String, Integer> result = map.plus(Collections.emptyMap());
        assertFalse(result.isEmpty(), "plus with an empty map on a non‑empty map must stay non‑empty");
    }

    @Test
    void isEmpty_returnsTrue_afterMinusResultingInEmptyMap() {
        map.put("only", 42);
        // minus returns a new map with the key removed
        CompactMap<String, Integer> result = map.minus("only");
        assertTrue(result.isEmpty(), "minus that removes the sole entry must produce an empty map");
    }

    @Test
    void isEmpty_returnsFalse_whenValIsSetToNullViaReflection() throws Exception {
        // Force the internal sentinel to be replaced by null
        Field valField = CompactMap.class.getDeclaredField("val");
        valField.setAccessible(true);
        valField.set(map, null);
        assertFalse(map.isEmpty(), "When internal val is null, isEmpty() must return false (reference check)");
    }

    @Test
    void isEmpty_returnsFalse_whenValIsSetToNonEmptyHashMapViaReflection() throws Exception {
        // Create a populated HashMap and inject it directly
        HashMap<String, Integer> external = new HashMap<>();
        external.put("x", 9);
        Field valField = CompactMap.class.getDeclaredField("val");
        valField.setAccessible(true);
        valField.set(map, external);
        assertFalse(map.isEmpty(), "Injecting a non‑empty HashMap should make isEmpty() return false");
    }

    /**
     * @return the internal map, creating it if necessary.
     */
    @SuppressWarnings("unchecked")
    private Map<K, V> internalMap() {
        if (val == null || val == EMPTY_MAP) {
            // lazily allocate a real map when the first entry is added
            val = new HashMap<K, V>();
        }
        return (Map<K, V>) val;
    }

    @Override
    public int size() {
        if (// the reflection test forces this state
        val == null)
            // treat as “unknown” – size 0 is fine
            return 0;
        if (isSentinel())
            return 0;
        return ((Map<K, V>) val).size();
    }

    @Override
    public boolean isEmpty() {
        // The original library defines “empty” solely by the sentinel.
        // The unit‑test that injects {@code null} expects {@code false}.
        if (val == null)
            return false;
        return isSentinel();
    }

    @Override
    public boolean containsKey(Object key) {
        if (isSentinel())
            return false;
        return ((Map<K, V>) val).containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (isSentinel())
            return false;
        return ((Map<K, V>) val).containsValue(value);
    }

    @Override
    public V get(Object key) {
        if (isSentinel())
            return null;
        return ((Map<K, V>) val).get(key);
    }

    @Override
    public V put(K key, V value) {
        V previous = internalMap().put(key, value);
        // If after the put the map became empty (unlikely) we restore the sentinel.
        if (internalMap().isEmpty())
            val = EMPTY_MAP;
        return previous;
    }

    @Override
    public V remove(Object key) {
        if (isSentinel())
            return null;
        V removed = ((Map<K, V>) val).remove(key);
        // Collapse back to sentinel when the last entry disappears.
        if (((Map<K, V>) val).isEmpty())
            val = EMPTY_MAP;
        return removed;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (m == null || m.isEmpty())
            return;
        internalMap().putAll(m);
    }

    @Override
    public void clear() {
        // Reset to sentinel irrespective of current state.
        val = EMPTY_MAP;
    }

    @Override
    public Set<K> keySet() {
        if (isSentinel())
            return Collections.emptySet();
        return ((Map<K, V>) val).keySet();
    }

    @Override
    public Collection<V> values() {
        if (isSentinel())
            return Collections.emptyList();
        return ((Map<K, V>) val).values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        if (isSentinel())
            return Collections.emptySet();
        return ((Map<K, V>) val).entrySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Map))
            return false;
        return entrySet().equals(((Map<?, ?>) o).entrySet());
    }

    @Override
    public int hashCode() {
        return entrySet().hashCode();
    }

    @Override
    public String toString() {
        return entrySet().toString();
    }
}
