package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link MapUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal operation of each public method.</li>
 *   <li>Edge cases such as {@code null} arguments, empty collections, self‑referencing maps, etc.</li>
 *   <li>Exception‑throwing behavior for illegal arguments or error conditions.</li>
 *   <li>Immutability of the maps returned by {@code dupe}, {@code mapOf}, {@code mapOfEntries},
 *       {@code cloneMapOfSets} and {@code cloneMapOfMaps}.</li>
 *   <li>Correct ordering detection performed by {@code detectMapOrdering} on a variety of
 *       standard JDK map implementations.</li>
 * </ul>
 *
 * No external mocking frameworks are used – all objects are created via their public
 * constructors or factory methods that are visible in the source code supplied.
 */
public class MapUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /*  Tests for MapUtilities.get(..)                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetReturnsValueWhenPresent() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        Integer result = MapUtilities.get(map, "one", -1);
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testGetReturnsDefaultWhenMissing() {
        Map<String, Integer> map = new HashMap<>();
        Integer result = MapUtilities.get(map, "missing", -1);
        assertEquals(Integer.valueOf(-1), result);
    }

    @Test
    public void testGetReturnsDefaultWhenValueIsNull() {
        Map<String, String> map = new HashMap<>();
        map.put("key", null);
        String result = MapUtilities.get(map, "key", "def");
        assertEquals("def", result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for MapUtilities.getOrThrow(..)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetOrThrowReturnsValueEvenIfNull() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("k", null);
        Object value = MapUtilities.getOrThrow(map, "k", new IllegalArgumentException("missing"));
        assertNull(value);
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowMapNullThrowsNPE() throws Exception {
        MapUtilities.getOrThrow(null, "any", new IllegalArgumentException("missing"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowThrowableNullThrowsNPE() throws Exception {
        Map<String, String> map = new HashMap<>();
        MapUtilities.getOrThrow(map, "any", null);
    }

    @Test
    public void testGetOrThrowKeyMissingThrowsProvidedException() {
        Map<String, Integer> map = new HashMap<>();
        IllegalStateException ex = new IllegalStateException("not there");
        try {
            MapUtilities.getOrThrow(map, "absent", ex);
            fail("Expected exception to be thrown");
        } catch (IllegalStateException e) {
            assertSame(ex, e);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for MapUtilities.isEmpty(..)                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsEmptyWithNullMap() {
        assertTrue(MapUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmptyWithEmptyMap() {
        assertTrue(MapUtilities.isEmpty(new HashMap<>()));
    }

    @Test
    public void testIsEmptyWithNonEmptyMap() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "b");
        assertFalse(MapUtilities.isEmpty(map));
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for MapUtilities.dupe(..)                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDupeMutableCopyIsIndependent() {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        original.put(String.class, new HashSet<>(Arrays.asList("a", "b")));
        original.put(Integer.class, new HashSet<>(Collections.singletonList("c")));

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, false);

        // modify original – copy must stay unchanged
        original.get(String.class).add("new");
        assertFalse(copy.get(String.class).contains("new"));

        // modify copy – original must stay unchanged
        copy.get(Integer.class).add("x");
        assertFalse(original.get(Integer.class).contains("x"));
    }

    @Test
    public void testDupeUnmodifiableCopyThrowsOnModification() {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        original.put(String.class, new HashSet<>(Arrays.asList("a", "b")));

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, true);

        // The map itself must be unmodifiable
        try {
            copy.put(Double.class, Collections.emptySet());
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }

        // The inner set must also be unmodifiable
        try {
            copy.get(String.class).add("c");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for MapUtilities.cloneMapOfSets(..)                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCloneMapOfSetsMutable() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("one", new HashSet<>(Arrays.asList(1, 2)));
        original.put("two", new HashSet<>(Collections.singletonList(3)));

        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);

        // structural equality
        assertEquals(original, clone);

        // independence
        original.get("one").add(99);
        assertFalse(clone.get("one").contains(99));
    }

    @Test
    public void testCloneMapOfSetsImmutable() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("k", new HashSet<>(Arrays.asList(1, 2)));

        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, true);

        // map is unmodifiable
        try {
            clone.put("new", Collections.emptySet());
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }

        // inner set is unmodifiable
        try {
            clone.get("k").add(3);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for MapUtilities.cloneMapOfMaps(..)                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCloneMapOfMapsMutable() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(1, "one");
        original.put("first", inner);

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);

        assertEquals(original, clone);

        // modify original – clone must stay unchanged
        inner.put(2, "two");
        assertFalse(clone.get("first").containsKey(2));
    }

    @Test
    public void testCloneMapOfMapsImmutable() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        original.put("a", Collections.singletonMap(1, "one"));

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, true);

        // map unmodifiable
        try {
            clone.put("b", Collections.emptyMap());
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }

        // inner map unmodifiable
        try {
            clone.get("a").put(2, "two");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for MapUtilities.mapToString(..)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMapToStringEmpty() {
        Map<String, Integer> map = new HashMap<>();
        assertEquals("{}", MapUtilities.mapToString(map));
    }

    @Test
    public void testMapToStringSimple() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("{a=1, b=2}", MapUtilities.mapToString(map));
    }

    @Test
    public void testMapToStringSelfReference() {
        Map<String, Object> map = new HashMap<>();
        map.put("self", map);
        assertEquals("{self=(this Map)}", MapUtilities.mapToString(map));
    }

    @Test
    public void testMapToStringKeyEqualsMapReference() {
        Map<Object, String> map = new HashMap<>();
        map.put(map, "value");
        assertEquals("{(this Map)=value}", MapUtilities.mapToString(map));
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for MapUtilities.mapOf(..)                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMapOfWithValidPairs() {
        Map<String, Integer> map = MapUtilities.mapOf("one", 1, "two", 2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(Integer.valueOf(2), map.get("two"));

        // unmodifiable – should throw on put
        try {
            map.put("three", 3);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testMapOfWithNoArgumentsReturnsEmpty() {
        Map<Object, Object> map = MapUtilities.mapOf();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testMapOfWithNullArrayReturnsEmpty() {
        Map<Object, Object> map = MapUtilities.mapOf((Object[]) null);
        assertTrue(map.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapOfWithOddNumberThrows() {
        MapUtilities.mapOf("a", 1, "b"); // three arguments -> odd
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapOfTooManyEntriesThrows() {
        // 11 entries => 22 arguments, which exceeds MAX_ENTRIES (10)
        Object[] args = new Object[22];
        for (int i = 0; i < 22; i++) {
            args[i] = i; // simple values; type erasure will make them keys & values
        }
        MapUtilities.mapOf(args);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for MapUtilities.mapOfEntries(..)                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMapOfEntriesValid() {
        Map.Entry<String, Integer> e1 = new SimpleEntry<>("x", 10);
        Map.Entry<String, Integer> e2 = new SimpleEntry<>("y", 20);
        Map<String, Integer> map = MapUtilities.mapOfEntries(e1, e2);

        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(10), map.get("x"));
        assertEquals(Integer.valueOf(20), map.get("y"));

        // verify immutability
        try {
            map.put("z", 30);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testMapOfEntriesEmptyArrayReturnsEmpty() {
        Map<String, Integer> map = MapUtilities.mapOfEntries();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testMapOfEntriesNullArrayReturnsEmpty() {
        Map<String, Integer> map = MapUtilities.mapOfEntries((Map.Entry<String, Integer>[]) null);
        assertTrue(map.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testMapOfEntriesWithNullEntryThrows() {
        MapUtilities.mapOfEntries(new SimpleEntry<>("a", 1), null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for MapUtilities.detectMapOrdering(..)                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDetectMapOrderingNullReturnsUnordered() {
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(null));
    }

    @Test
    public void testDetectMapOrderingLinkedHashMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(map));
    }

    @Test
    public void testDetectMapOrderingTreeMap() {
        Map<String, Integer> map = new TreeMap<>();
        map.put("b", 2);
        map.put("a", 1);
        assertEquals(CompactMap.SORTED, MapUtilities.detectMapOrdering(map));
    }

    @Test
    public void testDetectMapOrderingEnumMap() {
        EnumMap<Day, String> map = new EnumMap<>(Day.class);
        map.put(Day.MONDAY, "Mon");
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(map));
    }

    @Test
    public void testDetectMapOrderingHashMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("x", 1);
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(map));
    }

    // Helper enum for EnumMap test
    private enum Day { MONDAY, TUESDAY, WEDNESDAY }
}
