package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import static org.junit.Assert.*;

public class CaseInsensitiveMapTest {

    private static final String KEY_UPPER = "TeStKeY";
    private static final String KEY_LOWER = "testkey";

    @Before
    public void resetCacheAndRegistry() {
        // Reset the cache length to the default (100) before each test
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        // Restore the default registry (same as static initializer) to avoid side‑effects
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>>> defaultRegistry = new ArrayList<>();
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(Hashtable.class, size -> new Hashtable<>()));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(TreeMap.class, size -> new TreeMap<>()));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(ConcurrentSkipListMap.class, size -> new java.util.concurrent.ConcurrentSkipListMap<>()));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(ConcurrentNavigableMapNullSafe.class, size -> new ConcurrentNavigableMapNullSafe<>()));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(ConcurrentHashMapNullSafe.class, size -> new ConcurrentHashMapNullSafe<>(size)));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(WeakHashMap.class, size -> new java.util.WeakHashMap<>(size)));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(LinkedHashMap.class, size -> new LinkedHashMap<>(size)));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>(size)));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(java.util.concurrent.ConcurrentNavigableMap.class, size -> new java.util.concurrent.ConcurrentSkipListMap<>()));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(java.util.concurrent.ConcurrentMap.class, size -> new java.util.concurrent.ConcurrentHashMap<>(size)));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(java.util.NavigableMap.class, size -> new TreeMap<>()));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(java.util.SortedMap.class, size -> new TreeMap<>()));
        CaseInsensitiveMap.replaceRegistry(defaultRegistry);
    }

    @Test
    public void testPutAndGetCaseInsensitive() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put(KEY_UPPER, "value1");
        assertEquals("value1", map.get(KEY_LOWER));
        assertTrue(map.containsKey(KEY_LOWER));
        assertTrue(map.containsKey(KEY_UPPER));
    }

    @Test
    public void testRemoveAndContainsKeyWithNull() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put(null, 42);
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(42), map.remove(null));
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testKeySetPreservesOriginalCase() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("First", "1");
        map.put("SECOND", "2");
        map.put("third", "3");

        // The keySet iterator should return the original case strings
        Iterator<String> it = map.keySet().iterator();
        List<String> keys = new ArrayList<>();
        while (it.hasNext()) {
            keys.add(it.next());
        }
        assertTrue(keys.contains("First"));
        assertTrue(keys.contains("SECOND"));
        assertTrue(keys.contains("third"));
    }

    @Test
    public void testEntrySetKeyPreservesOriginalCase() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("AbC", "val");
        Map.Entry<String, String> entry = map.entrySet().iterator().next();

        assertEquals("AbC", entry.getKey()); // original case
        assertEquals("val", entry.getValue());

        // test setValue updates underlying map
        entry.setValue("newVal");
        assertEquals("newVal", map.get("abc"));
    }

    @Test
    public void testEqualsWithDifferentKeyCases() {
        CaseInsensitiveMap<String, String> map1 = new CaseInsensitiveMap<>();
        map1.put("KeyOne", "A");
        map1.put("KeyTwo", "B");

        Map<String, String> regularMap = new HashMap<>();
        regularMap.put("keyone", "A");
        regularMap.put("KEYTWO", "B");

        assertTrue(map1.equals(regularMap));
        assertTrue(regularMap.equals(map1));
    }

    @Test
    public void testComputeIfAbsentProvidesOriginalKey() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.computeIfAbsent("MiXeD", k -> k.length());
        assertEquals(Integer.valueOf(5), map.get("mixed"));
        // Ensure the function received the original case string
        final String[] captured = new String[1];
        map.computeIfAbsent("another", k -> {
            captured[0] = k;
            return 0;
        });
        assertEquals("another", captured[0]);
    }

    @Test
    public void testComputeIfPresentAndCompute() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("NUM", 10);

        // computeIfPresent should receive original key
        map.computeIfPresent("num", (k, v) -> {
            assertEquals("NUM", k);
            return v + 5;
        });
        assertEquals(Integer.valueOf(15), map.get("NUM"));

        // compute should replace the value
        map.compute("Num", (k, v) -> {
            assertEquals("NUM", k);
            return v * 2;
        });
        assertEquals(Integer.valueOf(30), map.get("num"));
    }

    @Test
    public void testMergeAndPutIfAbsent() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.putIfAbsent("Key", "first");
        assertEquals("first", map.get("KEY"));
        // putIfAbsent should not overwrite existing entry
        map.putIfAbsent("kEy", "second");
        assertEquals("first", map.get("key"));

        // merge should combine values
        map.merge("KEY", "added", (oldV, newV) -> oldV + "|" + newV);
        assertEquals("first|added", map.get("key"));
    }

    @Test
    public void testRemoveWithValue() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Test", "value");
        assertFalse(map.remove("test", "wrong"));
        assertTrue(map.remove("TEST", "value"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testReplaceMethods() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("one", 1);
        assertTrue(map.replace("ONE", 1, 2));
        assertEquals(Integer.valueOf(2), map.get("One"));
        assertEquals(Integer.valueOf(2), map.replace("one", 3));
        assertEquals(Integer.valueOf(3), map.get("ONE"));
    }

    @Test
    public void testForEachAndReplaceAll() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("a", 1);
        map.put("B", 2);

        final List<String> keysSeen = new ArrayList<>();
        final List<Integer> valuesSeen = new ArrayList<>();

        map.forEach((k, v) -> {
            keysSeen.add(k);
            valuesSeen.add(v);
        });

        assertTrue(keysSeen.containsAll(List.of("a", "B")));
        assertTrue(valuesSeen.containsAll(List.of(1, 2)));

        // replaceAll doubles each value, keys should stay original case
        map.replaceAll((k, v) -> v * 2);
        assertEquals(Integer.valueOf(2), map.get("A"));
        assertEquals(Integer.valueOf(4), map.get("b"));
    }

    @Test
    public void testCaseInsensitiveStringCacheBehavior() {
        // short string should be cached – same instance returned
        CaseInsensitiveString s1 = CaseInsensitiveString.of("true");
        CaseInsensitiveString s2 = CaseInsensitiveString.of("true");
        assertSame(s1, s2);

        // long string (> maxCacheLengthString) should not be cached – different instances
        String longStr = "x".repeat(200);
        CaseInsensitiveString l1 = CaseInsensitiveString.of(longStr);
        CaseInsensitiveString l2 = CaseInsensitiveString.of(longStr);
        assertNotSame(l1, l2);
    }

    @Test
    public void testReplaceCacheChangesIdentity() {
        // Ensure default cache works for a short string
        CaseInsensitiveString default1 = CaseInsensitiveString.of("code");
        CaseInsensitiveString default2 = CaseInsensitiveString.of("code");
        assertSame(default1, default2);

        // Replace cache with a fresh LRUCache
        LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> newCache = new LRUCache<>(10, LRUCache.StrategyType.THREADED);
        CaseInsensitiveMap.replaceCache(newCache);

        // After replacement, the previous cached instance is not in the new cache,
        // so a new instance is created, but subsequent calls return the same new instance.
        CaseInsensitiveString after1 = CaseInsensitiveString.of("code");
        CaseInsensitiveString after2 = CaseInsensitiveString.of("code");
        assertNotSame(default1, after1);
        assertSame(after1, after2);
    }

    @Test
    public void testReplaceRegistrySelectsBackingMap() {
        // Build a custom registry that only knows Hashtable
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> custom = new ArrayList<>();
        custom.add(new AbstractMap.SimpleEntry<>(Hashtable.class, size -> new Hashtable<>()));
        CaseInsensitiveMap.replaceRegistry(custom);

        // Source map is a Hashtable – the resulting wrapped map must be a Hashtable
        Hashtable<String, String> source = new Hashtable<>();
        source.put("alpha", "A");
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>(source);
        assertTrue(map.getWrappedMap() instanceof Hashtable);

        // Restore the original registry for subsequent tests
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> defaultRegistry = new ArrayList<>();
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(Hashtable.class, size -> new Hashtable<>()));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(TreeMap.class, size -> new TreeMap<>()));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(ConcurrentSkipListMap.class, size -> new java.util.concurrent.ConcurrentSkipListMap<>()));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(ConcurrentNavigableMapNullSafe.class, size -> new ConcurrentNavigableMapNullSafe<>()));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(ConcurrentHashMapNullSafe.class, size -> new ConcurrentHashMapNullSafe<>(size)));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(WeakHashMap.class, size -> new java.util.WeakHashMap<>(size)));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(LinkedHashMap.class, size -> new LinkedHashMap<>(size)));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>(size)));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(java.util.concurrent.ConcurrentNavigableMap.class, size -> new java.util.concurrent.ConcurrentSkipListMap<>()));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(java.util.concurrent.ConcurrentMap.class, size -> new java.util.concurrent.ConcurrentHashMap<>(size)));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(java.util.NavigableMap.class, size -> new TreeMap<>()));
        defaultRegistry.add(new AbstractMap.SimpleEntry<>(java.util.SortedMap.class, size -> new TreeMap<>()));
        CaseInsensitiveMap.replaceRegistry(defaultRegistry);
    }

    @Test
    public void testSetMaxCacheLengthStringValidation() {
        // valid length
        CaseInsensitiveMap.setMaxCacheLengthString(15);
        // invalid length should throw
        try {
            CaseInsensitiveMap.setMaxCacheLengthString(5);
            fail("Expected IllegalArgumentException for length < 10");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
    }

    @Test
    public void testCopyConstructorWithCaseInsensitiveMapSource() {
        CaseInsensitiveMap<String, String> original = new CaseInsensitiveMap<>();
        original.put("Key", "value");
        CaseInsensitiveMap<String, String> copy = new CaseInsensitiveMap<>(original, new LinkedHashMap<>());
        // The copy should contain the same entry and the original key case
        assertEquals("value", copy.get("key"));
        assertTrue(copy.keySet().contains("Key"));
    }

    @Test
    public void testKeySetToArrayPreservesOriginalCase() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("One", 1);
        map.put("TWO", 2);
        Object[] arr = map.keySet().toArray(new String[0]);
        assertArrayEquals(new String[]{"One", "TWO"}, arr);
    }

    @Test
    public void testKeySetRetainAll() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        // Retain only A and c (different cases)
        map.keySet().retainAll(List.of("a", "c"));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("A"));
        assertTrue(map.containsKey("c"));
        assertFalse(map.containsKey("B"));
    }

    @Test
    public void testEntrySetRetainAllAndRemoveAll() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("X", 10);
        map.put("Y", 20);
        map.put("Z", 30);

        // Retain only entry with key "y" (case‑insensitive) and value 20
        map.entrySet().retainAll(List.of(new CaseInsensitiveMap.CaseInsensitiveEntry<>(Map.entry("Y", 20))));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("y"));
        assertEquals(Integer.valueOf(20), map.get("Y"));

        // Now remove the remaining entry via removeAll
        map.entrySet().removeAll(List.of(new CaseInsensitiveMap.CaseInsensitiveEntry<>(Map.entry("y", 20))));
        assertTrue(map.isEmpty());
    }
}
