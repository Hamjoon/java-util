package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CaseInsensitiveMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction variants and error handling</li>
 *   <li>Basic Map operations (get, put, remove, containsKey, etc.)</li>
 *   <li>Case‑insensitive behaviour for {@code String} keys</li>
 *   <li>Preservation of original key case in {@code keySet()} and {@code entrySet()}</li>
 *   <li>Java 8 map methods (compute*, merge, replace*, forEach, replaceAll)</li>
 *   <li>Static configuration helpers (registry replacement, cache replacement, max‑cache length)</li>
 *   <li>Equality semantics and edge cases (null keys, non‑String keys, illegal arguments)</li>
 * </ul>
 *
 * The test class uses only the public API of {@code CaseInsensitiveMap},
 * {@code LRUCache}, and standard JDK classes – no mocking or subclassing.
 */
public class CaseInsensitiveMapTest {

    private CaseInsensitiveMap<String, String> map;

    @Before
    public void setUp() {
        map = new CaseInsensitiveMap<>();
    }

    /* -------------------------------------------------------
     *  Construction & basic error handling
     * ------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        CaseInsensitiveMap<String, Integer> m = new CaseInsensitiveMap<>(10);
        assertTrue(m.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeInitialCapacity() {
        new CaseInsensitiveMap<String, Integer>(-5);
    }

    @Test
    public void testConstructorWithInitialCapacityAndLoadFactor() {
        CaseInsensitiveMap<String, Integer> m = new CaseInsensitiveMap<>(10, 0.75f);
        assertTrue(m.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeLoadFactor() {
        new CaseInsensitiveMap<String, Integer>(10, -0.5f);
    }

    @Test
    public void testCopyConstructorCopiesEntriesAndPreservesBackingMapType() {
        TreeMap<String, String> source = new TreeMap<>();
        source.put("Alpha", "A");
        source.put("Beta", "B");

        CaseInsensitiveMap<String, String> copied = new CaseInsensitiveMap<>(source);
        assertEquals(2, copied.size());

        // Backing map should be a TreeMap (the same type as source)
        assertTrue(copied.getWrappedMap() instanceof TreeMap);
    }

    @Test(expected = NullPointerException.class)
    public void testCopyConstructorWithNullSourceThrowsNPE() {
        new CaseInsensitiveMap<String, String>((Map<String, String>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetermineBackingMapRejectsIdentityHashMap() {
        Map<String, String> src = new IdentityHashMap<>();
        src.put("a", "1");
        new CaseInsensitiveMap<>(src);
    }

    /* -------------------------------------------------------
     *  Basic Map operations (String keys)
     * ------------------------------------------------------- */

    @Test
    public void testPutAndGetCaseInsensitive() {
        map.put("KeyOne", "value1");
        assertEquals("value1", map.get("keyone"));
        assertEquals("value1", map.get("KEYONE"));
    }

    @Test
    public void testContainsKeyCaseInsensitive() {
        map.put("Hello", "world");
        assertTrue(map.containsKey("hello"));
        assertTrue(map.containsKey("HELLO"));
        assertFalse(map.containsKey("Hi"));
    }

    @Test
    public void testRemoveCaseInsensitive() {
        map.put("RemoveMe", "x");
        assertEquals("x", map.remove("removeme"));
        assertFalse(map.containsKey("RemoveMe"));
    }

    @Test
    public void testNullKeyBehaviour() {
        map.put(null, "nullValue");
        assertTrue(map.containsKey(null));
        assertEquals("nullValue", map.get(null));
        assertEquals("nullValue", map.remove(null));
        assertFalse(map.containsKey(null));
    }

    /* -------------------------------------------------------
     *  Non‑String keys (behave like a normal map)
     * ------------------------------------------------------- */

    @Test
    public void testNonStringKeyOperations() {
        CaseInsensitiveMap<Integer, String> intMap = new CaseInsensitiveMap<>();
        intMap.put(42, "answer");
        assertEquals("answer", intMap.get(42));
        assertTrue(intMap.containsKey(42));
        assertEquals("answer", intMap.remove(42));
        assertFalse(intMap.containsKey(42));
    }

