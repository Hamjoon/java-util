package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CaseInsensitiveMap}.
 *
 * The suite is organised as follows:
 * <ol>
 *   <li>Listing of all public API members (constructors + static methods + instance methods).</li>
 *   <li>Basic functional tests for each API member.</li>
 *   <li>Edge‑case / exception tests (null arguments, illegal arguments, duplicate registry entries, etc.).</li>
 *   <li>Integration tests that exercise interactions between the map and its internal case‑insensitive handling.</li>
 * </ol>
 *
 * The test class is deliberately self‑contained and does not rely on any external mocking framework.
 */
public class CaseInsensitiveMapTest {

    /* ----------------------------------------------------------------------
     *  1.  PUBLIC API LISTING (for reference – not used by the compiler)
     * ---------------------------------------------------------------------- */
    // static methods
    //   replaceRegistry(List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>>)
    //   replaceCache(LRUCache)
    //   setMaxCacheLengthString(int)
    // constructors
    //   CaseInsensitiveMap()
    //   CaseInsensitiveMap(int)
    //   CaseInsensitiveMap(int, float)
    //   CaseInsensitiveMap(Map<K,V>, Map<K,V>)
    //   CaseInsensitiveMap(Map<K,V>)
    // instance methods
    //   Map<K,V> getWrappedMap()
    //   V get(Object)
    //   boolean containsKey(Object)
    //   V put(K,V)
    //   V remove(Object)
    //   boolean equals(Object)
    //   Set<K> keySet()
    //   Set<Entry<K,V>> entrySet()
    //   V computeIfAbsent(K, Function<? super K,? extends V>)
    //   V computeIfPresent(K, BiFunction<? super K,? super V,? extends V>)
    //   V compute(K, BiFunction<? super K,? super V,? extends V>)
    //   V merge(K, V, BiFunction<? super V,? super V,? extends V>)
    //   V putIfAbsent(K,V)
    //   boolean remove(Object key, Object value)
    //   boolean replace(K, V oldValue, V newValue)
    //   V replace(K, V)
    //   void forEach(BiConsumer<? super K,? super V>)
    //   void replaceAll(BiFunction<? super K,? super V,? extends V>)

    /* ----------------------------------------------------------------------
     *  2.  TEST FIXTURE
     * ---------------------------------------------------------------------- */
    private CaseInsensitiveMap<String, String> map;

    @Before
    public void setUp() {
        map = new CaseInsensitiveMap<>();
    }

