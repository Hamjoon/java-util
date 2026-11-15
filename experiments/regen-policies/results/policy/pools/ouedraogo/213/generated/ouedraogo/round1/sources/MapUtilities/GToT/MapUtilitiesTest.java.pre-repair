package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

/**
 * Comprehensive JUnit‑4 test suite for {@link MapUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal usage of each public method.</li>
 *   <li>Edge‑case handling (null arguments, empty collections, self‑referencing maps, etc.).</li>
 *   <li>Exception scenarios (odd argument count, too many entries, null throwable, etc.).</li>
 *   <li>Immutability guarantees for the {@code mapOf}, {@code mapOfEntries},
 *       {@code dupe}, {@code cloneMapOfSets} and {@code cloneMapOfMaps} methods.</li>
 * </ul>
 *
 * The test class lives in the same package as {@code MapUtilities} so that package‑private
 * helpers can be accessed if needed, but only public API is exercised.
 */
public class MapUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /*  get(Map, key, default)                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetFoundValue() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        Integer result = MapUtilities.get(map, "one", -1);
        Assert.assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testGetMissingKeyReturnsDefault() {
        Map<String, Integer> map = new HashMap<>();
        Integer result = MapUtilities.get(map, "missing", -1);
        Assert.assertEquals(Integer.valueOf(-1), result);
    }

    @Test
    public void testGetKeyPresentWithNullValueReturnsDefault() {
        Map<String, Integer> map = new HashMap<>();
        map.put("nullValue", null);
        Integer result = MapUtilities.get(map, "nullValue", 42);
        Assert.assertEquals(Integer.valueOf(42), result);
    }

    @Test
    public void testGetNullMapThrowsNPE() {
        try {
            MapUtilities.get(null, "any", "def");
            Assert.fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  getOrThrow(Map, key, throwable)                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetOrThrowFoundKeyReturnsValue() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("k", "v");
        Object result = MapUtilities.getOrThrow(map, "k", new IllegalStateException("missing"));
        Assert.assertEquals("v", result);
    }

    @Test
    public void testGetOrThrowKeyPresentWithNullValue() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("k", null);
        Object result = MapUtilities.getOrThrow(map, "k", new IllegalStateException("missing"));
        Assert.assertNull(result);
    }

    @Test
    public void testGetOrThrowMissingKeyThrowsProvidedException() {
        Map<String, Integer> map = new HashMap<>();
        IllegalArgumentException toThrow = new IllegalArgumentException("not found");
        try {
            MapUtilities.getOrThrow(map, "absent", toThrow);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertSame(toThrow, e);
        }
    }

    @Test
    public void testGetOrThrowNullMapThrowsNPE() {
        try {
            MapUtilities.getOrThrow(null, "k", new RuntimeException());
            Assert.fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testGetOrThrowNullThrowableThrowsNPE() {
        Map<String, String> map = Collections.singletonMap("a", "b");
        try {
            MapUtilities.getOrThrow(map, "missing", null);
            Assert.fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  isEmpty(Map)                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsEmptyWithNullMap() {
        Assert.assertTrue(MapUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmptyWithEmptyMap() {
        Assert.assertTrue(MapUtilities.isEmpty(new HashMap<>()));
    }

    @Test
    public void testIsEmptyWithNonEmptyMap() {
        Map<String, String> map = new HashMap<>();
        map.put("x", "y");
        Assert.assertFalse(MapUtilities.isEmpty(map));
    }

    /* ---------------------------------------------------------------------- */
    /*  dupe(Map<Class<?>, Set<T>>, boolean)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDupeCreatesDeepCopy() {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        Set<String> set = new HashSet<>();
        set.add("alpha");
        original.put(String.class, set);

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, false);
        Assert.assertNotSame(original, copy);
        Assert.assertNotSame(set, copy.get(String.class));
        Assert.assertEquals(set, copy.get(String.class));

        // Mutate original – copy must stay unchanged
        set.add("beta");
        Assert.assertFalse(copy.get(String.class).contains("beta"));
    }

    @Test
    public void testDupeUnmodifiableResult() {
        Map<Class<?>, Set<Integer>> original = new LinkedHashMap<>();
        Set<Integer> nums = new HashSet<>();
        nums.add(1);
        original.put(Integer.class, nums);

        Map<Class<?>, Set<Integer>> copy = MapUtilities.dupe(original, true);
        Assert.assertTrue(isUnmodifiableMap(copy));

        // The inner set should also be unmodifiable
        Set<Integer> inner = copy.get(Integer.class);
        Assert.assertTrue(isUnmodifiableSet(inner));

        // Any mutation attempt must throw UnsupportedOperationException
        try {
            copy.put(Double.class, Collections.emptySet());
            Assert.fail("Expected UnsupportedOperationException on map mutation");
        } catch (UnsupportedOperationException ignored) {
        }

        try {
            inner.add(2);
            Assert.fail("Expected UnsupportedOperationException on set mutation");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    private boolean isUnmodifiableMap(Map<?, ?> map) {
        try {
            map.put(new Object(), new Object());
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }

    private boolean isUnmodifiableSet(Set<?> set) {
        try {
            set.add(new Object());
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  cloneMapOfSets(Map<T, Set<V>>, boolean)                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCloneMapOfSetsMutableCopy() {
        Map<String, Set<Integer>> original = new HashMap<>();
        Set<Integer> inner = new HashSet<>();
        inner.add(10);
        original.put("key", inner);

        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);
        Assert.assertNotSame(original, clone);
        Assert.assertNotSame(inner, clone.get("key"));
        Assert.assertEquals(inner, clone.get("key"));

        // Changing original does not affect clone
        inner.add(20);
        Assert.assertFalse(clone.get("key").contains(20));
    }

    @Test
    public void testCloneMapOfSetsImmutableCopy() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("k", new HashSet<>(Collections.singleton(1)));

        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, true);
        Assert.assertTrue(isUnmodifiableMap(clone));
        Assert.assertTrue(isUnmodifiableSet(clone.get("k")));

        // Mutations must fail
        try {
            clone.put("new", Collections.emptySet());
            Assert.fail("Expected UnsupportedOperationException on map mutation");
        } catch (UnsupportedOperationException ignored) {
        }

        try {
            clone.get("k").add(2);
            Assert.fail("Expected UnsupportedOperationException on set mutation");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  cloneMapOfMaps(Map<T, Map<U, V>>, boolean)                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCloneMapOfMapsMutableCopy() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(1, "one");
        original.put("outer", inner);

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);
        Assert.assertNotSame(original, clone);
        Assert.assertNotSame(inner, clone.get("outer"));
        Assert.assertEquals(inner, clone.get("outer"));

        // Mutate original inner map – clone must stay unchanged
        inner.put(2, "two");
        Assert.assertFalse(clone.get("outer").containsKey(2));
    }

    @Test
    public void testCloneMapOfMapsImmutableCopy() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        original.put("a", Collections.singletonMap(1, "one"));

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, true);
        Assert.assertTrue(isUnmodifiableMap(clone));
        Assert.assertTrue(isUnmodifiableMap(clone.get("a")));

        // Mutations must fail
        try {
            clone.put("b", Collections.emptyMap());
            Assert.fail("Expected UnsupportedOperationException on map mutation");
        } catch (UnsupportedOperationException ignored) {
        }

        try {
            clone.get("a").put(2, "two");
            Assert.fail("Expected UnsupportedOperationException on inner map mutation");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  mapToString(Map<K,V>)                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMapToStringEmpty() {
        Map<String, Integer> map = Collections.emptyMap();
        Assert.assertEquals("{}", MapUtilities.mapToString(map));
    }

    @Test
    public void testMapToStringSingleEntry() {
        Map<String, Integer> map = Collections.singletonMap("a", 1);
        Assert.assertEquals("{a=1}", MapUtilities.mapToString(map));
    }

    @Test
    public void testMapToStringMultipleEntriesPreserveOrder() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);
        Assert.assertEquals("{first=1, second=2, third=3}", MapUtilities.mapToString(map));
    }

    @Test
    public void testMapToStringSelfReference() {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put("self", map);
        map.put(map, "selfKey");
        Assert.assertEquals("{self=(this Map), (this Map)=selfKey}", MapUtilities.mapToString(map));
    }

    /* ---------------------------------------------------------------------- */
    /*  mapOf(Object... keyValues)                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMapOfNoArgumentsReturnsEmptyUnmodifiable() {
        Map<?, ?> result = MapUtilities.mapOf();
        Assert.assertTrue(result.isEmpty());
        Assert.assertTrue(isUnmodifiableMap(result));
    }

    @Test
    public void testMapOfEvenNumberOfArgumentsCreatesMap() {
        Map<String, Integer> result = MapUtilities.mapOf("one", 1, "two", 2);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(Integer.valueOf(1), result.get("one"));
        Assert.assertEquals(Integer.valueOf(2), result.get("two"));
        Assert.assertTrue(isUnmodifiableMap(result));
    }

    @Test
    public void testMapOfOddNumberOfArgumentsThrows() {
        try {
            MapUtilities.mapOf("a", 1, "b");
            Assert.fail("Expected IllegalArgumentException for odd argument count");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testMapOfTooManyEntriesThrows() {
        Object[] many = new Object[22]; // 11 pairs -> exceeds MAX_ENTRIES (10)
        for (int i = 0; i < many.length; i++) {
            many[i] = i; // keys and values are simple integers
        }
        try {
            MapUtilities.mapOf(many);
            Assert.fail("Expected IllegalArgumentException for too many entries");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testMapOfAllowsNullKeyAndValue() {
        Map<String, String> result = MapUtilities.mapOf(null, "value", "key", null);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("value", result.get(null));
        Assert.assertNull(result.get("key"));
    }

    /* ---------------------------------------------------------------------- */
    /*  mapOfEntries(Map.Entry<K,V>... entries)                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMapOfEntriesNoArgumentsReturnsEmptyUnmodifiable() {
        Map<?, ?> result = MapUtilities.mapOfEntries();
        Assert.assertTrue(result.isEmpty());
        Assert.assertTrue(isUnmodifiableMap(result));
    }

    @Test
    public void testMapOfEntriesCreatesMap() {
        Map.Entry<String, Integer> e1 = new AbstractMap.SimpleEntry<>("a", 1);
        Map.Entry<String, Integer> e2 = new AbstractMap.SimpleEntry<>("b", 2);
        Map<String, Integer> result = MapUtilities.mapOfEntries(e1, e2);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(Integer.valueOf(1), result.get("a"));
        Assert.assertEquals(Integer.valueOf(2), result.get("b"));
        Assert.assertTrue(isUnmodifiableMap(result));
    }

    @Test
    public void testMapOfEntriesNullArrayReturnsEmpty() {
        Map<?, ?> result = MapUtilities.mapOfEntries((Map.Entry<?, ?>[]) null);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testMapOfEntriesNullEntryThrowsNPE() {
        Map.Entry<String, Integer> good = new AbstractMap.SimpleEntry<>("x", 10);
        try {
            MapUtilities.mapOfEntries(good, null);
            Assert.fail("Expected NullPointerException for null entry");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Additional helper tests for enum and sorted maps used by detectMapOrdering */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDetectMapOrderingWithEnumMap() {
        EnumMap<TestEnum, String> enumMap = new EnumMap<>(TestEnum.class);
        enumMap.put(TestEnum.A, "a");
        String ordering = MapUtilities.detectMapOrdering(enumMap);
        // EnumMap preserves insertion order according to the enum declaration
        Assert.assertEquals(CompactMap.INSERTION, ordering);
    }

    @Test
    public void testDetectMapOrderingWithTreeMap() {
        TreeMap<Integer, String> sorted = new TreeMap<>();
        sorted.put(2, "b");
        sorted.put(1, "a");
        String ordering = MapUtilities.detectMapOrdering(sorted);
        Assert.assertEquals(CompactMap.SORTED, ordering);
    }

    // Simple enum used for the EnumMap test above
    private enum TestEnum { A, B, C }
}
