package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Comprehensive JUnit4 test suite for {@link CaseInsensitiveMap}.
 * Covers case‑insensitive behavior, key/value preservation, collection views,
 * map‑wide operations, and the static registry / cache utilities.
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
        // Restore the default registry after each test to avoid cross‑test contamination
        CaseInsensitiveMap.replaceRegistry(getDefaultRegistry());
    }

    /** Helper to rebuild the original registry (used after tests that replace it). */
    private static List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> getDefaultRegistry() {
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
        return list;
    }

    /** ------------------------------------ Basic put / get / contains ------------------------------------ */

    @Test
    public void testPutGetCaseInsensitive() {
        map.put("KeyOne", "value1");
        assertEquals("value1", map.get("keyone"));
        assertEquals("value1", map.get("KEYONE"));
        assertTrue(map.containsKey("kEyOnE"));
    }

    @Test
    public void testPutNonStringKey() {
        CaseInsensitiveMap<Integer, String> intMap = new CaseInsensitiveMap<>();
        intMap.put(42, "Answer");
        assertEquals("Answer", intMap.get(42));
        assertTrue(intMap.containsKey(42));
        assertNull(intMap.get(43));
    }

    @Test
    public void testRemoveCaseInsensitive() {
        map.put("RemoveMe", "gone");
        assertEquals("gone", map.remove("removeme"));
        assertFalse(map.containsKey("RemoveMe"));
    }

    @Test
    public void testRemoveWithValue() {
        map.put("K", "V");
        assertTrue(map.remove("k", "V"));
        assertFalse(map.containsKey("K"));
        map.put("K2", "V2");
        assertFalse(map.remove("k2", "WRONG"));
    }

    /** ------------------------------------ Equality ------------------------------------ */

    @Test
    public void testEqualsWithDifferentCaseKeys() {
        CaseInsensitiveMap<String, String> other = new CaseInsensitiveMap<>();
        map.put("Abc", "123");
        other.put("aBC", "123");
        assertTrue(map.equals(other));
        assertTrue(other.equals(map));
    }

    @Test
    public void testNotEqualsDifferentValues() {
        map.put("Key", "val1");
        CaseInsensitiveMap<String, String> other = new CaseInsensitiveMap<>();
        other.put("key", "val2");
        assertFalse(map.equals(other));
    }

    /** ------------------------------------ compute / merge family ------------------------------------ */

    @Test
    public void testComputeIfAbsentAddsWhenMissing() {
        map.put("Present", "yes");
        String result = map.computeIfAbsent("Missing", k -> {
            assertEquals("Missing", k); // original case should be passed
            return "added";
        });
        assertEquals("added", result);
        assertEquals("added", map.get("missing"));
    }

    @Test
    public void testComputeIfAbsentDoesNotOverwriteExisting() {
        map.put("Existing", "old");
        String result = map.computeIfAbsent("existing", k -> "new");
        assertEquals("old", result);
        assertEquals("old", map.get("EXISTING"));
    }

    @Test
    public void testComputeIfPresentUpdatesWhenPresent() {
        map.put("UpdateMe", "first");
        String newVal = map.computeIfPresent("updateme", (k, v) -> {
            assertEquals("updateme", k);
            return v + "-second";
        });
        assertEquals("first-second", newVal);
        assertEquals("first-second", map.get("UPDATEme"));
    }

    @Test
    public void testComputeIfPresentReturnsNullWhenAbsent() {
        assertNull(map.computeIfPresent("absent", (k, v) -> "won't be called"));
    }

    @Test
    public void testComputeReplacesValue() {
        map.put("Comp", "old");
        String res = map.compute("comp", (k, v) -> {
            assertEquals("comp", k);
            return v + "-new";
        });
        assertEquals("old-new", res);
        assertEquals("old-new", map.get("COMP"));
    }

    @Test
    public void testMergeCombinesValues() {
        map.put("M", "a");
        String merged = map.merge("m", "b", (oldV, newV) -> oldV + newV);
        assertEquals("ab", merged);
        assertEquals("ab", map.get("M"));
    }

    @Test
    public void testMergeInsertsWhenMissing() {
        String merged = map.merge("newKey", "first", (oldV, newV) -> oldV + newV);
        assertEquals("first", merged);
        assertEquals("first", map.get("NEWKEY"));
    }

    /** ------------------------------------ putIfAbsent / replace ------------------------------------ */

    @Test
    public void testPutIfAbsentDoesNotOverwrite() {
        map.put("pif", "v1");
        assertEquals("v1", map.putIfAbsent("PIF", "v2"));
        assertEquals("v1", map.get("pif"));
    }

    @Test
    public void testPutIfAbsentInsertsWhenAbsent() {
        assertNull(map.putIfAbsent("new", "val"));
        assertEquals("val", map.get("NEW"));
    }

    @Test
    public void testReplaceConditionalSuccess() {
        map.put("rep", "old");
        assertTrue(map.replace("REP", "old", "new"));
        assertEquals("new", map.get("rep"));
    }

    @Test
    public void testReplaceConditionalFailure() {
        map.put("rep", "old");
        assertFalse(map.replace("rep", "wrong", "new"));
        assertEquals("old", map.get("rep"));
    }

    @Test
    public void testReplaceUnconditional() {
        map.put("r", "first");
        assertEquals("first", map.replace("R", "second"));
        assertEquals("second", map.get("r"));
    }

    /** ------------------------------------ forEach & replaceAll ------------------------------------ */

    @Test
    public void testForEachProvidesOriginalKey() {
        map.put("Alpha", "A");
        map.put("Beta", "B");
        List<String> seenKeys = new ArrayList<>();
        List<String> seenValues = new ArrayList<>();
        map.forEach((k, v) -> {
            seenKeys.add(k);
            seenValues.add(v);
        });
        assertTrue(seenKeys.containsAll(Arrays.asList("Alpha", "Beta")));
        assertTrue(seenValues.containsAll(Arrays.asList("A", "B")));
    }

    @Test
    public void testReplaceAllTransformsValues() {
        map.put("X", "1");
        map.put("Y", "2");
        map.replaceAll((k, v) -> v + v);
        assertEquals("11", map.get("x"));
        assertEquals("22", map.get("Y"));
    }

    /** ------------------------------------ keySet view ------------------------------------ */

    @Test
    public void testKeySetIterationPreservesOriginalCase() {
        map.put("First", "1");
        map.put("Second", "2");
        Iterator<String> it = map.keySet().iterator();
        Set<String> seen = new HashSet<>();
        while (it.hasNext()) {
            seen.add(it.next());
        }
        assertTrue(seen.containsAll(Arrays.asList("First", "Second")));
    }

    @Test
    public void testKeySetToArray() {
        map.put("One", "1");
        map.put("Two", "2");
        String[] array = map.keySet().toArray(new String[0]);
        assertEquals(2, array.length);
        assertTrue(Arrays.asList(array).containsAll(Arrays.asList("One", "Two")));
    }

    @Test
    public void testKeySetRetainAll() {
        map.put("A", "a");
        map.put("B", "b");
        map.put("C", "c");
        Collection<String> keep = Arrays.asList("a", "C"); // note mixed case
        assertTrue(map.keySet().retainAll(keep));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("C"));
        assertFalse(map.containsKey("b"));
    }

    /** ------------------------------------ entrySet view ------------------------------------ */

    @Test
    public void testEntrySetIterationPreservesOriginalKey() {
        map.put("KeyOne", "val1");
        map.put("KeyTwo", "val2");
        Set<Map.Entry<String, String>> entries = map.entrySet();
        Set<String> keys = new HashSet<>();
        Set<String> values = new HashSet<>();
        for (Map.Entry<String, String> e : entries) {
            keys.add(e.getKey());
            values.add(e.getValue());
        }
        assertTrue(keys.containsAll(Arrays.asList("KeyOne", "KeyTwo")));
        assertTrue(values.containsAll(Arrays.asList("val1", "val2")));
    }

    @Test
    public void testEntrySetRemove() {
        map.put("Rem", "gone");
        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        assertTrue(map.entrySet().remove(entry));
        assertFalse(map.containsKey("Rem"));
    }

    @Test
    public void testEntrySetRetainAll() {
        map.put("A", "1");
        map.put("B", "2");
        map.put("C", "3");
        Set<Map.Entry<String, String>> retain = new HashSet<>();
        retain.add(new CaseInsensitiveMap<String, String>.CaseInsensitiveEntry(new AbstractMap.SimpleEntry<>("a", "1")));
        retain.add(new CaseInsensitiveMap<String, String>.CaseInsensitiveEntry(new AbstractMap.SimpleEntry<>("c", "3")));
        assertTrue(map.entrySet().retainAll(retain));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("A"));
        assertTrue(map.containsKey("C"));
        assertFalse(map.containsKey("B"));
    }

    /** ------------------------------------ CaseInsensitiveEntry behavior ------------------------------------ */

    @Test
    public void testCaseInsensitiveEntryKeyAndOriginalKey() {
        map.put("Original", "val");
        Map.Entry<String, String> raw = map.entrySet().iterator().next(); // returns CaseInsensitiveEntry
        assertTrue(raw instanceof CaseInsensitiveMap.CaseInsensitiveEntry);
        CaseInsensitiveMap.CaseInsensitiveEntry cie = (CaseInsensitiveMap.CaseInsensitiveEntry) raw;
        assertEquals("Original", cie.getKey()); // original case
        assertTrue(cie.getOriginalKey() instanceof CaseInsensitiveMap.CaseInsensitiveString);
        assertEquals("Original", cie.getOriginalKey().toString());
    }

    @Test
    public void testCaseInsensitiveEntrySetValueUpdatesMap() {
        map.put("Key", "old");
        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        CaseInsensitiveMap.CaseInsensitiveEntry cie = (CaseInsensitiveMap.CaseInsensitiveEntry) entry;
        cie.setValue("new");
        assertEquals("new", map.get("KEY"));
    }

    /** ------------------------------------ Registry replacement ------------------------------------ */

    @Test
    public void testReplaceRegistryWithCustomMapping() {
        // Build a simple registry that forces the backing map to be a HashMap
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> custom = new ArrayList<>();
        custom.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>(size)));
        CaseInsensitiveMap.replaceRegistry(custom);

        Map<String, String> source = new HashMap<>();
        source.put("One", "1");
        CaseInsensitiveMap<String, String> customMap = new CaseInsensitiveMap<>(source);
        assertTrue(customMap.getWrappedMap() instanceof HashMap);
        assertEquals("1", customMap.get("ONE"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplaceRegistryDuplicateClassThrows() {
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> bad = new ArrayList<>();
        bad.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>(size)));
        bad.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>(size)));
        CaseInsensitiveMap.replaceRegistry(bad);
    }

    @Test(expected = IllegalStateException.class)
    public void testReplaceRegistryIncorrectOrderThrows() {
        // Map is a supertype of HashMap; putting Map before HashMap should trigger order validation
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> bad = new ArrayList<>();
        bad.add(new AbstractMap.SimpleEntry<>(Map.class, size -> new HashMap<>(size)));
        bad.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>(size)));
        CaseInsensitiveMap.replaceRegistry(bad);
    }

    /** ------------------------------------ Cache replacement (basic sanity) ------------------------------------ */

    @Test
    public void testReplaceCacheDoesNotThrow() {
        LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> newCache = new LRUCache<>(10);
        CaseInsensitiveMap.replaceCache(newCache);
        // No direct observable effect; test passes if no exception is thrown
    }

    /** ------------------------------------ Backing map selection based on source type ------------------------------------ */

    @Test
    public void testDetermineBackingMapUsesTreeMapForTreeMapSource() {
        TreeMap<String, String> source = new TreeMap<>();
        source.put("z", "last");
        source.put("a", "first");
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(source);
        assertTrue(ciMap.getWrappedMap() instanceof TreeMap);
        // TreeMap preserves natural ordering; verify order via iterator
        Iterator<String> it = ciMap.keySet().iterator();
        assertEquals("a", it.next());
        assertEquals("z", it.next());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetermineBackingMapRejectsIdentityHashMap() {
        IdentityHashMap<String, String> source = new IdentityHashMap<>();
        source.put("X", "y");
        new CaseInsensitiveMap<>(source);
    }

    /** ------------------------------------ Size and clear behavior ------------------------------------ */

    @Test
    public void testSizeAndClear() {
        map.put("one", "1");
        map.put("two", "2");
        assertEquals(2, map.size());
        map.clear();
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    /** ------------------------------------ toString of entry ------------------------------------ */

    @Test
    public void testCaseInsensitiveEntryToString() {
        map.put("KeyStr", "val");
        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        assertEquals("KeyStr=val", entry.toString());
    }
}
