package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactMap}.
 *
 * The tests are organized to cover:
 * <ul>
 *   <li>All public API methods (constructors, map operations, builders, etc.)</li>
 *   <li>Typical usage scenarios</li>
 *   <li>Edge‑cases (null keys/values, case‑insensitivity, ordering, compact‑size thresholds)</li>
 *   <li>Exception handling (invalid builder options, deprecated methods, concurrent modification)</li>
 * </ul>
 *
 * No mocking or subclassing of library classes is performed – only the public API
 * defined in the source file is used.
 */
public class CompactMapTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private CompactMap<String, Integer> map;

    @Before
    public void setUp() {
        // default map – case‑sensitive, unordered, compactSize = 70
        map = new CompactMap<>();
    }

    /* -------------------------------------------------
     *  Constructor tests
     * ------------------------------------------------- */

    @Test
    public void testEmptyConstructor() {
        assertTrue("New map should be empty", map.isEmpty());
        assertEquals(0, map.size());
        assertEquals("{}", map.toString());
    }

    @Test
    public void testConstructorWithExistingMap() {
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put("b", 2);
        CompactMap<String, Integer> fromSrc = new CompactMap<>(src);
        assertEquals(2, fromSrc.size());
        assertTrue(fromSrc.containsKey("a"));
        assertTrue(fromSrc.containsKey("b"));
        assertEquals(Integer.valueOf(1), fromSrc.get("a"));
        assertEquals(Integer.valueOf(2), fromSrc.get("b"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullMapThrows() {
        new CompactMap<String, Integer>(null);
    }

    @Test
    public void testCompactSizeValidationInBuilder() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("compactSize must be >= 2");
        CompactMap.builder().compactSize(1).build();
    }

    /* -------------------------------------------------
     *  Basic map operations (size, isEmpty, clear)
     * ------------------------------------------------- */

    @Test
    public void testSizeAndIsEmptyTransitions() {
        assertTrue(map.isEmpty());
        map.put("one", 1);
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());

        map.put("two", 2);
        assertEquals(2, map.size());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testPutAndGetSingleEntry() {
        assertNull(map.put("key", 10));
        assertEquals(Integer.valueOf(10), map.get("key"));
        assertEquals(Integer.valueOf(10), map.put("key", 20)); // replace
        assertEquals(Integer.valueOf(20), map.get("key"));
    }

    @Test
    public void testPutNullKeyAndValue() {
        map.put(null, 99);
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(99), map.get(null));

        map.put("nullValue", null);
        assertTrue(map.containsKey("nullValue"));
        assertNull(map.get("nullValue"));
    }

    @Test
    public void testRemoveExistingAndMissingKey() {
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(Integer.valueOf(1), map.remove("a"));
        assertNull(map.get("a"));
        assertNull(map.remove("nonexistent"));
        assertEquals(1, map.size());
    }

    /* -------------------------------------------------
     *  containsKey / containsValue
     * ------------------------------------------------- */

    @Test
    public void testContainsKeyAndValue() {
        map.put("x", 5);
        map.put("y", null);
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey("y"));
        assertFalse(map.containsKey("z"));

        assertTrue(map.containsValue(5));
        assertTrue(map.containsValue(null));
        assertFalse(map.containsValue(99));
    }

    /* -------------------------------------------------
     *  putAll and bulk operations
     * ------------------------------------------------- */

    @Test
    public void testPutAllMergesCorrectly() {
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put("b", 2);
        map.putAll(src);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
    }

    @Test
    public void testPutAllWithNullOrEmptyMap() {
        map.putAll(null); // should be a no‑op (no NPE)
        assertTrue(map.isEmpty());

        map.putAll(Collections.emptyMap()); // still empty
        assertTrue(map.isEmpty());
    }

    /* -------------------------------------------------
     *  hashCode / equals
     * ------------------------------------------------- */

    @Test
    public void testHashCodeAndEqualsConsistency() {
        CompactMap<String, Integer> m1 = new CompactMap<>();
        CompactMap<String, Integer> m2 = new CompactMap<>();

        m1.put("a", 1);
        m1.put("b", 2);
        m2.put("b", 2);
        m2.put("a", 1);

        assertTrue(m1.equals(m2));
        assertEquals(m1.hashCode(), m2.hashCode());

        m2.put("c", 3);
        assertFalse(m1.equals(m2));
    }

    @Test
    public void testEqualsWithDifferentMapImplementations() {
        Map<String, Integer> hash = new HashMap<>();
        hash.put("k", 10);
        CompactMap<String, Integer> cm = new CompactMap<>(hash);
        assertTrue(cm.equals(hash));
        assertTrue(hash.equals(cm));
    }

    /* -------------------------------------------------
     *  toString
     * ------------------------------------------------- */

    @Test
    public void testToStringFormatting() {
        map.put("one", 1);
        map.put("two", 2);
        String s = map.toString();
        // order is unspecified for unordered map, just verify format
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
    }

    /* -------------------------------------------------
     *  keySet, values, entrySet basics
     * ------------------------------------------------- */

    @Test
    public void testKeySetView() {
        map.put("a", 1);
        map.put("b", 2);
        Set<String> keys = map.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));

        // remove via keySet
        assertTrue(keys.remove("a"));
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());
    }

    @Test
    public void testValuesView() {
        map.put("x", 10);
        map.put("y", 20);
        Collection<Integer> vals = map.values();
        assertEquals(2, vals.size());
        assertTrue(vals.contains(10));
        assertTrue(vals.contains(20));

        // remove via values collection
        assertTrue(vals.remove(10));
        assertFalse(map.containsKey("x"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetView() {
        map.put("k1", 100);
        map.put("k2", 200);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        assertEquals(2, entries.size());

        // verify contains works with a freshly created entry
        Map.Entry<String, Integer> e = new AbstractMap.SimpleEntry<>("k1", 100);
        assertTrue(entries.contains(e));

        // remove via entrySet
        assertTrue(entries.remove(e));
        assertFalse(map.containsKey("k1"));
        assertEquals(1, map.size());
    }

    /* -------------------------------------------------
     *  Builder options – ordering, case sensitivity,
     *  compact size threshold, single‑value key
     * ------------------------------------------------- */

    @Test
    public void testBuilderSortedOrder() {
        CompactMap<String, Integer> sorted = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .compactSize(3) // small threshold to force map transition quickly
                .build();

        sorted.put("c", 3);
        sorted.put("a", 1);
        sorted.put("b", 2);

        Iterator<String> it = sorted.keySet().iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
    }

    @Test
    public void testBuilderInsertionOrder() {
        CompactMap<String, Integer> insertion = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .compactSize(2)
                .build();

        insertion.put("first", 1);
        insertion.put("second", 2);
        insertion.put("third", 3);

        Iterator<String> it = insertion.keySet().iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
    }

    @Test
    public void testBuilderCaseInsensitive() {
        CompactMap<String, Integer> ci = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .compactSize(5)
                .build();

        ci.put("Key", 10);
        assertTrue(ci.containsKey("key"));
        assertEquals(Integer.valueOf(10), ci.get("KEY"));
    }

    @Test
    public void testSingleValueKeyOptimization() {
        // default single value key is "id"
        CompactMap<String, String> cm = new CompactMap<>();
        cm.put("id", "single");
        // The value should be stored directly (OBJECT state) – we verify behaviour via API
        assertEquals("single", cm.get("id"));
        assertEquals(1, cm.size());

        // Adding a different key forces transition to array storage
        cm.put("other", "value");
        assertEquals(2, cm.size());
        assertEquals("single", cm.get("id"));
        assertEquals("value", cm.get("other"));
    }

    @Test
    public void testCompactSizeTransition() {
        // Set compactSize low so we cross the threshold fast
        CompactMap<String, Integer> small = CompactMap.<String, Integer>builder()
                .compactSize(3)
                .build();

        small.put("a", 1);
        small.put("b", 2);
        small.put("c", 3);
        assertEquals(3, small.size());

        // Adding the fourth entry should switch to backing map (> compactSize)
        small.put("d", 4);
        assertEquals(4, small.size());
        assertTrue(small.containsKey("d"));
        assertEquals(Integer.valueOf(4), small.get("d"));
    }

    /* -------------------------------------------------
     *  Iterator behavior – remove & concurrent modification
     * ------------------------------------------------- */

    @Test
    public void testKeyIteratorRemove() {
        map.put("k1", 1);
        map.put("k2", 2);
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        String key = it.next();
        it.remove(); // remove the entry we just visited
        assertFalse(map.containsKey(key));
        assertEquals(1, map.size());
    }

    @Test
    public void testValueIteratorRemove() {
        map.put("a", 10);
        map.put("b", 20);
        Iterator<Integer> it = map.values().iterator();
        while (it.hasNext()) {
            Integer v = it.next();
            if (v.equals(10)) {
                it.remove();
            }
        }
        assertFalse(map.containsValue(10));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntryIteratorRemove() {
        map.put("x", 100);
        map.put("y", 200);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if (e.getKey().equals("x")) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("x"));
        assertEquals(1, map.size());
    }

    @Test
    public void testConcurrentModificationDetection() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        map.put("c", 3); // structural change after iterator creation

        thrown.expect(ConcurrentModificationException.class);
        it.hasNext(); // should throw CME
    }

    /* -------------------------------------------------
     *  Deprecated plus/minus methods – should throw
     * ------------------------------------------------- */

    @Test
    public void testMinusThrowsUnsupportedOperationException() {
        thrown.expect(UnsupportedOperationException.class);
        map.minus("any");
    }

    @Test
    public void testPlusThrowsUnsupportedOperationException() {
        thrown.expect(UnsupportedOperationException.class);
        map.plus("any");
    }

    /* -------------------------------------------------
     *  Builder sourceMap initialization and option
     *  compatibility validation
     * ------------------------------------------------- */

    @Test
    public void testBuilderWithSourceMap() {
        Map<String, Integer> src = new LinkedHashMap<>();
        src.put("first", 1);
        src.put("second", 2);
        CompactMap<String, Integer> cm = CompactMap.<String, Integer>builder()
                .sourceMap(src)
                .insertionOrder()
                .build();

        // Insertion order must be preserved
        Iterator<String> it = cm.keySet().iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
    }

    @Test
    public void testBuilderInvalidOrderingWithSourceMap() {
        Map<String, Integer> src = new TreeMap<>();
        src.put("a", 1);
        src.put("b", 2);
        thrown.expect(IllegalArgumentException.class);
        CompactMap.<String, Integer>builder()
                .sourceMap(src)
                .unordered() // invalid: source map is sorted
                .build();
    }

    /* -------------------------------------------------
     *  Helper method for unordered builder (noOrder)
     * ------------------------------------------------- */

    private CompactMap.Builder<String, Integer> unordered() {
        return CompactMap.builder().noOrder();
    }
}
