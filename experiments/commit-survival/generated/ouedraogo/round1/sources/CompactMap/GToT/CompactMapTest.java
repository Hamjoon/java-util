package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactMap}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>All public API methods (constructors, size, isEmpty, containsKey/Value, get, put, remove,
 *       putAll, clear, hashCode, equals, toString, keySet, values, entrySet, minus, plus).</li>
 *   <li>State transitions: EMPTY → SINGLE → ARRAY → MAP.</li>
 *   <li>Edge cases: null keys/values, duplicate keys, removal that forces a shrink,
 *       iterator fail‑fast behaviour, and exception throwing methods.</li>
 *   <li>Internal helpers via a protected‑access subclass (exposing {@code getLogicalValueType()}
 *       and {@code getSingleValueKey()}).</li>
 * </ul>
 */
public class CompactMapTest {

    /** Small compact size makes it easy to hit the ARRAY → MAP transition. */
    private static class SmallCompactMap<K, V> extends CompactMap<K, V> {
        private final int sizeLimit;

        SmallCompactMap(int sizeLimit) {
            this.sizeLimit = sizeLimit;
        }

        @Override
        protected int compactSize() {
            return sizeLimit;
        }

        /** expose protected helpers for testing */
        LogicalValueType logicalValueType() {
            return super.getLogicalValueType();
        }

        K singleKey() {
            return super.getLogicalSingleKey();
        }

        V singleValue() {
            return super.getLogicalSingleValue();
        }
    }

    private SmallCompactMap<String, String> map;

    @Before
    public void setUp() {
        // compactSize = 3 → ARRAY when size 2‑3, MAP when size ≥4
        map = new SmallCompactMap<>(3);
    }

    /* --------------------------  Constructors  -------------------------- */

    @Test
    public void testEmptyConstructor() {
        assertTrue("New map must be empty", map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testCopyConstructor() {
        map.put("a", "1");
        map.put("b", "2");
        SmallCompactMap<String, String> copy = new SmallCompactMap<>(3);
        copy.putAll(map);
        assertEquals(2, copy.size());
        assertEquals("1", copy.get("a"));
        assertEquals("2", copy.get("b"));
    }

    /* --------------------------  size / isEmpty  -------------------------- */

    @Test
    public void testSizeAndIsEmptyTransitions() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("k1", "v1");                // SINGLE
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.logicalValueType());

        map.put("k2", "v2");                // ARRAY (size 2)
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.logicalValueType());

