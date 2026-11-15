package com.cedarsoftware.util;

import org.junit.*;
import java.util.*;
import java.util.function.*;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CaseInsensitiveMap}.
 *
 * The tests are organised as follows:
 * <ul>
 *   <li>Listing of public API (for documentation purposes).</li>
 *   <li>Basic functional tests for each public method.</li>
 *   <li>Edge‑case and exception‑handling tests.</li>
 * </ul>
 *
 * The test class does **not** rely on any external libraries other than JUnit‑4
 * and the classes that are part of the supplied source code.
 */
public class CaseInsensitiveMapTest {

    /* -------------------------------------------------------------
     *  1.  PUBLIC API LISTING (for reference – not used by tests)
     * ------------------------------------------------------------- */
    // static void replaceRegistry(List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> newRegistry)
    // static void replaceCache(LRUCache lruCache)
    // static void setMaxCacheLengthString(int length)
    // CaseInsensitiveMap()
    // CaseInsensitiveMap(int initialCapacity)
    // CaseInsensitiveMap(int initialCapacity, float loadFactor)
    // CaseInsensitiveMap(Map<K,V> source, Map<K,V> mapInstance)
    // CaseInsensitiveMap(Map<K,V> source)
    // Map<K,V> getWrappedMap()
    // Set<K> keySet()
    // Set<Entry<K,V>> entrySet()
    // V get(Object key)
    // boolean containsKey(Object key)
    // V put(K key, V value)
    // V remove(Object key)
    // boolean equals(Object other)
    // V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
    // V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
    // V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
    // V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)
    // V putIfAbsent(K key, V value)
    // boolean remove(Object key, Object value)
    // boolean replace(K key, V oldValue, V newValue)
    // V replace(K key, V value)
    // void forEach(BiConsumer<? super K, ? super V> action)
    // void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)

    /* -------------------------------------------------------------
     *  2.  BASIC FUNCTIONAL TESTS
     * ------------------------------------------------------------- */

    @Test
    public void testEmptyConstructorAndBasicOperations() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        assertTrue(map.isEmpty());

