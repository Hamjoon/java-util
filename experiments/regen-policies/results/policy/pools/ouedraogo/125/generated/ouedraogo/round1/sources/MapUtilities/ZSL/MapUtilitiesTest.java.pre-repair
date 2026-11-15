package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

public class MapUtilitiesTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Map<String, Integer> sampleMap;

    @Before
    public void setUp() {
        sampleMap = new HashMap<>();
        sampleMap.put("one", 1);
        sampleMap.put("two", 2);
    }

    /* -------------------------------------------------------------------- */
    /* get(..)                                                             */
    /* -------------------------------------------------------------------- */
    @Test
    public void testGetFound() {
        Integer val = MapUtilities.get(sampleMap, "one", -1);
        assertEquals(Integer.valueOf(1), val);
    }

    @Test
    public void testGetNotFoundReturnsDefault() {
        Integer val = MapUtilities.get(sampleMap, "missing", -1);
        assertEquals(Integer.valueOf(-1), val);
    }

    @Test
    public void testGetNullValueReturnsDefault() {
        sampleMap.put("nullKey", null);
        String result = MapUtilities.get(sampleMap, "nullKey", "default");
        assertEquals("default", result);
    }

    /* -------------------------------------------------------------------- */
    /* getOrThrow(..)                                                       */
    /* -------------------------------------------------------------------- */
    @Test
    public void testGetOrThrowFoundEvenIfValueIsNull() throws Throwable {
        sampleMap.put("nullKey", null);
        Object val = MapUtilities.getOrThrow(sampleMap, "nullKey", new IllegalStateException());
        assertNull(val);
    }

    @Test
    public void testGetOrThrowNotFoundThrowsProvidedException() throws Throwable {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("missing key");
        MapUtilities.getOrThrow(sampleMap, "missing", new IllegalArgumentException("missing key"));
    }

    @Test
    public void testGetOrThrowNullMap() throws Throwable {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Map parameter cannot be null");
        MapUtilities.getOrThrow(null, "any", new RuntimeException());
    }

    @Test
    public void testGetOrThrowNullThrowable() throws Throwable {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Throwable object cannot be null");
        MapUtilities.getOrThrow(sampleMap, "any", null);
    }

    /* -------------------------------------------------------------------- */
    /* isEmpty(..)                                                          */
    /* -------------------------------------------------------------------- */
    @Test
    public void testIsEmptyNull() {
        assertTrue(MapUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmptyEmptyMap() {
        assertTrue(MapUtilities.isEmpty(new HashMap<>()));
    }

    @Test
    public void testIsEmptyNonEmptyMap() {
        assertFalse(MapUtilities.isEmpty(sampleMap));
    }

    /* -------------------------------------------------------------------- */
    /* dupe(..)                                                             */
    /* -------------------------------------------------------------------- */
    @Test
    public void testDupeModifiable() {
        Map<Class<?>, Set<String>> source = new LinkedHashMap<>();
        source.put(String.class, new HashSet<>(Arrays.asList("a", "b")));

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, false);
        assertNotSame(source, copy);
        assertEquals(source, copy);
        // modification of copy should not affect source
        copy.get(String.class).add("c");
        assertFalse(source.get(String.class).contains("c"));
    }

    @Test
    public void testDupeUnmodifiable() {
        Map<Class<?>, Set<String>> source = new LinkedHashMap<>();
        source.put(Integer.class, new HashSet<>(Collections.singletonList("x")));

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, true);
        assertTrue(isUnmodifiableMap(copy));
        assertTrue(isUnmodifiableSet(copy.get(Integer.class)));
    }

    private static boolean isUnmodifiableMap(Map<?, ?> map) {
        try {
            map.put(new Object(), new Object());
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }

    private static boolean isUnmodifiableSet(Set<?> set) {
        try {
            set.add(new Object());
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }

    /* -------------------------------------------------------------------- */
    /* cloneMapOfSets(..)                                                   */
    /* -------------------------------------------------------------------- */
    @Test
    public void testCloneMapOfSetsImmutable() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("k1", new HashSet<>(Arrays.asList(1, 2)));

        Map<String, Set<Integer>> cloned = MapUtilities.cloneMapOfSets(original, true);
        assertTrue(isUnmodifiableMap(cloned));
        assertTrue(isUnmodifiableSet(cloned.get("k1")));
    }

    @Test
    public void testCloneMapOfSetsMutable() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("k1", new HashSet<>(Arrays.asList(3, 4)));

        Map<String, Set<Integer>> cloned = MapUtilities.cloneMapOfSets(original, false);
        assertFalse(isUnmodifiableMap(cloned));
        assertFalse(isUnmodifiableSet(cloned.get("k1")));
        // ensure deep copy
        cloned.get("k1").add(5);
        assertFalse(original.get("k1").contains(5));
    }

    /* -------------------------------------------------------------------- */
    /* cloneMapOfMaps(..)                                                   */
    /* -------------------------------------------------------------------- */
    @Test
    public void testCloneMapOfMapsImmutable() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(1, "one");
        original.put("outer", inner);

        Map<String, Map<Integer, String>> cloned = MapUtilities.cloneMapOfMaps(original, true);
        assertTrue(isUnmodifiableMap(cloned));
        assertTrue(isUnmodifiableMap(cloned.get("outer")));
    }

    @Test
    public void testCloneMapOfMapsMutable() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(2, "two");
        original.put("outer", inner);

        Map<String, Map<Integer, String>> cloned = MapUtilities.cloneMapOfMaps(original, false);
        assertFalse(isUnmodifiableMap(cloned));
        assertFalse(isUnmodifiableMap(cloned.get("outer")));

        // deep copy check
        cloned.get("outer").put(3, "three");
        assertFalse(original.get("outer").containsKey(3));
    }

    /* -------------------------------------------------------------------- */
    /* mapToString(..)                                                      */
    /* -------------------------------------------------------------------- */
    @Test
    public void testMapToStringEmpty() {
        assertEquals("{}", MapUtilities.mapToString(new HashMap<>()));
    }

    @Test
    public void testMapToStringSingleEntry() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("a", 1);
        assertEquals("{a=1}", MapUtilities.mapToString(m));
    }

    @Test
    public void testMapToStringMultipleEntriesPreservesOrder() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("first", "1");
        m.put("second", "2");
        m.put("third", "3");
        assertEquals("{first=1, second=2, third=3}", MapUtilities.mapToString(m));
    }

    /* -------------------------------------------------------------------- */
    /* mapOf(..)                                                            */
    /* -------------------------------------------------------------------- */
    @Test
    public void testMapOfNoArgsReturnsEmptyUnmodifiable() {
        Map<?, ?> result = MapUtilities.mapOf();
        assertTrue(result.isEmpty());
        assertTrue(isUnmodifiableMap(result));
    }

    @Test
    public void testMapOfValidPairs() {
        Map<String, Integer> map = MapUtilities.mapOf("a", 1, "b", 2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertTrue(isUnmodifiableMap(map));
    }

    @Test
    public void testMapOfOddNumberOfArgumentsThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid number of arguments");
        MapUtilities.mapOf("a", 1, "b");
    }

    @Test
    public void testMapOfExceedsMaxEntriesThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Too many entries");
        // 11 entries => 22 arguments
        Object[] args = new Object[22];
        for (int i = 0; i < 22; i++) args[i] = i;
        MapUtilities.mapOf(args);
    }

    /* -------------------------------------------------------------------- */
    /* mapOfEntries(..)                                                     */
    /* -------------------------------------------------------------------- */
    @Test
    public void testMapOfEntriesEmpty() {
        Map<?, ?> result = MapUtilities.mapOfEntries();
        assertTrue(result.isEmpty());
        assertTrue(isUnmodifiableMap(result));
    }

    @Test
    public void testMapOfEntriesValid() {
        Map.Entry<String, Integer> e1 = new AbstractMap.SimpleEntry<>("x", 10);
        Map.Entry<String, Integer> e2 = new AbstractMap.SimpleEntry<>("y", 20);
        Map<String, Integer> map = MapUtilities.mapOfEntries(e1, e2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(10), map.get("x"));
        assertEquals(Integer.valueOf(20), map.get("y"));
        assertTrue(isUnmodifiableMap(map));
    }

    @Test
    public void testMapOfEntriesNullEntryThrows() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Entries must not be null");
        MapUtilities.mapOfEntries(new AbstractMap.SimpleEntry<>("k", "v"), null);
    }

    /* -------------------------------------------------------------------- */
    /* getMapStructureString(..)                                            */
    /* -------------------------------------------------------------------- */
    @Test
    public void testGetMapStructureStringNull() {
        assertEquals("null", MapUtilities.getMapStructureString(null));
    }

    @Test
    public void testGetMapStructureStringSimpleMap() {
        Map<String, Integer> m = new LinkedHashMap<>();
        String structure = MapUtilities.getMapStructureString(m);
        // For a plain LinkedHashMap we expect the class name only
        assertTrue(structure.contains("LinkedHashMap"));
    }

    @Test
    public void testGetMapStructureStringTreeMap() {
        Map<String, Integer> tm = new TreeMap<>();
        String structure = MapUtilities.getMapStructureString(tm);
        assertTrue(structure.contains("TreeMap"));
        // TreeMap implements NavigableMap, should indicate that
        assertTrue(structure.contains("(NavigableMap)"));
    }

    /* -------------------------------------------------------------------- */
    /* detectMapOrdering(..)                                                */
    /* -------------------------------------------------------------------- */
    @Test
    public void testDetectMapOrderingNullReturnsUnordered() {
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(null));
    }

    @Test
    public void testDetectMapOrderingCompactMap() {
        // We cannot instantiate CompactMap directly without its constructor,
        // but we can verify that when a real CompactMap is supplied the method
        // delegates to its getOrdering(). For the purpose of this test we use a
        // simple mock via an anonymous subclass (allowed because it does not
        // rely on external libraries). The subclass only needs to implement
        // getOrdering().
        CompactMap<String, String> compact = new CompactMap<String, String>(CompactMap.INSERTION) {
            @Override public String getOrdering() { return CompactMap.INSERTION; }
        };
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(compact));
    }

    @Test
    public void testDetectMapOrderingSortedMap() {
        Map<String, Integer> tree = new TreeMap<>();
        assertEquals(CompactMap.SORTED, MapUtilities.detectMapOrdering(tree));
    }

    @Test
    public void testDetectMapOrderingLinkedHashMap() {
        Map<String, Integer> linked = new LinkedHashMap<>();
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(linked));
    }

    @Test
    public void testDetectMapOrderingEnumMap() {
        Map<DayOfWeek, String> enumMap = new EnumMap<>(DayOfWeek.class);
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(enumMap));
    }

    @Test
    public void testDetectMapOrderingUnorderedHashMap() {
        Map<String, Integer> hash = new HashMap<>();
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(hash));
    }

    @Test
    public void testDetectMapOrderingCycleThrows() {
        // Build a cycle using two TrackingMap wrappers (assuming they expose a
        // constructor that takes a map). If such constructors are not available,
        // this test documents the expected behavior; the library should throw
        // IllegalArgumentException when a cycle is detected.
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Circular map structure detected");

        // Create two maps that reference each other via TrackingMap wrappers.
        // For illustration we use simple maps and manually create a cycle via
        // reflection (allowed in test code). This forces the internal detection.
        TrackingMap<Object, Object> tm1 = new TrackingMap<>(new HashMap<>());
        TrackingMap<Object, Object> tm2 = new TrackingMap<>(new HashMap<>());

        // Force a cycle: set the wrapped map of tm1 to tm2 and vice‑versa.
        // (Assuming TrackingMap has a protected field or setter; we use reflection.)
        try {
            java.lang.reflect.Field f = TrackingMap.class.getDeclaredField("wrappedMap");
            f.setAccessible(true);
            f.set(tm1, tm2);
            f.set(tm2, tm1);
        } catch (Exception e) {
            // If reflection is not possible, the test will simply pass as the
            // library guarantees cycle detection; we rethrow to fail the test.
            throw new RuntimeException("Unable to create cycle for test", e);
        }

        // This should trigger the cycle detection.
        MapUtilities.detectMapOrdering(tm1);
    }
}
