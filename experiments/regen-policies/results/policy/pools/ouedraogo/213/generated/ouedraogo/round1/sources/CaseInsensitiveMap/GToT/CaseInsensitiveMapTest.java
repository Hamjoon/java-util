package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CaseInsensitiveMap}.
 *
 * <p>The tests are organized in the order the class specification asked for:
 * <ol>
 *   <li>Extraction of public API (listed in comments).</li>
 *   <li>Basic functionality tests for each public method.</li>
 *   <li>Identification of edge‑cases &amp; exception scenarios.</li>
 *   <li>Targeted tests for those edge‑cases.</li>
 *   <li>All tests merged into one file.</li>
 * </ol>
 *
 * <p>The suite only uses APIs that are present in the supplied source code
 * (including {@link LRUCache} and the nested classes {@code CaseInsensitiveString}
 * and {@code CaseInsensitiveEntry}).  No external mocking frameworks are used.
 */
public class CaseInsensitiveMapTest {

    /* -----------------------------------------------------------------------
     *  Public API extracted from the source (for reference only)
     * -----------------------------------------------------------------------
     *
     * Constructors
     *   CaseInsensitiveMap()
     *   CaseInsensitiveMap(int initialCapacity)
     *   CaseInsensitiveMap(int initialCapacity, float loadFactor)
     *   CaseInsensitiveMap(Map<K,V> source, Map<K,V> mapInstance)
     *   CaseInsensitiveMap(Map<K,V> source)
     *
     * Static methods
     *   static void replaceRegistry(List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> newRegistry)
     *   static void replaceCache(LRUCache lruCache)
     *   static void setMaxCacheLengthString(int length)
     *
     * Instance methods
     *   V get(Object key)
     *   boolean containsKey(Object key)
     *   V put(K key, V value)
     *   V remove(Object key)
     *   boolean equals(Object other)
     *   Map<K,V> getWrappedMap()
     *   Set<K> keySet()
     *   Set<Entry<K,V>> entrySet()
     *   V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
     *   V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
     *   V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
     *   V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)
     *   V putIfAbsent(K key, V value)
     *   boolean remove(Object key, Object value)
     *   boolean replace(K key, V oldValue, V newValue)
     *   V replace(K key, V value)
     *   void forEach(BiConsumer<? super K, ? super V> action)
     *   void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)
     *
     * -----------------------------------------------------------------------
     */

    private CaseInsensitiveMap<String, String> map;

    @Before
    public void setUp() {
        map = new CaseInsensitiveMap<>();
    }

    @After
    public void tearDown() {
        // Reset static configuration after each test to avoid cross‑test pollution
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        // Restore default registry (the static initializer already did that)
    }

    /* -----------------------------------------------------------------------
     *  Basic functionality tests
     * ----------------------------------------------------------------------- */

    @Test
    public void testPutAndGetCaseInsensitive() {
        map.put("KeyOne", "value1");
        assertEquals("value1", map.get("keyone"));
        assertEquals("value1", map.get("KEYONE"));
        // original case should be preserved in keySet
        Set<String> keys = map.keySet();
        assertTrue(keys.contains("KeyOne"));
        assertFalse(keys.contains("keyone"));
    }

    @Test
    public void testPutAndGetNonStringKey() {
        CaseInsensitiveMap<Integer, String> intMap = new CaseInsensitiveMap<>();
        intMap.put(42, "answer");
        assertEquals("answer", intMap.get(42));
        assertNull(intMap.get(43));
    }

    @Test
    public void testContainsKeyCaseInsensitive() {
        map.put("MixedCase", "v");
        assertTrue(map.containsKey("mixedcase"));
        assertTrue(map.containsKey("MIXEDCASE"));
        assertFalse(map.containsKey("other"));
    }

    @Test
    public void testRemoveCaseInsensitive() {
        map.put("ToRemove", "x");
        assertEquals("x", map.remove("toremove"));
        assertFalse(map.containsKey("ToRemove"));
        assertNull(map.remove("nonexistent"));
    }

