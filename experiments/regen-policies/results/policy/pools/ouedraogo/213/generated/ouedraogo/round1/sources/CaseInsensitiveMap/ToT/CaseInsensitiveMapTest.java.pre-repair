package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CaseInsensitiveMap}.
 *
 * The tests are written as if three independent experts had each contributed a
 * test case for every public method.  The suite therefore contains a mix of
 * typical usage, edge‑cases and error‑scenario validations.
 */
public class CaseInsensitiveMapTest {

    private CaseInsensitiveMap<String, String> map;

    @Before
    public void setUp() {
        map = new CaseInsensitiveMap<>();
    }

    @After
    public void tearDown() {
        map = null;
        // Restore the default registry after any test that mutates it
        CaseInsensitiveMap.replaceRegistry(defaultRegistry());
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods
     * --------------------------------------------------------------------- */

    private static List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> defaultRegistry() {
        // replicate the static initializer content – this is safe because the list is immutable
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> list = new ArrayList<>();
        list.add(new AbstractMap.SimpleEntry<>(Hashtable.class, size -> new Hashtable<>()));
        list.add(new AbstractMap.SimpleEntry<>(TreeMap.class, size -> new TreeMap<>()));
        list.add(new AbstractMap.SimpleEntry<>(ConcurrentSkipListMap.class, size -> new ConcurrentSkipListMap<>()));
        list.add(new AbstractMap.SimpleEntry<>(ConcurrentNavigableMapNullSafe.class, size -> new ConcurrentNavigableMapNullSafe<>()));
        list.add(new AbstractMap.SimpleEntry<>(ConcurrentHashMapNullSafe.class, size -> new ConcurrentHashMapNullSafe<>(size)));
        list.add(new AbstractMap.SimpleEntry<>(WeakHashMap.class, size -> new WeakHashMap<>(size)));
        list.add(new AbstractMap.SimpleEntry<>(LinkedHashMap.class, size -> new LinkedHashMap<>(size)));
        list.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>(size)));
        list.add(new AbstractMap.SimpleEntry<>(ConcurrentNavigableMap.class, size -> new ConcurrentSkipListMap<>()));
        list.add(new AbstractMap.SimpleEntry<>(ConcurrentMap.class, size -> new ConcurrentHashMap<>(size)));
        list.add(new AbstractMap.SimpleEntry<>(NavigableMap.class, size -> new TreeMap<>()));
        list.add(new AbstractMap.SimpleEntry<>(SortedMap.class, size -> new TreeMap<>()));
        return Collections.unmodifiableList(list);
    }

    private static <K, V> Map<K, V> copyMap(Map<K, V> src) {
        Map<K, V> copy = new LinkedHashMap<>();
        copy.putAll(src);
        return copy;
    }

    /* --------------------------------------------------------------------- *
     *  Constructor tests
     * --------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesLinkedHashMap() {
        assertTrue(map.getWrappedMap() instanceof LinkedHashMap);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>(20);
        assertTrue(m.getWrappedMap() instanceof LinkedHashMap);
        // capacity is not directly observable; we ensure no exception is thrown
        assertTrue(m.isEmpty());
    }

    @Test
    public void testConstructorWithCapacityAndLoadFactor() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>(30, 0.75f);
        assertTrue(m.getWrappedMap() instanceof LinkedHashMap);
        assertTrue(m.isEmpty());
    }

    @Test
    public void testCopyConstructorWithSourceMap() {
        Map<String, String> src = new HashMap<>();
        src.put("Alpha", "A");
        src.put("Beta", "B");

        CaseInsensitiveMap<String, String> copy = new CaseInsensitiveMap<>(src);
        // Underlying map type should be HashMap (registry order picks HashMap after LinkedHashMap)
        assertTrue(copy.getWrappedMap() instanceof HashMap);
        assertEquals("A", copy.get("ALPHA"));
        assertEquals("B", copy.get("beta"));
        // Original case preserved in keySet
        Set<String> keys = copy.keySet();
        assertTrue(keys.contains("Alpha"));
        assertTrue(keys.contains("Beta"));
    }

    @Test(expected = NullPointerException.class)
    public void testCopyConstructorRejectsNullSource() {
        new CaseInsensitiveMap<>(null);
    }

    @Test
    public void testCopyConstructorFromIdentityHashMapThrows() {
        Map<String, String> src = new IdentityHashMap<>();
        src.put("X", "1");
        try {
            new CaseInsensitiveMap<>(src);
            fail("Expected IllegalArgumentException for IdentityHashMap source");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testCopyConstructorFromCaseInsensitiveMapUsesOptimizedCopy() {
        map.put("One", "1");
        map.put("Two", "2");
        CaseInsensitiveMap<String, String> copy = new CaseInsensitiveMap<>(map, new LinkedHashMap<>());
        // underlying map should contain the internal CaseInsensitiveString keys directly
        assertEquals(2, copy.size());
        assertEquals("1", copy.get("ONE"));
        assertTrue(copy.getWrappedMap().keySet().iterator().next() instanceof CaseInsensitiveMap.CaseInsensitiveString);
    }

    /* --------------------------------------------------------------------- *
     *  Static utility method tests
     * --------------------------------------------------------------------- */

    @Test
    public void testReplaceCacheAcceptsNonNullCache() {
        LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> newCache =
                new LRUCache<>(10, LRUCache.StrategyType.THREADED);
        CaseInsensitiveMap.replaceCache(newCache);
        // Verify that a new CIS can be created after cache replacement
        CaseInsensitiveMap.CaseInsensitiveString cis = CaseInsensitiveMap.CaseInsensitiveString.of("sample");
        assertEquals("sample", cis.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceCacheRejectsNull() {
        CaseInsensitiveMap.replaceCache(null);
    }

    @Test
    public void testSetMaxCacheLengthStringValid() {
        CaseInsensitiveMap.setMaxCacheLengthString(50);
        // No exception – internal static field changed
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMaxCacheLengthStringTooSmall() {
        CaseInsensitiveMap.setMaxCacheLengthString(5);
    }

    @Test
    public void testReplaceRegistryValidReplacement() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> newReg = new ArrayList<>();
        newReg.add(new AbstractMap.SimpleEntry<>(TreeMap.class, size -> new TreeMap<>()));
        newReg.add(new AbstractMap.SimpleEntry<>(LinkedHashMap.class, size -> new LinkedHashMap<>(size)));

        CaseInsensitiveMap.replaceRegistry(newReg);
        // Build a map from a TreeMap source – should use TreeMap as backing
        TreeMap<String, String> src = new TreeMap<>();
        src.put("A", "a");
        CaseInsensitiveMap<String, String> cim = new CaseInsensitiveMap<>(src);
        assertTrue(cim.getWrappedMap() instanceof TreeMap);
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceRegistryRejectsNullList() {
        CaseInsensitiveMap.replaceRegistry(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplaceRegistryRejectsDuplicateClass() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> dup = new ArrayList<>();
        dup.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        dup.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        CaseInsensitiveMap.replaceRegistry(dup);
    }

    @Test(expected = IllegalStateException.class)
    public void testReplaceRegistryRejectsWrongOrder() {
        // Hashtable is a superclass of Hashtable? The order must be most specific first.
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> bad = new ArrayList<>();
        bad.add(new AbstractMap.SimpleEntry<>(Map.class, size -> new HashMap<>()));
        bad.add(new AbstractMap.SimpleEntry<>(Hashtable.class, size -> new Hashtable<>()));
        CaseInsensitiveMap.replaceRegistry(bad);
    }

    /* --------------------------------------------------------------------- *
     *  Basic map operations – case‑insensitivity
     * --------------------------------------------------------------------- */

    @Test
    public void testPutAndGetCaseInsensitive() {
        map.put("KeyOne", "Value1");
        assertEquals("Value1", map.get("keyone"));
        assertEquals("Value1", map.get("KEYONE"));
    }

    @Test
    public void testContainsKeyCaseInsensitive() {
        map.put("MyKey", "X");
        assertTrue(map.containsKey("mykey"));
        assertTrue(map.containsKey("MYKEY"));
    }

    @Test
    public void testRemoveCaseInsensitive() {
        map.put("RemoveMe", "Y");
        assertEquals("Y", map.remove("removeme"));
        assertFalse(map.containsKey("RemoveMe"));
    }

    @Test
    public void testNonStringKeyBehaviour() {
        CaseInsensitiveMap<Integer, String> intMap = new CaseInsensitiveMap<>();
        intMap.put(42, "Answer");
        assertEquals("Answer", intMap.get(42));
        assertTrue(intMap.containsKey(42));
        intMap.remove(42);
        assertFalse(intMap.containsKey(42));
    }

    @Test
    public void testEqualsConsidersCaseInsensitivity() {
        CaseInsensitiveMap<String, String> m1 = new CaseInsensitiveMap<>();
        m1.put("A", "1");
        m1.put("b", "2");

        CaseInsensitiveMap<String, String> m2 = new CaseInsensitiveMap<>();
        m2.put("a", "1");
        m2.put("B", "2");

        assertTrue(m1.equals(m2));
        assertTrue(m2.equals(m1));
    }

    @Test
    public void testEqualsWithDifferentValues() {
        CaseInsensitiveMap<String, String> m1 = new CaseInsensitiveMap<>();
        m1.put("X", "1");
        CaseInsensitiveMap<String, String> m2 = new CaseInsensitiveMap<>();
        m2.put("x", "2");
        assertFalse(m1.equals(m2));
    }

    @Test
    public void testEqualsWithNonMap() {
        assertFalse(map.equals("not a map"));
    }

    /* --------------------------------------------------------------------- *
     *  keySet view tests
     * --------------------------------------------------------------------- */

    @Test
    public void testKeySetPreservesOriginalCase() {
        map.put("First", "1");
        map.put("second", "2");
        Set<String> ks = map.keySet();
        assertTrue(ks.contains("First"));
        assertTrue(ks.contains("second"));
        // iterator should yield original case
        Iterator<String> it = ks.iterator();
        List<String> collected = new ArrayList<>();
        while (it.hasNext()) collected.add(it.next());
        assertTrue(collected.containsAll(Arrays.asList("First", "second")));
    }

    @Test
    public void testKeySetRemoveUpdatesMap() {
        map.put("DeleteMe", "X");
        assertTrue(map.keySet().remove("deleteme"));
        assertFalse(map.containsKey("DeleteMe"));
    }

    @Test
    public void testKeySetRetainAllKeepsOnlySpecified() {
        map.put("A", "1");
        map.put("B", "2");
        map.put("C", "3");
        Set<String> retain = new HashSet<>(Arrays.asList("a", "c"));
        assertTrue(map.keySet().retainAll(retain));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("A"));
        assertTrue(map.containsKey("C"));
        assertFalse(map.containsKey("B"));
    }

    @Test
    public void testKeySetToArray() {
        map.put("One", "1");
        map.put("Two", "2");
        String[] arr = map.keySet().toArray(new String[0]);
        assertArrayEquals(new String[]{"One", "Two"}, arr);
    }

    /* --------------------------------------------------------------------- *
     *  entrySet view tests
     * --------------------------------------------------------------------- */

    @Test
    public void testEntrySetIterationPreservesOriginalKey() {
        map.put("Alpha", "A");
        map.put("Beta", "B");
        Set<Map.Entry<String, String>> entries = map.entrySet();
        for (Map.Entry<String, String> e : entries) {
            assertTrue(e.getKey().equals("Alpha") || e.getKey().equals("Beta"));
            assertTrue(e.getValue().equals("A") || e.getValue().equals("B"));
        }
    }

    @Test
    public void testEntrySetRemoveByEntry() {
        map.put("Kill", "Me");
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("kill", "Me");
        assertTrue(map.entrySet().remove(entry));
        assertFalse(map.containsKey("Kill"));
    }

    @Test
    public void testEntrySetRetainAllFiltersCorrectly() {
        map.put("X", "1");
        map.put("Y", "2");
        map.put("Z", "3");
        Set<Map.Entry<String, String>> retain = new HashSet<>();
        retain.add(new AbstractMap.SimpleEntry<>("x", "1"));
        retain.add(new AbstractMap.SimpleEntry<>("z", "3"));
        assertTrue(map.entrySet().retainAll(retain));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("X"));
        assertTrue(map.containsKey("Z"));
        assertFalse(map.containsKey("Y"));
    }

    @Test
    public void testEntrySetToArray() {
        map.put("K1", "V1");
        map.put("K2", "V2");
        Object[] arr = map.entrySet().toArray();
        assertEquals(2, arr.length);
        // each element should be a CaseInsensitiveEntry exposing original keys
        for (Object o : arr) {
            assertTrue(o instanceof CaseInsensitiveMap.CaseInsensitiveEntry);
            CaseInsensitiveMap.CaseInsensitiveEntry e = (CaseInsensitiveMap.CaseInsensitiveEntry) o;
            assertTrue(e.getKey().equals("K1") || e.getKey().equals("K2"));
        }
    }

    /* --------------------------------------------------------------------- *
     *  compute / merge / putIfAbsent family
     * --------------------------------------------------------------------- */

    @Test
    public void testComputeIfAbsentCreatesValueCaseInsensitive() {
        map.computeIfAbsent("newKey", k -> "generated");
        assertEquals("generated", map.get("NEWKEY"));
    }

    @Test
    public void testComputeIfAbsentDoesNotOverwriteExisting() {
        map.put("EXIST", "old");
        map.computeIfAbsent("exist", k -> "new");
        assertEquals("old", map.get("exist"));
    }

    @Test
    public void testComputeIfPresentUpdatesValue() {
        map.put("upd", "first");
        map.computeIfPresent("UPD", (k, v) -> v + "-second");
        assertEquals("first-second", map.get("upd"));
    }

    @Test
    public void testComputeIfPresentNoEffectWhenMissing() {
        map.computeIfPresent("missing", (k, v) -> "won't happen");
        assertFalse(map.containsKey("missing"));
    }

    @Test
    public void testComputeReplacesValue() {
        map.put("c", "1");
        map.compute("C", (k, v) -> v == null ? "init" : v + "x");
        assertEquals("1x", map.get("c"));
    }

    @Test
    public void testMergeCombinesValues() {
        map.put("m", "A");
        map.merge("M", "B", (oldV, newV) -> oldV + newV);
        assertEquals("AB", map.get("m"));
    }

    @Test
    public void testMergeWhenAbsent() {
        map.merge("new", "val", (a, b) -> a + b);
        assertEquals("val", map.get("NEW"));
    }

    @Test
    public void testPutIfAbsentDoesNotOverwrite() {
        map.put("p", "first");
        map.putIfAbsent("P", "second");
        assertEquals("first", map.get("p"));
    }

    @Test
    public void testRemoveKeyValuePair() {
        map.put("pair", "value");
        assertTrue(map.remove("PAIR", "value"));
        assertFalse(map.containsKey("pair"));
    }

    @Test
    public void testRemoveKeyValuePairFailsWhenValueDiffers() {
        map.put("pair", "value");
        assertFalse(map.remove("pair", "other"));
        assertTrue(map.containsKey("pair"));
    }

    @Test
    public void testReplaceThreeArgSuccess() {
        map.put("r", "old");
        assertTrue(map.replace("R", "old", "new"));
        assertEquals("new", map.get("r"));
    }

    @Test
    public void testReplaceThreeArgFailure() {
        map.put("r", "old");
        assertFalse(map.replace("R", "notOld", "new"));
        assertEquals("old", map.get("r"));
    }

    @Test
    public void testReplaceTwoArg() {
        map.put("single", "first");
        assertEquals("first", map.replace("SINGLE", "second"));
        assertEquals("second", map.get("single"));
    }

    @Test
    public void testForEachUnwrapsKey() {
        map.put("Foo", "bar");
        final List<String> captured = new ArrayList<>();
        map.forEach((k, v) -> captured.add(k + "=" + v));
        assertEquals(Collections.singletonList("Foo=bar"), captured);
    }

    @Test
    public void testReplaceAllUnwrapsKey() {
        map.put("A", "1");
        map.replaceAll((k, v) -> k.toLowerCase() + ":" + v);
        assertEquals("a:1", map.get("a"));
    }

    /* --------------------------------------------------------------------- *
     *  Registry‑driven backing map selection
     * --------------------------------------------------------------------- */

    @Test
    public void testDetermineBackingMapSelectsTreeMapWhenSourceIsTreeMap() {
        TreeMap<String, String> src = new TreeMap<>();
        src.put("t", "val");
        CaseInsensitiveMap<String, String> cim = new CaseInsensitiveMap<>(src);
        assertTrue(cim.getWrappedMap() instanceof TreeMap);
    }

    @Test
    public void testDetermineBackingMapSelectsHashtableWhenSourceIsHashtable() {
        Hashtable<String, String> src = new Hashtable<>();
        src.put("h", "val");
        CaseInsensitiveMap<String, String> cim = new CaseInsensitiveMap<>(src);
        assertTrue(cim.getWrappedMap() instanceof Hashtable);
    }

    @Test
    public void testDetermineBackingMapDefaultsToLinkedHashMap() {
        Map<String, String> src = new HashMap<>();
        src.put("x", "y");
        CaseInsensitiveMap<String, String> cim = new CaseInsensitiveMap<>(src);
        assertTrue(cim.getWrappedMap() instanceof LinkedHashMap);
    }

    @Test
    public void testDetermineBackingMapCopiesEntriesCorrectly() {
        Map<String, String> src = new HashMap<>();
        src.put("One", "1");
        src.put("Two", "2");
        CaseInsensitiveMap<String, String> cim = new CaseInsensitiveMap<>(src);
        assertEquals("1", cim.get("one"));
        assertEquals("2", cim.get("TWO"));
    }

    /* --------------------------------------------------------------------- *
     *  CaseInsensitiveString cache behaviour (indirect)
     * --------------------------------------------------------------------- */

    @Test
    public void testCaseInsensitiveStringFactoryCachesCommonValues() {
        CaseInsensitiveMap.CaseInsensitiveString s1 = CaseInsensitiveMap.CaseInsensitiveString.of("true");
        CaseInsensitiveMap.CaseInsensitiveString s2 = CaseInsensitiveMap.CaseInsensitiveString.of("TRUE");
        // Both should be the same cached instance because caching is case‑insensitive
        assertSame(s1, s2);
    }

    @Test
    public void testCaseInsensitiveStringFactoryCreatesNewInstanceForLongStrings() {
        // Strings longer than default maxCacheLengthString (100) are not cached
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 150; i++) sb.append('a');
        String longStr = sb.toString();

        CaseInsensitiveMap.CaseInsensitiveString s1 = CaseInsensitiveMap.CaseInsensitiveString.of(longStr);
        CaseInsensitiveMap.CaseInsensitiveString s2 = CaseInsensitiveMap.CaseInsensitiveString.of(longStr);
        assertNotSame(s1, s2);
        assertEquals(s1, s2); // equality still works
    }

    /* --------------------------------------------------------------------- *
     *  Miscellaneous internal helper validation
     * --------------------------------------------------------------------- */

    @Test
    public void testConvertKeyHandlesNullGracefully() {
        // Null keys are allowed – they are passed through unchanged
        map.put(null, "nullVal");
        assertEquals("nullVal", map.get(null));
        assertTrue(map.containsKey(null));
        map.remove(null);
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testEntrySetSetValueUpdatesUnderlyingMap() {
        map.put("k", "v1");
        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        entry.setValue("v2");
        assertEquals("v2", map.get("K"));
    }

    @Test
    public void testCaseInsensitiveEntryEqualsAndHashCode() {
        map.put("Key", "Val");
        Map.Entry<String, String> e1 = map.entrySet().iterator().next();
        Map.Entry<String, String> e2 = new AbstractMap.SimpleEntry<>("key", "Val");
        assertTrue(e1.equals(e2));
        assertEquals(e1.hashCode(), e2.hashCode());
    }
}
