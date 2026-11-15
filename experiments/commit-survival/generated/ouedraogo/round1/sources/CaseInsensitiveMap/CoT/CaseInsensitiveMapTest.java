package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import java.util.*;
import static org.junit.Assert.*;

public class CaseInsensitiveMapTest {

    private CaseInsensitiveMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CaseInsensitiveMap<>();
    }

    /* ---------- basic functionality ---------- */

    @Test
    public void testPutAndGetCaseInsensitive() {
        map.put("Hello", 1);
        assertEquals(Integer.valueOf(1), map.get("hello"));
        assertEquals(Integer.valueOf(1), map.get("HeLLo"));
    }

    @Test
    public void testPutOverwriteDifferentCase() {
        map.put("Key", 10);
        assertEquals(Integer.valueOf(10), map.get("key"));
        // put with different case should replace the previous entry
        map.put("kEy", 20);
        assertEquals(Integer.valueOf(20), map.get("KEY"));
        assertEquals(1, map.size());
        // original case stored should be the last inserted one
        Set<String> keys = map.keySet();
        assertTrue(keys.contains("kEy"));
        assertFalse(keys.contains("Key"));
    }

    @Test
    public void testContainsKey() {
        map.put("Test", 5);
        assertTrue(map.containsKey("test"));
        assertTrue(map.containsKey("TEST"));
        map.put("Number", 7);
        assertTrue(map.containsKey("Number"));
        assertFalse(map.containsKey("Missing"));
    }

    @Test
    public void testRemove() {
        map.put("RemoveMe", 99);
        assertEquals(Integer.valueOf(99), map.remove("removeme"));
        assertFalse(map.containsKey("REMOVEme"));
        assertNull(map.get("REMOVEME"));
    }

    @Test
    public void testNullKey() {
        map.put(null, 42);
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(42), map.get(null));
        assertEquals(Integer.valueOf(42), map.remove(null));
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testNonStringKey() {
        CaseInsensitiveMap<Object, String> mixedMap = new CaseInsensitiveMap<>();
        Integer intKey = 123;
        mixedMap.put(intKey, "int");
        assertEquals("int", mixedMap.get(intKey));
        assertTrue(mixedMap.containsKey(intKey));
        assertFalse(mixedMap.containsKey("123"));
    }

    /* ---------- keySet view ---------- */

    @Test
    public void testKeySetContainsAndRemove() {
        map.put("Alpha", 1);
        map.put("Beta", 2);
        Set<String> ks = map.keySet();

        // contains is case‑insensitive
        assertTrue(ks.contains("alpha"));
        assertTrue(ks.contains("ALPHA"));
        assertTrue(ks.contains("beta"));

        // remove via keySet (case‑insensitive)
        assertTrue(ks.remove("ALPHA"));
        assertFalse(map.containsKey("alpha"));
        assertEquals(1, map.size());

        // removing a non‑present key returns false
        assertFalse(ks.remove("gamma"));
    }

    @Test
    public void testKeySetRetainAll() {
        map.put("One", 1);
        map.put("Two", 2);
        map.put("Three", 3);
        Set<String> ks = map.keySet();

        // retain only "two" (different case) and a key that does not exist
        List<String> toRetain = Arrays.asList("tWO", "four");
        assertTrue(ks.retainAll(toRetain));
        assertEquals(1, ks.size());
        assertTrue(ks.contains("Two"));
        assertFalse(ks.contains("One"));
    }

    @Test
    public void testKeySetToArray() {
        map.put("First", 1);
        map.put("Second", 2);
        Object[] arr = map.keySet().toArray();
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.contains("First"));
        assertTrue(list.contains("Second"));
    }

    /* ---------- entrySet view ---------- */

    @Test
    public void testEntrySetContainsAndRemove() {
        map.put("X", 10);
        map.put("Y", 20);
        Set<Map.Entry<String, Integer>> es = map.entrySet();

        Map.Entry<String, Integer> entryX = new AbstractMap.SimpleEntry<>("x", 10);
        assertTrue(es.contains(entryX));

        // remove using entry with different case
        assertTrue(es.remove(entryX));
        assertFalse(map.containsKey("X"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetRemoveAll() {
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        Set<Map.Entry<String, Integer>> es = map.entrySet();

        List<Map.Entry<String, Integer>> toRemove = new ArrayList<>();
        toRemove.add(new AbstractMap.SimpleEntry<>("a", 1));
        toRemove.add(new AbstractMap.SimpleEntry<>("b", 2));

        assertTrue(es.removeAll(toRemove));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("C"));
    }

    @Test
    public void testPutAllWithCaseInsensitiveEntry() {
        // Build a source map that contains a CaseInsensitiveEntry
        CaseInsensitiveMap<String, Integer> source = new CaseInsensitiveMap<>();
        source.put("mixed", 100);

        // Manually create a CaseInsensitiveEntry wrapping the underlying entry
        Map.Entry<String, Integer> rawEntry = source.getWrappedMap().entrySet().iterator().next();
        CaseInsensitiveMap<String, Integer>.CaseInsensitiveEntry ciEntry =
                source.new CaseInsensitiveEntry(rawEntry);

        // Build a map that contains that entry
        Map<String, Integer> wrapper = new HashMap<>();
        wrapper.put(ciEntry.getKey(), ciEntry.getValue()); // key is original string

        // Destination map
        CaseInsensitiveMap<String, Integer> dest = new CaseInsensitiveMap<>();
        dest.putAll(wrapper); // normal entries
        // now also putAll a map that contains the ciEntry directly
        Map<Object, Object> weird = new HashMap<>();
        weird.put(ciEntry, ciEntry.getValue());
        // the generic signature forces a cast, but we can use raw types
        @SuppressWarnings("unchecked")
        Map<? extends String, ? extends Integer> rawMap = (Map<? extends String, ? extends Integer>) weird;
        dest.putAll(rawMap); // should handle the CaseInsensitiveEntry inside

        assertEquals(Integer.valueOf(100), dest.get("MIXED"));
        assertEquals(1, dest.size()); // only one logical entry
    }

    /* ---------- putObject ---------- */

    @Test
    public void testPutObject() {
        // String key – should be wrapped
        Object old = map.putObject("StrKey", 5);
        assertNull(old);
        assertEquals(Integer.valueOf(5), map.get("strkey"));

        // Non‑String key – passes through unchanged
        Object old2 = map.putObject(777, 9);
        assertNull(old2);
        assertEquals(Integer.valueOf(9), map.get(777));
    }

    /* ---------- equals & hashCode ---------- */

    @Test
    public void testEqualsAndHashCode() {
        CaseInsensitiveMap<String, Integer> a = new CaseInsensitiveMap<>();
        CaseInsensitiveMap<String, Integer> b = new CaseInsensitiveMap<>();

        a.put("Foo", 1);
        a.put("Bar", 2);

        b.put("foo", 1);
        b.put("BAR", 2);

        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());

        // Different value should make them unequal
        b.put("Foo", 9);
        assertFalse(a.equals(b));
    }

    /* ---------- deprecated operations ---------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testMinusUnsupported() {
        map.minus("anything");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPlusUnsupported() {
        map.plus("anything");
    }

    /* ---------- toString, values, clear, isEmpty ---------- */

    @Test
    public void testToStringAndValues() {
        map.put("A", 1);
        map.put("B", 2);
        String s = map.toString();
        assertTrue(s.contains("A=1"));
        assertTrue(s.contains("B=2"));

        Collection<Integer> vals = map.values();
        assertTrue(vals.containsAll(Arrays.asList(1, 2)));

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /* ---------- constructors with capacity ---------- */

    @Test
    public void testConstructorsWithCapacity() {
        CaseInsensitiveMap<String, String> c1 = new CaseInsensitiveMap<>(10);
        CaseInsensitiveMap<String, String> c2 = new CaseInsensitiveMap<>(10, 0.75f);
        assertNotNull(c1);
        assertNotNull(c2);
    }

    @Test
    public void testWrapExistingMapConstructor() {
        Map<String, Integer> src = new HashMap<>();
        src.put("X", 5);
        src.put("Y", 6);
        CaseInsensitiveMap<String, Integer> wrapped = new CaseInsensitiveMap<>(src);
        assertEquals(Integer.valueOf(5), wrapped.get("x"));
        assertEquals(Integer.valueOf(6), wrapped.get("Y"));
    }
}