    /* -------------------------------------------------------
     *  Preservation of original key case in keySet & entrySet
     * ------------------------------------------------------- */

    @Test
    public void testKeySetReturnsOriginalCase() {
        map.put("MixedCase", "v");
        Set<String> keys = map.keySet();
        assertTrue(keys.contains("MixedCase"));
        assertFalse(keys.contains("mixedcase"));
        Iterator<String> it = keys.iterator();
        assertEquals("MixedCase", it.next());
    }

    @Test
    public void testEntrySetKeyPreservesOriginalCase() {
        map.put("FooBar", "baz");
        Set<Map.Entry<String, String>> entries = map.entrySet();
        assertEquals(1, entries.size());
        Map.Entry<String, String> e = entries.iterator().next();
        assertEquals("FooBar", e.getKey());
        assertEquals("baz", e.getValue());
    }

    @Test
    public void testEntrySetToArrayPreservesOriginalKey() {
        map.put("KeyA", "1");
        map.put("KeyB", "2");
        Object[] array = map.entrySet().toArray();
        Set<String> keys = new HashSet<>();
        for (Object o : array) {
            @SuppressWarnings("unchecked")
            Map.Entry<String, String> entry = (Map.Entry<String, String>) o;
            keys.add(entry.getKey());
        }
        assertTrue(keys.containsAll(Arrays.asList("KeyA", "KeyB")));
    }

    @Test
    public void testKeySetToArrayWithSuppliedArray() {
        map.put("One", "1");
        String[] a = new String[2];
        String[] result = map.keySet().toArray(a);
        assertSame(a, result);
        assertEquals("One", result[0]);
        assertNull(result[1]); // extra slot should be null
    }

