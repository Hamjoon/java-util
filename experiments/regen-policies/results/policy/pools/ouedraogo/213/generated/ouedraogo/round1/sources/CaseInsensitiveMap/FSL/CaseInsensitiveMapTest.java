package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link CaseInsensitiveMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors</li>
 *   <li>Basic map operations (put, get, containsKey, remove, etc.)</li>
 *   <li>Case‑insensitive handling of {@link String} keys</li>
 *   <li>Preservation of original key case in {@code keySet()} and {@code entrySet()}</li>
 *   <li>Map‑specific behaviours (computeIfAbsent, computeIfPresent, compute, merge, …)</li>
 *   <li>Utility static methods (replaceRegistry, replaceCache, setMaxCacheLengthString)</li>
 *   <li>Special behaviours of the inner classes {@code CaseInsensitiveEntry} and {@code CaseInsensitiveString}</li>
 * </ul>
 *
 * Only APIs that appear in the supplied source or listed dependency signatures are used.
 */
public class CaseInsensitiveMapTest {

    private CaseInsensitiveMap<String, String> map;

    @Before
    public void setUp() {
        map = new CaseInsensitiveMap<>();
    }

    /* -------------------------------------------------------
     *  Constructor and backing‑map selection tests
     * ------------------------------------------------------- */
    @Test
    public void testDefaultConstructorUsesLinkedHashMap() {
        assertTrue(map.getWrappedMap() instanceof LinkedHashMap);
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>(10);
        assertTrue(m.getWrappedMap() instanceof LinkedHashMap);
        assertEquals(0, m.size());
    }