        map.put("Key", "Value");
        assertEquals(1, map.size());
        // case‑insensitive get
        assertEquals("Value", map.get("key"));
        assertEquals("Value", map.get("KEY"));
        // containsKey is case‑insensitive
        assertTrue(map.containsKey("kEy"));
        // remove works case‑insensitively
        assertEquals("Value", map.remove("KeY"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>(10);
        map.put("A", 1);
        assertEquals(Integer.valueOf(1), map.get("a"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeInitialCapacity() {
        new CaseInsensitiveMap<String, String>(-5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeLoadFactor() {
        new CaseInsensitiveMap<String, String>(10, -0.75f);
    }

    @Test
    public void testCopyConstructorWithSourceMap() {
        Map<String, String> source = new HashMap<>();
        source.put("First", "1");
        source.put("Second", "2");
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>(source);
        assertEquals("1", map.get("first"));
        assertEquals("2", map.get("SECOND"));
        assertEquals(2, map.size());
    }

    @Test(expected = NullPointerException.class)
    public void testCopyConstructorNullSource() {
        new CaseInsensitiveMap<String, String>((Map<String, String>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCopyConstructorFromIdentityHashMap() {
        Map<String, String> idMap = new IdentityHashMap<>();
        idMap.put("one", "1");
        new CaseInsensitiveMap<>(idMap);
    }

    @Test
    public void testGetWrappedMap() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Alpha", "A");
        Map<String, String> wrapped = map.getWrappedMap();
        assertTrue(wrapped instanceof Map);
        // underlying key is a CaseInsensitiveString, not the original String
        Object internalKey = wrapped.keySet().iterator().next();
        assertTrue(internalKey instanceof CaseInsensitiveMap.CaseInsensitiveString);
        assertEquals("Alpha", internalKey.toString());
    }

    @Test
    public void testKeySetIterationPreservesOriginalCase() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("One", 1);
        map.put("TWO", 2);
        Set<String> keys = map.keySet();
        List<String> collected = new ArrayList<>();
        for (String k : keys) {
            collected.add(k);
        }
        // order is insertion order (LinkedHashMap by default)
        assertEquals(Arrays.asList("One", "TWO"), collected);
    }

    @Test
    public void testEntrySetAndCaseInsensitiveEntry() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Hello", "World");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertEquals(1, entrySet.size());
        Map.Entry<String, String> entry = entrySet.iterator().next();

        // The entry returned is a CaseInsensitiveEntry
        assertTrue(entry instanceof CaseInsensitiveMap.CaseInsensitiveEntry);
        // getKey returns the original string case
        assertEquals("Hello", entry.getKey());
        // getOriginalKey returns the internal CaseInsensitiveString
        CaseInsensitiveMap.CaseInsensitiveEntry cisEntry = (CaseInsensitiveMap.CaseInsensitiveEntry) entry;
        assertTrue(cisEntry.getOriginalKey() instanceof CaseInsensitiveMap.CaseInsensitiveString);
        // value matches
        assertEquals("World", entry.getValue());

        // test equals / hashCode of the entry
        Map.Entry<String, String> other = new AbstractMap.SimpleEntry<>("Hello", "World");
        assertTrue(entry.equals(other));
        assertEquals(entry.hashCode(), other.hashCode());
    }

    @Test
    public void testComputeIfAbsentKeyIsOriginalString() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("Test", 10);

        final List<String> capturedKey = new ArrayList<>();
        Integer result = map.computeIfAbsent("tESt", k -> {
            capturedKey.add(k);   // should receive original case "tESt"
            return 20;
        });
        // existing value should be returned, mapping function not called
        assertEquals(Integer.valueOf(10), result);
        assertTrue(capturedKey.isEmpty());

        // absent key triggers mapping function
        Integer newVal = map.computeIfAbsent("NEW", k -> {
            capturedKey.add(k);
            return 30;
        });
        assertEquals(Integer.valueOf(30), newVal);
        assertEquals(Collections.singletonList("NEW"), capturedKey);
    }

    @Test
    public void testComputeIfPresentKeyIsOriginalString() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Key", "old");

        final List<String> capturedKey = new ArrayList<>();
        String res = map.computeIfPresent("kEy", (k, v) -> {
            capturedKey.add(k); // should be "kEy"
            return v.toUpperCase();
        });
        assertEquals("OLD", res);
        assertEquals(Collections.singletonList("kEy"), capturedKey);
    }

    @Test
    public void testComputeKeyAndValue() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("A", 1);

        // compute on existing key
        Integer r1 = map.compute("a", (k, v) -> {
            assertEquals("a", k); // original case passed
            return v + 10;
        });
        assertEquals(Integer.valueOf(11), r1);
        assertEquals(Integer.valueOf(11), map.get("A"));

        // compute on absent key
        Integer r2 = map.compute("B", (k, v) -> {
            assertEquals("B", k);
            assertNull(v);
            return 5;
        });
        assertEquals(Integer.valueOf(5), r2);
        assertEquals(Integer.valueOf(5), map.get("b"));
    }

    @Test
    public void testMerge() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("X", 1);
        // merge when key exists
        Integer merged = map.merge("x", 2, Integer::sum);
        assertEquals(Integer.valueOf(3), merged);
        assertEquals(Integer.valueOf(3), map.get("X"));

        // merge when key absent
        Integer merged2 = map.merge("Y", 4, Integer::sum);
        assertEquals(Integer.valueOf(4), merged2);
        assertEquals(Integer.valueOf(4), map.get("y"));
    }

    @Test
    public void testPutIfAbsent() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        assertNull(map.putIfAbsent("Key", "First"));
        assertEquals("First", map.get("key"));
        // second call should return existing value and not replace
        assertEquals("First", map.putIfAbsent("kEy", "Second"));
        assertEquals("First", map.get("KEY"));
    }

    @Test
    public void testRemoveKeyValue() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Alpha", "A");
        // wrong value – nothing removed
        assertFalse(map.remove("ALPHA", "B"));
        assertTrue(map.containsKey("alpha"));
        // correct value – removed
        assertTrue(map.remove("ALpha", "A"));
        assertFalse(map.containsKey("alpha"));
    }

