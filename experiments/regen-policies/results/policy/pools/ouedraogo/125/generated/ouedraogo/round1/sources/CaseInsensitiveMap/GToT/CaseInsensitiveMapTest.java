package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.*;

import static org.junit.Assert.*;

public class CaseInsensitiveMapTest {

    // -----------------------------------------------------------------------
    // Helpers to restore static state after tests that modify it
    // -----------------------------------------------------------------------
    private List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> originalRegistry;
    private LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> originalCache;
    private int originalMaxCacheLength;

    @Before
    public void captureStaticState() throws Exception {
        // capture original registry
        Field regField = CaseInsensitiveMap.class.getDeclaredField("mapRegistry");
        regField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> reg =
                (List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>>) regField.get(null);
        // make a defensive copy (entries are immutable)
        originalRegistry = new ArrayList<>(reg);

        // capture original cache
        Field cacheField = CaseInsensitiveMap.class.getDeclaredField("ciStringCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> cache =
                (LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString>) cacheField.get(null);
        originalCache = cache;

        // capture original max cache length
        Field maxLenField = CaseInsensitiveMap.class.getDeclaredField("maxCacheLengthString");
        maxLenField.setAccessible(true);
        originalMaxCacheLength = maxLenField.getInt(null);
    }

    @After
    public void restoreStaticState() throws Exception {
        // restore registry
        Field regField = CaseInsensitiveMap.class.getDeclaredField("mapRegistry");
        regField.setAccessible(true);
        regField.set(null, Collections.unmodifiableList(new ArrayList<>(originalRegistry)));

        // restore cache
        Field cacheField = CaseInsensitiveMap.class.getDeclaredField("ciStringCache");
        cacheField.setAccessible(true);
        cacheField.set(null, originalCache);

        // restore max cache length
        Field maxLenField = CaseInsensitiveMap.class.getDeclaredField("maxCacheLengthString");
        maxLenField.setAccessible(true);
        maxLenField.setInt(null, originalMaxCacheLength);
    }

    // -----------------------------------------------------------------------
    // Constructor and basic map operations
    // -----------------------------------------------------------------------
    @Test
    public void testDefaultConstructorAndBasicOperations() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        assertTrue(map.isEmpty());

        // put and get (case‑insensitive)
        map.put("Key", 1);
        assertEquals(Integer.valueOf(1), map.get("key"));
        assertEquals(Integer.valueOf(1), map.get("KEY"));

        // containsKey
        assertTrue(map.containsKey("kEy"));
        assertFalse(map.containsKey("other"));

        // remove
        assertEquals(Integer.valueOf(1), map.remove("KeY"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>(10);
        map.put("A", "a");
        assertEquals("a", map.get("a"));
    }

    @Test
    public void testConstructorWithInitialCapacityAndLoadFactor() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>(5, 0.75f);
        map.put("B", "b");
        assertEquals("b", map.get("B"));
    }

    @Test
    public void testCopyConstructorFromExistingMap() {
        Map<String, String> src = new HashMap<>();
        src.put("One", "1");
        src.put("Two", "2");
        CaseInsensitiveMap<String, String> copy = new CaseInsensitiveMap<>(src);
        assertEquals("1", copy.get("ONE"));
        assertEquals("2", copy.get("two"));
        assertEquals(2, copy.size());
    }

    @Test(expected = NullPointerException.class)
    public void testCopyConstructorNullSourceThrowsNPE() {
        new CaseInsensitiveMap<>(null);
    }

    @Test
    public void testConstructorWithSourceAndDestinationMap() {
        Map<String, String> src = new LinkedHashMap<>();
        src.put("X", "x");
        src.put("Y", "y");
        Map<String, String> dest = new TreeMap<>();
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>(src, dest);
        assertTrue(map.getWrappedMap() instanceof TreeMap);
        assertEquals("x", map.get("x"));
        assertEquals("y", map.get("Y"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetermineBackingMapRejectsIdentityHashMap() {
        Map<String, String> src = new IdentityHashMap<>();
        src.put("a", "1");
        // The constructor uses determineBackingMap internally and should throw
        new CaseInsensitiveMap<>(src);
    }

    // -----------------------------------------------------------------------
    // Non‑String keys should behave like a normal map
    // -----------------------------------------------------------------------
    @Test
    public void testNonStringKeyBehaviour() {
        CaseInsensitiveMap<Integer, String> map = new CaseInsensitiveMap<>();
        map.put(10, "ten");
        assertEquals("ten", map.get(10));
        assertTrue(map.containsKey(10));
        assertEquals("ten", map.remove(10));
        assertTrue(map.isEmpty());
    }

    // -----------------------------------------------------------------------
    // equals() contract and case‑insensitive equality
    // -----------------------------------------------------------------------
    @Test
    public void testEqualsWithCaseInsensitiveKeys() {
        CaseInsensitiveMap<String, Integer> m1 = new CaseInsensitiveMap<>();
        m1.put("Abc", 1);
        CaseInsensitiveMap<String, Integer> m2 = new CaseInsensitiveMap<>();
        m2.put("aBC", 1);
        assertTrue(m1.equals(m2));
        assertTrue(m2.equals(m1));

        // different values -> not equal
        m2.put("ABC", 2);
        assertFalse(m1.equals(m2));
    }

    @Test
    public void testEqualsAgainstNonMap() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("test", 5);
        assertFalse(map.equals("not a map"));
    }

    // -----------------------------------------------------------------------
    // keySet view – original case preservation and mutation
    // -----------------------------------------------------------------------
    @Test
    public void testKeySetPreservesOriginalCase() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("CamelCase", "value");
        Set<String> keys = map.keySet();
        assertTrue(keys.contains("CamelCase"));
        assertTrue(keys.contains("camelcase")); // contains uses case‑insensitive logic
        Iterator<String> it = keys.iterator();
        assertEquals("CamelCase", it.next());
    }

    @Test
    public void testKeySetToArrayWithLargerArray() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("A", 1);
        map.put("B", 2);
        String[] larger = new String[5];
        larger[0] = "X";
        larger[1] = "Y";
        String[] result = map.keySet().toArray(larger);
        assertEquals("A", result[0]);
        assertEquals("B", result[1]);
        assertNull(result[2]); // per contract
        assertEquals("Y", result[3]); // untouched elements remain unchanged
    }

    @Test
    public void testKeySetRetainAll() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("One", 1);
        map.put("Two", 2);
        map.put("Three", 3);
        List<String> keep = Arrays.asList("one", "THREE");
        assertTrue(map.keySet().retainAll(keep));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("ONE"));
        assertTrue(map.containsKey("three"));
        assertFalse(map.containsKey("Two"));
    }

    // -----------------------------------------------------------------------
    // entrySet view – original key exposure and mutation
    // -----------------------------------------------------------------------
    @Test
    public void testEntrySetOriginalKeyAndRemoval() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("KeyOne", "v1");
        map.put("KeyTwo", "v2");
        Set<Map.Entry<String, String>> entries = map.entrySet();
        assertEquals(2, entries.size());

        // verify that each entry returns original key
        for (Map.Entry<String, String> e : entries) {
            assertTrue(e.getKey().equals("KeyOne") || e.getKey().equals("KeyTwo"));
        }

        // remove via entrySet
        Map.Entry<String, String> toRemove = null;
        for (Map.Entry<String, String> e : entries) {
            if (e.getKey().equals("KeyOne")) {
                toRemove = e;
                break;
            }
        }
        assertNotNull(toRemove);
        assertTrue(entries.remove(toRemove));
        assertFalse(map.containsKey("keyone"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetRemoveAllAndRetainAll() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        // removeAll
        List<Map.Entry<String, Integer>> toRemove = new ArrayList<>();
        for (Map.Entry<String, Integer> e : entries) {
            if (e.getKey().equalsIgnoreCase("b")) {
                toRemove.add(e);
            }
        }
        assertTrue(entries.removeAll(toRemove));
        assertFalse(map.containsKey("b"));
        assertEquals(2, map.size());

        // retainAll (keep only "a")
        List<Map.Entry<String, Integer>> toRetain = new ArrayList<>();
        for (Map.Entry<String, Integer> e : entries) {
            if (e.getKey().equalsIgnoreCase("a")) {
                toRetain.add(e);
            }
        }
        assertTrue(entries.retainAll(toRetain));
        assertTrue(map.containsKey("a"));
        assertFalse(map.containsKey("c"));
        assertEquals(1, map.size());
    }

    // -----------------------------------------------------------------------
    // computeIfAbsent / computeIfPresent / compute / merge
    // -----------------------------------------------------------------------
    @Test
    public void testComputeIfAbsentCaseInsensitive() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.computeIfAbsent("Test", k -> 42);
        assertEquals(Integer.valueOf(42), map.get("test"));
        // second call should not invoke function
        map.computeIfAbsent("TEST", k -> fail("Function should not be called"));
        assertEquals(Integer.valueOf(42), map.get("test"));
    }

    @Test
    public void testComputeIfPresentCaseInsensitive() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("Alpha", 10);
        map.computeIfPresent("ALPHA", (k, v) -> v + 5);
        assertEquals(Integer.valueOf(15), map.get("alpha"));
    }

    @Test
    public void testComputeCaseInsensitive() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("X", 1);
        map.compute("x", (k, v) -> v == null ? 0 : v + 9);
        assertEquals(Integer.valueOf(10), map.get("X"));
    }

    @Test
    public void testMergeCaseInsensitive() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Key", "first");
        map.merge("key", "second", (oldV, newV) -> oldV + "+" + newV);
        assertEquals("first+second", map.get("KEY"));
    }

    // -----------------------------------------------------------------------
    // putIfAbsent, replace, replaceAll, forEach
    // -----------------------------------------------------------------------
    @Test
    public void testPutIfAbsentAndReplaceMethods() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        assertNull(map.putIfAbsent("Key", "v1"));
        assertEquals("v1", map.get("key"));
        assertEquals("v1", map.putIfAbsent("KEY", "v2")); // should not replace
        assertEquals("v1", map.get("key"));

        // replace(K, old, new)
        assertTrue(map.replace("kEy", "v1", "v3"));
        assertEquals("v3", map.get("KEY"));
        assertFalse(map.replace("key", "v1", "v4")); // old value mismatch

        // replace(K, new)
        assertEquals("v3", map.replace("KeY", "v5"));
        assertEquals("v5", map.get("key"));
    }

    @Test
    public void testReplaceAllAndForEach() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("a", 1);
        map.put("B", 2);
        // replaceAll doubles each value
        map.replaceAll((k, v) -> v * 2);
        assertEquals(Integer.valueOf(2), map.get("A"));
        assertEquals(Integer.valueOf(4), map.get("b"));

        // forEach collects entries into a list, verifying original key case
        List<String> collected = new ArrayList<>();
        map.forEach((k, v) -> collected.add(k + "=" + v));
        assertTrue(collected.contains("a=2"));
        assertTrue(collected.contains("B=4"));
    }

    // -----------------------------------------------------------------------
    // remove(Object, Object) overload
    // -----------------------------------------------------------------------
    @Test
    public void testRemoveKeyAndValue() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Key", "Value");
        assertTrue(map.remove("KEY", "Value"));
        assertFalse(map.containsKey("key"));
        // trying to remove non‑matching value should fail
        map.put("Another", "One");
        assertFalse(map.remove("another", "Two"));
        assertTrue(map.containsKey("another"));
    }

    // -----------------------------------------------------------------------
    // Static configuration: setMaxCacheLengthString, replaceCache, replaceRegistry
    // -----------------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testSetMaxCacheLengthStringTooSmall() {
        CaseInsensitiveMap.setMaxCacheLengthString(5);
    }

    @Test
    public void testReplaceCacheWithNullLeadsToNPEOnNewString() {
        // replace the cache with null
        CaseInsensitiveMap.replaceCache(null);
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        try {
            map.put("any", "value");
            fail("Expected NullPointerException because cache is null");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testReplaceRegistryWithInvalidArguments() {
        // null registry
        try {
            CaseInsensitiveMap.replaceRegistry(null);
            fail("Expected NullPointerException for null registry");
        } catch (NullPointerException ignored) {}

        // duplicate class entry
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> dup = new ArrayList<>();
        dup.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        dup.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        try {
            CaseInsensitiveMap.replaceRegistry(dup);
            fail("Expected IllegalArgumentException for duplicate class");
        } catch (IllegalArgumentException ignored) {}

        // incorrect ordering (Map before HashMap)
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> badOrder = new ArrayList<>();
        badOrder.add(new AbstractMap.SimpleEntry<>(Map.class, size -> new HashMap<>()));
        badOrder.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>()));
        try {
            CaseInsensitiveMap.replaceRegistry(badOrder);
            fail("Expected IllegalStateException for ordering error");
        } catch (IllegalStateException ignored) {}
    }

    @Test
    public void testReplaceRegistryWithValidCustomRegistry() {
        // a minimal but valid registry (TreeMap only)
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> custom = new ArrayList<>();
        custom.add(new AbstractMap.SimpleEntry<>(TreeMap.class, size -> new TreeMap<>()));
        // must also include a fallback (LinkedHashMap) to avoid later failures
        custom.add(new AbstractMap.SimpleEntry<>(LinkedHashMap.class, size -> new LinkedHashMap<>()));
        CaseInsensitiveMap.replaceRegistry(custom);

        // creating a map from a source TreeMap should now use a TreeMap internally
        Map<String, String> src = new TreeMap<>();
        src.put("Z", "z");
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>(src);
        assertTrue(map.getWrappedMap() instanceof TreeMap);
    }

    // -----------------------------------------------------------------------
    // CaseInsensitiveEntry specific behavior
    // -----------------------------------------------------------------------
    @Test
    public void testCaseInsensitiveEntryKeyAndEquality() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("MixedCase", "val");
        Map.Entry<String, String> entry = map.entrySet().iterator().next(); // wraps as CaseInsensitiveEntry
        assertTrue(entry instanceof CaseInsensitiveMap.CaseInsensitiveEntry);
        CaseInsensitiveMap.CaseInsensitiveEntry cie = (CaseInsensitiveMap.CaseInsensitiveEntry) entry;

        // getKey returns original case
        assertEquals("MixedCase", cie.getKey());

        // getOriginalKey returns the internal CaseInsensitiveString
        assertTrue(cie.getOriginalKey() instanceof CaseInsensitiveMap.CaseInsensitiveString);

        // equality uses original key
        Map.Entry<String, String> other = new AbstractMap.SimpleEntry<>("MixedCase", "val");
        assertTrue(cie.equals(other));
        assertEquals(cie.hashCode(), other.hashCode());
    }

    // -----------------------------------------------------------------------
    // Internal CaseInsensitiveString behavior through public API
    // -----------------------------------------------------------------------
    @Test
    public void testCaseInsensitiveStringEqualityThroughMap() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Hello", "world");
        // Different case should be equal
        assertTrue(map.getWrappedMap().keySet().iterator().next()
                .equals(new CaseInsensitiveMap.CaseInsensitiveString("HELLO")));
    }

    // -----------------------------------------------------------------------
    // Verify that long strings bypass the cache (no exception, just different instance)
    // -----------------------------------------------------------------------
    @Test
    public void testLongStringBypassesCache() {
        // set a small max cache length to force bypass
        CaseInsensitiveMap.setMaxCacheLengthString(10);
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        String longKey = "thisIsAVeryLongKeyExceedingLimit";
        map.put(longKey, 1);
        // second put with same content should create a *different* cached instance internally,
        // but map still works case‑insensitively
        map.put(longKey.toUpperCase(), 2);
        assertEquals(Integer.valueOf(2), map.get(longKey.toLowerCase()));
    }
}
