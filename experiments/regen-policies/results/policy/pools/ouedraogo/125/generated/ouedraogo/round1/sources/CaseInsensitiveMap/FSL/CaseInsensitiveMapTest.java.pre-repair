package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit4 test suite for {@link CaseInsensitiveMap}.
 * Covers constructors, core Map operations, keySet/entrySet behaviour,
 * Java‑8 default methods, cache handling and registry validation.
 */
public class CaseInsensitiveMapTest {

    private static final String LONG_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> originalCache;
    private int originalMaxCacheLength;

    @Before
    public void setUp() {
        // Preserve original static state
        originalCache = getCurrentCache();
        originalMaxCacheLength = getCurrentMaxCacheLength();
    }

    @After
    public void tearDown() {
        // Restore original static state
        CaseInsensitiveMap.replaceCache(originalCache);
        CaseInsensitiveMap.setMaxCacheLengthString(originalMaxCacheLength);
    }

    // ----------------------------------------------------------------------
    // Helper methods to access private static fields via reflection
    // ----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> getCurrentCache() {
        try {
            java.lang.reflect.Field f = CaseInsensitiveMap.class.getDeclaredField("ciStringCache");
            f.setAccessible(true);
            return (LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString>) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int getCurrentMaxCacheLength() {
        try {
            java.lang.reflect.Field f = CaseInsensitiveMap.class.getDeclaredField("maxCacheLengthString");
            f.setAccessible(true);
            return f.getInt(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------
    @Test
    public void testDefaultConstructor() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        assertTrue(map.isEmpty());
        map.put("One", 1);
        assertEquals(Integer.valueOf(1), map.get("one"));
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>(10);
        map.put("Ten", 10);
        assertEquals(Integer.valueOf(10), map.get("TEN"));
    }

    @Test
    public void testConstructorWithInitialCapacityAndLoadFactor() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>(5, 0.75f);
        map.put("Five", 5);
        assertEquals(Integer.valueOf(5), map.get("fIvE"));
    }

    @Test
    public void testCopyConstructorFromSourceMap() {
        Map<String, String> source = new TreeMap<>();
        source.put("A", "alpha");
        source.put("b", "beta");
        CaseInsensitiveMap<String, String> copy = new CaseInsensitiveMap<>(source);
        // original case must be preserved in keySet
        Set<String> keys = copy.keySet();
        assertTrue(keys.contains("A"));
        assertTrue(keys.contains("b"));
        // case‑insensitive look‑up
        assertEquals("alpha", copy.get("a"));
        assertEquals("beta", copy.get("B"));
    }

    @Test(expected = NullPointerException.class)
    public void testCopyConstructorNullSource() {
        new CaseInsensitiveMap<>(null);
    }

    @Test
    public void testCopyConstructorWithMapInstance() {
        Map<String, Integer> src = new HashMap<>();
        src.put("X", 100);
        Map<String, Integer> dest = new LinkedHashMap<>();
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>(src, dest);
        assertEquals(Integer.valueOf(100), map.get("x"));
        assertEquals(1, map.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCopyConstructorWithNonEmptyMapInstance() {
        Map<String, Integer> src = new HashMap<>();
        src.put("Y", 200);
        Map<String, Integer> dest = new HashMap<>();
        dest.put("Z", 300); // make it non‑empty
        new CaseInsensitiveMap<>(src, dest);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetermineBackingMapRejectsIdentityHashMap() {
        // expose protected method via subclass
        class TestableCIM extends CaseInsensitiveMap<Object, Object> {
            Map<Object, Object> callDetermine(Map<Object, Object> src) {
                return super.determineBackingMap(src);
            }
        }
        TestableCIM cim = new TestableCIM();
        Map<Object, Object> idMap = new IdentityHashMap<>();
        cim.callDetermine(idMap);
    }

    // ----------------------------------------------------------------------
    // Basic Map operations (put, get, containsKey, remove, equals)
    // ----------------------------------------------------------------------
    @Test
    public void testBasicOperationsWithStringKeys() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("KeyOne", "value1");
        map.put("KEYtwo", "value2");
        assertEquals("value1", map.get("keyone"));
        assertTrue(map.containsKey("keYtWo"));
        assertEquals("value2", map.remove("KEYTWO"));
        assertFalse(map.containsKey("keytwo"));
        assertEquals(1, map.size());
    }

    @Test
    public void testBasicOperationsWithNonStringKeys() {
        CaseInsensitiveMap<Integer, String> map = new CaseInsensitiveMap<>();
        map.put(10, "ten");
        assertEquals("ten", map.get(10));
        assertTrue(map.containsKey(10));
        assertEquals("ten", map.remove(10));
        assertFalse(map.containsKey(10));
    }

    @Test
    public void testEqualsIsCaseInsensitive() {
        CaseInsensitiveMap<String, Integer> m1 = new CaseInsensitiveMap<>();
        CaseInsensitiveMap<String, Integer> m2 = new CaseInsensitiveMap<>();
        m1.put("Alpha", 1);
        m2.put("ALPHA", 1);
        assertTrue(m1.equals(m2));
        assertTrue(m2.equals(m1));

        m2.put("Beta", 2);
        assertFalse(m1.equals(m2));
    }

    // ----------------------------------------------------------------------
    // computeIfAbsent / computeIfPresent / compute / merge
    // ----------------------------------------------------------------------
    @Test
    public void testComputeIfAbsentReceivesOriginalKey() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.computeIfAbsent("TestKey", k -> k.length());
        assertEquals(Integer.valueOf(7), map.get("testkey"));
    }

    @Test
    public void testComputeIfPresentReceivesOriginalKey() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("Abc", 5);
        map.computeIfPresent("aBC", (k, v) -> v + k.length());
        assertEquals(Integer.valueOf(8), map.get("ABC"));
    }

    @Test
    public void testCompute() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("Num", 10);
        map.compute("NUM", (k, v) -> v * 2);
        assertEquals(Integer.valueOf(20), map.get("num"));
    }

    @Test
    public void testMerge() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("Merge", 3);
        map.merge("MERGE", 4, Integer::sum);
        assertEquals(Integer.valueOf(7), map.get("merge"));
        // merge where key absent
        map.merge("newKey", 5, Integer::sum);
        assertEquals(Integer.valueOf(5), map.get("NEWKEY"));
    }