    @After
    public void tearDown() {
        // Reset static configuration that may have been altered by tests
        CaseInsensitiveMap.replaceCache(new LRUCache<>(1000));
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    /* ----------------------------------------------------------------------
     *  3.  BASIC FUNCTIONAL TESTS
     * ---------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testInitialCapacityConstructor() {
        CaseInsensitiveMap<String,String> m = new CaseInsensitiveMap<>(10);
        assertTrue(m.isEmpty());
    }

    @Test
    public void testInitialCapacityAndLoadFactorConstructor() {
        CaseInsensitiveMap<String,String> m = new CaseInsensitiveMap<>(10, 0.75f);
        assertTrue(m.isEmpty());
    }

    @Test
    public void testCopyConstructorPreservesEntriesAndCaseInsensitivity() {
        Map<String,String> src = new HashMap<>();
        src.put("One", "1");
        src.put("Two", "2");
        CaseInsensitiveMap<String,String> copied = new CaseInsensitiveMap<>(src);
        // case‑insensitive lookup
        assertEquals("1", copied.get("ONE"));
        assertEquals("2", copied.get("two"));
        // original case is preserved in keySet
        assertTrue(copied.keySet().contains("One"));
        assertFalse(copied.keySet().contains("ONE"));
    }

    @Test
    public void testCopyConstructorWithCustomBackingMap() {
        Map<String,String> src = new HashMap<>();
        src.put("A", "alpha");
        Map<String,String> backing = new LinkedHashMap<>();
        CaseInsensitiveMap<String,String> m = new CaseInsensitiveMap<>(src, backing);
        assertEquals("alpha", m.get("a"));
        assertSame(backing, m.getWrappedMap());
    }

    @Test
    public void testPutAndGetCaseInsensitive() {
        map.put("Key", "value");
        assertEquals("value", map.get("key"));
        assertEquals("value", map.get("KEY"));
    }

    @Test
    public void testContainsKeyCaseInsensitive() {
        map.put("Hello", "world");
        assertTrue(map.containsKey("hello"));
        assertTrue(map.containsKey("HELLO"));
    }

    @Test
    public void testRemoveCaseInsensitive() {
        map.put("RemoveMe", "x");
        assertEquals("x", map.remove("removeme"));
        assertNull(map.get("RemoveMe"));
    }

    @Test
    public void testKeySetPreservesOriginalCase() {
        map.put("MixedCase", "v");
        Set<String> ks = map.keySet();
        assertTrue(ks.contains("MixedCase"));
        assertFalse(ks.contains("mixedcase"));
    }

    @Test
    public void testEntrySetProvidesOriginalKeyInEntry() {
        map.put("EntryKey", "val");
        Set<Entry<String,String>> es = map.entrySet();
        assertEquals(1, es.size());
        Entry<String,String> e = es.iterator().next();
        assertEquals("EntryKey", e.getKey());          // original case
        assertEquals("val", e.getValue());
        // ensure the underlying entry is a CaseInsensitiveEntry
        assertTrue(e instanceof CaseInsensitiveMap.CaseInsensitiveEntry);
    }

    @Test
    public void testComputeIfAbsent() {
        map.computeIfAbsent("cIKey", k -> "computed");
        assertEquals("computed", map.get("CIKEY"));
    }

    @Test
    public void testComputeIfPresent() {
        map.put("foo", "bar");
        map.computeIfPresent("FOO", (k, v) -> v.toUpperCase());
        assertEquals("BAR", map.get("foo"));
    }

    @Test
    public void testCompute() {
        map.compute("xyz", (k, v) -> v == null ? "first" : v + "again");
        assertEquals("first", map.get("XYZ"));
        map.compute("XYZ", (k, v) -> v + "-second");
        assertEquals("first-second", map.get("xyz"));
    }

    @Test
    public void testMerge() {
        map.put("merge", "a");
        map.merge("MERGE", "b", (v1, v2) -> v1 + v2);
        assertEquals("ab", map.get("merge"));
        // merging a non‑existing key creates it
        map.merge("newKey", "c", (v1, v2) -> v1 + v2);
        assertEquals("c", map.get("NEWKEY"));
    }

    @Test
    public void testPutIfAbsent() {
        map.putIfAbsent("once", "1");
        assertEquals("1", map.get("ONCE"));
        map.putIfAbsent("ONCE", "2");
        assertEquals("1", map.get("once")); // unchanged
    }

    @Test
    public void testRemoveKeyValuePair() {
        map.put("pair", "value");
        assertFalse(map.remove("pair", "notValue"));
        assertTrue(map.remove("PAIR", "value"));
        assertFalse(map.containsKey("pair"));
    }

    @Test
    public void testReplaceThreeArgument() {
        map.put("rKey", "old");
        assertTrue(map.replace("RKEY", "old", "new"));
        assertEquals("new", map.get("rkey"));
        assertFalse(map.replace("RKEY", "old", "another"));
    }

    @Test
    public void testReplaceTwoArgument() {
        map.put("single", "first");
        assertEquals("first", map.replace("SINGLE", "second"));
        assertEquals("second", map.get("single"));
    }

    @Test
    public void testForEachProvidesOriginalKey() {
        map.put("Alpha", "A");
        map.put("Beta", "B");
        final List<String> seen = new ArrayList<>();
        map.forEach((k, v) -> seen.add(k + "=" + v));
        assertTrue(seen.contains("Alpha=A"));
        assertTrue(seen.contains("Beta=B"));
    }

    @Test
    public void testReplaceAllUpdatesValuesUsingOriginalKey() {
        map.put("num", "1");
        map.replaceAll((k, v) -> v + "_x");
        assertEquals("1_x", map.get("NUM"));
    }

    @Test
    public void testEqualsAgainstRegularMap() {
        map.put("a", "1");
        map.put("B", "2");
        Map<String,String> regular = new HashMap<>();
        regular.put("A", "1");
        regular.put("b", "2");
        assertTrue(map.equals(regular));
        assertTrue(regular.equals(map)); // HashMap's equals works both ways
    }

    /* ----------------------------------------------------------------------
     *  4.  EDGE‑CASE / EXCEPTION TESTS
     * ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testReplaceCacheNullThrowsNPE() {
        CaseInsensitiveMap.replaceCache(null);
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceRegistryNullListThrowsNPE() {
        CaseInsensitiveMap.replaceRegistry(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMaxCacheLengthStringTooSmallThrows() {
        CaseInsensitiveMap.setMaxCacheLengthString(5);
    }

    @Test
    public void testReplaceRegistryRejectsDuplicateClass() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> dup = new ArrayList<>();
        dup.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        dup.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        try {
            CaseInsensitiveMap.replaceRegistry(dup);
            fail("Expected IllegalArgumentException for duplicate class");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Duplicate map type"));
        }
    }

    @Test
    public void testReplaceRegistryRejectsWrongOrder() {
        // TreeMap (more specific) after HashMap (more general) => ordering error
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> wrongOrder = new ArrayList<>();
        wrongOrder.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        wrongOrder.add(new AbstractMap.SimpleEntry<>(TreeMap.class, size -> new TreeMap<>()));
        try {
            CaseInsensitiveMap.replaceRegistry(wrongOrder);
            fail("Expected IllegalStateException for mapping order error");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Mapping order error"));
        }
    }

    @Test(expected = NullPointerException.class)
    public void testCopyConstructorNullSourceThrows() {
        new CaseInsensitiveMap<>(null);
    }

    @Test(expected = NullPointerException.class)
    public void testCopyConstructorNullSourceAndInstanceThrows() {
        new CaseInsensitiveMap<>(null, new HashMap<>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCopyConstructorWithNonEmptyInstanceThrows() {
        Map<String,String> src = new HashMap<>();
        src.put("k", "v");
        Map<String,String> nonEmpty = new HashMap<>();
        nonEmpty.put("x", "y");
        new CaseInsensitiveMap<>(src, nonEmpty);
    }

    @Test
    public void testComputeIfAbsentNullMappingFunctionThrows() {
        try {
            map.computeIfAbsent("oops", null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // JDK Map implementation throws NPE
        }
    }

    @Test
    public void testComputeIfPresentNullBiFunctionThrows() {
        map.put("test", "value");
        try {
            map.computeIfPresent("test", null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testComputeNullBiFunctionThrows() {
        try {
            map.compute("any", null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testMergeNullRemappingFunctionThrows() {
        map.put("k", "v");
        try {
            map.merge("k", "x", null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testForEachNullActionThrows() {
        try {
            map.forEach(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testReplaceAllNullFunctionThrows() {
        try {
            map.replaceAll(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testLongStringBypassesCache() {
        // Reduce cache length to a small value so the test string is considered "long"
        CaseInsensitiveMap.setMaxCacheLengthString(10);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(2)); // fresh cache with tiny capacity

        String longKey = "thisIsAVeryLongKeyExceedingLimit";
        map.put(longKey, "value1");
        // Put the same long key again – because it is not cached, the internal
        // cache size should stay at 0 (cannot be accessed directly, but we can
        // verify that the map still works and no exception is thrown)
        map.put(longKey, "value2");
        assertEquals("value2", map.get(longKey));
    }

    @Test
    public void testKeySetToArrayRespectsOriginalCase() {
        map.put("AbC", "val");
        String[] arr = map.keySet().toArray(new String[0]);
        assertArrayEquals(new String[]{"AbC"}, arr);
    }

    @Test
    public void testEntrySetToArrayRespectsOriginalKey() {
        map.put("XYZ", "123");
        @SuppressWarnings("unchecked")
        Entry<String, String>[] arr = map.entrySet().toArray(new Entry[0]);
        assertEquals(1, arr.length);
        assertEquals("XYZ", arr[0].getKey());
        assertEquals("123", arr[0].getValue());
    }

    @Test
    public void testKeySetRetainAllRemovesUnlistedKeysCaseInsensitively() {
        map.put("One", "1");
        map.put("Two", "2");
        Collection<String> keep = Arrays.asList("ONE"); // different case
        assertTrue(map.keySet().retainAll(keep));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("one"));
        assertFalse(map.containsKey("two"));
    }

    @Test
    public void testEntrySetRemoveAllHandlesMixedTypesGracefully() {
        map.put("a", "1");
        map.put("b", "2");
        List<Object> toRemove = new ArrayList<>();
        toRemove.add(new AbstractMap.SimpleEntry<>("A", "1"));
        toRemove.add("notAnEntry"); // should be ignored
        assertTrue(map.entrySet().removeAll(toRemove));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("b"));
    }

    @Test
    public void testEntrySetRetainAllClearsWhenEmptyCollection() {
        map.put("x", "1");
        map.put("y", "2");
        assertTrue(map.entrySet().retainAll(Collections.emptyList()));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testKeySetRemoveReturnsTrueWhenElementIsRemoved() {
        map.put("DelKey", "v");
        assertTrue(map.keySet().remove("delkey"));
        assertFalse(map.containsKey("DelKey"));
    }

    @Test
    public void testKeySetRemoveReturnsFalseWhenElementAbsent() {
        assertFalse(map.keySet().remove("nonexistent"));
    }

    @Test
    public void testKeySetContainsHandlesCaseInsensitivity() {
        map.put("CaseTest", "val");
        assertTrue(map.keySet().contains("casetest"));
        assertFalse(map.keySet().contains("other"));
    }

    @Test
    public void testEntrySetIteratorNextReturnsOriginalKey() {
        map.put("IterKey", "v");
        Iterator<Entry<String, String>> it = map.entrySet().iterator();
        assertTrue(it.hasNext());
        Entry<String, String> e = it.next();
        assertEquals("IterKey", e.getKey());
    }

    @Test
    public void testEntrySetIteratorRemoveUpdatesMap() {
        map.put("R1", "v1");
        map.put("R2", "v2");
        Iterator<Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> e = it.next();
            if ("R1".equals(e.getKey())) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("R1"));
        assertTrue(map.containsKey("R2"));
    }

    @Test
    public void testCaseInsensitiveEntryEqualsAndHashCode() {
        map.put("KeyOne", "val");
        Entry<String, String> e1 = map.entrySet().iterator().next();
        // Build another entry with same original key/value
        Entry<String, String> e2 = new CaseInsensitiveMap.CaseInsensitiveEntry<>(new AbstractMap.SimpleEntry<>("KeyOne", "val"));
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    public void testCaseInsensitiveStringEqualityAndHashCode() {
        // Directly use the public inner class
        CaseInsensitiveMap.CaseInsensitiveString cis1 = new CaseInsensitiveMap.CaseInsensitiveString("TeSt");
        CaseInsensitiveMap.CaseInsensitiveString cis2 = new CaseInsensitiveMap.CaseInsensitiveString("test");
        assertTrue(cis1.equals(cis2));
        assertEquals(cis1.hashCode(), cis2.hashCode());
        assertTrue(cis1.equals("TEST"));
    }

    @Test
    public void testCaseInsensitiveStringCompareTo() {
        CaseInsensitiveMap.CaseInsensitiveString cis = new CaseInsensitiveMap.CaseInsensitiveString("abc");
        assertEquals(0, cis.compareTo("ABC"));
        assertTrue(cis.compareTo("abd") < 0);
        assertTrue(cis.compareTo(123) < 0); // non‑String considered greater
    }
}