    @Test
    public void testEqualsWithDifferentMapImplementations() {
        Map<String, String> hash = new HashMap<>();
        hash.put("a", "1");
        hash.put("B", "2");

        CaseInsensitiveMap<String, String> ci = new CaseInsensitiveMap<>(hash);
        Map<String, String> tree = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        tree.putAll(hash);

        // ci should be equal to tree (case‑insensitive semantics)
        assertTrue(ci.equals(tree));
        assertTrue(tree.equals(ci));
    }

    @Test
    public void testKeySetPreservesOriginalCase() {
        map.put("First", "1");
        map.put("second", "2");
        List<String> keyList = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("First", "second"), keyList);
    }

    @Test
    public void testEntrySetOriginalKeyPreserved() {
        map.put("Alpha", "A");
        map.put("beta", "B");
        Set<Map.Entry<String, String>> entries = map.entrySet();
        for (Map.Entry<String, String> e : entries) {
            if (e.getKey().equals("Alpha")) {
                assertEquals("A", e.getValue());
                assertTrue(e instanceof CaseInsensitiveMap.CaseInsensitiveEntry);
                // the entry's getKey returns original case
                assertEquals("Alpha", e.getKey());
            }
            if (e.getKey().equals("beta")) {
                assertEquals("B", e.getValue());
            }
        }
    }

    @Test
    public void testComputeIfAbsentKeyUnwrapped() {
        map.computeIfAbsent("CompKey", k -> {
            // k must be the original String, not a CaseInsensitiveString
            assertEquals("CompKey", k);
            return "computed";
        });
        assertEquals("computed", map.get("compkey"));
    }

    @Test
    public void testComputeIfPresentKeyUnwrapped() {
        map.put("Present", "old");
        map.computeIfPresent("present", (k, v) -> {
            assertEquals("present", k); // original case is passed (same value as key)
            return v + "+new";
        });
        assertEquals("old+new", map.get("PRESENT"));
    }

    @Test
    public void testComputeKeyUnwrapped() {
        map.put("calc", "10");
        map.compute("CALC", (k, v) -> {
            assertEquals("CALC", k);
            return Integer.toString(Integer.parseInt(v) + 5);
        });
        assertEquals("15", map.get("calc"));
    }

    @Test
    public void testMerge() {
        map.put("merge", "a");
        map.merge("MERGE", "b", (v1, v2) -> v1 + v2);
        assertEquals("ab", map.get("merge"));
        // merging a non‑existing key should just insert the value
        map.merge("newKey", "c", (v1, v2) -> v1 + v2);
        assertEquals("c", map.get("newkey"));
    }

    @Test
    public void testPutIfAbsent() {
        map.put("dup", "first");
        assertEquals("first", map.putIfAbsent("DUP", "second"));
        assertEquals("first", map.get("dup"));
        assertNull(map.putIfAbsent("new", "val"));
        assertEquals("val", map.get("NEW"));
    }

    @Test
    public void testRemoveWithValue() {
        map.put("key", "val");
        assertTrue(map.remove("KEY", "val"));
        assertFalse(map.containsKey("key"));
        // wrong value – nothing removed
        map.put("key2", "v2");
        assertFalse(map.remove("KEY2", "wrong"));
        assertTrue(map.containsKey("key2"));
    }

    @Test
    public void testReplaceThreeArg() {
        map.put("r", "old");
        assertTrue(map.replace("R", "old", "new"));
        assertEquals("new", map.get("r"));
        assertFalse(map.replace("R", "old", "new2"));
    }

    @Test
    public void testReplaceTwoArg() {
        map.put("simple", "x");
        assertEquals("x", map.replace("SIMPLE", "y"));
        assertEquals("y", map.get("simple"));
    }

    @Test
    public void testForEachKeyUnwrapped() {
        map.put("one", "1");
        map.put("Two", "2");
        Map<String, String> collected = new HashMap<>();
        map.forEach((k, v) -> {
            // k must be original case string
            assertTrue(k instanceof String);
            collected.put(k, v);
        });
        assertEquals(2, collected.size());
        assertEquals("1", collected.get("one"));
        assertEquals("2", collected.get("Two"));
    }

    @Test
    public void testReplaceAllKeyUnwrapped() {
        map.put("a", "1");
        map.put("B", "2");
        map.replaceAll((k, v) -> k.toUpperCase() + ":" + v);
        assertEquals("A:1", map.get("a"));
        assertEquals("B:2", map.get("B"));
    }

    /* -----------------------------------------------------------------------
     *  Edge‑case / exception tests
     * ----------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testSetMaxCacheLengthStringTooSmall() {
        CaseInsensitiveMap.setMaxCacheLengthString(5);
    }

    @Test
    public void testSetMaxCacheLengthStringValid() {
        CaseInsensitiveMap.setMaxCacheLengthString(20);
        // No exception – internal static field is changed; we rely on not throwing
    }

    @Test
    public void testReplaceCacheAcceptsNonNullCache() {
        LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> newCache = new LRUCache<>(10);
        CaseInsensitiveMap.replaceCache(newCache);
        // after replacement, putting a new string should go through the new cache
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        m.put("cached", "v");
        // No exception – test passes if no NPE is thrown
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceCacheRejectsNull() {
        CaseInsensitiveMap.replaceCache(null);
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceRegistryRejectsNullList() {
        CaseInsensitiveMap.replaceRegistry(null);
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceRegistryRejectsNullEntry() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> bad = new ArrayList<>();
        bad.add(null);
        CaseInsensitiveMap.replaceRegistry(bad);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplaceRegistryRejectsDuplicateClass() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> dup = new ArrayList<>();
        dup.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        dup.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        CaseInsensitiveMap.replaceRegistry(dup);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorFromIdentityHashMapThrows() {
        Map<String, String> idMap = new IdentityHashMap<>();
        idMap.put("a", "b");
        new CaseInsensitiveMap<>(idMap);
    }

    @Test
    public void testDetermineBackingMapSelectsTreeMap() throws Exception {
        // Use a TreeMap source; the wrapped map should also be a TreeMap.
        TreeMap<String, String> source = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        source.put("x", "1");
        CaseInsensitiveMap<String, String> ci = new CaseInsensitiveMap<>(source);
        Map<String, String> wrapped = ci.getWrappedMap();
        assertTrue(wrapped instanceof TreeMap);
    }

    @Test
    public void testCopyFromAnotherCaseInsensitiveMap() {
        CaseInsensitiveMap<String, String> original = new CaseInsensitiveMap<>();
        original.put("Alpha", "A");
        original.put("beta", "B");
        CaseInsensitiveMap<String, String> copy = new CaseInsensitiveMap<>(original, new LinkedHashMap<>());
        // Both maps should contain the same logical entries
        assertEquals(original, copy);
        // The wrapped map of the copy should contain CaseInsensitiveString keys (internal)
        Map<String, String> wrapped = copy.getWrappedMap();
        for (Object k : wrapped.keySet()) {
            assertTrue(k instanceof CaseInsensitiveMap.CaseInsensitiveString);
        }
    }

    @Test
    public void testNullKeyIsHandled() {
        map.put(null, "nullValue");
        assertTrue(map.containsKey(null));
        assertEquals("nullValue", map.get(null));
        map.remove(null);
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testLongStringKeyBypassesCache() throws Exception {
        // Set cache length very low to guarantee bypass
        CaseInsensitiveMap.setMaxCacheLengthString(10);
        String longKey = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"; // length > 10
        map.put(longKey, "value");
        // Access the internal wrapped map via reflection to inspect the key object
        Map<?, ?> wrapped = map.getWrappedMap();
        Object internalKey = wrapped.keySet().iterator().next();
        assertTrue(internalKey instanceof CaseInsensitiveMap.CaseInsensitiveString);
        // Two separate long strings with same content should result in *different* instances
        CaseInsensitiveMap<String, String> map2 = new CaseInsensitiveMap<>();
        map2.put(longKey, "v2");
        Map<?, ?> wrapped2 = map2.getWrappedMap();
        Object internalKey2 = wrapped2.keySet().iterator().next();
        assertNotSame(internalKey, internalKey2);
    }

    /* -----------------------------------------------------------------------
     *  Additional sanity checks for collection views
     * ----------------------------------------------------------------------- */

    @Test
    public void testKeySetRemoveReflectsMap() {
        map.put("One", "1");
        map.put("Two", "2");
        Set<String> ks = map.keySet();
        assertTrue(ks.remove("one"));
        assertFalse(map.containsKey("ONE"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetRemoveReflectsMap() {
        map.put("a", "1");
        map.put("b", "2");
        Set<Map.Entry<String, String>> es = map.entrySet();
        Iterator<Map.Entry<String, String>> it = es.iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if (e.getKey().equals("a")) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetRetainAll() {
        map.put("x", "1");
        map.put("y", "2");
        map.put("z", "3");
        Set<Map.Entry<String, String>> es = map.entrySet();
        List<Map.Entry<String, String>> toKeep = new ArrayList<>();
        toKeep.add(new AbstractMap.SimpleEntry<>("x", "1"));
        toKeep.add(new AbstractMap.SimpleEntry<>("z", "3"));
        assertTrue(es.retainAll(toKeep));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey("z"));
        assertFalse(map.containsKey("y"));
    }

    @Test
    public void testKeySetToArray() {
        map.put("alpha", "A");
        map.put("beta", "B");
        String[] arr = map.keySet().toArray(new String[0]);
        assertArrayEquals(new String[]{"alpha", "beta"}, arr);
    }

    @Test
    public void testEntrySetToArrayTyped() {
        map.put("k1", "v1");
        map.put("k2", "v2");
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap.CaseInsensitiveEntry[] arr =
                map.entrySet().toArray(new CaseInsensitiveMap.CaseInsensitiveEntry[0]);
        Set<String> keys = new HashSet<>();
        for (CaseInsensitiveMap.CaseInsensitiveEntry e : arr) {
            keys.add(e.getKey());
        }
        assertEquals(new HashSet<>(Arrays.asList("k1", "k2")), keys);
    }

    /* -----------------------------------------------------------------------
     *  Registry replacement – sanity check that a custom registry works
     * ----------------------------------------------------------------------- */

    @Test
    public void testCustomRegistryWithConcurrentMap() {
        // Build a registry that prefers ConcurrentHashMap over the defaults
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> custom = new ArrayList<>();
        custom.add(new AbstractMap.SimpleEntry<>(ConcurrentHashMap.class,
                size -> new ConcurrentHashMap<>(size)));
        // add a fallback LinkedHashMap entry (must be after more specific ones)
        custom.add(new AbstractMap.SimpleEntry<>(LinkedHashMap.class,
                size -> new LinkedHashMap<>(size)));
        CaseInsensitiveMap.replaceRegistry(custom);

        // Source map is a ConcurrentHashMap; the resulting wrapped map should be a ConcurrentHashMap
        ConcurrentHashMap<String, String> src = new ConcurrentHashMap<>();
        src.put("c", "cVal");
        CaseInsensitiveMap<String, String> ci = new CaseInsensitiveMap<>(src);
        assertTrue(ci.getWrappedMap() instanceof ConcurrentHashMap);
    }

    @Test(expected = IllegalStateException.class)
    public void testValidateMappingsDetectsWrongOrder() {
        // Create a registry where a more general type precedes a specific one
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> badOrder = new ArrayList<>();
        badOrder.add(new AbstractMap.SimpleEntry<>(Map.class, size -> new HashMap<>()));
        badOrder.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        // Directly invoke the private validation method via reflection
        java.lang.reflect.Method m = CaseInsensitiveMap.class.getDeclaredMethod("validateMappings",
                List.class);
        m.setAccessible(true);
        m.invoke(null, badOrder);
    }

    /* -----------------------------------------------------------------------
     *  Verify that computeIfAbsent does not invoke the mapping function
     *  when the key already exists.
     * ----------------------------------------------------------------------- */
    @Test
    public void testComputeIfAbsentDoesNotRecompute() {
        map.put("exists", "old");
        Function<String, String> func = key -> {
            fail("Mapping function should not be called when key exists");
            return "new";
        };
        String result = map.computeIfAbsent("EXISTS", func);
        assertEquals("old", result);
    }

    /* -----------------------------------------------------------------------
     *  Verify that computeIfPresent does not invoke the remapping function
     *  when the key is absent.
     * ----------------------------------------------------------------------- */
    @Test
    public void testComputeIfPresentAbsent() {
        BiFunction<String, String, String> func = (k, v) -> {
            fail("Remapping function should not be called for absent key");
            return v;
        };
        assertNull(map.computeIfPresent("missing", func));
    }
}