    // ----------------------------------------------------------------------
    // putIfAbsent, remove(key,value), replace, replaceAll, forEach
    // ----------------------------------------------------------------------
    @Test
    public void testPutIfAbsent() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.putIfAbsent("Key", "first");
        map.putIfAbsent("kEy", "second");
        assertEquals("first", map.get("KEY"));
    }

    @Test
    public void testRemoveKeyValue() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("A", "alpha");
        assertTrue(map.remove("a", "alpha"));
        assertFalse(map.remove("a", "alpha"));
    }

    @Test
    public void testReplaceMethods() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("B", "beta");
        assertTrue(map.replace("b", "beta", "BETA"));
        assertEquals("BETA", map.get("B"));
        assertEquals("BETA", map.replace("B", "new"));
        assertEquals("new", map.get("b"));
    }

    @Test
    public void testForEachUnwrapsKeys() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("One", 1);
        map.put("Two", 2);
        Map<String, Integer> collected = new HashMap<>();
        map.forEach(collected::put);
        assertEquals(Integer.valueOf(1), collected.get("One"));
        assertEquals(Integer.valueOf(2), collected.get("Two"));
    }

    @Test
    public void testReplaceAllUnwrapsKeys() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("x", 1);
        map.put("Y", 2);
        map.replaceAll((k, v) -> v * 10);
        assertEquals(Integer.valueOf(10), map.get("X"));
        assertEquals(Integer.valueOf(20), map.get("y"));
    }

    // ----------------------------------------------------------------------
    // keySet behaviour
    // ----------------------------------------------------------------------
    @Test
    public void testKeySetIterationPreservesOriginalCase() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("First", 1);
        map.put("SECOND", 2);
        Iterator<String> it = map.keySet().iterator();
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        assertTrue(iterated.contains("First"));
        assertTrue(iterated.contains("SECOND"));
    }

    @Test
    public void testKeySetToArray() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("A", "a");
        map.put("B", "b");
        String[] array = map.keySet().toArray(new String[0]);
        assertArrayEquals(new String[]{"A", "B"}, array);
    }

    @Test
    public void testKeySetRemove() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("RemoveMe", "value");
        boolean changed = map.keySet().remove("removeme");
        assertTrue(changed);
        assertFalse(map.containsKey("RemoveMe"));
    }

    @Test
    public void testKeySetRetainAllCaseInsensitive() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        boolean changed = map.keySet().retainAll(Arrays.asList("A", "C"));
        assertTrue(changed);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("c"));
        assertFalse(map.containsKey("b"));
    }

    // ----------------------------------------------------------------------
    // entrySet behaviour
    // ----------------------------------------------------------------------
    @Test
    public void testEntrySetIterationOriginalKey() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("KeyOne", "v1");
        map.put("KEYTWO", "v2");
        for (Map.Entry<String, String> e : map.entrySet()) {
            assertTrue(e.getKey().equals("KeyOne") || e.getKey().equals("KEYTWO"));
        }
    }

    @Test
    public void testEntrySetToArray() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("X", 10);
        map.put("Y", 20);
        Object[] arr = map.entrySet().toArray();
        assertEquals(2, arr.length);
        for (Object o : arr) {
            assertTrue(o instanceof CaseInsensitiveMap.CaseInsensitiveEntry);
            CaseInsensitiveMap.CaseInsensitiveEntry entry = (CaseInsensitiveMap.CaseInsensitiveEntry) o;
            assertTrue(entry.getKey().equals("X") || entry.getKey().equals("Y"));
        }
    }

    @Test
    public void testEntrySetRemove() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Del", "value");
        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        boolean removed = map.entrySet().remove(entry);
        assertTrue(removed);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testEntrySetRetainAll() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        Set<Map.Entry<String, Integer>> retain = new HashSet<>();
        retain.add(new CaseInsensitiveMap.CaseInsensitiveEntry<>(new AbstractMap.SimpleEntry<>("A", 1)));
        retain.add(new CaseInsensitiveMap.CaseInsensitiveEntry<>(new AbstractMap.SimpleEntry<>("C", 3)));
        boolean changed = map.entrySet().retainAll(retain);
        assertTrue(changed);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("c"));
        assertFalse(map.containsKey("b"));
    }

    // ----------------------------------------------------------------------
    // Cache handling
    // ----------------------------------------------------------------------
    @Test
    public void testReplaceCacheAndCachePopulation() {
        LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> newCache = new LRUCache<>(5);
        CaseInsensitiveMap.replaceCache(newCache);

        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("CacheKey", 1);

        assertTrue(newCache.containsKey("CacheKey"));
    }

    @Test
    public void testMaxCacheLengthStringDisablesCachingForLongKeys() {
        // Reduce max cache length to a small value
        CaseInsensitiveMap.setMaxCacheLengthString(5);
        LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> cache = new LRUCache<>(10);
        CaseInsensitiveMap.replaceCache(cache);

        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put(LONG_STRING, 99);

        // LONG_STRING length > 5, should NOT be cached
        assertFalse(cache.containsKey(LONG_STRING));
    }

    // ----------------------------------------------------------------------
    // Registry validation
    // ----------------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testReplaceRegistryRejectsDuplicateClass() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> bad = new ArrayList<>();
        bad.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        bad.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        CaseInsensitiveMap.replaceRegistry(bad);
    }

    @Test(expected = IllegalStateException.class)
    public void testReplaceRegistryDetectsIncorrectOrder() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> list = new ArrayList<>();
        // TreeMap is a subclass of SortedMap; putting SortedMap before TreeMap should cause order error
        list.add(new AbstractMap.SimpleEntry<>(SortedMap.class, size -> new TreeMap<>()));
        list.add(new AbstractMap.SimpleEntry<>(TreeMap.class, size -> new TreeMap<>()));
        CaseInsensitiveMap.replaceRegistry(list);
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

    // ----------------------------------------------------------------------
    // Additional Map default methods
    // ----------------------------------------------------------------------
    @Test
    public void testComputeIfAbsentWhenAlreadyPresent() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("dup", 42);
        map.computeIfAbsent("DUP", k -> 100);
        assertEquals(Integer.valueOf(42), map.get("dup"));
    }

    @Test
    public void testComputeIfPresentWhenAbsentDoesNothing() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.computeIfPresent("missing", (k, v) -> 999);
        assertNull(map.get("missing"));
    }

    @Test
    public void testMergeWithNullRemappingFunction() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("m", 1);
        map.merge("M", 2, (oldV, newV) -> null); // should remove the entry
        assertFalse(map.containsKey("m"));
    }

    @Test
    public void testReplaceAllUsesOriginalKeyInBiFunction() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Key", "value");
        map.replaceAll((k, v) -> k.toUpperCase() + ":" + v);
        assertEquals("KEY:value", map.get("key"));
    }
}
