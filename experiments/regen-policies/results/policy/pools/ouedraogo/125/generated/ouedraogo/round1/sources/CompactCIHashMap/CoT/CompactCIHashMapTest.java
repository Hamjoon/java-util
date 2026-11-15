/**
 * JUnit‑4 test suite for {@code com.cedarsoftware.util.CompactCIHashMap}.
 *
 * The tests cover:
 *   • All public constructors and methods inherited from {@code CompactMap}
 *   • Basic functional verification
 *   • Edge‑case and exception scenarios (null arguments, case‑insensitivity, etc.)
 *
 * NOTE: The test class uses only the public API that is visible in the supplied
 * source code and the declared API of {@code CompactMap}. No mocking or
 * subclassing of library classes is performed.
 */
package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

import static org.junit.Assert.*;

public class CompactCIHashMapTest {

    private CompactCIHashMap<String, String> map;

    @Before
    public void setUp() {
        map = new CompactCIHashMap<>();
    }

    /* --------------------------------------------------------------
     *  List of public methods (including inherited ones) to be tested
     * --------------------------------------------------------------
     *
     * Constructors
     *   CompactCIHashMap()
     *   CompactCIHashMap(Map<K,V> other)
     *
     * Inherited from CompactMap (all are public)
     *   int size()
     *   boolean isEmpty()
     *   boolean containsKey(Object key)
     *   boolean containsValue(Object value)
     *   V get(Object key)
     *   V put(K key, V value)
     *   V remove(Object key)
     *   void putAll(Map<? extends K, ? extends V> m)
     *   void clear()
     *   int hashCode()
     *   boolean equals(Object o)
     *   String toString()
     *   Set<K> keySet()
     *   Collection<V> values()
     *   Set<Entry<K,V>> entrySet()
     *   Map<K,V> minus(Object removeMe)
     *   Map<K,V> plus(Object right)
     */

    /* ==========================
     *  BASIC FUNCTIONALITY TESTS
     * ========================== */

    @Test
    public void testEmptyMapBehavior() {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertTrue(map.keySet().isEmpty());
        assertTrue(map.values().isEmpty());
        assertTrue(map.entrySet().isEmpty());
    }

    @Test
    public void testPutAndGet_CaseInsensitive() {
        map.put("KeyOne", "Value1");
        // Retrieval with different case
        assertEquals("Value1", map.get("keyone"));
        assertEquals("Value1", map.get("KEYONE"));
        // containsKey must be case‑insensitive as well
        assertTrue(map.containsKey("kEyOnE"));
    }

    @Test
    public void testPutOverwriteWithDifferentCase() {
        map.put("Test", "First");
        // Overwrite using a key that differs only by case
        String previous = map.put("tEST", "Second");
        assertEquals("First", previous);
        assertEquals(1, map.size());
        assertEquals("Second", map.get("test"));
    }