    @Test
    public void testReplaceThreeArg() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("One", "1");
        // old value mismatch
        assertFalse(map.replace("ONE", "2", "X"));
        assertEquals("1", map.get("one"));
        // old value matches
        assertTrue(map.replace("ONE", "1", "X"));
        assertEquals("X", map.get("one"));
    }

    @Test
    public void testReplaceTwoArg() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Key", "old");
        assertEquals("old", map.replace("kEy", "new"));
        assertEquals("new", map.get("KEY"));
    }

    @Test
    public void testForEachReceivesOriginalKey() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("First", 1);
        map.put("Second", 2);

        final List<String> visited = new ArrayList<>();
        map.forEach((k, v) -> visited.add(k));

        assertTrue(visited.containsAll(Arrays.asList("First", "Second")));
    }

    @Test
    public void testReplaceAllReceivesOriginalKey() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("A", 1);
        map.put("B", 2);

        map.replaceAll((k, v) -> {
            assertTrue(k.equals("A") || k.equals("B"));
            return v * 10;
        });
        assertEquals(Integer.valueOf(10), map.get("a"));
        assertEquals(Integer.valueOf(20), map.get("b"));
    }

    @Test
    public void testNonStringKeyBehaviour() {
        CaseInsensitiveMap<Integer, String> map = new CaseInsensitiveMap<>();
        map.put(100, "hundred");
        assertEquals("hundred", map.get(100));
        assertTrue(map.containsKey(100));
        map.remove(100);
        assertFalse(map.containsKey(100));
    }

    @Test
    public void testNullKeyBehaviour() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put(null, "nullValue");
        assertEquals("nullValue", map.get(null));
        assertTrue(map.containsKey(null));
        map.remove(null);
        assertFalse(map.containsKey(null));
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceCacheNull() {
        CaseInsensitiveMap.replaceCache(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMaxCacheLengthStringTooSmall() {
        CaseInsensitiveMap.setMaxCacheLengthString(5);
    }

    /* -------------------------------------------------------------
     *  3.  EDGE / EXCEPTION TESTS FOR STATIC REGISTRY METHODS
     * ------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testReplaceRegistryNullList() {
        CaseInsensitiveMap.replaceRegistry(null);
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceRegistryContainsNullEntry() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> bad = new ArrayList<>();
        bad.add(null);
        CaseInsensitiveMap.replaceRegistry(bad);
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceRegistryEntryWithNullKey() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> bad = new ArrayList<>();
        bad.add(new AbstractMap.SimpleEntry<>(null, size -> new HashMap<>()));
        CaseInsensitiveMap.replaceRegistry(bad);
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceRegistryEntryWithNullValue() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> bad = new ArrayList<>();
        bad.add(new AbstractMap.SimpleEntry<>(HashMap.class, null));
        CaseInsensitiveMap.replaceRegistry(bad);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplaceRegistryDuplicateClass() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> dup = new ArrayList<>();
        dup.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        dup.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        CaseInsensitiveMap.replaceRegistry(dup);
    }

    @Test(expected = IllegalStateException.class)
    public void testReplaceRegistryIncorrectOrder() {
        // Map.class (super) before HashMap.class (sub) violates ordering rule
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> badOrder = new ArrayList<>();
        badOrder.add(new AbstractMap.SimpleEntry<>(Map.class, size -> new HashMap<>()));
        badOrder.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        CaseInsensitiveMap.replaceRegistry(badOrder);
    }

    @Test(expected = IllegalStateException.class)
    public void testReplaceRegistryContainsUnsupportedIdentityHashMap() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> list = new ArrayList<>();
        list.add(new AbstractMap.SimpleEntry<>(IdentityHashMap.class, size -> new IdentityHashMap<>()));
        // add a valid entry after it to avoid empty list
        list.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        CaseInsensitiveMap.replaceRegistry(list);
    }

    /* -------------------------------------------------------------
     *  4.  CLEANUP (restore original registry after any successful replace)
     * ------------------------------------------------------------- */
    // The tests that successfully replace the registry are those that *throw* an exception,
    // therefore the original registry is never altered. No explicit cleanup is required.
}