    @Test
    public void testKeySetRetainAll() {
        map.put("A", "1");
        map.put("B", "2");
        map.put("C", "3");
        List<String> keep = Arrays.asList("b", "c");
        assertTrue(map.keySet().retainAll(keep));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("B"));
        assertTrue(map.containsKey("C"));
        assertFalse(map.containsKey("A"));
    }

    /* -------------------------------------------------------
     *  Equality semantics
     * ------------------------------------------------------- */

    @Test
    public void testEqualsIgnoresStringCase() {
        CaseInsensitiveMap<String, Integer> m1 = new CaseInsensitiveMap<>();
        CaseInsensitiveMap<String, Integer> m2 = new CaseInsensitiveMap<>();
        m1.put("Xyz", 10);
        m2.put("xyz", 10);
        assertTrue(m1.equals(m2));
        assertTrue(m2.equals(m1));
    }

    @Test
    public void testEqualsWithDifferentValues() {
        map.put("Key", "v1");
        CaseInsensitiveMap<String, String> other = new CaseInsensitiveMap<>();
        other.put("key", "v2");
        assertFalse(map.equals(other));
    }

    @Test
    public void testEqualsWithNonMapObject() {
        assertFalse(map.equals("not a map"));
    }

    /* -------------------------------------------------------
     *  Java 8 map methods – key transparency
     * ------------------------------------------------------- */

    @Test
    public void testComputeIfAbsentProvidesOriginalKey() {
        Function<String, String> mapping = k -> "valueFor-" + k; // k should be original case
        map.put("AbC", "old");
        String result = map.computeIfAbsent("abc", mapping);
        assertEquals("old", result); // existing entry, mapping not called
        map.remove("AbC");
        result = map.computeIfAbsent("Abc", mapping);
        assertEquals("valueFor-Abc", result);
        assertEquals("valueFor-Abc", map.get("ABC"));
    }

    @Test
    public void testComputeIfPresentProvidesOriginalKey() {
        BiFunction<String, String, String> remapper = (k, v) -> v + "-" + k;
        map.put("Hello", "World");
        String result = map.computeIfPresent("hello", remapper);
        assertEquals("World-Hello", result);
        assertEquals("World-Hello", map.get("HELLO"));
    }

    @Test
    public void testComputeProvidesOriginalKey() {
        BiFunction<String, String, String> remapper = (k, v) -> (v == null ? "new-" : v + "-") + k;
        String result = map.compute("newKey", remapper);
        assertEquals("new-newKey", result);
        assertEquals("new-newKey", map.get("NEWKEY"));
    }

    @Test
    public void testMergeWorksCaseInsensitive() {
        map.put("MergeMe", "first");
        BiFunction<String, String, String> merger = (oldV, newV) -> oldV + "+" + newV;
        String result = map.merge("mergeme", "second", merger);
        assertEquals("first+second", result);
        assertEquals("first+second", map.get("MERGEME"));
    }

    @Test
    public void testPutIfAbsentCaseInsensitive() {
        map.put("Key", "value");
        assertEquals("value", map.putIfAbsent("KEY", "newValue"));
        assertEquals("value", map.get("key"));
    }

    @Test
    public void testRemoveKeyValuePairCaseInsensitive() {
        map.put("Del", "val");
        assertTrue(map.remove("DEL", "val"));
        assertFalse(map.containsKey("del"));
    }

    @Test
    public void testReplaceThreeArgCaseInsensitive() {
        map.put("R", "old");
        assertTrue(map.replace("r", "old", "new"));
        assertEquals("new", map.get("R"));
    }

    @Test
    public void testReplaceTwoArgCaseInsensitive() {
        map.put("X", "old");
        assertEquals("old", map.replace("x", "new"));
        assertEquals("new", map.get("X"));
    }

    @Test
    public void testReplaceAllSeesOriginalKey() {
        map.put("One", "1");
        map.put("Two", "2");
        BiFunction<String, String, String> func = (k, v) -> v + "-" + k;
        map.replaceAll(func);
        assertEquals("1-One", map.get("ONE"));
        assertEquals("2-Two", map.get("two"));
    }

    @Test
    public void testForEachProvidesOriginalKey() {
        map.put("Alpha", "A");
        map.put("Beta", "B");
        List<String> seen = new ArrayList<>();
        BiConsumer<String, String> consumer = (k, v) -> seen.add(k + ":" + v);
        map.forEach(consumer);
        assertTrue(seen.contains("Alpha:A"));
        assertTrue(seen.contains("Beta:B"));
    }

    /* -------------------------------------------------------
     *  Static configuration helpers & error handling
     * ------------------------------------------------------- */

    @Test
    public void testReplaceCacheAcceptsNewCache() {
        LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> newCache = new LRUCache<>(5);
        CaseInsensitiveMap.replaceCache(newCache);
        // No exception means success; we also verify that the cache field is updated via a simple operation
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        m.put("CacheTest", "v");
        assertEquals("v", m.get("cachetest"));
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceCacheRejectsNull() {
        CaseInsensitiveMap.replaceCache(null);
    }

    @Test
    public void testSetMaxCacheLengthStringAcceptsValidValue() {
        CaseInsensitiveMap.setMaxCacheLengthString(200);
        // Verify that a long string (>200) still works (no exception)
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>();
        String longStr = new String(new char[250]).replace('\0', 'a');
        m.put(longStr, "long");
        assertEquals("long", m.get(longStr));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMaxCacheLengthStringRejectsTooSmall() {
        CaseInsensitiveMap.setMaxCacheLengthString(5);
    }

    @Test
    public void testReplaceRegistryUpdatesBackingMapSelection() {
        // Build a custom registry that maps LinkedHashMap to a fresh LinkedHashMap instance
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> custom = new ArrayList<>();
        custom.add(new AbstractMap.SimpleEntry<>(LinkedHashMap.class,
                size -> new LinkedHashMap<Integer, Integer>(size) {
                    // Anonymous subclass to detect usage
                    @Override
                    public String toString() {
                        return "CustomLinkedHashMap";
                    }
                }));
        CaseInsensitiveMap.replaceRegistry(custom);

        // Source is a LinkedHashMap; the new registry should be used.
        LinkedHashMap<String, String> src = new LinkedHashMap<>();
        src.put("X", "1");
        CaseInsensitiveMap<String, String> cim = new CaseInsensitiveMap<>(src);
        assertTrue(cim.getWrappedMap().toString().contains("CustomLinkedHashMap"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplaceRegistryDetectsDuplicateClass() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> dup = new ArrayList<>();
        dup.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        dup.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        CaseInsensitiveMap.replaceRegistry(dup);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplaceRegistryDetectsIncorrectOrder() {
        // TreeMap is assignable from NavigableMap, so placing NavigableMap before TreeMap is invalid
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> badOrder = new ArrayList<>();
        badOrder.add(new AbstractMap.SimpleEntry<>(NavigableMap.class, size -> new TreeMap<>()));
        badOrder.add(new AbstractMap.SimpleEntry<>(TreeMap.class, size -> new TreeMap<>()));
        CaseInsensitiveMap.replaceRegistry(badOrder);
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceRegistryRejectsNullList() {
        CaseInsensitiveMap.replaceRegistry(null);
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceRegistryRejectsNullEntry() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> list = new ArrayList<>();
        list.add(null);
        CaseInsensitiveMap.replaceRegistry(list);
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceRegistryRejectsEntryWithNullKey() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> list = new ArrayList<>();
        list.add(new AbstractMap.SimpleEntry<>(null, size -> new HashMap<>()));
        CaseInsensitiveMap.replaceRegistry(list);
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceRegistryRejectsEntryWithNullFactory() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> list = new ArrayList<>();
        list.add(new AbstractMap.SimpleEntry<>(HashMap.class, null));
        CaseInsensitiveMap.replaceRegistry(list);
    }

    /* -------------------------------------------------------
     *  Additional edge cases for Map contract methods
     * ------------------------------------------------------- */

    @Test
    public void testClearAndIsEmpty() {
        map.put("a", "1");
        map.put("b", "2");
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testContainsValue() {
        map.put("Key", "value");
        assertTrue(map.containsValue("value"));
        assertFalse(map.containsValue("other"));
    }

    @Test
    public void testValuesCollectionReflectsUpdates() {
        map.put("K1", "V1");
        Collection<String> values = map.values();
        assertTrue(values.contains("V1"));
        map.put("K2", "V2");
        assertTrue(values.contains("V2"));
        map.remove("K1");
        assertFalse(values.contains("V1"));
    }

    @Test
    public void testKeySetRemoveReflectsMap() {
        map.put("ToRemove", "x");
        assertTrue(map.keySet().remove("toremove"));
        assertFalse(map.containsKey("ToRemove"));
    }

    @Test
    public void testEntrySetRemoveReflectsMap() {
        map.put("EKey", "EVal");
        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        assertTrue(map.entrySet().remove(entry));
        assertFalse(map.containsKey("EKey"));
    }

    @Test
    public void testEntrySetRemoveAll() {
        map.put("A", "1");
        map.put("B", "2");
        Set<Map.Entry<String, String>> entries = map.entrySet();
        List<Map.Entry<String, String>> toRemove = new ArrayList<>(entries);
        assertTrue(entries.removeAll(toRemove));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testEntrySetRetainAllEmptyCollectionClearsMap() {
        map.put("X", "1");
        map.put("Y", "2");
        assertTrue(map.entrySet().retainAll(Collections.emptyList()));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testEntrySetRetainAllKeepsOnlySpecifiedEntries() {
        map.put("One", "1");
        map.put("Two", "2");
        map.put("Three", "3");
        Set<Map.Entry<String, String>> entries = map.entrySet();
        Set<Map.Entry<String, String>> keep = new HashSet<>();
        keep.add(new CaseInsensitiveMap<String, String>.CaseInsensitiveEntry(
                new AbstractMap.SimpleEntry<>("Two", "2")));
        assertTrue(entries.retainAll(keep));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("Two"));
    }

    @Test
    public void testKeySetToArrayWithExactSizeArray() {
        map.put("X", "1");
        String[] arr = new String[1];
        String[] result = map.keySet().toArray(arr);
        assertSame(arr, result);
        assertEquals("X", result[0]);
    }
}
