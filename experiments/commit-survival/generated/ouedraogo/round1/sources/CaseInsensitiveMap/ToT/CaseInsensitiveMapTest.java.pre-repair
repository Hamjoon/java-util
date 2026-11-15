package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class CaseInsensitiveMapTest {

    private CaseInsensitiveMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CaseInsensitiveMap<>();
    }

    /* ---------- Constructor Tests ---------- */

    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>(10);
        assertTrue(m.isEmpty());
        assertEquals(0, m.size());
    }

    @Test
    public void testConstructorWithInitialCapacityAndLoadFactor() {
        CaseInsensitiveMap<String, String> m = new CaseInsensitiveMap<>(5, 0.75f);
        assertTrue(m.isEmpty());
    }

    @Test
    public void testCopyConstructorWithSourceAndDestinationMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("One", 1);
        source.put("Two", 2);
        Map<String, Integer> dest = new LinkedHashMap<>();
        CaseInsensitiveMap<String, Integer> copy = new CaseInsensitiveMap<>(source, dest);
        assertEquals(2, copy.size());
        assertTrue(copy.containsKey("one"));
        assertTrue(copy.containsKey("ONE"));
        assertEquals(Integer.valueOf(1), copy.get("ONE"));
    }

    @Test
    public void testConstructorWrappingVariousMapTypes() {
        // TreeMap
        TreeMap<String, Integer> tree = new TreeMap<>();
        tree.put("Alpha", 1);
        CaseInsensitiveMap<String, Integer> wrappedTree = new CaseInsensitiveMap<>(tree);
        assertTrue(wrappedTree.containsKey("ALPHA"));
        assertEquals(Integer.valueOf(1), wrappedTree.get("alpha"));

        // ConcurrentHashMap
        ConcurrentHashMap<String, Integer> chm = new ConcurrentHashMap<>();
        chm.put("Beta", 2);
        CaseInsensitiveMap<String, Integer> wrappedChm = new CaseInsensitiveMap<>(chm);
        assertTrue(wrappedChm.containsKey("beta"));
        assertEquals(Integer.valueOf(2), wrappedChm.get("BETA"));
    }

    /* ---------- Basic Map Operations ---------- */

    @Test
    public void testPutAndGetWithStringKeyCaseInsensitive() {
        map.put("Key", 10);
        assertEquals(Integer.valueOf(10), map.get("key"));
        assertEquals(Integer.valueOf(10), map.get("KEY"));
    }

    @Test
    public void testPutAndGetWithNonStringKey() {
        CaseInsensitiveMap<Object, String> objMap = new CaseInsensitiveMap<>();
        Integer intKey = 42;
        objMap.put(intKey, "answer");
        assertEquals("answer", objMap.get(intKey));
        // non‑String key should not be case‑insensitive
        assertNull(objMap.get("42"));
    }

    @Test
    public void testPutObjectMethod() {
        map.putObject("Foo", 1);
        assertEquals(Integer.valueOf(1), map.get("foo"));
        map.putObject(123, "bar");
        assertEquals("bar", map.get(123));
    }

    @Test
    public void testPutAllCopiesEntriesRespectingCaseInsensitivity() {
        Map<String, Integer> source = new HashMap<>();
        source.put("A", 1);
        source.put("b", 2);
        map.putAll(source);
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("B"));
        assertEquals(Integer.valueOf(1), map.get("A"));
        assertEquals(Integer.valueOf(2), map.get("B"));
    }

    @Test
    public void testRemoveWithStringKeyCaseInsensitive() {
        map.put("RemoveMe", 99);
        Integer removed = map.remove("removeme");
        assertEquals(Integer.valueOf(99), removed);
        assertFalse(map.containsKey("REMOVEme"));
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        map.put("Exists", 5);
        assertTrue(map.containsKey("exists"));
        assertTrue(map.containsValue(5));
        assertFalse(map.containsKey("missing"));
        assertFalse(map.containsValue(0));
    }

    @Test
    public void testSizeIsEmptyClear() {
        assertTrue(map.isEmpty());
        map.put("One", 1);
        map.put("Two", 2);
        assertEquals(2, map.size());
        assertFalse(map.isEmpty());
        map.clear();
        assertTrue(map.isEmpty());
    }

    /* ---------- Equality, Hashing, and String Representation ---------- */

    @Test
    public void testEqualsAndHashCode() {
        CaseInsensitiveMap<String, Integer> other = new CaseInsensitiveMap<>();
        map.put("Key", 10);
        other.put("key", 10);
        assertTrue(map.equals(other));
        assertEquals(map.hashCode(), other.hashCode());

        other.put("another", 5);
        assertFalse(map.equals(other));
    }

    @Test
    public void testToStringDelegatesToWrappedMap() {
        map.put("A", 1);
        map.put("B", 2);
        String s = map.toString();
        // The exact order is not guaranteed for LinkedHashMap, but both entries must appear
        assertTrue(s.contains("A=1"));
        assertTrue(s.contains("B=2"));
    }

    /* ---------- Deprecated Operations ---------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testMinusThrowsUnsupportedOperationException() {
        map.minus("anything");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPlusThrowsUnsupportedOperationException() {
        map.plus("anything");
    }

    /* ---------- keySet Tests ---------- */

    @Test
    public void testKeySetContainsAndRemove() {
        map.put("Alpha", 1);
        map.put("Beta", 2);
        Set<String> keys = map.keySet();
        assertTrue(keys.contains("ALPHA"));
        assertTrue(keys.contains("beta"));
        assertTrue(keys.remove("ALPHA"));
        assertFalse(map.containsKey("alpha"));
        assertEquals(1, keys.size());
    }

    @Test
    public void testKeySetRemoveAll() {
        map.put("One", 1);
        map.put("Two", 2);
        map.put("Three", 3);
        Set<String> keys = map.keySet();
        keys.removeAll(Arrays.asList("ONE", "THREE"));
        assertFalse(map.containsKey("one"));
        assertFalse(map.containsKey("three"));
        assertTrue(map.containsKey("two"));
        assertEquals(1, keys.size());
    }

    @Test
    public void testKeySetRetainAll() {
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        Set<String> keys = map.keySet();
        keys.retainAll(Arrays.asList("b", "c"));
        assertFalse(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsKey("c"));
        assertEquals(2, keys.size());
    }

    @Test
    public void testKeySetToArray() {
        map.put("X", 9);
        map.put("Y", 8);
        Object[] arr = map.keySet().toArray();
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.contains("X"));
        assertTrue(list.contains("Y"));
    }

    @Test
    public void testKeySetIteratorRemove() {
        map.put("First", 1);
        map.put("Second", 2);
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String k = it.next();
            if ("first".equalsIgnoreCase(k)) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("first"));
        assertEquals(1, map.size());
    }

    /* ---------- entrySet Tests ---------- */

    @Test
    public void testEntrySetContainsAndRemove() {
        map.put("KeyOne", 100);
        map.put("KeyTwo", 200);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        Map.Entry<String, Integer> lookup = new AbstractMap.SimpleEntry<>("keyone", 100);
        assertTrue(entries.contains(lookup));

        assertTrue(entries.remove(lookup));
        assertFalse(map.containsKey("keyone"));
        assertEquals(1, entries.size());
    }

    @Test
    public void testEntrySetRemoveAll() {
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        List<Map.Entry<String, Integer>> toRemove = new ArrayList<>();
        toRemove.add(new AbstractMap.SimpleEntry<>("a", 1));
        toRemove.add(new AbstractMap.SimpleEntry<>("c", 3));

        assertTrue(entries.removeAll(toRemove));
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsKey("c"));
        assertTrue(map.containsKey("b"));
    }

    @Test
    public void testEntrySetRetainAll() {
        map.put("X", 10);
        map.put("Y", 20);
        map.put("Z", 30);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        List<Map.Entry<String, Integer>> toRetain = new ArrayList<>();
        toRetain.add(new AbstractMap.SimpleEntry<>("y", 20));
        toRetain.add(new AbstractMap.SimpleEntry<>("z", 999)); // mismatched value should be removed

        assertTrue(entries.retainAll(toRetain));
        assertFalse(map.containsKey("x"));
        assertTrue(map.containsKey("y"));
        assertFalse(map.containsKey("z"));
        assertEquals(Integer.valueOf(20), map.get("Y"));
    }

    @Test
    public void testEntrySetIteratorReturnsCaseInsensitiveEntry() {
        map.put("Key", 5);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        assertTrue(it.hasNext());
        Map.Entry<String, Integer> e = it.next();
        assertTrue(e instanceof CaseInsensitiveMap.CaseInsensitiveEntry);
        // key should be the original String, not CaseInsensitiveString
        assertEquals("Key", e.getKey());
        // setValue should modify underlying map
        e.setValue(99);
        assertEquals(Integer.valueOf(99), map.get("key"));
    }

    /* ---------- CaseInsensitiveString Tests ---------- */

    @Test
    public void testCaseInsensitiveStringEquality() {
        CaseInsensitiveMap.CaseInsensitiveString cis1 = new CaseInsensitiveMap.CaseInsensitiveString("TeSt");
        CaseInsensitiveMap.CaseInsensitiveString cis2 = new CaseInsensitiveMap.CaseInsensitiveString("test");
        assertEquals(cis1, cis2);
        assertEquals(cis1.hashCode(), cis2.hashCode());
        assertTrue(cis1.equals("TEST"));
        assertTrue(cis1.equals("test"));
    }

    @Test
    public void testCaseInsensitiveStringCompareTo() {
        CaseInsensitiveMap.CaseInsensitiveString cis = new CaseInsensitiveMap.CaseInsensitiveString("abc");
        assertEquals(0, cis.compareTo("ABC"));
        assertTrue(cis.compareTo("abd") < 0);
        assertTrue(cis.compareTo("abb") > 0);
        // non‑String compared to String should be less than (as per implementation)
        assertEquals(-1, cis.compareTo(new Object()));
    }

    @Test
    public void testCaseInsensitiveEntryGetOriginalKey() {
        map.put("Original", 123);
        // Retrieve entry via entrySet iterator
        CaseInsensitiveMap.CaseInsensitiveEntry entry = (CaseInsensitiveMap.CaseInsensitiveEntry) map.entrySet().iterator().next();
        // getKey returns original String
        assertEquals("Original", entry.getKey());
        // getOriginalKey returns the internal CaseInsensitiveString
        Object internalKey = entry.getOriginalKey();
        assertTrue(internalKey instanceof CaseInsensitiveMap.CaseInsensitiveString);
        assertEquals("Original", internalKey.toString());
    }

    /* ---------- getWrappedMap ---------- */

    @Test
    public void testGetWrappedMapReflectsChanges() {
        Map<String, Integer> wrapped = map.getWrappedMap();
        assertTrue(wrapped.isEmpty());
        map.put("A", 1);
        assertTrue(wrapped.containsKey(new CaseInsensitiveMap.CaseInsensitiveString("a")));
        assertEquals(Integer.valueOf(1), wrapped.get(new CaseInsensitiveMap.CaseInsensitiveString("A")));
    }

    /* ---------- Null handling ---------- */

    @Test
    public void testNullKeyAndValue() {
        // Null key is allowed (as per underlying map behavior)
        map.put(null, 42);
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(42), map.get(null));

        // Null value
        map.put("NullVal", null);
        assertTrue(map.containsKey("nullval"));
        assertNull(map.get("NULLVAL"));
    }

    /* ---------- Complex Scenario: Mixed Key Types ---------- */

    @Test
    public void testMixedKeyTypesDoNotInterfere() {
        map.put("StringKey", 1);
        map.put(123, 2); // Integer key
        assertEquals(Integer.valueOf(1), map.get("stringkey"));
        assertEquals(Integer.valueOf(2), map.get(123));
        // Ensure integer key is not affected by string case‑insensitivity
        assertFalse(map.containsKey("123"));
    }

    /* ---------- Ensure keySet iterator returns original Strings ---------- */

    @Test
    public void testKeySetIteratorReturnsOriginalStringKeys() {
        map.put("MixedCase", 10);
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        String key = it.next();
        assertEquals("MixedCase", key);
    }
}