    @Test
    public void testRemove_CaseInsensitive() {
        map.put("RemoveMe", "gone");
        assertEquals("gone", map.remove("remOVEMe"));
        assertFalse(map.containsKey("REMOVEME"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testContainsValue() {
        map.put("A", "alpha");
        map.put("B", "beta");
        assertTrue(map.containsValue("alpha"));
        assertFalse(map.containsValue("gamma"));
    }

    @Test
    public void testPutAllCopiesCaseInsensitive() {
        Map<String, String> source = new HashMap<>();
        source.put("One", "1");
        source.put("Two", "2");
        map.putAll(source);

        assertEquals(2, map.size());
        assertEquals("1", map.get("ONE"));
        assertEquals("2", map.get("two"));
    }

    @Test
    public void testClear() {
        map.put("x", "1");
        map.put("y", "2");
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testMinusReturnsNewMapWithoutKey() {
        map.put("keep", "yes");
        map.put("drop", "no");
        Map<String, String> result = map.minus("DROP"); // case‑insensitive removal
        assertEquals(1, result.size());
        assertTrue(result.containsKey("keep"));
        assertFalse(result.containsKey("drop"));
        // original map must stay unchanged
        assertEquals(2, map.size());
    }

    @Test
    public void testPlusAddsEntryAndReturnsNewMap() {
        map.put("a", "1");
        Map<String, String> result = map.plus(Collections.singletonMap("B", "2"));
        assertEquals(2, result.size());
        assertEquals("1", result.get("A"));
        assertEquals("2", result.get("b"));
        // original map unchanged
        assertEquals(1, map.size());
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactCIHashMap<String, String> other = new CompactCIHashMap<>();
        map.put("Key", "Value");
        other.put("key", "Value"); // different case but same logical entry
        assertEquals(map, other);
        assertEquals(map.hashCode(), other.hashCode());
    }

    @Test
    public void testToStringContainsEntries() {
        map.put("one", "1");
        map.put("two", "2");
        String s = map.toString();
        assertTrue(s.contains("one"));
        assertTrue(s.contains("two"));
        assertTrue(s.contains("1"));
        assertTrue(s.contains("2"));
    }

    @Test
    public void testKeySetValuesAndEntrySetReflectMapContent() {
        map.put("Alpha", "A");
        map.put("Beta", "B");

        Set<String> keys = map.keySet();
        Collection<String> values = map.values();
        Set<Map.Entry<String, String>> entries = map.entrySet();

        assertEquals(2, keys.size());
        assertTrue(keys.contains("alpha"));
        assertTrue(keys.contains("BETA"));

        assertEquals(2, values.size());
        assertTrue(values.contains("A"));
        assertTrue(values.contains("B"));

        assertEquals(2, entries.size());
        for (Map.Entry<String, String> e : entries) {
            assertTrue(map.containsKey(e.getKey()));
            assertEquals(map.get(e.getKey()), e.getValue());
        }
    }

    /* ==============================
     *  EDGE CASE & EXCEPTION TESTS
     * ============================== */

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullMapThrowsNPE() {
        new CompactCIHashMap<>(null);
    }

    @Test(expected = NullPointerException.class)
    public void testPutNullKeyThrowsNPE() {
        map.put(null, "someValue");
    }

    @Test(expected = NullPointerException.class)
    public void testGetNullKeyThrowsNPE() {
        map.put("present", "value");
        map.get(null);
    }

    @Test(expected = NullPointerException.class)
    public void testContainsKeyNullThrowsNPE() {
        map.containsKey(null);
    }

    @Test
    public void testMinusWithNullArgumentReturnsSameMap() {
        map.put("a", "1");
        Map<String, String> result = map.minus(null);
        // According to typical semantics, removing null does nothing.
        assertEquals(map, result);
    }

    @Test
    public void testPlusWithNullArgumentThrowsNPE() {
        try {
            map.plus(null);
            fail("Expected NullPointerException when calling plus(null)");
        } catch (NullPointerException ignored) {
            // Expected behaviour
        }
    }

    @Test
    public void testPutAllWithNullMapThrowsNPE() {
        try {
            map.putAll(null);
            fail("Expected NullPointerException when calling putAll(null)");
        } catch (NullPointerException ignored) {
            // Expected behaviour
        }
    }

    @Test
    public void testConstructionFromExistingMapIsCaseInsensitive() {
        Map<String, String> source = new HashMap<>();
        source.put("MixedCase", "value");
        CompactCIHashMap<String, String> fromMap = new CompactCIHashMap<>(source);
        assertEquals("value", fromMap.get("mixedcase"));
        assertTrue(fromMap.containsKey("MIXEDCASE"));
    }

    @Test
    public void testSizeAndIsEmptyAfterSeriesOfOperations() {
        assertTrue(map.isEmpty());
        map.put("k1", "v1");
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());

        map.put("k2", "v2");
        assertEquals(2, map.size());

        map.remove("K1");
        assertEquals(1, map.size());

        map.clear();
        assertTrue(map.isEmpty());
    }
}