        map.put("k3", "v3");                // ARRAY (size 3, still array)
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.logicalValueType());

        map.put("k4", "v4");                // transition to MAP (size 4)
        assertEquals(4, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.logicalValueType());
    }

    /* --------------------------  containsKey / containsValue  -------------------------- */

    @Test
    public void testContainsKeyAndValue() {
        map.put("x", "X");
        map.put("y", "Y");
        map.put(null, "nullKey");
        map.put("nullVal", null);

        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey(null));
        assertFalse(map.containsKey("missing"));

        assertTrue(map.containsValue("Y"));
        assertTrue(map.containsValue(null));
        assertFalse(map.containsValue("absent"));
    }

    /* --------------------------  get  -------------------------- */

    @Test
    public void testGet() {
        map.put("k", "v");
        assertEquals("v", map.get("k"));
        assertNull(map.get("missing"));
        map.put(null, "nullKey");
        assertEquals("nullKey", map.get(null));
    }

    /* --------------------------  put  -------------------------- */

    @Test
    public void testPutOverwriteAndReturnValues() {
        // first insert – returns null
        assertNull(map.put("a", "1"));
        // overwrite – returns previous value
        assertEquals("1", map.put("a", "2"));
        assertEquals("2", map.get("a"));
    }

    @Test
    public void testPutArrayExpansionAndMapSwitch() {
        // Fill until just before MAP transition (compactSize = 3)
        map.put("k1", "v1"); // SINGLE
        map.put("k2", "v2"); // ARRAY size 2
        map.put("k3", "v3"); // ARRAY size 3
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.logicalValueType());

        // Adding fourth entry forces switch to MAP
        map.put("k4", "v4");
        assertEquals(CompactMap.LogicalValueType.MAP, map.logicalValueType());
        assertEquals("v4", map.get("k4"));
    }

    /* --------------------------  remove  -------------------------- */

    @Test
    public void testRemoveSingleEntry() {
        map.put("only", "value");
        assertEquals("value", map.remove("only"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testRemoveFromArrayAndShrink() {
        map.put("a", "1");
        map.put("b", "2"); // now ARRAY size 2
        assertEquals("1", map.remove("a"));
        // After removal of one of two entries we should be back to SINGLE
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.logicalValueType());
        assertEquals("2", map.get("b"));
    }

    @Test
    public void testRemoveFromMapAndDownsizeToArray() {
        // Build MAP (size 4)
        map.put("1", "1");
        map.put("2", "2");
        map.put("3", "3");
        map.put("4", "4"); // now MAP
        assertEquals(CompactMap.LogicalValueType.MAP, map.logicalValueType());

        // Remove one entry → size becomes 3 → should switch back to ARRAY
        map.remove("4");
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.logicalValueType());
    }

    @Test
    public void testRemoveNonExistingKeyReturnsNull() {
        map.put("a", "1");
        assertNull(map.remove("missing"));
    }

    /* --------------------------  putAll  -------------------------- */

    @Test
    public void testPutAllEmptyMap() {
        Map<String, String> source = new HashMap<>();
        source.put("x", "X");
        source.put("y", "Y");
        map.putAll(source);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey("y"));
    }

    @Test
    public void testPutAllTriggersMapSwitch() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("a", "1");
        source.put("b", "2");
        source.put("c", "3");
        source.put("d", "4"); // 4 > compactSize (3) → should create MAP directly
        map.putAll(source);
        assertEquals(CompactMap.LogicalValueType.MAP, map.logicalValueType());
        assertEquals("4", map.get("d"));
    }

    @Test
    public void testPutAllWithNullArgumentDoesNothing() {
        map.put("k", "v");
        map.putAll(null);
        assertEquals(1, map.size());
        assertEquals("v", map.get("k"));
    }

    /* --------------------------  clear  -------------------------- */

    @Test
    public void testClearResetsState() {
        map.put("a", "1");
        map.put("b", "2");
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.logicalValueType());
    }

    /* --------------------------  hashCode & equals  -------------------------- */

    @Test
    public void testHashCodeConsistency() {
        map.put("x", "1");
        map.put("y", "2");
        int h1 = map.hashCode();

        // Create a standard HashMap with same entries and compare hash codes
        Map<String, String> other = new HashMap<>();
        other.put("x", "1");
        other.put("y", "2");
        assertEquals(other.hashCode(), h1);
    }

    @Test
    public void testEqualsVariousStates() {
        // Empty maps
        SmallCompactMap<String, String> otherEmpty = new SmallCompactMap<>(3);
        assertTrue(map.equals(otherEmpty));

        // Single entry
        map.put("k", "v");
        otherEmpty.put("k", "v");
        assertTrue(map.equals(otherEmpty));

        // Array state
        map.put("a", "1");
        map.put("b", "2");
        otherEmpty.put("a", "1");
        otherEmpty.put("b", "2");
        assertTrue(map.equals(otherEmpty));

        // Map state
        map.put("c", "3"); // forces MAP
        otherEmpty.put("c", "3");
        assertTrue(map.equals(otherEmpty));

        // Different size
        otherEmpty.remove("c");
        assertFalse(map.equals(otherEmpty));
    }

    @Test
    public void testEqualsAgainstStandardMap() {
        map.put("one", "1");
        map.put("two", "2");
        Map<String, String> hash = new HashMap<>();
        hash.put("one", "1");
        hash.put("two", "2");
        assertTrue(map.equals(hash));
        assertTrue(hash.equals(map)); // HashMap's equals uses Map contract
    }

    @Test
    public void testToStringFormatting() {
        assertEquals("{}", map.toString());
        map.put("a", "1");
        assertEquals("{a=1}", map.toString());
        map.put("b", "2");
        // Order is not guaranteed for MAP state; for ARRAY state order is insertion order
        String s = map.toString();
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("a=1"));
        assertTrue(s.contains("b=2"));
    }

    /* --------------------------  keySet, values, entrySet  -------------------------- */

    @Test
    public void testKeySetOperations() {
        map.put("k1", "v1");
        map.put("k2", "v2");
        Set<String> ks = map.keySet();

        assertTrue(ks.contains("k1"));
        assertEquals(2, ks.size());

        ks.remove("k1");
        assertFalse(map.containsKey("k1"));
        assertEquals(1, map.size());

        ks.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testValuesCollection() {
        map.put("a", "1");
        map.put("b", "2");
        Collection<String> vals = map.values();

        assertTrue(vals.contains("1"));
        assertTrue(vals.contains("2"));
        assertEquals(2, vals.size());

        // Remove via iterator
        Iterator<String> it = vals.iterator();
        while (it.hasNext()) {
            if ("1".equals(it.next())) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetOperationsAndIteratorFailFast() {
        map.put("x", "X");
        map.put("y", "Y");
        Set<Map.Entry<String, String>> entries = map.entrySet();

        assertEquals(2, entries.size());

        // remove via entrySet iterator
        Iterator<Map.Entry<String, String>> it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if (e.getKey().equals("x")) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("x"));
        assertEquals(1, map.size());

        // fail‑fast test
        Iterator<Map.Entry<String, String>> failIt = entries.iterator();
        map.put("z", "Z"); // structural change
        try {
            failIt.next();
            fail("Expected ConcurrentModificationException");
        } catch (ConcurrentModificationException expected) {
            // ok
        }
    }

    /* --------------------------  minus / plus (unsupported)  -------------------------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testMinusUnsupported() {
        map.minus("any");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPlusUnsupported() {
        map.plus("any");
    }

    /* --------------------------  CompactMapEntry behaviour  -------------------------- */

    @Test
    public void testCompactMapEntryEqualsAndHashCode() {
        CompactMap<String, String>.CompactMapEntry e1 = map.new CompactMapEntry("k", "v");
        CompactMap<String, String>.CompactMapEntry e2 = map.new CompactMapEntry("k", "v");
        CompactMap<String, String>.CompactMapEntry e3 = map.new CompactMapEntry("k", "different");

        assertEquals(e1, e2);
        assertNotEquals(e1, e3);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    public void testCompactMapEntrySetValueWritesThrough() {
        map.put("key", "old");
        CompactMap<String, String>.CompactMapEntry entry = map.new CompactMapEntry("key", "old");
        entry.setValue("new");
        // The setValue should have updated the outer map
        assertEquals("new", map.get("key"));
    }

    /* --------------------------  Iterator edge cases  -------------------------- */

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveWithoutNext() {
        Iterator<String> it = map.keySet().iterator();
        it.remove(); // illegal because next() not called
    }

    @Test(expected = NoSuchElementException.class)
    public void testIteratorNextBeyondEnd() {
        map.put("a", "1");
        Iterator<String> it = map.keySet().iterator();
        it.next(); // first element
        it.next(); // should throw NoSuchElementException
    }

    @Test
    public void testCopyIteratorsWhenUnderlyingMapIsSorted() {
        // Force useCopyIterator() by returning a SortedMap from getNewMap()
        SmallCompactMap<String, String> sortedMap = new SmallCompactMap<>(3) {
            @Override
            protected Map<String, String> getNewMap() {
                return new TreeMap<>();
            }
        };
        sortedMap.put("b", "2");
        sortedMap.put("a", "1");
        // The iterator order must be sorted (a, b)
        Iterator<String> keyIt = sortedMap.keySet().iterator();
        assertEquals("a", keyIt.next());
        assertEquals("b", keyIt.next());
    }

    /* --------------------------  Helper methods verification  -------------------------- */

    @Test
    public void testLogicalValueTypeReporting() {
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.logicalValueType());

        map.put("k", "v"); // SINGLE (OBJECT)
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.logicalValueType());

        map.put("k2", "v2"); // ARRAY
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.logicalValueType());

        map.put("k3", "v3");
        map.put("k4", "v4"); // MAP
        assertEquals(CompactMap.LogicalValueType.MAP, map.logicalValueType());
    }

    @Test
    public void testSingleKeyAndValueAccessors() {
        map.put("alpha", "A");
        assertEquals("alpha", map.singleKey());
        assertEquals("A", map.singleValue());

        map.put("beta", "B"); // now ARRAY, singleKey/Value should still reflect original entry
        assertEquals("alpha", map.singleKey());
        assertEquals("A", map.singleValue());
    }
}
