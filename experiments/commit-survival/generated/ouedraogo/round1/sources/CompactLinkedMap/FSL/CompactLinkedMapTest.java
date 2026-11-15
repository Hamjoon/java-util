package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link CompactLinkedMap}.  The tests focus on
 * the public contract inherited from {@link CompactMap} as well as the
 * specific behaviour of {@link CompactLinkedMap} (e.g. the switch to a
 * {@link LinkedHashMap} delegate after the compact size is exceeded).
 *
 * <p>Only the APIs that appear in the supplied source code or in the
 * listed {@code CompactMap} signature are used – no external library
 * classes, no mocking frameworks and no subclassing of library
 * types.</p>
 */
public class CompactLinkedMapTest {

    /**
     * Helper that uses reflection to obtain the protected {@code compactSize()}
     * value from a {@link CompactMap} instance.  This is required to drive
     * the test that verifies the transition to the {@link LinkedHashMap}
     * delegate.
     */
    private static int getCompactSize(CompactMap<?, ?> map) throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        return (int) m.invoke(map);
    }

    @Test
    public void testDefaultConstructorAndBasicOperations() {
        CompactLinkedMap<String, Integer> map = new CompactLinkedMap<>();

        // initially empty
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        // put / get
        assertNull(map.put("one", 1));
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(1, map.size());

        // containsKey / containsValue
        assertTrue(map.containsKey("one"));
        assertTrue(map.containsValue(1));
        assertFalse(map.containsKey("two"));
        assertFalse(map.containsValue(2));

        // replace existing value
        assertEquals(Integer.valueOf(1), map.put("one", 10));
        assertEquals(Integer.valueOf(10), map.get("one"));
        assertEquals(1, map.size());

        // remove
        assertEquals(Integer.valueOf(10), map.remove("one"));
        assertFalse(map.containsKey("one"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testCopyConstructorCopiesEntries() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "alpha");
        source.put("b", "beta");
        source.put("c", "gamma");

        CompactLinkedMap<String, String> copy = new CompactLinkedMap<>(source);

        // the copy should contain exactly the same mappings
        assertEquals(source.size(), copy.size());
        for (Map.Entry<String, String> e : source.entrySet()) {
            assertTrue(copy.containsKey(e.getKey()));
            assertEquals(e.getValue(), copy.get(e.getKey()));
        }

        // modifications to the original must not affect the copy
        source.put("d", "delta");
        assertFalse(copy.containsKey("d"));
    }

    @Test
    public void testTransitionToLinkedHashMapDelegate() throws Exception {
        CompactLinkedMap<Integer, String> map = new CompactLinkedMap<>();

        // discover the compact size defined by the superclass
        int compactSize = getCompactSize(map);
        assertTrue("compactSize should be non‑negative", compactSize >= 0);

        // Fill the map up to the compact size – still using the compact
        // internal representation.
        for (int i = 0; i < compactSize; i++) {
            map.put(i, "v" + i);
        }
        assertEquals(compactSize, map.size());

        // Insert one more entry – this forces the map to switch to a
        // LinkedHashMap delegate (as documented in the class Javadoc).
        map.put(compactSize, "trigger");
        assertEquals(compactSize + 1, map.size());

        // Verify that iteration order now matches insertion order.
        Iterator<Map.Entry<Integer, String>> it = map.entrySet().iterator();
        for (int i = 0; i <= compactSize; i++) {
            assertTrue("expected entry for key " + i, it.hasNext());
            Map.Entry<Integer, String> e = it.next();
            assertEquals(Integer.valueOf(i), e.getKey());
            assertEquals(i == compactSize ? "trigger" : "v" + i, e.getValue());
        }
        assertFalse(it.hasNext());

        // Ensure that the underlying delegate is indeed a LinkedHashMap.
        // We can do this indirectly by checking that the map preserves
        // insertion order after a removal and re‑insertion of a key.
        map.remove(0);
        map.put(0, "v0");
        Iterator<Integer> keyIt = map.keySet().iterator();
        // key 0 should now be the last element because it was re‑added.
        for (int i = 1; i <= compactSize; i++) {
            assertTrue(keyIt.hasNext());
            assertEquals(Integer.valueOf(i), keyIt.next());
        }
        assertTrue(keyIt.hasNext());
        assertEquals(Integer.valueOf(0), keyIt.next());
        assertFalse(keyIt.hasNext());
    }

    @Test
    public void testMinusAndPlusCreateNewMaps() {
        CompactLinkedMap<String, Integer> original = new CompactLinkedMap<>();
        original.put("x", 1);
        original.put("y", 2);
        original.put("z", 3);

        // plus should return a new map with the extra entry, leaving original untouched
        Map<String, Integer> plusMap = original.plus("w");
        assertEquals(4, plusMap.size());
        assertTrue(plusMap.containsKey("w"));
        assertFalse(original.containsKey("w"));
        assertEquals(Integer.valueOf(1), plusMap.get("x"));
        assertEquals(Integer.valueOf(2), plusMap.get("y"));
        assertEquals(Integer.valueOf(3), plusMap.get("z"));

        // minus should return a new map without the specified key
        Map<String, Integer> minusMap = original.minus("y");
        assertEquals(2, minusMap.size());
        assertFalse(minusMap.containsKey("y"));
        assertTrue(minusMap.containsKey("x"));
        assertTrue(minusMap.containsKey("z"));
        // original must stay unchanged
        assertTrue(original.containsKey("y"));
    }

    @Test
    public void testEqualsAndHashCodeConsistency() {
        CompactLinkedMap<String, String> mapA = new CompactLinkedMap<>();
        CompactLinkedMap<String, String> mapB = new CompactLinkedMap<>();

        mapA.put("k1", "v1");
        mapA.put("k2", "v2");
        mapA.put("k3", "v3");

        // Insert in different order into mapB
        mapB.put("k3", "v3");
        mapB.put("k1", "v1");
        mapB.put("k2", "v2");

        // Maps should be equal irrespective of insertion order
        assertEquals(mapA, mapB);
        assertEquals(mapB, mapA);
        assertEquals(mapA.hashCode(), mapB.hashCode());

        // Changing one map must break equality
        mapB.put("k4", "v4");
        assertNotEquals(mapA, mapB);
    }

    @Test
    public void testClearAndRemoveAllEntries() {
        CompactLinkedMap<Integer, String> map = new CompactLinkedMap<>();
        map.put(10, "ten");
        map.put(20, "twenty");
        map.put(30, "thirty");

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        // After clear we can reuse the map
        map.put(40, "forty");
        assertEquals(1, map.size());
        assertEquals("forty", map.get(40));
    }

    @Test
    public void testKeySetValuesAndEntrySetViews() {
        CompactLinkedMap<String, Integer> map = new CompactLinkedMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        // keySet view
        assertEquals(3, map.keySet().size());
        assertTrue(map.keySet().containsAll(java.util.Arrays.asList("a", "b", "c")));

        // values view
        assertEquals(3, map.values().size());
        assertTrue(map.values().containsAll(java.util.Arrays.asList(1, 2, 3)));

        // entrySet view
        assertEquals(3, map.entrySet().size());
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            assertEquals(Integer.valueOf(e.getKey().charAt(0) - 'a' + 1), e.getValue());
        }

        // Removing via iterator must affect the underlying map
        Iterator<String> keyIter = map.keySet().iterator();
        while (keyIter.hasNext()) {
            String k = keyIter.next();
            if ("b".equals(k)) {
                keyIter.remove();
            }
        }
        assertFalse(map.containsKey("b"));
        assertEquals(2, map.size());
    }

    @Test
    public void testNullKeyAndNullValueHandling() {
        CompactLinkedMap<String, String> map = new CompactLinkedMap<>();

        // Null key
        map.put(null, "nullKey");
        assertTrue(map.containsKey(null));
        assertEquals("nullKey", map.get(null));

        // Null value
        map.put("nullValue", null);
        assertTrue(map.containsKey("nullValue"));
        assertNull(map.get("nullValue"));

        // Both null
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertNull(map.get(null));

        // Verify size accounting
        assertEquals(2, map.size()); // keys: null and "nullValue"
    }

    @Test
    public void testPutAllAndConstructorFromMap() {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        source.put("three", 3);

        CompactLinkedMap<String, Integer> map = new CompactLinkedMap<>();
        map.putAll(source);

        // map must now contain exactly the same entries in the same order
        assertEquals(source.size(), map.size());
        Iterator<Map.Entry<String, Integer>> srcIt = source.entrySet().iterator();
        Iterator<Map.Entry<String, Integer>> mapIt = map.entrySet().iterator();
        while (srcIt.hasNext() && mapIt.hasNext()) {
            Map.Entry<String, Integer> se = srcIt.next();
            Map.Entry<String, Integer> me = mapIt.next();
            assertEquals(se.getKey(), me.getKey());
            assertEquals(se.getValue(), me.getValue());
        }
        assertFalse(srcIt.hasNext());
        assertFalse(mapIt.hasNext());
    }

    @Test
    public void testToStringRepresentation() {
        CompactLinkedMap<String, Integer> map = new CompactLinkedMap<>();
        map.put("x", 100);
        map.put("y", 200);
        String s = map.toString();

        // The string must contain the keys and values; exact format is not
        // mandated but typical Map toString looks like {x=100, y=200}
        assertTrue(s.contains("x=100"));
        assertTrue(s.contains("y=200"));
        assertTrue(s.startsWith("{") && s.endsWith("}"));
    }
}