    @Test
    public void testConstructorWithInitialCapacityAndLoadFactor() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>(10, 0.75f);
        assertTrue(m.getWrappedMap() instanceof LinkedHashMap);
        assertEquals(0, m.size());
    }

    @Test
    public void testCopyConstructorSelectsTreeMapWhenSourceIsTreeMap() {
        TreeMap<String, String> source = new TreeMap<>();
        source.put("A", "a");
        source.put("B", "b");
        CaseInsensitiveMap<String, String> copied = new CaseInsensitiveMap<>(source);
        assertTrue(copied.getWrappedMap() instanceof TreeMap);
        assertEquals("a", copied.get("a"));
        assertEquals("b", copied.get("B"));
    }

    @Test
    public void testCopyConstructorSelectsLinkedHashMapWhenSourceIsUnsupported() {
        HashMap<String, String> source = new HashMap<>();
        source.put("X", "x");
        CaseInsensitiveMap<String, String> copied = new CaseInsensitiveMap<>(source);
        assertTrue(copied.getWrappedMap() instanceof LinkedHashMap);
        assertEquals("x", copied.get("x"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetermineBackingMapRejectsIdentityHashMap() {
        Map<String, String> source = new IdentityHashMap<>();
        source.put("foo", "bar");
        new CaseInsensitiveMap<>(source);
    }

    @Test
    public void testCopyOptimizationWhenSourceIsCaseInsensitiveMap() {
        CaseInsensitiveMap<String, String> source = new CaseInsensitiveMap<>();
        source.put("KeyOne", "1");
        source.put("KeyTwo", "2");
        CaseInsensitiveMap<String, String> copy = new CaseInsensitiveMap<>(source, new LinkedHashMap<>());
        assertEquals(2, copy.size());
        assertEquals("1", copy.get("keyone"));
        assertEquals("2", copy.get("KEYTWO"));
    }

    /* -------------------------------------------------------
     *  Basic Map operations
     * ------------------------------------------------------- */
    @Test
    public void testPutAndGetCaseInsensitive() {
        map.put("Hello", "world");
        assertEquals("world", map.get("hello"));
        assertEquals("world", map.get("HELLO"));
    }

    @Test
    public void testContainsKeyCaseInsensitive() {
        map.put("MiXeD", "value");
        assertTrue(map.containsKey("mixed"));
        assertTrue(map.containsKey("MIXED"));
    }

    @Test
    public void testRemoveCaseInsensitive() {
        map.put("ToRemove", "val");
        assertEquals("val", map.remove("toremove"));
        assertNull(map.get("ToRemove"));
    }

    @Test
    public void testNonStringKeyBehaviour() {
        CaseInsensitiveMap<Integer, String> intMap = new CaseInsensitiveMap<>();
        intMap.put(42, "answer");
        assertEquals("answer", intMap.get(42));
        assertTrue(intMap.containsKey(42));
        assertEquals("answer", intMap.remove(42));
        assertFalse(intMap.containsKey(42));
    }

    @Test
    public void testNullKeyBehaviour() {
        map.put(null, "nullValue");
        assertEquals("nullValue", map.get(null));
        assertTrue(map.containsKey(null));
        assertEquals("nullValue", map.remove(null));
        assertFalse(map.containsKey(null));
    }

    /* -------------------------------------------------------
     *  keySet view tests
     * ------------------------------------------------------- */
    @Test
    public void testKeySetPreservesOriginalCase() {
        map.put("FirstKey", "1");
        map.put("secondKey", "2");
        List<String> keys = new ArrayList<>(map.keySet());
        assertTrue(keys.contains("FirstKey"));
        assertTrue(keys.contains("secondKey"));
    }

    @Test
    public void testKeySetRemoveUpdatesMap() {
        map.put("RemoveMe", "x");
        assertTrue(map.keySet().remove("removeme"));
        assertFalse(map.containsKey("RemoveMe"));
        assertEquals(0, map.size());
    }

    @Test
    public void testKeySetRetainAll() {
        map.put("A", "1");
        map.put("B", "2");
        map.put("C", "3");
        List<String> toRetain = Collections.singletonList("b");
        assertTrue(map.keySet().retainAll(toRetain));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("B"));
    }

    @Test
    public void testKeySetToArray() {
        map.put("Alpha", "a");
        map.put("Beta", "b");
        String[] array = map.keySet().toArray(new String[0]);
        assertArrayEquals(new String[]{"Alpha", "Beta"}, array);
    }

    /* -------------------------------------------------------
     *  entrySet view tests
     * ------------------------------------------------------- */
    @Test
    public void testEntrySetIterationPreservesOriginalKey() {
        map.put("One", "1");
        map.put("Two", "2");
        for (Map.Entry<String, String> e : map.entrySet()) {
            assertTrue(e.getKey().equals("One") || e.getKey().equals("Two"));
        }
    }

    @Test
    public void testEntrySetContains() {
        map.put("Key", "val");
        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        assertTrue(map.entrySet().contains(entry));
    }

    @Test
    public void testEntrySetRemove() {
        map.put("Del", "gone");
        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        assertTrue(map.entrySet().remove(entry));
        assertFalse(map.containsKey("Del"));
    }

    @Test
    public void testEntrySetRetainAll() {
        map.put("X", "1");
        map.put("Y", "2");
        map.put("Z", "3");
        List<Map.Entry<String, String>> retain = new ArrayList<>();
        retain.add(new CaseInsensitiveMap<String, String>.CaseInsensitiveEntry(
                new AbstractMap.SimpleEntry<>("Y", "2")));
        assertTrue(map.entrySet().retainAll(retain));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("Y"));
    }

    @Test
    public void testEntrySetToArray() {
        map.put("K1", "v1");
        map.put("K2", "v2");
        Object[] array = map.entrySet().toArray();
        assertEquals(2, array.length);
        for (Object o : array) {
            assertTrue(o instanceof CaseInsensitiveMap.CaseInsensitiveEntry);
        }
    }

    @Test
    public void testCaseInsensitiveEntryOriginalKey() {
        map.put("Original", "val");
        CaseInsensitiveMap<String, String>.CaseInsensitiveEntry entry =
                (CaseInsensitiveMap<String, String>.CaseInsensitiveEntry) map.entrySet().iterator().next();
        assertEquals("Original", entry.getKey());               // original case
        assertEquals("Original", entry.getOriginalKey().toString()); // internal CIS string
    }

    @Test
    public void testCaseInsensitiveEntryEqualsAndHashCode() {
        CaseInsensitiveMap<String, String>.CaseInsensitiveEntry e1 =
                new CaseInsensitiveMap<String, String>.CaseInsensitiveEntry(
                        new AbstractMap.SimpleEntry<>("same", "v"));
        CaseInsensitiveMap<String, String>.CaseInsensitiveEntry e2 =
                new CaseInsensitiveMap<String, String>.CaseInsensitiveEntry(
                        new AbstractMap.SimpleEntry<>("same", "v"));
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    /* -------------------------------------------------------
     *  compute / merge family
     * ------------------------------------------------------- */
    @Test
    public void testComputeIfAbsentReceivesOriginalKey() {
        final String[] receivedKey = new String[1];
        map.computeIfAbsent("Abc", k -> {
            receivedKey[0] = k;          // should be "Abc", not a CaseInsensitiveString
            return "computed";
        });
        assertEquals("Abc", receivedKey[0]);
        assertEquals("computed", map.get("aBc"));
    }

    @Test
    public void testComputeIfPresentReceivesOriginalKey() {
        map.put("XYZ", "old");
        final String[] receivedKey = new String[1];
        map.computeIfPresent("xyz", (k, v) -> {
            receivedKey[0] = k;
            return v + "new";
        });
        assertEquals("XYZ", receivedKey[0]); // original case stored
        assertEquals("oldnew", map.get("XYZ"));
    }

    @Test
    public void testComputeReplacesValueAndReceivesOriginalKey() {
        map.put("Key", "val");
        final String[] receivedKey = new String[1];
        map.compute("KEY", (k, v) -> {
            receivedKey[0] = k;
            return v + "+";
        });
        assertEquals("Key", receivedKey[0]);
        assertEquals("val+", map.get("key"));
    }

    @Test
    public void testMergeWorksCaseInsensitive() {
        map.put("MergeMe", "first");
        map.merge("MERGEME", "second", (v1, v2) -> v1 + "|" + v2);
        assertEquals("first|second", map.get("mergeme"));
    }

    /* -------------------------------------------------------
     *  putIfAbsent, remove(key,value), replace family
     * ------------------------------------------------------- */
    @Test
    public void testPutIfAbsentDoesNotOverwrite() {
        map.put("once", "1");
        assertEquals("1", map.putIfAbsent("ONCE", "2"));
        assertEquals("1", map.get("once"));
    }

    @Test
    public void testRemoveKeyValueReturnsTrueWhenMatch() {
        map.put("pair", "val");
        assertTrue(map.remove("PAIR", "val"));
        assertFalse(map.containsKey("pair"));
    }

    @Test
    public void testRemoveKeyValueReturnsFalseWhenValueDiffers() {
        map.put("pair", "val");
        assertFalse(map.remove("PAIR", "other"));
        assertTrue(map.containsKey("pair"));
    }

    @Test
    public void testReplaceThreeArgSuccess() {
        map.put("rKey", "old");
        assertTrue(map.replace("RKEY", "old", "new"));
        assertEquals("new", map.get("rkey"));
    }

    @Test
    public void testReplaceThreeArgFailure() {
        map.put("rKey", "old");
        assertFalse(map.replace("RKEY", "wrong", "new"));
        assertEquals("old", map.get("rkey"));
    }

    @Test
    public void testReplaceOneArgReturnsOldValue() {
        map.put("single", "first");
        assertEquals("first", map.replace("SINGLE", "second"));
        assertEquals("second", map.get("single"));
    }

    /* -------------------------------------------------------
     *  forEach and replaceAll
     * ------------------------------------------------------- */
    @Test
    public void testForEachPassesOriginalKey() {
        map.put("Loop", "1");
        final String[] keySeen = new String[1];
        map.forEach((k, v) -> keySeen[0] = k);
        assertEquals("Loop", keySeen[0]);
    }

    @Test
    public void testReplaceAllUpdatesValuesUsingOriginalKey() {
        map.put("A", "1");
        map.put("b", "2");
        map.replaceAll((k, v) -> k.toUpperCase() + ":" + v);
        assertEquals("A:1", map.get("a"));
        assertEquals("B:2", map.get("B"));
    }

    /* -------------------------------------------------------
     *  Static utility methods
     * ------------------------------------------------------- */
    @Test
    public void testReplaceCacheAcceptsNewCache() {
        LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> newCache = new LRUCache<>(10);
        CaseInsensitiveMap.replaceCache(newCache);
        // No exception means success; verify that a new string uses the new cache
        CaseInsensitiveMap.CaseInsensitiveString cis1 = CaseInsensitiveMap.CaseInsensitiveString.of("cacheTest");
        CaseInsensitiveMap.CaseInsensitiveString cis2 = CaseInsensitiveMap.CaseInsensitiveString.of("cacheTest");
        assertSame(cis1, cis2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMaxCacheLengthStringRejectsTooSmallValue() {
        CaseInsensitiveMap.setMaxCacheLengthString(5);
    }

    @Test
    public void testSetMaxCacheLengthStringAcceptsValidValue() {
        CaseInsensitiveMap.setMaxCacheLengthString(15);
        // Verify that a longer string (>15) is not cached while a short one is.
        String longStr = "abcdefghijklmnopqrstu"; // 21 chars
        String shortStr = "shortKey";

        CaseInsensitiveMap.CaseInsensitiveString cisLong1 = CaseInsensitiveMap.CaseInsensitiveString.of(longStr);
        CaseInsensitiveMap.CaseInsensitiveString cisLong2 = CaseInsensitiveMap.CaseInsensitiveString.of(longStr);
        // Not cached → distinct instances
        assertNotSame(cisLong1, cisLong2);

        CaseInsensitiveMap.CaseInsensitiveString cisShort1 = CaseInsensitiveMap.CaseInsensitiveString.of(shortStr);
        CaseInsensitiveMap.CaseInsensitiveString cisShort2 = CaseInsensitiveMap.CaseInsensitiveString.of(shortStr);
        // Cached → same instance
        assertSame(cisShort1, cisShort2);
    }

    @Test
    public void testReplaceRegistryWithValidList() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> newReg = new ArrayList<>();
        newReg.add(new AbstractMap.SimpleEntry<>(Hashtable.class, size -> new Hashtable<>()));
        newReg.add(new AbstractMap.SimpleEntry<>(TreeMap.class, size -> new TreeMap<>()));
        // Must be ordered most specific → most general; this list is valid.
        CaseInsensitiveMap.replaceRegistry(newReg);
        // Verify that a TreeMap source now yields a TreeMap backing map.
        TreeMap<String, String> src = new TreeMap<>();
        src.put("t", "v");
        CaseInsensitiveMap<String, String> cim = new CaseInsensitiveMap<>(src);
        assertTrue(cim.getWrappedMap() instanceof TreeMap);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplaceRegistryDetectsDuplicateClass() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> dup = new ArrayList<>();
        dup.add(new AbstractMap.SimpleEntry<>(Hashtable.class, size -> new Hashtable<>()));
        dup.add(new AbstractMap.SimpleEntry<>(Hashtable.class, size -> new Hashtable<>()));
        CaseInsensitiveMap.replaceRegistry(dup);
    }

    @Test(expected = IllegalStateException.class)
    public void testReplaceRegistryDetectsIncorrectOrder() {
        // Hashtable is a super‑class of ConcurrentHashMap; putting it after ConcurrentHashMap violates ordering.
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> badOrder = new ArrayList<>();
        badOrder.add(new AbstractMap.SimpleEntry<>(ConcurrentHashMap.class, size -> new ConcurrentHashMap<>()));
        badOrder.add(new AbstractMap.SimpleEntry<>(Hashtable.class, size -> new Hashtable<>()));
        // The static validator will throw.
        CaseInsensitiveMap.replaceRegistry(badOrder);
    }

    /* -------------------------------------------------------
     *  Direct conversion helper tests (convertKey)
     * ------------------------------------------------------- */
    @Test
    public void testConvertKeyWrapsStringIntoCaseInsensitiveString() throws Exception {
        // Use reflection to invoke the private convertKey method.
        java.lang.reflect.Method m = CaseInsensitiveMap.class.getDeclaredMethod("convertKey", Object.class);
        m.setAccessible(true);
        Object result = m.invoke(map, "MixedCase");
        assertTrue(result instanceof CaseInsensitiveMap.CaseInsensitiveString);
        assertEquals("MixedCase", result.toString());
    }

    @Test
    public void testConvertKeyLeavesNonStringUntouched() throws Exception {
        java.lang.reflect.Method m = CaseInsensitiveMap.class.getDeclaredMethod("convertKey", Object.class);
        m.setAccessible(true);
        Object result = m.invoke(map, 123);
        assertTrue(result instanceof Integer);
        assertEquals(123, result);
    }
}
